package com.ryanharter.auto.value.gson;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.Defaults;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Primitives;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.AnnotationSpec;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueGsonExtension extends AutoValueExtension {

  /** Compiler flag to indicate that collections/maps should default to their empty forms. Default is to default to null. */
  static final String COLLECTIONS_DEFAULT_TO_EMPTY = "autovaluegson.defaultCollectionsToEmpty";

  private static final String GENERATED_COMMENTS = "https://github.com/rharter/auto-value-gson";

  private static final AnnotationSpec GENERATED =
      AnnotationSpec.builder(Generated.class)
          .addMember("value", "$S", AutoValueGsonExtension.class.getName())
          .addMember("comments", "$S", GENERATED_COMMENTS)
          .build();

  // see com.google.auto.value.processor.Optionalish
  private static final ImmutableSet<String> OPTIONAL_CLASS_NAMES = ImmutableSet.of(
      "com.google.common.base.Optional",
      "java.util.Optional",
      "java.util.OptionalDouble",
      "java.util.OptionalInt",
      "java.util.OptionalLong");

  public static class Property {
    final String methodName;
    final String humanName;
    final ExecutableElement element;
    final TypeName type;
    final ImmutableSet<String> annotations;
    final TypeMirror typeAdapter;
    final boolean instanceOfOptional;

    public Property(String humanName, ExecutableElement element) {
      this.methodName = element.getSimpleName().toString();
      this.humanName = humanName;
      this.element = element;

      type = TypeName.get(element.getReturnType());
      annotations = buildAnnotations(element);

      typeAdapter = getAnnotationValue(element, GsonTypeAdapter.class);
      instanceOfOptional = isOptional(element.getReturnType());
    }

    public static TypeMirror getAnnotationValue(Element foo, Class<?> annotation) {
      AnnotationMirror am = getAnnotationMirror(foo, annotation);
      if (am == null) {
        return null;
      }
      AnnotationValue av = getAnnotationValue(am, "value");
      return av == null ? null : (TypeMirror) av.getValue();
    }

    private static AnnotationMirror getAnnotationMirror(Element typeElement, Class<?> clazz) {
      String clazzName = clazz.getName();
      for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
        if (m.getAnnotationType().toString().equals(clazzName)) {
          return m;
        }
      }
      return null;
    }

    private static AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String key) {
      Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
        if (entry.getKey().getSimpleName().toString().equals(key)) {
          return entry.getValue();
        }
      }
      return null;
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

    public boolean shouldSerialize() {
      Ignore ignore = element.getAnnotation(Ignore.class);
      return ignore == null || ignore.value() == Ignore.Type.DESERIALIZATION;
    }

    public boolean shouldDeserialize() {
      Ignore ignore = element.getAnnotation(Ignore.class);
      return ignore == null || ignore.value() == Ignore.Type.SERIALIZATION;
    }

    public boolean nullable() {
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

  private boolean collectionsDefaultToEmpty = false;

  @Override
  public boolean applicable(Context context) {
    Messager messager = context.processingEnvironment().getMessager();

    // validate @AutoValueGsonBuilder usage
    Set<ExecutableElement> annotatedMethods = ElementFilter
        .methodsIn(context.autoValueClass().getEnclosedElements())
        .stream()
        .filter(e -> MoreElements.isAnnotationPresent(e, AutoValueGsonBuilder.class))
        .collect(Collectors.toSet());

    if (context.hasBuilder()) {
      @SuppressWarnings("ConstantConditions") // should never NPE because class has a builder
      TypeMirror builderType = findBuilderTypeElement(context.autoValueClass()).asType();

      if (annotatedMethods.size() == 1) {
        ExecutableElement builderMethod = Iterables.getOnlyElement(annotatedMethods);
        boolean validApplication = isValidBuilderMethod(builderType).test(builderMethod);
        if (!validApplication) {
          messager.printMessage(Kind.ERROR, String.format("Found invalid @AutoValudGsonBuilder "
                  + "usage in %s. @AutoValueGsonBuilder may only be applied to a single "
                  + "non-private, static method with no args that return the @AutoValue.Builder "
                  + "annotated type.",
                  context.autoValueClass().getQualifiedName()));
        }
      }

      if (annotatedMethods.size() > 1) {
        messager.printMessage(Kind.ERROR, String.format("Found more than one method annotated with "
            + "@AutoValueGsonBuilder in %s.", context.autoValueClass().getQualifiedName()));
      }

      // if no @AutoValueGsonBuilder annotated, see if builder() can be inferred
      if (annotatedMethods.isEmpty()) {
        long inferredBuilders = ElementFilter
            .methodsIn(context.autoValueClass().getEnclosedElements()).stream()
            .filter(isValidBuilderMethod(builderType))
            .count();
        if (inferredBuilders > 1) {
          messager.printMessage(Kind.ERROR, String.format("Default builder could not be inferred "
                  + "from %s. If there is more than one non-private, static, zero arg methods "
                  + "returning the @AutoValue.Builder annotated type, indicate which method "
                  + "AutoValueGson should use with @AutoValueGsonBuilder.",
              context.autoValueClass().getQualifiedName()));
        }
      }
    } else {
      Optional<ExecutableElement> strayMethod = annotatedMethods.stream().findFirst();
      strayMethod.ifPresent(executableElement ->
          messager.printMessage(Kind.ERROR, String.format("%s does not have an @AutoValue.Builder "
              + "annotated builder, but found @AutoValueGsonBuilder annotated method <%s>",
          context.autoValueClass(),
          executableElement.getSimpleName())));
    }

    return isValidTypeAdapter(context);
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
    ProcessingEnvironment env = context.processingEnvironment();
    collectionsDefaultToEmpty = Boolean.parseBoolean(env.getOptions()
        .getOrDefault(COLLECTIONS_DEFAULT_TO_EMPTY, "false"));
    boolean generatedAnnotationAvailable = context.processingEnvironment()
        .getElementUtils()
        .getTypeElement("javax.annotation.Generated") != null;
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

    TypeSpec typeAdapter = createTypeAdapter(context, classNameClass, autoValueClass, properties, params);

    TypeSpec.Builder subclass = TypeSpec.classBuilder(classNameClass)
        .superclass(superclasstype)
        .addType(typeAdapter)
        .addMethod(generateConstructor(types));

    if (generatedAnnotationAvailable) {
      subclass.addAnnotation(GENERATED);
    }

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
      if (!property.shouldDeserialize() && !property.shouldSerialize()) {
        continue;
      }
      TypeName type = property.type.isPrimitive() ? property.type.box() : property.type;
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

  public TypeSpec createTypeAdapter(Context context, ClassName className,
      ClassName autoValueClassName, List<Property> properties, List<TypeVariableName> typeParams) {
    ClassName typeAdapterClass = ClassName.get(TypeAdapter.class);
    TypeName autoValueTypeName = autoValueClassName;
    if (!typeParams.isEmpty()) {
      autoValueTypeName = ParameterizedTypeName.get(autoValueClassName, typeParams.toArray(new TypeName[typeParams.size()]));
    }
    ParameterizedTypeName superClass = ParameterizedTypeName.get(typeAdapterClass, autoValueTypeName);

    ParameterSpec gsonParam = ParameterSpec.builder(Gson.class, "gson").build();
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(gsonParam);

    if (!typeParams.isEmpty()) {

      ParameterSpec typeAdapter = ParameterSpec
          .builder(ParameterizedTypeName.get(ClassName.get(TypeToken.class), WildcardTypeName.subtypeOf(autoValueTypeName)), "typeToken")
          .build();
      constructor.addParameter(typeAdapter);

      constructor.addStatement("$1T type = ($1T) $2N.getType()", ParameterizedType.class, typeAdapter);
      constructor.addStatement("$T[] typeArgs = type.getActualTypeArguments()", Type.class);
    }

    ProcessingEnvironment processingEnvironment = context.processingEnvironment();
    TypeMirror typeAdapterFactory = processingEnvironment
            .getElementUtils()
            .getTypeElement("com.google.gson.TypeAdapterFactory")
            .asType();
    Types typeUtils = processingEnvironment.getTypeUtils();

    ImmutableMap<Property, FieldSpec> adapters = createFields(properties);
    for (Property prop : properties) {
      if (!prop.shouldDeserialize() && !prop.shouldSerialize()) {
        continue;
      }
      FieldSpec field = adapters.get(prop);
      if (prop.typeAdapter != null) {
        if (typeUtils.isAssignable(prop.typeAdapter, typeAdapterFactory)) {
          if (prop.type instanceof ParameterizedTypeName || prop.type instanceof TypeVariableName) {
            constructor.addStatement("this.$N = ($T) new $T().create($N, $L)", field, field.type, TypeName.get(prop.typeAdapter),
                    gsonParam, makeParameterizedType(prop.type, typeParams));
          } else {
            constructor.addStatement("this.$N = new $T().create($N, $T.get($T.class))", field, TypeName.get(prop.typeAdapter),
                    gsonParam, TypeToken.class, prop.type);
          }
        } else {
          constructor.addStatement("this.$N = new $T()", field, TypeName.get(prop.typeAdapter));
        }
      } else if (prop.type instanceof ParameterizedTypeName || prop.type instanceof TypeVariableName) {
        constructor.addStatement("this.$N = ($T) $N.getAdapter($L)", field, field.type, gsonParam,
            makeParameterizedType(prop.type, typeParams));
      } else {
        TypeName type = prop.type.isPrimitive() ? prop.type.box() : prop.type;
        constructor.addStatement("this.$N = $N.getAdapter($T.class)", field, gsonParam, type);
      }
    }

    ClassName gsonTypeAdapterName = className.nestedClass("GsonTypeAdapter");

    MethodSpec readMethod = context.hasBuilder() ?
        createReadMethod(className, autoValueTypeName, properties, adapters, context.autoValueClass(), context.builder(), processingEnvironment) :
        createReadMethod(className, autoValueTypeName, properties, adapters);

    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(gsonTypeAdapterName)
        .addTypeVariables(typeParams)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .superclass(superClass)
        .addFields(adapters.values())
        .addMethod(constructor.build())
        .addMethod(createWriteMethod(autoValueTypeName, adapters))
        .addMethod(readMethod);

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

    writeMethod.beginControlFlow("if ($N == null)", annotatedParam);
    writeMethod.addStatement("$N.nullValue()", jsonWriter);
    writeMethod.addStatement("return");
    writeMethod.endControlFlow();

    writeMethod.addStatement("$N.beginObject()", jsonWriter);
    for (Map.Entry<Property, FieldSpec> entry : adapters.entrySet()) {
      Property prop = entry.getKey();
      if (!prop.shouldSerialize()) {
        continue;
      }
      FieldSpec field = entry.getValue();

      writeMethod.addStatement("$N.name($S)", jsonWriter, prop.serializedName());
      writeMethod.addStatement("$N.write($N, $N.$N())", field, jsonWriter, annotatedParam, prop.methodName);
    }
    writeMethod.addStatement("$N.endObject()", jsonWriter);

    return writeMethod.build();
  }

  public MethodSpec createReadMethod(ClassName className,
                                     TypeName autoValueClassName,
                                     List<Property> properties,
                                     ImmutableMap<Property, FieldSpec> adapters) {
    ParameterSpec jsonReader = ParameterSpec.builder(JsonReader.class, "jsonReader").build();
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("read")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(autoValueClassName)
        .addParameter(jsonReader)
        .addException(IOException.class);

    ClassName token = ClassName.get(JsonToken.NULL.getDeclaringClass());

    readMethod.beginControlFlow("if ($N.peek() == $T.NULL)", jsonReader, token);
    readMethod.addStatement("$N.nextNull()", jsonReader);
    readMethod.addStatement("return null");
    readMethod.endControlFlow();

    readMethod.addStatement("$N.beginObject()", jsonReader);

    // add the properties
    Map<Property, FieldSpec> fields = new LinkedHashMap<>(properties.size());
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

    readMethod.beginControlFlow("while ($N.hasNext())", jsonReader);

    FieldSpec name = FieldSpec.builder(String.class, "_name").build();
    readMethod.addStatement("$T $N = $N.nextName()", name.type, name, jsonReader);

    readMethod.beginControlFlow("if ($N.peek() == $T.NULL)", jsonReader, token);
    readMethod.addStatement("$N.nextNull()", jsonReader);
    readMethod.addStatement("continue");
    readMethod.endControlFlow();

    readMethod.beginControlFlow("switch ($N)", name);
    for (Property prop : properties) {
      if (!prop.shouldDeserialize()) {
        continue;
      }
      FieldSpec field = fields.get(prop);

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
    if (autoValueClassName instanceof ParameterizedTypeName) {
      format.append("<>");
    }
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

  private MethodSpec createReadMethod(ClassName className,
      TypeName autoValueClassName,
      List<Property> properties,
      ImmutableMap<Property, FieldSpec> adapters,
      TypeElement autoValueClassElement,
      BuilderContext builderContext,
      ProcessingEnvironment processingEnvironment) {
    ParameterSpec jsonReader = ParameterSpec.builder(JsonReader.class, "jsonReader").build();
    MethodSpec.Builder readMethod = MethodSpec.methodBuilder("read")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(autoValueClassName)
        .addParameter(jsonReader)
        .addException(IOException.class);

    ClassName token = ClassName.get(JsonToken.NULL.getDeclaringClass());

    readMethod.beginControlFlow("if ($N.peek() == $T.NULL)", jsonReader, token);
    readMethod.addStatement("$N.nextNull()", jsonReader);
    readMethod.addStatement("return null");
    readMethod.endControlFlow();

    ExecutableElement defaultBuilder = findDefaultBuilder(autoValueClassElement, builderContext.builderClass());

    ClassName builderClassName = ClassName.get(builderContext.builderClass());
    FieldSpec builder = FieldSpec.builder(builderClassName, "builder").build();
    if (defaultBuilder != null) {
      ClassName baseClass = ClassName.get(autoValueClassElement);
      readMethod.addStatement("$T $N = $T.$L", builder.type, builder, baseClass, defaultBuilder);
    } else {
      ClassName finalBuilderClassName = className.nestedClass(builderClassName.simpleName());
      readMethod.addStatement("$T $N = new $T()", builder.type, builder, finalBuilderClassName);
    }

    readMethod.addStatement("$N.beginObject()", jsonReader);

    readMethod.beginControlFlow("while ($N.hasNext())", jsonReader);

    FieldSpec name = FieldSpec.builder(String.class, "name").build();
    readMethod.addStatement("$T $N = $N.nextName()", name.type, name, jsonReader);

    readMethod.beginControlFlow("if ($N.peek() == $T.NULL)", jsonReader, token);
    readMethod.addStatement("$N.nextNull()", jsonReader);
    readMethod.addStatement("continue");
    readMethod.endControlFlow();

    readMethod.beginControlFlow("switch ($N)", name);
    for (Property prop : properties) {
      if (!prop.shouldDeserialize()) {
        continue;
      }

      for (String alternate : prop.serializedNameAlternate()) {
        readMethod.addCode("case $S:\n", alternate);
      }
      readMethod.beginControlFlow("case $S:", prop.serializedName());
      Set<ExecutableElement> setters = builderContext.setters().get(prop.humanName);

      // search overloaded methods for a type match
      Optional<CodeBlock> setterBlock = setters.stream()
          .filter(e -> ClassName.get(e.getParameters().get(0).asType()).equals(prop.type))
          .findFirst()
          .map(method ->
              CodeBlock.builder()
                  .addStatement("$N.$N($N.read($N))", builder, method.getSimpleName(), adapters.get(prop), jsonReader)
                  .build());

      // if the property is Optional<T>, see if there is an overloaded setter that accepts T
      if (!setterBlock.isPresent() && prop.instanceOfOptional) {
        final ClassName optionalPropTypeArgument = (ClassName) ((ParameterizedTypeName) prop.type).typeArguments.get(0);
        setterBlock = setters.stream()
            .filter(e -> ClassName.get(e.getParameters().get(0).asType()).equals(optionalPropTypeArgument))
            .findFirst()
            .map(method -> {
              String propertyVarName = prop.humanName + "Property";
              return CodeBlock.builder()
                  .addStatement("$T $L = $N.read($N)", prop.type, propertyVarName,
                      adapters.get(prop), jsonReader)
                  .beginControlFlow("if ($L.isPresent())", propertyVarName)
                  .addStatement("$N.$N($L.get())", builder, method.getSimpleName(), propertyVarName)
                  .endControlFlow()
                  .build();
            });
      }

      readMethod.addCode(setterBlock.get());
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

    // find the build method to use
    ExecutableElement buildMethod = findBuildMethod(autoValueClassElement, builderContext.builderClass());
    if (buildMethod != null) {
      readMethod.addStatement("return $N.$N()", builder, buildMethod.getSimpleName());
    } else {
      processingEnvironment.getMessager().printMessage(Kind.ERROR,
          String.format("Could not determine the build method in %s. If there is more than one "
                  + "non-private, non-static, zero arg method returning %s, annotate the build method "
                  + "to use with @AutoValueGsonBuild.",
              builderContext.builderClass().getQualifiedName(), autoValueClassName));
    }

    return readMethod.build();
  }

  /** Returns a default value for initializing well-known types, or else {@code null}. */
  private CodeBlock getDefaultValue(Property prop, FieldSpec field) {
    if (field.type.isPrimitive()) {
      String defaultValue = getDefaultPrimitiveValue(field.type);
      if (defaultValue != null) {
        return CodeBlock.of("$L", defaultValue);
      } else {
        return CodeBlock.of("$T.valueOf(null)", field.type, field, field.type.box());
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
    if (collectionsDefaultToEmpty) {
      try {
        Class<?> clazz = Class.forName(typeElement.getQualifiedName()
            .toString());
        if (clazz.isAssignableFrom(List.class)) {
          return CodeBlock.of("$T.emptyList()", TypeName.get(Collections.class));
        } else if (clazz.isAssignableFrom(Map.class)) {
          return CodeBlock.of("$T.emptyMap()", TypeName.get(Collections.class));
        } else if (clazz.isAssignableFrom(Set.class)) {
          return CodeBlock.of("$T.emptySet()", TypeName.get(Collections.class));
        } else if (clazz.isAssignableFrom(ImmutableList.class)) {
          return CodeBlock.of("$T.of()", TypeName.get(ImmutableList.class));
        } else if (clazz.isAssignableFrom(ImmutableMap.class)) {
          return CodeBlock.of("$T.of()", TypeName.get(ImmutableMap.class));
        } else if (clazz.isAssignableFrom(ImmutableSet.class)) {
          return CodeBlock.of("$T.of()", TypeName.get(ImmutableSet.class));
        } else {
          return null;
        }
      } catch (ClassNotFoundException e) {
        return null;
      }
    } else {
      return null;
    }
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

  private CodeBlock makeParameterizedType(TypeName typeName, List<TypeVariableName> typeParams) {
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
    } else {
      block.add("$T.class", typeArg);
    }
  }

  @Nullable
  private static TypeElement findBuilderTypeElement(TypeElement autoValueClass) {
    return ElementFilter.typesIn(autoValueClass.getEnclosedElements())
        .stream()
        .filter(e-> MoreElements.isAnnotationPresent(e, AutoValue.Builder.class))
        .findFirst()
        .orElse(null);
  }

  @Nullable
  private static ExecutableElement findDefaultBuilder(TypeElement autoValueClass, TypeElement builderClass) {
    Set<ExecutableElement> builderMethods = ElementFilter.methodsIn(autoValueClass.getEnclosedElements())
        .stream()
        .filter(MoreElements.hasModifiers(PRIVATE).negate())
        .filter(MoreElements.hasModifiers(Modifier.STATIC))
        .filter(e -> e.getParameters().isEmpty())
        .filter(e -> MoreTypes.equivalence().equivalent(e.getReturnType(), builderClass.asType()))
        .collect(Collectors.toSet());

    if (builderMethods.isEmpty()) {
      return null;
    } else if (builderMethods.size() == 1) {
      return Iterables.getOnlyElement(builderMethods);
    } else {
      // Assume only one annotated method. Multiple matches are caught in applicable() step
      return builderMethods.stream()
          .filter(e -> MoreElements.isAnnotationPresent(e, AutoValueGsonBuilder.class))
          .findFirst().orElse(null);
    }
  }

  private static Predicate<ExecutableElement> isValidBuilderMethod(TypeMirror builderType) {
    return ((Predicate<ExecutableElement>) (e -> MoreTypes.equivalence().equivalent(e.getReturnType(), builderType)))
        .and(MoreElements.hasModifiers(PRIVATE).negate())
        .and(MoreElements.hasModifiers(STATIC))
        .and(e -> e.getParameters().isEmpty());
  }

  /**
   * Searches the builder for a non-private, non-static, zero arg method returning the @AutoValue
   * annotated type.
   */
  @Nullable
  private static ExecutableElement findBuildMethod(
      TypeElement autoValueClass, TypeElement builderElement) {
    Set<ExecutableElement> methods = ElementFilter.methodsIn(builderElement.getEnclosedElements())
        .stream()
        .filter(MoreElements.hasModifiers(STATIC, PRIVATE).negate())
        .filter(e -> e.getParameters().isEmpty())
        .filter(e -> MoreTypes.equivalence().equivalent(e.getReturnType(), autoValueClass.asType()))
        .collect(Collectors.toSet());

    if (methods.size() == 1) {
      return Iterables.getOnlyElement(methods);
    } else {
      Set<ExecutableElement> annotatedMethods = methods.stream()
          .filter(e -> MoreElements.isAnnotationPresent(e, AutoValueGsonBuild.class))
          .collect(Collectors.toSet());

      if (annotatedMethods.size() == 1) {
        return Iterables.getOnlyElement(annotatedMethods);
      }
    }

    return null;
  }

  static boolean isValidTypeAdapter(Context context) {
    Messager messager = context.processingEnvironment().getMessager();

    // check that the class contains a public static method returning a TypeAdapter
    TypeElement type = context.autoValueClass();
    TypeName typeName = TypeName.get(type.asType());
    ParameterizedTypeName typeAdapterType = ParameterizedTypeName.get(
        ClassName.get(TypeAdapter.class), typeName);
    TypeName returnedTypeAdapter = null;
    for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
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
        if (pTypeName.rawType.equals(argument)) {
          return true;
        }
      } else {
        messager.printMessage(Kind.WARNING,
            String.format("Found public static method returning TypeAdapter<%s> on %s class. "
                + "Skipping GsonTypeAdapter generation.", argument, type));
      }
    } else {
      messager.printMessage(Kind.WARNING, "Found public static method returning "
          + "TypeAdapter with no type arguments, skipping GsonTypeAdapter generation.");
    }

    return false;
  }

  static boolean isOptional(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }
    DeclaredType declaredType = MoreTypes.asDeclared(type);
    TypeElement typeElement = MoreElements.asType(declaredType.asElement());
    return OPTIONAL_CLASS_NAMES.contains(typeElement.getQualifiedName().toString())
        && typeElement.getTypeParameters().size() == declaredType.getTypeArguments().size();
  }
}
