package com.ryanharter.autogson;

import autovalue.shaded.com.google.common.common.collect.Lists;
import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValueExtension;
import com.google.auto.value.processor.AutoAnnotationProcessor;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import jdk.nashorn.internal.ir.debug.JSONWriter;
import org.omg.CORBA.PUBLIC_MEMBER;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileManager;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.ABSTRACT;

/**
 * Created by rharter on 7/20/15.
 */
@AutoService(AutoValueExtension.class)
public class AutoGsonExtension implements AutoValueExtension {

  public static class Property {
    String name;
    ExecutableElement element;
    TypeName type;

    public Property() {}

    public Property(String name, ExecutableElement element) {
      this.name = name;
      this.element = element;

      type = TypeName.get(element.getReturnType());
    }

    public String serializedName() {
      SerializedName serializedName = element.getAnnotation(SerializedName.class);
      if (serializedName != null) {
        return serializedName.value();
      } else {
        return name;
      }
    }
  }

  @Override
  public boolean applicable(Context context) {
    return true;
  }

  @Override
  public boolean mustBeAtEnd(Context context) {
    return false;
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
    List<Property> properties = readProperties(context.properties());

    String fqAutoValueClass = context.autoValueClass().getQualifiedName().toString();
    Map<String, TypeName> types = convertPropertiesToTypes(context.properties());

    ClassName classNameClass = ClassName.get(context.packageName(), className);
    ClassName autoValueClass = ClassName.bestGuess(context.autoValueClass().getQualifiedName().toString());

    TypeSpec typeAdapter = createTypeAdapter(className, fqAutoValueClass, properties);
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
      SerializedName serializedName = el.getAnnotation(SerializedName.class);
      if (serializedName != null) {
        types.put(serializedName.value(), TypeName.get(el.getReturnType()));
      } else {
        types.put(entry.getKey(), TypeName.get(el.getReturnType()));
      }
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

  public TypeSpec createTypeAdapter(String className, String autoValueClassName, List<Property> properties) {
    ClassName annotatedClass = ClassName.bestGuess(autoValueClassName);
    ClassName typeAdapterClass = ClassName.get(TypeAdapter.class);
    ParameterizedTypeName superClass = ParameterizedTypeName.get(typeAdapterClass, annotatedClass);

    FieldSpec gsonField = FieldSpec.builder(Gson.class, "gson").build();

    String customTypeAdapterClass = String.format("%sTypeAdapter", annotatedClass.simpleName());
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(customTypeAdapterClass)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .superclass(superClass)
        .addField(gsonField)
        .addMethod(MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(Gson.class, "gson")
                .addStatement("this.$N = gson", gsonField)
                .build()
        )
        .addMethod(createWriteMethod(gsonField, autoValueClassName, properties))
        .addMethod(createReadMethod(gsonField, className, autoValueClassName, properties));


    return classBuilder.build();
  }

  public MethodSpec createWriteMethod(FieldSpec gsonField, String autoValueClassName, List<Property> properties) {
    ParameterSpec jsonWriter = ParameterSpec.builder(JsonWriter.class, "jsonWriter").build();
    ClassName annotatedClass = ClassName.bestGuess(autoValueClassName);
    ParameterSpec annotatedParam = ParameterSpec.builder(annotatedClass, "object").build();
    MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("write")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addParameter(jsonWriter)
        .addParameter(annotatedParam)
        .addException(IOException.class);

    writeMethod.addStatement("$N.beginObject()", jsonWriter);
    for (Property prop : properties) {
      writeMethod.addStatement("$N.name($S)", jsonWriter, prop.serializedName());
      writeMethod.addStatement("$N.getAdapter($T.class).write($N, $N.$N())", gsonField,
          prop.type.isPrimitive() ? prop.type.box() : prop.type, jsonWriter, annotatedParam, prop.name);
    }
    writeMethod.addStatement("$N.endObject()", jsonWriter);

    return writeMethod.build();
  }

  public MethodSpec createReadMethod(FieldSpec gsonField, String className,
                                     String autoValueClassName, List<Property> properties) {
    ParameterSpec jsonReader = ParameterSpec.builder(JsonReader.class, "jsonReader").build();
    ClassName annotatedClass = ClassName.bestGuess(autoValueClassName);
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("read")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(annotatedClass)
        .addParameter(jsonReader)
        .addException(IOException.class);

    readMethod.addStatement("$N.beginObject()", jsonReader);

    // add the properties
    Map<Property, FieldSpec> fields = new LinkedHashMap<Property, FieldSpec>(properties.size());
    for (Property prop : properties) {
      FieldSpec field = FieldSpec.builder(prop.type, prop.name).build();
      fields.put(prop, field);

      readMethod.addStatement("$T $N = null", field.type.isPrimitive() ? field.type.box() : field.type, field);
    }

    readMethod.beginControlFlow("while ($N.hasNext())", jsonReader);

    FieldSpec name = FieldSpec.builder(String.class, "_name").build();
    readMethod.addStatement("$T $N = $N.nextName()", name.type, name, jsonReader);

    boolean first = true;
    for (Map.Entry<Property, FieldSpec> entry : fields.entrySet()) {
      Property prop = entry.getKey();
      FieldSpec field = entry.getValue();
      if (first) readMethod.beginControlFlow("if ($S.equals($N))", prop.serializedName(), name);
      else readMethod.nextControlFlow("else if ($S.equals($N))", prop.serializedName(), name);
      readMethod.addStatement("$N = $N.getAdapter($T.class).read($N)", field, gsonField,
          field.type.isPrimitive() ? field.type.box() : field.type, jsonReader);
      first = false;
    }
    readMethod.endControlFlow(); // if/else if
    readMethod.endControlFlow(); // while

    readMethod.addStatement("$N.endObject()", jsonReader);

    StringBuilder format = new StringBuilder("return new ");
    format.append(className.replaceAll("\\$", ""));
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
}
