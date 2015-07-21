package com.ryanharter.autogson;

import autovalue.shaded.com.google.common.common.collect.Lists;
import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValueExtension;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import org.omg.CORBA.PUBLIC_MEMBER;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileManager;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
        .superclass(TypeVariableName.get(classToExtend))
        .addField(createSerializer(className, fqAutoValueClass, types))
        .addField(createDeserializer(className, fqAutoValueClass, types))
        .addMethod(generateConstructor(types));

    if (isFinal) {
      subclass.addModifiers(Modifier.FINAL);
    } else {
      subclass.addModifiers(Modifier.ABSTRACT);
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

  FieldSpec createSerializer(String className, String classToExtend, Map<String, TypeName> properties) {
    ClassName annotatedClass = ClassName.bestGuess(classToExtend);
    String annotatedParamName = annotatedClass.simpleName().toLowerCase();
    ClassName serializerType = ClassName.get(JsonSerializer.class);
    ParameterizedTypeName jsonSerializer = ParameterizedTypeName.get(serializerType, annotatedClass);
    TypeSpec.Builder serializerImpl = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(jsonSerializer);

    ParameterSpec annotated = ParameterSpec.builder(annotatedClass, annotatedParamName).build();
    ParameterSpec type = ParameterSpec.builder(Type.class, "type").build();
    ParameterSpec context = ParameterSpec.builder(JsonSerializationContext.class, "context").build();
    MethodSpec.Builder serializerMethod = MethodSpec.methodBuilder("serialize")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(JsonElement.class)
        .addParameter(annotated)
        .addParameter(type)
        .addParameter(context)
        .addStatement("final $T jsonObject = new $T()", JsonObject.class, JsonObject.class);

    for (String property : properties.keySet()) {
      serializerMethod.addStatement("jsonObject.add($S, context.serialize($N.$N()))", property, annotated.name, property);
    }

    serializerMethod.addStatement("return jsonObject");

    serializerImpl.addMethod(serializerMethod.build());

    return FieldSpec
        .builder(jsonSerializer, "SERIALIZER", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer(serializerImpl.build().toString())
        .build();
  }

  FieldSpec createDeserializer(String className, String classToExtend, Map<String, TypeName> properties) {
    ClassName annotatedClass = ClassName.bestGuess(classToExtend);
    String annotatedParamName = annotatedClass.simpleName().toLowerCase();
    ClassName serializerType = ClassName.get(JsonDeserializer.class);
    ParameterizedTypeName jsonDeserializer = ParameterizedTypeName.get(serializerType, annotatedClass);
    TypeSpec.Builder deserializerImpl = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(jsonDeserializer);

    ParameterSpec jsonElement = ParameterSpec.builder(JsonElement.class, "jsonElement").build();
    ParameterSpec type = ParameterSpec.builder(Type.class, "type").build();
    ParameterSpec context = ParameterSpec.builder(JsonDeserializationContext.class, "context").build();
    MethodSpec.Builder deserializerMethod = MethodSpec.methodBuilder("deserialize")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(annotatedClass)
        .addParameter(jsonElement)
        .addParameter(type)
        .addParameter(context)
        .addException(JsonParseException.class)
        .beginControlFlow("if ($N.isJsonNull())", jsonElement)
        .addStatement("return null")
        .endControlFlow();

    FieldSpec object = FieldSpec.builder(JsonObject.class, "object").build();
    deserializerMethod.addStatement("$T $N = $N.getAsJsonObject()", object.type, object, jsonElement);

    StringBuilder thisFormat = new StringBuilder("return new ");
    thisFormat.append(className.replaceAll("\\$", ""));
    thisFormat.append("(");
    List<String> args = new ArrayList<String>(properties.size());
    int i = properties.size();
    for (Map.Entry<String, TypeName> entry : properties.entrySet()) {
      String name = entry.getKey();
      TypeName prop = entry.getValue();
      deserializerMethod.addStatement("$T $N = $N.deserialize($N.get($S), $T.class)", prop, name, context, object, name, prop);

      args.add(name);
      thisFormat.append("$N");
      if (i > 1) thisFormat.append(", ");
      i--;
    }
    thisFormat.append(")");
    deserializerMethod.addStatement(thisFormat.toString(), args.toArray());

    deserializerImpl.addMethod(deserializerMethod.build());

    return FieldSpec
        .builder(jsonDeserializer, "DESERIALIZER", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer(deserializerImpl.build().toString())
        .build();
  }

  private Map<String, TypeName> convertPropertiesToTypes(Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<String, TypeName>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      types.put(entry.getKey(), TypeName.get(entry.getValue().getReturnType()));
    }
    return types;
  }
}
