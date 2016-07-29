package com.ryanharter.auto.value.gson;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.Defaults;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.primitives.Primitives;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
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

    public String[] serializedNameAlternate() {
      SerializedName serializedName = element.getAnnotation(SerializedName.class);
      if (serializedName != null) {
        return serializedName.alternate();
      } else {
        return new String[0];
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

      // If the original type uses generics, user's don't have to nest the generic type args
      if (typeName instanceof ParameterizedTypeName) {
        ParameterizedTypeName pTypeName = (ParameterizedTypeName) typeName;
        if (pTypeName.rawType.equals(argument)) {
          return true;
        }
      } else {
        messager.printMessage(Diagnostic.Kind.WARNING,
            String.format("Found public static method returning TypeAdapter<%s> on %s class. "
                + "Skipping GsonTypeAdapter generation.", argument, type));
      }
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
    ClassName autoValueClass = ClassName.get(context.autoValueClass());

    List<? extends TypeParameterElement> typeParams = context.autoValueClass().getTypeParameters();
    List<TypeVariableName> params = new ArrayList<>(typeParams.size());
    TypeName superclasstype = ClassName.get(context.packageName(), classToExtend);
    if (!typeParams.isEmpty()) {
      for (TypeParameterElement typeParam : typeParams) {
        params.add(TypeVariableName.get(typeParam));
      }
      superclasstype = ParameterizedTypeName.get(ClassName.get(context.packageName(), classToExtend), params.toArray(new TypeName[params.size()]));
    }

    TypeSpec typeAdapter = createTypeAdapter(classNameClass, autoValueClass, properties, params);

    TypeSpec.Builder subclass = TypeSpec.classBuilder(classNameClass)
        .superclass(superclasstype)
        .addType(typeAdapter)
        .addMethod(generateConstructor(types));

    if (!typeParams.isEmpty()) {
      subclass.addTypeVariables(params);
    }

    if (isFinal) {
      subclass.addModifiers(FINAL);
    } else {
      subclass.addModifiers(ABSTRACT);
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
      TypeName type = property.type.isPrimitive() ? property.type.box() : property.type;
      if (type instanceof TypeVariableName) {
        type = TypeName.get(Object.class);
      }
      ParameterizedTypeName adp = ParameterizedTypeName.get(jsonAdapter, type);
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

  public TypeSpec createTypeAdapter(ClassName className, ClassName autoValueClassName, List<Property> properties, List<TypeVariableName> typeParams) {
    ClassName typeAdapterClass = ClassName.get(TypeAdapter.class);
    ParameterizedTypeName superClass = ParameterizedTypeName.get(typeAdapterClass, autoValueClassName);

    ImmutableMap<Property, FieldSpec> adapters = createFields(properties);

    ParameterSpec gsonParam = ParameterSpec.builder(Gson.class, "gson").build();
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(gsonParam);

    if (!typeParams.isEmpty()) {
      ImmutableMap<Property, FieldSpec> parameterizedAdapters = filterParameterizedAdapters(adapters, true);

      ParameterSpec typeAdapter = ParameterSpec
          .builder(ParameterizedTypeName.get(ClassName.get(TypeToken.class), WildcardTypeName.subtypeOf(autoValueClassName)), "typeToken")
          .build();
      constructor.addParameter(typeAdapter);

      constructor.addStatement("$T type = $N.getType()", Type.class, typeAdapter);
      constructor.beginControlFlow("if (type instanceof $T)", ParameterizedType.class);
      constructor.addStatement("$1T pType = ($1T) type", ParameterizedType.class);
      constructor.addStatement("$T[] typeArgs = pType.getActualTypeArguments()", Type.class);
      for (Map.Entry<Property, FieldSpec> entry : parameterizedAdapters.entrySet()) {
        Property prop = entry.getKey();
        FieldSpec field = entry.getValue();
        constructor.addStatement("this.$N = ($T) $N.getAdapter($T.get(typeArgs[$L]))", field,
            field.type, gsonParam, TypeToken.class, typeParams.indexOf(prop.type));
      }
      constructor.nextControlFlow("else");
      for (Map.Entry<Property, FieldSpec> entry : parameterizedAdapters.entrySet()) {
        Property prop = entry.getKey();
        FieldSpec field = entry.getValue();
        constructor.addStatement("this.$N = $N.getAdapter($T.get(Object.class))", field, gsonParam,
            TypeToken.class);
      }
      constructor.endControlFlow();
    }

    ImmutableMap<Property, FieldSpec> nonparameterizedAdapters = filterParameterizedAdapters(adapters, false);
    for (Map.Entry<Property, FieldSpec> entry : nonparameterizedAdapters.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();
      if (entry.getKey().type instanceof ParameterizedTypeName) {
        constructor.addStatement("this.$N = $N.getAdapter($L)", field, gsonParam,
            makeType((ParameterizedTypeName) prop.type));
      } else {
        TypeName type = prop.type.isPrimitive() ? prop.type.box() : prop.type;
        constructor.addStatement("this.$N = $N.getAdapter($T.class)", field, gsonParam, type);
      }
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

  private ImmutableMap<Property, FieldSpec> filterParameterizedAdapters(Map<Property, FieldSpec> adapters, boolean parameterized) {
    ImmutableMap.Builder<Property, FieldSpec> out = new ImmutableMap.Builder<>();
    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
      if ((entry.getKey().type instanceof TypeVariableName) == parameterized) {
        out.put(entry);
      }
    }
    return out.build();
  }

  public MethodSpec createWriteMethod(ClassName autoValueClassName,
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
                                     ClassName autoValueClassName,
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
      TypeName fieldType = prop.type;
      if (prop.type instanceof TypeVariableName) {
        fieldType = WildcardTypeName.get(Object.class);
      }
      FieldSpec field = FieldSpec.builder(fieldType, prop.humanName).build();
      fields.put(prop, field);
      if (field.type.isPrimitive()) {
        String defaultValue = getDefaultPrimitiveValue(field.type);
        if (defaultValue != null) {
          readMethod.addStatement("$T $N = $L", field.type, field, defaultValue);
        } else {
          readMethod.addStatement("$T $N = $T.valueOf(null)", field.type, field, field.type.box());
        }
      } else {
        readMethod.addStatement("$T $N = null", field.type, field);
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

      for (String alternate : prop.serializedNameAlternate()) {
        readMethod.addCode("case $S:\n", alternate);
      }
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
