package com.ryanharter.auto.value.gson;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.Defaults;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.Primitives;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.*;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueGsonExtension extends AutoValueExtension {

  public static class Property {
    final String methodName;
    final String humanName;
    final ExecutableElement element;
    final TypeName type;
    final ImmutableSet<String> annotations;

    public Property(String humanName, ExecutableElement element) {
      this.methodName = element.getSimpleName().toString();
      this.humanName = humanName;
      this.element = element;

      type = TypeName.get(element.getReturnType());
      annotations = buildAnnotations(element);
    }

    public String serializedName() {
      SerializedName serializedName = element.getAnnotation(SerializedName.class);
      if (serializedName != null) {
        return serializedName.value();
      } else {
        return humanName;
      }
    }

    public Boolean nullable() {
      return annotations.contains("Nullable");
    }

    private ImmutableSet<String> buildAnnotations(ExecutableElement element) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();

      List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
      for (AnnotationMirror annotation : annotations) {
        builder.add(annotation.getAnnotationType().asElement().getSimpleName().toString());
      }

      return builder.build();
    }
  }

  @Override
  public boolean applicable(Context context) {
    // check that the class contains a public static method returning a TypeAdapter
    TypeElement type = context.autoValueClass();
    TypeName typeName = TypeName.get(type.asType());
    ParameterizedTypeName typeAdapterType = ParameterizedTypeName.get(
        ClassName.get(TypeAdapter.class), typeName);
    TypeName returnedTypeAdapter = null;
    for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
      if (method.getModifiers().contains(Modifier.STATIC)
          && method.getModifiers().contains(Modifier.PUBLIC)) {
        TypeMirror rType = method.getReturnType();
        TypeName returnType = TypeName.get(rType);
        if (returnType.equals(typeAdapterType)) {
          return true;
        }

        if (returnType.equals(typeAdapterType.rawType)
          || (returnType instanceof ParameterizedTypeName
            && ((ParameterizedTypeName) returnType).rawType.equals(typeAdapterType.rawType))) {

          // we have a generic type and the type adapter types match
          if (typeName instanceof ParameterizedTypeName && ((ParameterizedTypeName)typeName).typeArguments.size() > 0) {
            return true;
          }

          returnedTypeAdapter = returnType;
        }
      }
    }

    if (returnedTypeAdapter == null) {
      return false;
    }

    // emit a warning if the user added a method returning a TypeAdapter, but not of the right type
    Messager messager = context.processingEnvironment().getMessager();
    if (returnedTypeAdapter instanceof ParameterizedTypeName) {
      ParameterizedTypeName paramReturnType = (ParameterizedTypeName) returnedTypeAdapter;
      TypeName argument = paramReturnType.typeArguments.get(0);
      messager.printMessage(Diagnostic.Kind.WARNING,
          String.format("Found public static method returning TypeAdapter<%s> on %s class. "
              + "Skipping GsonTypeAdapter generation.", argument, type));
    } else {
      messager.printMessage(Diagnostic.Kind.WARNING, "Found public static method returning "
          + "TypeAdapter with no type arguments, skipping GsonTypeAdapter generation.");
    }

    return false;
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
    List<Property> properties = readProperties(context.properties());

    Map<String, TypeName> types = convertPropertiesToTypes(context.properties());

    ClassName classNameClass = ClassName.get(context.packageName(), className);

    TypeName autoValueClass = ClassName.get(context.autoValueClass());
    boolean hasGenericType = false;
    TypeName classToExtendTypeName = TypeVariableName.get(classToExtend);
    List<? extends TypeParameterElement> typeParameters = context.autoValueClass().getTypeParameters();
    TypeVariableName[] typeNames = new TypeVariableName[typeParameters.size()];
    if (!typeParameters.isEmpty()) {
      for(int i = 0; i < typeParameters.size(); i++) {
        typeNames[i] = (TypeVariableName) TypeName.get(typeParameters.get(i).asType());
      }

      classToExtendTypeName = ParameterizedTypeName.get(ClassName.get(context.packageName(), classToExtend), typeNames);
      autoValueClass = ParameterizedTypeName.get(context.autoValueClass().asType());
      hasGenericType = true;
    }

    TypeSpec typeAdapter = createTypeAdapter(classNameClass, autoValueClass, properties, hasGenericType);

    TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
        .superclass(classToExtendTypeName)
        .addType(typeAdapter)
        .addMethod(generateConstructor(types));

    if (isFinal) {
      subclass.addModifiers(FINAL);
    } else {
      subclass.addModifiers(ABSTRACT);
    }

    if (hasGenericType) {
      subclass.addTypeVariables(Arrays.asList(typeNames));
    }

    return JavaFile.builder(context.packageName(), subclass.build()).build().toString();
  }

  public List<Property> readProperties(Map<String, ExecutableElement> properties) {
    List<Property> values = new LinkedList<Property>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      values.add(new Property(entry.getKey(), entry.getValue()));
    }
    return values;
  }

  ImmutableMap<Property, FieldSpec> createFields(List<Property> properties) {
    ImmutableMap.Builder<Property, FieldSpec> fields = ImmutableMap.builder();

    ClassName jsonAdapter = ClassName.get(TypeAdapter.class);
    for (Property property : properties) {

      ParameterizedTypeName adp;
      if (property.type instanceof TypeVariableName) {
        // generic type
        adp = ParameterizedTypeName.get(jsonAdapter, WildcardTypeName.supertypeOf(ClassName.OBJECT));
      } else {
        TypeName type = property.type.isPrimitive() ? property.type.box() : property.type;
        adp = ParameterizedTypeName.get(jsonAdapter, type);
      }

      fields.put(property,
              FieldSpec.builder(adp, property.humanName + "Adapter", PRIVATE, FINAL).build());
    }

    return fields.build();
  }

  MethodSpec generateConstructor(Map<String, TypeName> properties) {
    List<ParameterSpec> params = Lists.newArrayList();
    for (Map.Entry<String, TypeName> entry : properties.entrySet()) {
      params.add(ParameterSpec.builder(entry.getValue(), entry.getKey()).build());
    }

    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    for (int i = properties.size(); i > 0; i--) {
      superFormat.append("$N");
      if (i > 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), properties.keySet().toArray());

    return builder.build();
  }

  /**
   * Converts the ExecutableElement properties to TypeName properties
   */
  Map<String, TypeName> convertPropertiesToTypes(Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<String, TypeName>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      ExecutableElement el = entry.getValue();
      types.put(entry.getKey(), TypeName.get(el.getReturnType()));
    }
    return types;
  }

  public TypeSpec createTypeAdapter(ClassName className, TypeName autoValueTypeName, List<Property> properties,
                                    boolean hasGeneric) {
    ClassName autoValueClassName;
    if (hasGeneric) {
      autoValueClassName = ((ParameterizedTypeName) autoValueTypeName).rawType;
    } else {
      autoValueClassName = (ClassName) autoValueTypeName;
    }

    ClassName typeAdapterClass = ClassName.get(TypeAdapter.class);
    ParameterizedTypeName superClass = ParameterizedTypeName.get(typeAdapterClass, autoValueClassName);

    ImmutableMap<Property, FieldSpec> adapters = createFields(properties);

    ParameterSpec gsonParam = ParameterSpec.builder(Gson.class, "gson").build();
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(gsonParam);

    if (hasGeneric) {
      ParameterizedTypeName typeToken = ParameterizedTypeName.get(ClassName.get(TypeToken.class),
              WildcardTypeName.subtypeOf(autoValueClassName));
      ParameterSpec typeTokenParam = ParameterSpec.builder(typeToken, "typeToken").build();
      constructor.addParameter(typeTokenParam);
    }

    List<Map.Entry<Property, FieldSpec>> generics = new ArrayList<>();
    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();
      TypeName entryType = entry.getKey().type;

      if (entryType instanceof ParameterizedTypeName) {
        constructor.addStatement("this.$N = $N.getAdapter($L)", field, gsonParam,
            makeType((ParameterizedTypeName) prop.type));
      } else if (entryType instanceof TypeVariableName) {
        generics.add(entry);
      } else {
        TypeName type = prop.type.isPrimitive() ? prop.type.box() : prop.type;
        constructor.addStatement("this.$N = $N.getAdapter($T.class)", field, gsonParam, type);
      }
    }

    if (hasGeneric) {
      constructor.addStatement("$T type = typeToken.getType()", Type.class);
      constructor.beginControlFlow("if (type instanceof ParameterizedType)");
      constructor.addStatement("$T parameterizedType = (ParameterizedType) type", ParameterizedType.class);
      constructor.addStatement("Type[] typeArgs = parameterizedType.getActualTypeArguments()");

      int count = 0;
      for (Map.Entry<Property, FieldSpec> generic : generics) {
        constructor.addStatement("TypeToken token$L = TypeToken.get(typeArgs[$L])", count, count);
        constructor.addStatement("this.$N = $N.getAdapter(token$L)", generic.getValue(), gsonParam, count++);
      }

      constructor.nextControlFlow("else");

      for (Map.Entry<Property, FieldSpec> generic : generics) {
        constructor.addStatement("this.$N = $N.getAdapter(Object.class)", generic.getValue(), gsonParam);
      }

      constructor.endControlFlow();
    }

    TypeSpec.Builder classBuilder = TypeSpec.classBuilder("GsonTypeAdapter")
        .addModifiers(PUBLIC, STATIC, FINAL)
        .superclass(superClass)
        .addFields(adapters.values())
        .addMethod(constructor.build())
        .addMethod(createWriteMethod(autoValueClassName, adapters))
        .addMethod(createReadMethod(className, autoValueClassName, adapters));


    return classBuilder.build();
  }

  public MethodSpec createWriteMethod(TypeName autoValueClassName,
                                      ImmutableMap<Property, FieldSpec> adapters) {
    ParameterSpec jsonWriter = ParameterSpec.builder(JsonWriter.class, "jsonWriter").build();
    ParameterSpec annotatedParam = ParameterSpec.builder(autoValueClassName, "object").build();
    MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("write")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addParameter(jsonWriter)
        .addParameter(annotatedParam)
        .addException(IOException.class);

    writeMethod.addStatement("$N.beginObject()", jsonWriter);
    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();

      if (prop.nullable()) {
        writeMethod.beginControlFlow("if ($N.$N() != null)", annotatedParam, prop.methodName);
        writeMethod.addStatement("$N.name($S)", jsonWriter, prop.serializedName());
        writeMethod.addStatement("$N.write($N, $N.$N())", field, jsonWriter, annotatedParam, prop.methodName);
        writeMethod.endControlFlow();
      } else {
        writeMethod.addStatement("$N.name($S)", jsonWriter, prop.serializedName());
        writeMethod.addStatement("$N.write($N, $N.$N())", field, jsonWriter, annotatedParam, prop.methodName);
      }
    }
    writeMethod.addStatement("$N.endObject()", jsonWriter);

    return writeMethod.build();
  }

  public MethodSpec createReadMethod(ClassName className,
                                     TypeName autoValueClassName,
                                     ImmutableMap<Property, FieldSpec> adapters) {
    ParameterSpec jsonReader = ParameterSpec.builder(JsonReader.class, "jsonReader").build();
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("read")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(autoValueClassName)
        .addParameter(jsonReader)
        .addException(IOException.class);

    ClassName token = ClassName.get(JsonToken.NULL.getClass());

    readMethod.addStatement("$N.beginObject()", jsonReader);

    // add the properties
    Map<Property, FieldSpec> fields = new LinkedHashMap<Property, FieldSpec>(adapters.size());
    for (Property prop : adapters.keySet()) {
      FieldSpec field = FieldSpec.builder(prop.type, prop.humanName).build();
      fields.put(prop, field);
      if (field.type.isPrimitive()) {
        String defaultValue = getDefaultPrimitiveValue(field.type);
        if (defaultValue != null) {
          readMethod.addStatement("$T $N = $L", field.type, field, defaultValue);
        } else {
          readMethod.addStatement("$T $N = $T.valueOf(null)", field.type, field, field.type.box());
        }
      } else {
        if (field.type instanceof TypeVariableName) {
          readMethod.addStatement("Object $N = null", field);
        } else {
          readMethod.addStatement("$T $N = null", field.type, field);
        }
      }
    }

    readMethod.beginControlFlow("while ($N.hasNext())", jsonReader);

    FieldSpec name = FieldSpec.builder(String.class, "_name").build();
    readMethod.addStatement("$T $N = $N.nextName()", name.type, name, jsonReader);

    // check if JSON field value is NULL
    readMethod.beginControlFlow("if ($N.peek() == $T.NULL)", jsonReader, token);
    readMethod.addStatement("$N.skipValue()", jsonReader);
    readMethod.addStatement("continue");
    readMethod.endControlFlow();

    readMethod.beginControlFlow("switch ($N)", name);
    for (Map.Entry<Property, FieldSpec> entry : fields.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();

      readMethod.beginControlFlow("case $S:", prop.serializedName());
      readMethod.addStatement("$N = $N.read($N)", field, adapters.get(prop), jsonReader);
      readMethod.addStatement("break");
      readMethod.endControlFlow();
    }

    // skip value if field is not serialized...
    readMethod.beginControlFlow("default:");
    readMethod.addStatement("$N.skipValue()", jsonReader);
    readMethod.endControlFlow();

    readMethod.endControlFlow(); // switch
    readMethod.endControlFlow(); // while

    readMethod.addStatement("$N.endObject()", jsonReader);

    StringBuilder format = new StringBuilder("return new ");
    format.append(className.simpleName().replaceAll("\\$", ""));
    format.append("(");
    Iterator<FieldSpec> iterator = fields.values().iterator();
    while (iterator.hasNext()) {
      iterator.next();
      format.append("$N");
      if (iterator.hasNext()) format.append(", ");
    }
    format.append(")");
    readMethod.addStatement(format.toString(), fields.values().toArray());

    return readMethod.build();
  }

  /**
   *
   * @param type
   * @return the default primitive value as a String.  Returns null if unable to determine default value
     */
  private String getDefaultPrimitiveValue(TypeName type) {
    String valueString = null;
    try {
      Class<?> primitiveClass = Primitives.unwrap(Class.forName(type.box().toString()));
      if (primitiveClass != null) {
        Object defaultValue = Defaults.defaultValue(primitiveClass);
        if (defaultValue != null) {
          valueString = defaultValue.toString();
          if (!Strings.isNullOrEmpty(valueString)) {
            switch (type.toString()) {
              case "double":
                valueString = valueString + "d";
                break;
              case "float":
                valueString = valueString + "f";
                break;
              case "long":
                valueString = valueString + "L";
                break;
              case "char":
                valueString = "'" + valueString + "'";
                break;
            }
          }
        }
      }
    } catch (ClassNotFoundException ignored) {
      //Swallow and return null
    }

    return valueString;
  }

  private CodeBlock makeType(ParameterizedTypeName type) {
    CodeBlock.Builder block = CodeBlock.builder();
    block.add("new $T<$T>(){}", TypeToken.class, type);
    return block.build();
  }
}
