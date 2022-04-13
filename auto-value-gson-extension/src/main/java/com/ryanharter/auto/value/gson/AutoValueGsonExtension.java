// modified by mapbox
package com.ryanharter.auto.value.gson;

import com.google.auto.common.GeneratedAnnotations;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.Defaults;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.ryanharter.auto.value.gson.internal.WildcardUtil;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import io.sweers.autotransient.AutoTransient;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.ryanharter.auto.value.gson.AutoValueGsonExtension.USE_FIELD_NAME_POLICY;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.Modifier.VOLATILE;
import static javax.lang.model.util.ElementFilter.methodsIn;

@SupportedOptions(USE_FIELD_NAME_POLICY)
@AutoService(AutoValueExtension.class)
public class AutoValueGsonExtension extends AutoValueExtension {

  public static final String GENERATED_COMMENTS = "https://github.com/rharter/auto-value-gson";
  static final String USE_FIELD_NAME_POLICY = "autovaluegson.useFieldNamePolicy";

  static class Property {

    @Nullable
    static Property create(
        Messager messager,
        String humanName,
        ExecutableElement element,
        TypeMirror actualType
    ) {
      Property property = new Property(humanName, element, actualType);
      if (property.isTransient() && !property.nullable()) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Required property cannot be transient!", element);
        return null;
      } else {
        return property;
      }
    }

    final String methodName;
    final String humanName;
    final ExecutableElement element;
    final TypeName type;
    final TypeName builderType;
    final ImmutableSet<AnnotationMirror> typeAnnotations;
    final ImmutableSet<AnnotationMirror> methodAnnotations;
    final boolean nullable;
    final boolean isTransient;

    private Property(String humanName, ExecutableElement element, TypeMirror actualType) {
      this.methodName = element.getSimpleName().toString();
      this.humanName = humanName;
      this.element = element;

      type = TypeName.get(actualType);
      builderType = TypeName.get(element.getReturnType());
      typeAnnotations = ImmutableSet.copyOf(element.getReturnType().getAnnotationMirrors());
      methodAnnotations = ImmutableSet.copyOf(element.getAnnotationMirrors());
      nullable = nullableTypeAnnotation().isPresent() || nullableMethodAnnotation().isPresent();
      isTransient = element.getAnnotation(AutoTransient.class) != null;
    }

    String serializedName() {
      SerializedName serializedName = element.getAnnotation(SerializedName.class);
      if (serializedName != null) {
        return serializedName.value();
      } else {
        return humanName;
      }
    }

    String[] serializedNameAlternate() {
      SerializedName serializedName = element.getAnnotation(SerializedName.class);
      if (serializedName != null) {
        return serializedName.alternate();
      } else {
        return new String[0];
      }
    }

    boolean hasSerializedNameAnnotation() {
      SerializedName serializedName = element.getAnnotation(SerializedName.class);
      return serializedName != null;
    }

    boolean isTransient() {
      return isTransient;
    }

    boolean nullable() {
      return nullable;
    }

    private Optional<AnnotationMirror> nullableIn(Set<AnnotationMirror> annotations) {
      return annotations.stream()
          .filter(
              annot ->
                  MoreElements.asType(annot.getAnnotationType().asElement())
                      .getSimpleName()
                      .contentEquals("Nullable"))
          .findFirst();
    }

    Optional<AnnotationMirror> nullableTypeAnnotation() {
      return nullableIn(typeAnnotations);
    }

