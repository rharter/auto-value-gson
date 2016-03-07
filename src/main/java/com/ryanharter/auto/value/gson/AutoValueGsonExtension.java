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
import com.google.gson.TypeAdapterFactory;
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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueGsonExtension extends AutoValueExtension {

  public static class Property {
    String name;
    ExecutableElement element;
    TypeName type;
    ImmutableSet<String> annotations;

    public Property() {
    }

    public Property(String name, ExecutableElement element) {
      this.name = name;
      this.element = element;

      type = TypeName.get(element.getReturnType());
      annotations = buildAnnotations(element);
    }

    public String serializedName() {
      SerializedName serializedName = element.getAnnotation(SerializedName.class);
      if (serializedName != null) {
        return serializedName.value();
      } else {
        return name;
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
    return true;
  }
  
  @Override
  public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
    List<Property> properties = readProperties(context.properties());

    Map<String, TypeName> types = convertPropertiesToTypes(context.properties());

    ClassName classNameClass = ClassName.get(context.packageName(), className);
    ClassName autoValueClass = ClassName.bestGuess(context.autoValueClass().getQualifiedName().toString());

    TypeSpec typeAdapter = createTypeAdapter(classNameClass, autoValueClass, properties);
    TypeSpec typeAdapterFactory = createTypeAdapterFactory(classNameClass, autoValueClass, typeAdapter, types);
    MethodSpec typeAdapterFactoryMethod = createTypeAdapterFactoryMethod(typeAdapterFactory);

    TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
        .superclass(TypeVariableName.get(classToExtend))
        .addType(typeAdapterFactory)
        .addType(typeAdapter)
        .addMethod(generateConstructor(types))
        .addMethod(typeAdapterFactoryMethod);

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
      ParameterizedTypeName adp = ParameterizedTypeName.get(jsonAdapter, type);
      fields.put(property,
              FieldSpec.builder(adp, property.name + "Adapter", PRIVATE, FINAL).build());
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

  public MethodSpec createTypeAdapterFactoryMethod(TypeSpec typeAdapterFactory) {
    return MethodSpec.methodBuilder("typeAdapterFactory")
        .addModifiers(PUBLIC, STATIC)
        .returns(ClassName.bestGuess(typeAdapterFactory.name))
        .addStatement("return new $N()", typeAdapterFactory)
        .build();
  }

  public TypeSpec createTypeAdapterFactory(ClassName className, ClassName autoValueClassName, TypeSpec typeAdapter, Map<String, TypeName> properties) {
    String customTypeAdapterFactoryName = String.format("%sTypeAdapterFactory", autoValueClassName.simpleName());

    TypeVariableName t = TypeVariableName.get("T");
    TypeName typeAdapterType = ParameterizedTypeName.get(ClassName.get(TypeAdapter.class), t);
    TypeName typeTokenType = ParameterizedTypeName.get(ClassName.get(TypeToken.class), t);
    ParameterSpec gson = ParameterSpec.builder(Gson.class, "gson").build();
    ParameterSpec typeToken = ParameterSpec.builder(typeTokenType, "typeToken").build();
    MethodSpec createMethod = MethodSpec.methodBuilder("create")
        .addModifiers(PUBLIC)
        .addAnnotation(Override.class)
        .addTypeVariable(t)
        .returns(typeAdapterType)
        .addParameter(gson)
        .addParameter(typeToken)
        .addStatement("if (!$T.class.isAssignableFrom($N.getRawType())) return null", autoValueClassName, typeToken)
        .addStatement("return ($T) new $N($N)", typeAdapterType, typeAdapter, gson)
        .build();

    return TypeSpec.classBuilder(customTypeAdapterFactoryName)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addSuperinterface(TypeAdapterFactory.class)
        .addMethod(createMethod)
        .build();
  }

  public TypeSpec createTypeAdapter(ClassName className, ClassName autoValueClassName, List<Property> properties) {
    ClassName typeAdapterClass = ClassName.get(TypeAdapter.class);
    ParameterizedTypeName superClass = ParameterizedTypeName.get(typeAdapterClass, autoValueClassName);

    ImmutableMap<Property, FieldSpec> adapters = createFields(properties);

    ParameterSpec gsonParam = ParameterSpec.builder(Gson.class, "gson").build();
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(gsonParam);

    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
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

    String customTypeAdapterClass = String.format("%sTypeAdapter", autoValueClassName.simpleName());
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(customTypeAdapterClass)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .superclass(superClass)
        .addFields(adapters.values())
        .addMethod(constructor.build())
        .addMethod(createWriteMethod(autoValueClassName, adapters))
        .addMethod(createReadMethod(className, autoValueClassName, adapters));


    return classBuilder.build();
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
        writeMethod.beginControlFlow("if ($N.$N() != null)", annotatedParam, prop.name);
        writeMethod.addStatement("$N.name($S)", jsonWriter, prop.serializedName());
        writeMethod.addStatement("$N.write($N, $N.$N())", field, jsonWriter, annotatedParam, prop.name);
        writeMethod.endControlFlow();
      } else {
        writeMethod.addStatement("$N.name($S)", jsonWriter, prop.serializedName());
        writeMethod.addStatement("$N.write($N, $N.$N())", field, jsonWriter, annotatedParam, prop.name);
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
      FieldSpec field = FieldSpec.builder(prop.type, prop.name).build();
      fields.put(prop, field);
      if(field.type.isPrimitive()){
        String defaultValue = getDefaultPrimitiveValue(field.type);
        if(defaultValue != null) {
          readMethod.addStatement("$T $N = $L", field.type, field, defaultValue);
        }else {
          readMethod.addStatement("$T $N = $T.valueOf(null)", field.type, field, field.type.box());
        }

      }else {
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

  private String getDefaultPrimitiveValue(TypeName type) {
    String valueString = null;
    try {
      Class<?> primitiveClass = Primitives.unwrap(Class.forName(type.box().toString()));
      if(primitiveClass != null) {
        Object defaultValue = Defaults.defaultValue(primitiveClass);
        if(defaultValue != null) {
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
            }
          }
        }
      }

    } catch (Exception e) {
    }
    return valueString;
  }

  private CodeBlock makeType(ParameterizedTypeName type) {
    CodeBlock.Builder block = CodeBlock.builder();
    block.add("new TypeToken<$T>(){}", type);
    return block.build();
  }
}
