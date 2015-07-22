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
import javax.tools.JavaFileManager;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    String fqAutoValueClass = context.autoValueClass().getQualifiedName().toString();
    Map<String, TypeName> types = convertPropertiesToTypes(context.properties());

    ClassName classNameClass = ClassName.get(context.packageName(), className);
    ClassName autoValueClass = ClassName.bestGuess(fqAutoValueClass);

    TypeSpec typeAdapter = createTypeAdapter(className, fqAutoValueClass, types);
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

  private Map<String, TypeName> convertPropertiesToTypes(Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<String, TypeName>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      types.put(entry.getKey(), TypeName.get(entry.getValue().getReturnType()));
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

  public TypeSpec createTypeAdapter(String className, String autoValueClassName, Map<String, TypeName> properties) {
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

  public MethodSpec createWriteMethod(FieldSpec gsonField, String autoValueClassName, Map<String, TypeName> properties) {
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
    for (Map.Entry<String, TypeName> property : properties.entrySet()) {
      writeMethod.addStatement("$N.getAdapter($T.class).write($N, $N.$N())", gsonField,
          property.getValue(), jsonWriter, annotatedParam, property.getKey());
    }
    writeMethod.addStatement("$N.endObject()", jsonWriter);

    return writeMethod.build();
  }

  public MethodSpec createReadMethod(FieldSpec gsonField, String className,
                                     String autoValueClassName, Map<String, TypeName> properties) {
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
    List<PropertyHolder> props = new ArrayList<PropertyHolder>(properties.size());
    for (Map.Entry<String, TypeName> prop : properties.entrySet()) {
      FieldSpec field = FieldSpec.builder(prop.getValue(), prop.getKey()).build();
      props.add(new PropertyHolder(prop.getKey(), prop.getValue(), field));

      readMethod.addStatement("$T $N = null", field.type, field);
    }

    readMethod.beginControlFlow("while ($N.hasNext())", jsonReader);

    FieldSpec name = FieldSpec.builder(String.class, "_name").build();
    readMethod.addStatement("$T $N = $N.nextName()", name.type, name, jsonReader);

    boolean first = true;
    for (PropertyHolder prop : props) {
      if (first) readMethod.beginControlFlow("if ($S.equals($N))", prop.name, name);
      else readMethod.nextControlFlow("else if ($S.equals($N))", prop.name, name);
      readMethod.addStatement("$N = $N.getAdapter($T.class).read($N)", prop.field, gsonField, prop.field.type, jsonReader);
      first = false;
    }
    readMethod.endControlFlow(); // if/else if
    readMethod.endControlFlow(); // while

    readMethod.addStatement("$N.endObject()", jsonReader);

    StringBuilder format = new StringBuilder("return new ");
    format.append(className.replaceAll("\\$", ""));
    format.append("(");
    List<FieldSpec> args = new ArrayList<FieldSpec>(props.size());
    for (int i = 0, n = props.size(); i < n; i++) {
      PropertyHolder prop = props.get(i);
      args.add(prop.field);
      format.append("$N");
      if (i < n - 1) format.append(", ");
    }
    format.append(")");
    readMethod.addStatement(format.toString(), args.toArray());

    return readMethod.build();
  }

  private static class PropertyHolder {
    String name;
    TypeName type;
    FieldSpec field;

    public PropertyHolder(String name, TypeName type, FieldSpec field) {
      this.name = name;
      this.type = type;
      this.field = field;
    }
  }
}