    Optional<AnnotationMirror> nullableMethodAnnotation() {
      return nullableIn(methodAnnotations);
    }
  }

  private boolean useFieldNamePolicy = false;

  @Override
  public IncrementalExtensionType incrementalType(ProcessingEnvironment processingEnvironment) {
    return IncrementalExtensionType.ISOLATING;
  }

  @Override
  public boolean applicable(Context context) {
    useFieldNamePolicy = context.processingEnvironment()
        .getOptions()
        .containsKey(USE_FIELD_NAME_POLICY);
    return isApplicable(context.autoValueClass(), context.processingEnvironment().getMessager());
  }

  public static boolean isApplicable(TypeElement type, Messager messager) {
    //noinspection UnstableApiUsage
    if (MoreElements.isAnnotationPresent(type, GenerateTypeAdapter.class)) {
      return true;
    }

    // check that the class contains a non-private static method returning a TypeAdapter
    TypeName typeName = TypeName.get(type.asType());
    ParameterizedTypeName typeAdapterType = ParameterizedTypeName.get(
        ClassName.get(TypeAdapter.class), typeName);
    TypeName returnedTypeAdapter = null;
    for (ExecutableElement method : methodsIn(type.getEnclosedElements())) {
      if (method.getModifiers().contains(STATIC) && !method.getModifiers().contains(PRIVATE)) {
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
    if (returnedTypeAdapter instanceof ParameterizedTypeName) {
      ParameterizedTypeName paramReturnType = (ParameterizedTypeName) returnedTypeAdapter;
      TypeName argument = paramReturnType.typeArguments.get(0);

      // If the original type uses generics, user's don't have to nest the generic type args
      if (typeName instanceof ParameterizedTypeName) {
        ParameterizedTypeName pTypeName = (ParameterizedTypeName) typeName;
        return pTypeName.rawType.equals(argument);
      } else {
        messager.printMessage(Diagnostic.Kind.WARNING,
            String.format("Found static method returning TypeAdapter<%s> on %s class. "
                + "Skipping GsonTypeAdapter generation.", argument, type));
      }
    } else {
      messager.printMessage(Diagnostic.Kind.WARNING, "Found static method returning "
          + "TypeAdapter with no type arguments, skipping GsonTypeAdapter generation.");
    }
    return false;
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
    ProcessingEnvironment env = context.processingEnvironment();
    Optional<AnnotationSpec> generatedAnnotationSpec =
        GeneratedAnnotations.generatedAnnotation(env.getElementUtils(), env.getSourceVersion())
            .map(AutoValueGsonExtension::createGeneratedAnnotationSpec);
    TypeElement type = context.autoValueClass();
    boolean generateExternalAdapter = type.getAnnotation(GenerateTypeAdapter.class) != null;
    List<Property> properties = Lists.newArrayList();
    for (Map.Entry<String, ExecutableElement> entry : context.properties().entrySet()) {
      Property property = Property.create(
          context.processingEnvironment().getMessager(),
          entry.getKey(),
          entry.getValue(),
          context.propertyTypes().get(entry.getKey())
      );
      if (property == null) {
        return null;
      }
      properties.add(property);
    }

    Map<String, TypeName> types = convertPropertiesToTypes(context.properties());

    ClassName classNameClass = ClassName.get(context.packageName(), className);
    ClassName autoValueClass = ClassName.get(context.autoValueClass());

    List<? extends TypeParameterElement> typeParams = context.autoValueClass().getTypeParameters();
    List<TypeVariableName> params = new ArrayList<>(typeParams.size());
    ClassName superclassRawType = ClassName.get(context.packageName(), classToExtend);
    TypeName superClassType = superclassRawType;
    if (!typeParams.isEmpty()) {
      for (TypeParameterElement typeParam : typeParams) {
        params.add(TypeVariableName.get(typeParam));
      }
      superClassType = ParameterizedTypeName.get(ClassName.get(context.packageName(), classToExtend), params.toArray(new TypeName[params.size()]));
    }

    ClassName adapterClassName = generateExternalAdapter
        ? ClassName.get(context.packageName(), Joiner.on("_").join(autoValueClass.simpleNames()) + "_GsonTypeAdapter")
        : classNameClass.nestedClass("GsonTypeAdapter");
    ClassName finalSuperClass = generateExternalAdapter ? classNameClass : superclassRawType;

    TypeSpec typeAdapter = createTypeAdapter(type, classNameClass, autoValueClass, adapterClassName,
        finalSuperClass, properties, params, context.builder().orElse(null), context.processingEnvironment());

    Filer filer = context.processingEnvironment().getFiler();
    if (generateExternalAdapter) {
      try {
        TypeSpec.Builder builder = typeAdapter.toBuilder();
        generatedAnnotationSpec.ifPresent(builder::addAnnotation);
        JavaFile.builder(context.packageName(), builder.build())
            .skipJavaLangImports(true)
            .build()
            .writeTo(filer);
      } catch (IOException e) {
        context.processingEnvironment().getMessager()
            .printMessage(Diagnostic.Kind.ERROR,
                String.format(
                    "Failed to write external TypeAdapter for element \"%s\" with reason \"%s\"",
                    type,
                    e.getMessage()));
      }

      ClassName proguardTarget = ClassName.get(context.autoValueClass());
      List<String> adapterConstructorParams = Lists.newArrayList();
      typeAdapter.methodSpecs.stream().filter(MethodSpec::isConstructor).findFirst()
          .ifPresent(c -> {
            for (ParameterSpec p : c.parameters) {
              adapterConstructorParams.add(proguardNameOf(p.type));
            }
          });

      ProguardConfig proguardConfig = ProguardConfig.create(
          proguardTarget,
          adapterClassName,
          adapterConstructorParams
      );
      try {
        proguardConfig.writeTo(filer, context.autoValueClass());
      } catch (IOException e) {
        context.processingEnvironment().getMessager()
            .printMessage(Diagnostic.Kind.ERROR,
                String.format(
                    "Failed to write proguard file for element \"%s\" with reason \"%s\"",
                    context.autoValueClass(),
                    e.getMessage()));
      }
      return null;
    } else {
      TypeSpec.Builder subclass = TypeSpec.classBuilder(classNameClass)
          .superclass(superClassType)
          .addType(typeAdapter.toBuilder()
              .addModifiers(STATIC)
              .build())
          .addMethod(generateConstructor(properties, types));

      generatedAnnotationSpec.ifPresent(subclass::addAnnotation);

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
  }

  private static AnnotationSpec createGeneratedAnnotationSpec(TypeElement generatedAnnotationTypeElement) {
    return AnnotationSpec.builder(ClassName.get(generatedAnnotationTypeElement))
      .addMember("value", "$S", AutoValueGsonExtension.class.getName())
      .addMember("comments", "$S", GENERATED_COMMENTS)
      .build();
  }

  private ImmutableMap<TypeName, FieldSpec> createFields(List<Property> properties) {
    ImmutableMap.Builder<TypeName, FieldSpec> fields = ImmutableMap.builder();

    ClassName jsonAdapter = ClassName.get(TypeAdapter.class);
    Set<TypeName> seenTypes = Sets.newHashSet();
    NameAllocator nameAllocator = new NameAllocator();
    for (Property property : properties) {
      if (property.isTransient()) {
        continue;
      }
      TypeName type = property.type.isPrimitive() ? property.type.box() : property.type;
      ParameterizedTypeName adp = ParameterizedTypeName.get(jsonAdapter, type);
      if (!seenTypes.contains(property.type)) {
        fields.put(property.type,
                FieldSpec.builder(adp,
                    nameAllocator.newName(simpleName(property.type)) + "_adapter", PRIVATE, VOLATILE)
                    .build());
        seenTypes.add(property.type);
      }
    }

    return fields.build();
  }

  private static String simpleName(TypeName typeName) {
    if (typeName instanceof ClassName) {
      return UPPER_CAMEL.to(LOWER_CAMEL, ((ClassName) typeName).simpleName());
    } else if (typeName instanceof ParameterizedTypeName) {
      ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) typeName;
      return UPPER_CAMEL.to(LOWER_CAMEL, parameterizedTypeName.rawType.simpleName())
          + (parameterizedTypeName.typeArguments.isEmpty() ? "" : "__")
          + simpleName(parameterizedTypeName.typeArguments);
    } else if (typeName instanceof ArrayTypeName) {
      return "array__" + simpleName(((ArrayTypeName) typeName).componentType);
    } else if (typeName instanceof WildcardTypeName) {
      WildcardTypeName wildcardTypeName = (WildcardTypeName) typeName;
      return "wildcard__"
          + simpleName(ImmutableList.<TypeName>builder().addAll(wildcardTypeName.lowerBounds)
          .addAll(wildcardTypeName.upperBounds)
          .build());
    } else if (typeName instanceof TypeVariableName) {
      TypeVariableName variable = (TypeVariableName) typeName;
      return variable.name
          + (variable.bounds.isEmpty() ? "" : "__")
          + simpleName(variable.bounds);
    } else {
      return typeName.toString();
    }
  }

  private static String simpleName(List<TypeName> typeNames) {
    return Joiner.on("_").join(typeNames.stream()
            .map(AutoValueGsonExtension::simpleName)
            .collect(Collectors.toList()));
  }

  private MethodSpec generateConstructor(List<Property> properties, Map<String, TypeName> types) {
    List<ParameterSpec> params = Lists.newArrayList();
    for (Property property : properties) {
      TypeName type = property.type;
      if (property.nullableTypeAnnotation().isPresent()) {
        type = type.annotated(AnnotationSpec.get(property.nullableTypeAnnotation().get()));
      }
      ParameterSpec.Builder builder = ParameterSpec.builder(type, property.humanName);
      if (property.nullableMethodAnnotation().isPresent()) {
        builder.addAnnotation(AnnotationSpec.get(property.nullableMethodAnnotation().get()));
      }
      params.add(builder.build());
    }

    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    for (int i = properties.size(); i > 0; i--) {
      superFormat.append("$N");
      if (i > 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), types.keySet().toArray());

    return builder.build();
  }

  /**
   * Converts the ExecutableElement properties to TypeName properties
   */
  private Map<String, TypeName> convertPropertiesToTypes(Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      ExecutableElement el = entry.getValue();
      types.put(entry.getKey(), TypeName.get(el.getReturnType()));
    }
    return types;
  }

  private TypeSpec createTypeAdapter(
      TypeElement autoValueType,
      ClassName className,
      ClassName autoValueClassName,
      ClassName gsonTypeAdapterName,
      ClassName superClassType,
      List<Property> properties,
      List<TypeVariableName> typeParams,
      @Nullable BuilderContext builderContext,
      ProcessingEnvironment processingEnvironment) {
    ClassName typeAdapterClass = ClassName.get(TypeAdapter.class);
    final TypeName autoValueTypeName = !typeParams.isEmpty()
        ? ParameterizedTypeName.get(autoValueClassName, typeParams.toArray(new TypeName[typeParams.size()]))
        : autoValueClassName;
    ParameterizedTypeName superClass = ParameterizedTypeName.get(typeAdapterClass, autoValueTypeName);

    ParameterSpec gsonParam = ParameterSpec.builder(Gson.class, "gson").build();
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addParameter(gsonParam);

    if (!typeParams.isEmpty()) {

      ParameterSpec typeAdapter = ParameterSpec
          .builder(ArrayTypeName.of(Type.class), "types")
          .build();
      constructor.addParameter(typeAdapter);
      constructor.addStatement("typeArgs = $N", typeAdapter);
    }

    ImmutableMap<TypeName, FieldSpec> adapters = createFields(properties);
    if (useFieldNamePolicy) {
      constructor.addStatement("$1T fields = new $1T()", ParameterizedTypeName.get(ArrayList.class, String.class));
      for (Property prop : properties) {
        constructor.addStatement("fields.add($S)", prop.humanName);
      }
    }
    constructor.addStatement("this.gson = gson");
    if (useFieldNamePolicy) {
      constructor.addStatement("this.realFieldNames = $T.renameFields($T.class, fields, gson.fieldNamingStrategy())",
          ClassName.get("com.ryanharter.auto.value.gson.internal", "Util"),
          superClassType);
    }

    ClassName jsonAdapter = ClassName.get(TypeAdapter.class);
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(gsonTypeAdapterName)
        .addOriginatingElement(autoValueType)
        .addTypeVariables(typeParams)
        .addModifiers(FINAL)
        .superclass(superClass)
        .addFields(adapters.values());

    if (useFieldNamePolicy) {
      classBuilder.addField(FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String.class), "realFieldNames", PRIVATE, FINAL).build());
    }

    classBuilder.addField(FieldSpec.builder(Gson.class, "gson", PRIVATE, FINAL).build())
        .addMethod(constructor.build())
        .addMethod(createWriteMethod(autoValueTypeName, properties, adapters,
            jsonAdapter, typeParams))
        .addMethod(createReadMethod(className, autoValueClassName, autoValueTypeName, properties,
            adapters, jsonAdapter, typeParams, builderContext, processingEnvironment))
        .addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(String.class)
            .addStatement("return new $T().append($S).append($S).append($S).toString()",
                StringBuilder.class,
                "TypeAdapter(",
                Joiner.on(".").join(autoValueClassName.simpleNames()),
                ")"
            )
            .build());

    if (!typeParams.isEmpty()) {
      classBuilder.addField(FieldSpec.builder(Type[].class, "typeArgs", PRIVATE, FINAL).build());
    }

    return classBuilder.build();
  }

  private static void addConditionalAdapterAssignment(CodeBlock.Builder block,
                                               FieldSpec adapterField,
                                               Property prop,
                                               ClassName jsonAdapter,
                                               List<TypeVariableName> typeParams) {
    TypeName type = prop.type.isPrimitive() ? prop.type.box() : prop.type;
    ParameterizedTypeName adp = ParameterizedTypeName.get(jsonAdapter, type);

    block.addStatement("$T $N = this.$N", adapterField.type, adapterField, adapterField);
    block.beginControlFlow("if ($N == null)", adapterField);
    if (prop.type instanceof ParameterizedTypeName || prop.type instanceof TypeVariableName) {
      block.addStatement("this.$N = $N = ($T) gson.getAdapter($L)", adapterField, adapterField,
          adp, makeParameterizedType(prop.type, typeParams));
    } else {
      block.addStatement("this.$N = $N = gson.getAdapter($T.class)", adapterField, adapterField, type);
    }
    block.endControlFlow();
  }

  private static void addBuilderFieldSetting(Builder block,
                                             Property prop,
                                             FieldSpec adapter,
                                             ParameterSpec jsonReader,
                                             FieldSpec builder,
                                             BuilderContext builderContext,
                                             ProcessingEnvironment processingEnv) {
    Set<ExecutableElement> setters = builderContext.setters().get(prop.humanName);
    if (setters == null || setters.isEmpty()) {
      ExecutableElement propertyBuilder = builderContext.propertyBuilders().get(prop.humanName);
      if (propertyBuilder == null) {
        processingEnv
            .getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR, "No setter or builder for " + prop.humanName, prop.element);
      } else {
        // This duplicates AutoValue's handling of addAll and putAll, for Guava's ImmutableList and
        // ImmutableMap (etc). We have for example `ImmutableList<String> getFoo()` but we don't
        // have `setFoo(ImmutableList<String>)`, we only have
        // `ImmutableList.Builder<String> fooBuilder()`. So we need to do
        // `builder.fooBuilder().addAll(adapter.read(jsonReader)`.
        Set<String> methodNames =
            methodsIn(
                    MoreTypes.asTypeElement(propertyBuilder.getReturnType()).getEnclosedElements())
                .stream()
                .map(e -> e.getSimpleName().toString())
                .collect(Collectors.toSet());
        String addAllPutAll =
            methodNames.contains("addAll")
                ? "addAll"
                : methodNames.contains("putAll")
                    ? "putAll"
                    : null;
        if (addAllPutAll == null) {
          processingEnv
              .getMessager()
              .printMessage(
                  Diagnostic.Kind.ERROR,
                  "Don't know how to get Gson values for " + prop.humanName + " into this builder",
                  propertyBuilder);
          return;
        }
        block.addStatement(
            "$N.$N().$N($N.read($N))",
            builder,
            propertyBuilder.getSimpleName(),
            addAllPutAll,
            adapter,
            jsonReader);
      }
      return;
    }
    // We're looking for a method that we can call with a parameter that is a value of the property
    // type. The parameter type might be exactly the same, but it could also be a supertype.
    // For example, AutoValue allows you to set a property of type ImmutableList<String> using a
    // parameter of type List<String> (it generates a call to ImmutableList.copyOf).
    // Similarly, it allows you to set a property of type Optional<String> with a parameter of type
    // String (it generates a call to Optional.of). So we look for those cases.
    // There's a further quirk, which is that the T in @AutoValue class Foo<T> is not the same as
    // @AutoValue.Builder class Foo.Builder<T>, so if the return type of Foo.getBar() is
    // ImmutableList<T> then that won't look the same as the parameter type of
    // Foo.Builder.setBar(ImmutableList<T>). AutoValue has some tricky logic to juggle the types
    // but here we just assume the types will work if their erasures do. Otherwise AutoValue would
    // presumably reject the getter/setter combination with a helpful error message.
    Types typeUtils = processingEnv.getTypeUtils();
    TypeMirror propertyType = prop.element.getReturnType();
    for (ExecutableElement setter : setters) {
      if (typeUtils.isAssignable(
              typeUtils.erasure(propertyType), typeUtils.erasure(setter.getParameters().get(0).asType()))) {
        block.addStatement(
            "$N.$N($N.read($N))", builder, setter.getSimpleName(), adapter, jsonReader);
        return;
      }
    }
    Optional<TypeMirror> optionalArgType = optionalArgType(propertyType, typeUtils);
    if (optionalArgType.isPresent()) {
      for (ExecutableElement setter : setters) {
        if (typeUtils.isAssignable(
            typeUtils.erasure(optionalArgType.get()), typeUtils.erasure(setter.getParameters().get(0).asType()))) {
          // The AutoValue property is `Optional<String> foo()` but we only have a setter
          // `setFoo(String)`.
          // We'll generate `adapter.read(jsonReader).ifPresent(x$ -> builder.setFoo(x$))`.
          block.addStatement(
              "$N.read($N).ifPresent(x$$ -> $N.$N(x$$))",
              adapter,
              jsonReader,
              builder,
              setter.getSimpleName());
          return;
        }
      }
    }
    // This shouldn't happen: AutoValue should have rejected the builder if it didn't have a way to
    // set this property. But it may be that AutoValue has output an error message and still let
    // this extension run.
    String errorMsg = "Setter not found for " + prop.element;
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, errorMsg, prop.element);
  }

  private static Optional<TypeMirror> optionalArgType(TypeMirror typeMirror, Types typeUtils) {
    if (typeMirror.getKind() != TypeKind.DECLARED) {
      return Optional.empty();
    }
    TypeElement typeElement = MoreTypes.asTypeElement(typeMirror);
    switch (typeElement.getQualifiedName().toString()) {
      case "java.util.OptionalInt":
        return Optional.of(typeUtils.getPrimitiveType(TypeKind.INT));
      case "java.util.OptionalLong":
        return Optional.of(typeUtils.getPrimitiveType(TypeKind.LONG));
      case "java.util.OptionalDouble":
        return Optional.of(typeUtils.getPrimitiveType(TypeKind.DOUBLE));
      case "java.util.Optional":
      case "com.google.common.base.Optional":
        DeclaredType declaredType = MoreTypes.asDeclared(typeMirror);
        // The size check is mostly in case we're looking at a raw type here.
        if (declaredType.getTypeArguments().size() != 1) {
          return Optional.empty();
        }
        return Optional.of(declaredType.getTypeArguments().get(0));
      default:
        return Optional.empty();
    }
  }

  private static void addFieldSetting(CodeBlock.Builder block,
                                      Property prop,
                                      Map<Property, FieldSpec> fields,
                                      FieldSpec adapter,
                                      ParameterSpec jsonReader) {
    block.addStatement("$N = $N.read($N)", fields.get(prop), adapter, jsonReader);
  }

  private MethodSpec createWriteMethod(TypeName autoValueClassName,
      List<Property> properties,
      ImmutableMap<TypeName, FieldSpec> adapters,
      ClassName jsonAdapter,
      List<TypeVariableName> typeParams) {
    ParameterSpec jsonWriter = ParameterSpec.builder(JsonWriter.class, "jsonWriter").build();
    ParameterSpec annotatedParam = ParameterSpec.builder(autoValueClassName, "object").build();
    MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("write")
        .addAnnotation(Override.class)
        .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", "\"unchecked\"")
            .build())
        .addModifiers(PUBLIC)
        .addParameter(jsonWriter)
        .addParameter(annotatedParam)
        .addException(IOException.class);

    writeMethod.beginControlFlow("if ($N == null)", annotatedParam);
    writeMethod.addStatement("$N.nullValue()", jsonWriter);
    writeMethod.addStatement("return");
    writeMethod.endControlFlow();

    writeMethod.addStatement("$N.beginObject()", jsonWriter);
    for (Property prop : properties) {
      if (prop.isTransient()) {
        continue;
      }

      if (isUnrecognisedJsonPropertiesContainer(prop)) {
        writeMethod.beginControlFlow("if(object.$L() != null)", prop.methodName);

        TypeName stringTypeAdapterClass = ParameterizedTypeName.get(TypeAdapter.class, String.class);

        TypeName unrecognizedMapEntryType = ParameterizedTypeName.get(Map.Entry.class, String.class, Object.class);
        writeMethod.beginControlFlow("for ($T entry : object.$L().entrySet())", unrecognizedMapEntryType, prop.methodName);

        writeMethod.addStatement("jsonWriter.name(entry.getKey())");
        writeMethod.addStatement("$T adapter = gson.getAdapter(entry.getValue().getClass())", TypeAdapter.class);
        writeMethod.addStatement("adapter.write(jsonWriter, entry.getValue())");

        writeMethod.endControlFlow(); // for map entries
        writeMethod.endControlFlow(); // if(object.$L() != null)
        continue;
      }

      if (prop.hasSerializedNameAnnotation()) {
        writeMethod.addStatement("$N.name($S)", jsonWriter, prop.serializedName());
      } else if (useFieldNamePolicy) {
        writeMethod.addStatement("$N.name(realFieldNames.get($S))", jsonWriter, prop.humanName);
      } else {
        writeMethod.addStatement("$N.name($S)", jsonWriter, prop.humanName);
      }
      // for adapters handling non-primitive values, initialize the
      // adapter only when the value is actually present (non-null),
      // otherwise use a generic method of writing the null value
      FieldSpec adapterField = adapters.get(prop.type);
      CodeBlock.Builder block = CodeBlock.builder();
      if (!prop.type.isPrimitive()) {
          writeMethod.beginControlFlow("if ($N.$N() == null)", annotatedParam, prop.methodName);
          writeMethod.addStatement("$N.nullValue()", jsonWriter);
          writeMethod.nextControlFlow("else");
          addConditionalAdapterAssignment(block, adapterField, prop, jsonAdapter, typeParams);
          writeMethod.addCode(block.build());
          writeMethod.addStatement("$N.write($N, $N.$N())", adapterField, jsonWriter, annotatedParam, prop.methodName);
          writeMethod.endControlFlow();
      } else {
        block.add("{\n");
        block.indent();
        addConditionalAdapterAssignment(block, adapterField, prop, jsonAdapter, typeParams);
        block.addStatement("$N.write($N, $N.$N())", adapterField, jsonWriter, annotatedParam, prop.methodName);
        block.unindent();
        block.add("}\n");
        writeMethod.addCode(block.build());
      }
    }
    writeMethod.addStatement("$N.endObject()", jsonWriter);

    return writeMethod.build();
  }

  private MethodSpec createReadMethod(ClassName className,
      ClassName autoValueClassName,
      TypeName autoValueTypeName,
      List<Property> properties,
      ImmutableMap<TypeName, FieldSpec> adapters,
      ClassName jsonAdapter,
      List<TypeVariableName> typeParams,
      @Nullable BuilderContext builderContext,
      ProcessingEnvironment processingEnvironment) {
    ParameterSpec jsonReader = ParameterSpec.builder(JsonReader.class, "jsonReader").build();
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("read")
        .addAnnotation(Override.class)
        .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", "\"unchecked\"")
            .build())
        .addModifiers(PUBLIC)
        .returns(autoValueTypeName)
        .addParameter(jsonReader)
        .addException(IOException.class);

    // Validate the builderContext if there is one.
    if (builderContext != null) {
      if (!builderContext.buildMethod().isPresent()) {
        processingEnvironment.getMessager()
            .printMessage(
                Diagnostic.Kind.ERROR,
                "Could not determine the build method. Make sure it is named \"build\".",
                builderContext.builderType());
        return readMethod.build();
      }

      Set<ExecutableElement> builderMethods = builderContext.builderMethods();

      if (builderMethods.size() > 1) {
        Set<ExecutableElement> annotatedMethods = builderMethods.stream()
            .filter(e -> MoreElements.isAnnotationPresent(e, AutoValueGsonBuilder.class))
            .collect(Collectors.toSet());

        if (annotatedMethods.size() > 1) {
          processingEnvironment.getMessager()
              .printMessage(
                  Diagnostic.Kind.ERROR,
                  "Too many @AutoValueGsonBuilder annotated builder methods.",
                  annotatedMethods.stream().findAny().get()
              );
          return readMethod.build();
        }

        if (annotatedMethods.isEmpty()) {
          processingEnvironment.getMessager().printMessage(
              Diagnostic.Kind.ERROR,
              "Too many builder methods. Annotate builder method with @AutoValueGsonBuilder.",
              builderMethods.stream().findAny().get()
          );
          return readMethod.build();
        }
      }
    }

    ClassName token = ClassName.get(JsonToken.NULL.getDeclaringClass());

    readMethod.beginControlFlow("if ($N.peek() == $T.NULL)", jsonReader, token);
    readMethod.addStatement("$N.nextNull()", jsonReader);
    readMethod.addStatement("return null");
    readMethod.endControlFlow();

    Property unrecognisedJsonPropertiesContainer = properties.stream()
      .filter(this::isUnrecognisedJsonPropertiesContainer)
      .findFirst()
      .orElse(null);
    TypeName mapOfObjects = ParameterizedTypeName.get(LinkedHashMap.class, String.class, Object.class);
    if (unrecognisedJsonPropertiesContainer != null) {
      readMethod.addStatement("$T unrecognised = null", mapOfObjects);
    }

    readMethod.addStatement("$N.beginObject()", jsonReader);

    // Will be empty if using a AutoValue builder
    Map<Property, FieldSpec> fields = new LinkedHashMap<>(properties.size());
    // Will be absent if not using AutoValue builder
    Optional<FieldSpec> builderField = Optional.ofNullable(builderContext)
        .map(ctx -> FieldSpec
            .builder(TypeName.get(ctx.builderType().asType()), "builder")
            .build());

    if (builderField.isPresent()) {
      Set<ExecutableElement> builderMethods = builderContext.builderMethods();

      if (builderMethods.size() == 0) {
        // If no builder method defined, instantiate directly.
        readMethod.addStatement("$T $N = new $T.$L()", builderField.get().type, builderField.get(),
            className, builderContext.builderType().getSimpleName());
      } else {
        ExecutableElement builderMethod;
        if (builderMethods.size() == 1) {
          // If there is only 1, use it.
          builderMethod = builderMethods.stream().findFirst().get();
        } else {
          // Otherwise, find the only builder method that is annotated.
          Set<ExecutableElement> annotatedMethods = builderMethods.stream()
              .filter(e -> MoreElements.isAnnotationPresent(e, AutoValueGsonBuilder.class))
              .collect(Collectors.toSet());

          if (annotatedMethods.size() == 1) {
            builderMethod = annotatedMethods.stream().findFirst().get();
          } else {
            throw new IllegalStateException();
          }
        }

        readMethod.addStatement("$T $N = $T.$N()", builderField.get().type, builderField.get(),
            autoValueClassName, builderMethod.getSimpleName());
      }
    } else {
      // add the properties
      for (Property prop : properties) {
        TypeName fieldType = prop.type;
        FieldSpec field = FieldSpec.builder(fieldType, prop.humanName).build();
        fields.put(prop, field);

        CodeBlock defaultValue = getDefaultValue(prop, field);
        readMethod.addCode("$[$T $N = ", field.type, field);
        if (defaultValue != null) {
          readMethod.addCode(defaultValue);
        } else {
          readMethod.addCode("$L", "null");
        }
        readMethod.addCode(";\n$]");
      }
    }

    readMethod.beginControlFlow("while ($N.hasNext())", jsonReader);

    FieldSpec name = FieldSpec.builder(String.class, "_name").build();
    readMethod.addStatement("$T $N = $N.nextName()", name.type, name, jsonReader);

    readMethod.beginControlFlow("if ($N.peek() == $T.NULL)", jsonReader, token);
    readMethod.addStatement("$N.nextNull()", jsonReader);
    readMethod.addStatement("continue");
    readMethod.endControlFlow();

    readMethod.beginControlFlow("switch ($N)", name);
    for (Property prop : properties) {
      if (prop.isTransient()) {
        continue;
      }
      if (prop.hasSerializedNameAnnotation()) {
        for (String alternate : prop.serializedNameAlternate()) {
          readMethod.addCode("case $S:\n", alternate);
        }
        readMethod.beginControlFlow("case $S:", prop.serializedName());
        FieldSpec adapterField = adapters.get(prop.type);
        CodeBlock.Builder block = CodeBlock.builder();
        addConditionalAdapterAssignment(block, adapterField, prop, jsonAdapter, typeParams);
        if (builderField.isPresent()) {
          addBuilderFieldSetting(
              block, prop, adapterField, jsonReader, builderField.get(), builderContext, processingEnvironment);
        } else {
          addFieldSetting(block, prop, fields, adapterField, jsonReader);
        }
        readMethod.addCode(block.build());
        readMethod.addStatement("break");
        readMethod.endControlFlow();
      }
    }

    // skip value if field is not serialized...
    readMethod.beginControlFlow("default:");
    for (Property prop : properties) {
      if (prop.isTransient()) {
        continue;
      }
      if (prop == unrecognisedJsonPropertiesContainer) {
        continue;
      }
      if (!prop.hasSerializedNameAnnotation()) {
        if (useFieldNamePolicy) {
          readMethod.beginControlFlow("if (realFieldNames.get($S).equals(_name))", prop.humanName);
        } else {
          readMethod.beginControlFlow("if ($S.equals(_name))", prop.humanName);
        }
        FieldSpec adapterField = adapters.get(prop.type);
        CodeBlock.Builder block = CodeBlock.builder();
        addConditionalAdapterAssignment(block, adapterField, prop, jsonAdapter, typeParams);
        if (builderField.isPresent()) {
          addBuilderFieldSetting(
              block, prop, adapterField, jsonReader, builderField.get(), builderContext, processingEnvironment);
        } else {
          addFieldSetting(block, prop, fields, adapterField, jsonReader);
        }
        readMethod.addCode(block.build());
        readMethod.addStatement("continue");
        readMethod.endControlFlow();
      }
    }
    if (unrecognisedJsonPropertiesContainer != null) {

      readMethod.beginControlFlow("if (unrecognised == null)");
      readMethod.addStatement("unrecognised = new $T()", mapOfObjects);
      readMethod.addStatement("builder.$L(unrecognised)", unrecognisedJsonPropertiesContainer.methodName);
      readMethod.endControlFlow();

      readMethod.addStatement("$T number = gson.fromJson(jsonReader, $T.class)", JsonElement.class, JsonElement.class);
      readMethod.addStatement("unrecognised.put(_name, number)");

      readMethod.addStatement("continue");
    } else {
      readMethod.addStatement("$N.skipValue()", jsonReader);
    }

    readMethod.endControlFlow(); // default case

    readMethod.endControlFlow(); // switch
    readMethod.endControlFlow(); // while

    readMethod.addStatement("$N.endObject()", jsonReader);

    if (builderField.isPresent()) {
      readMethod.addStatement("return $N.$L()", builderField.get(), builderContext.buildMethod().get().getSimpleName());
    } else {
      StringBuilder format = new StringBuilder("return new ");
      format.append(className.simpleName().replaceAll("\\$", ""));
      if (autoValueTypeName instanceof ParameterizedTypeName) {
        format.append("<>");
      }
      format.append("(");
      Iterator<FieldSpec> iterator = fields.values().iterator();
      while (iterator.hasNext()) {
        iterator.next();
        format.append("$N");
        if (iterator.hasNext())
          format.append(", ");
      }
      format.append(")");
      readMethod.addStatement(format.toString(), fields.values().toArray());
    }

    return readMethod.build();
  }

  private boolean isUnrecognisedJsonPropertiesContainer(Property unrecognised) {
    return unrecognised.methodAnnotations.stream()
      .anyMatch(
        annot ->
          annot.getAnnotationType().asElement().getSimpleName().contentEquals(UnrecognisedJsonProperties.class.getSimpleName())
      );
  }

  /**
   * Returns a default value for initializing well-known types, or else {@code null}.
   */
  private CodeBlock getDefaultValue(Property prop, FieldSpec field) {
    if (field.type.isPrimitive()) {
      String defaultValue = getDefaultPrimitiveValue(field.type);
      if (defaultValue != null) {
        return CodeBlock.of("$L", defaultValue);
      } else {
        return CodeBlock.of("$T.valueOf(null)", field.type);
      }
    }
    if (prop.nullable()) {
      return null;
    }
    TypeMirror type = prop.element.getReturnType();
    if (type.getKind() != TypeKind.DECLARED) {
      return null;
    }
    TypeElement typeElement = MoreTypes.asTypeElement(type);
    if (typeElement == null) {
      return null;
    }

    return null;
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

  private static CodeBlock makeParameterizedType(TypeName typeName, List<TypeVariableName> typeParams) {
    CodeBlock.Builder block = CodeBlock.builder();
    if (typeName instanceof TypeVariableName) {
      block.add("$T.get(typeArgs[$L])", TypeToken.class, typeParams.indexOf(typeName));
    } else{
      ParameterizedTypeName paramType = (ParameterizedTypeName) typeName;
      block.add("$T.getParameterized($T.class", TypeToken.class, paramType.rawType);
      for (TypeName type : paramType.typeArguments) {
        buildParameterizedTypeArguments(block, type, typeParams);
      }
      block.add(")");
    }
    return block.build();
  }

  private static void buildParameterizedTypeArguments(CodeBlock.Builder block, TypeName typeArg,
                                                      List<TypeVariableName> typeParams) {
    block.add(", ");
    if (typeArg instanceof ParameterizedTypeName) { // type argument itself can be parameterized
      ParameterizedTypeName paramTypeArg = (ParameterizedTypeName) typeArg;
      block.add("$T.getParameterized($T.class", TypeToken.class, paramTypeArg.rawType);
      for (TypeName type : paramTypeArg.typeArguments) {
        buildParameterizedTypeArguments(block, type, typeParams);
      }
      block.add(").getType()");
    } else if (typeArg instanceof TypeVariableName) {
      block.add("typeArgs[$L]", typeParams.indexOf(typeArg));
    } else if (typeArg instanceof WildcardTypeName) {
      WildcardTypeName wildcard = (WildcardTypeName) typeArg;
      TypeName target;
      String method;
      if (wildcard.lowerBounds.size() == 1) {
        target = wildcard.lowerBounds.get(0);
        method = "supertypeOf";
      } else if (wildcard.upperBounds.size() == 1) {
        target = wildcard.upperBounds.get(0);
        method = "subtypeOf";
      } else {
        throw new IllegalArgumentException(
            "Unrepresentable wildcard type. Cannot have more than one bound: " + wildcard);
      }
      block.add("$T.$L($T.class)", WildcardUtil.class, method, target);
    } else {
      block.add("$T.class", typeArg);
    }
  }

  @Nullable
  private static ClassName rawType(TypeName typeName) {
    if (typeName instanceof ClassName) {
      return (ClassName) typeName;
    } else if (typeName instanceof ArrayTypeName) {
      return rawType(((ArrayTypeName) typeName).componentType);
    } else if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType;
    } else if (typeName instanceof WildcardTypeName) {
      return rawType(((WildcardTypeName) typeName).upperBounds.get(0));
    } else {
      return null;
    }
  }

  private static String proguardNameOf(TypeName typeName) {
    if (typeName instanceof ClassName) {
      return ((ClassName) typeName).reflectionName();
    } else if (typeName instanceof ArrayTypeName) {
      return proguardNameOf(((ArrayTypeName) typeName).componentType) + "[]";
    } else if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType.reflectionName();
    } else if (typeName instanceof TypeVariableName) {
      return "java.lang.Object";
    } else {
      throw new UnsupportedOperationException("unrecognised TypeName type: " + typeName);
    }
  }
}
