package com.ryanharter.auto.value.gson;

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
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
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
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Generated;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@AutoService(AutoValueExtension.class)
public class AutoValueGsonExtension extends AutoValueExtension {

  /** Compiler flag to indicate that collections/maps should default to their empty forms. Default is to default to null. */
  static final String COLLECTIONS_DEFAULT_TO_EMPTY = "autovaluegson.defaultCollectionsToEmpty";

  /** Compiler flag to indicate that generated TypeAdapters should be mutable with setters for defaults. */
  static final String MUTABLE_ADAPTERS_WITH_DEFAULT_SETTERS
      = "autovaluegson.mutableAdaptersWithDefaultSetters";

  private static final String GENERATED_COMMENTS = "https://github.com/rharter/auto-value-gson";

  private static final AnnotationSpec GENERATED =
      AnnotationSpec.builder(Generated.class)
          .addMember("value", "$S", AutoValueGsonExtension.class.getName())
          .addMember("comments", "$S", GENERATED_COMMENTS)
          .build();

  public static class Property {
    final String methodName;
    final String humanName;
    final ExecutableElement element;
    final TypeName type;
    final ImmutableSet<String> annotations;
    final TypeMirror typeAdapter;
    final boolean nullable;

    public Property(String humanName, ExecutableElement element) {
      this.methodName = element.getSimpleName().toString();
      this.humanName = humanName;
      this.element = element;

      type = TypeName.get(element.getReturnType());
      annotations = buildAnnotations(element);
      nullable = nullableAnnotation() != null;

      typeAdapter = getAnnotationValue(element, GsonTypeAdapter.class);
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
      return nullable;
    }

    public String nullableAnnotation() {
      for (String annotationString : annotations) {
        if (annotationString.equals("@Nullable") || annotationString.endsWith(".Nullable")) {
          return annotationString;
        }
      }
      return null;
    }

    private ImmutableSet<String> buildAnnotations(ExecutableElement element) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();

      for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
        builder.add(annotation.getAnnotationType().asElement().toString());
      }

      return builder.build();
    }
  }

  private boolean defaultSetters = false;
  private boolean collectionsDefaultToEmpty = false;

  @Override
  public boolean applicable(Context context) {
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
    ProcessingEnvironment env = context.processingEnvironment();
    defaultSetters = Boolean.parseBoolean(context.processingEnvironment().getOptions()
        .getOrDefault(MUTABLE_ADAPTERS_WITH_DEFAULT_SETTERS, "false"));
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
        .addMethod(generateConstructor(properties, types));

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

  ImmutableMap<TypeName, FieldSpec> createFields(List<Property> properties) {
    ImmutableMap.Builder<TypeName, FieldSpec> fields = ImmutableMap.builder();

    ClassName jsonAdapter = ClassName.get(TypeAdapter.class);
    Set<TypeName> seenTypes = Sets.newHashSet();
    NameAllocator nameAllocator = new NameAllocator();
    for (Property property : properties) {
      if (!property.shouldDeserialize() && !property.shouldSerialize()) {
        continue;
      }
      TypeName type = property.type.isPrimitive() ? property.type.box() : property.type;
      ParameterizedTypeName adp = ParameterizedTypeName.get(jsonAdapter, type);
      if (!seenTypes.contains(property.type)) {
        fields.put(property.type,
                FieldSpec.builder(adp,
                    nameAllocator.newName(simpleName(property.type)) + "_adapter", PRIVATE, FINAL)
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

  private Map<Property, FieldSpec> createDefaultValueFields(List<Property> properties) {
    ImmutableMap.Builder<Property, FieldSpec> builder = ImmutableMap.builder();
    for (Property prop : properties) {
      FieldSpec fieldSpec = FieldSpec.builder(prop.type, "default" + upperCamelizeHumanName(prop), PRIVATE).build();
      CodeBlock defaultValue = getDefaultValue(prop, fieldSpec);
      if (defaultValue == null) {
        defaultValue = CodeBlock.of("null");
      }
      builder.put(prop, fieldSpec.toBuilder()
              .initializer(defaultValue)
              .build());
    }
    return builder.build();
  }

  private String upperCamelizeHumanName(Property prop) {
    return LOWER_CAMEL.to(UPPER_CAMEL, prop.humanName);
  }

  MethodSpec generateConstructor(List<Property> properties, Map<String, TypeName> types) {
    List<ParameterSpec> params = Lists.newArrayList();
      for (Property property : properties) {
          ParameterSpec.Builder builder = ParameterSpec.builder(property.type, property.humanName);
          if (property.nullable()) {
              builder.addAnnotation(ClassName.bestGuess(property.nullableAnnotation()));
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
  Map<String, TypeName> convertPropertiesToTypes(Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<String, TypeName>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      ExecutableElement el = entry.getValue();
      types.put(entry.getKey(), TypeName.get(el.getReturnType()));
    }
    return types;
  }

  public TypeSpec createTypeAdapter(Context context, ClassName className, ClassName autoValueClassName, List<Property> properties, List<TypeVariableName> typeParams) {
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

    Map<Property, FieldSpec> defaultValueFields = Collections.emptyMap();
    if (defaultSetters) {
      defaultValueFields = createDefaultValueFields(properties);
    }
    ImmutableMap<TypeName, FieldSpec> adapters = createFields(properties);
    Set<FieldSpec> initializedFields = Sets.newHashSet();
    for (Property prop : properties) {
      if (defaultSetters && !prop.shouldDeserialize() && !prop.nullable()) {
        // Property should be ignored for deserialization but is not marked as nullable - we require a default value
        constructor.addParameter(prop.type, "default" + upperCamelizeHumanName(prop));
        constructor.addStatement("this.$N = default$L", defaultValueFields.get(prop), upperCamelizeHumanName(prop));
      }
      FieldSpec field = adapters.get(prop.type);
      if ((!prop.shouldDeserialize() && !prop.shouldSerialize()) || initializedFields.contains(field)) {
        continue;
      }
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
      initializedFields.add(field);
    }

    ClassName gsonTypeAdapterName = className.nestedClass("GsonTypeAdapter");

    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(gsonTypeAdapterName)
        .addTypeVariables(typeParams)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .superclass(superClass)
        .addFields(adapters.values())
        .addMethod(constructor.build())
        .addMethod(createWriteMethod(autoValueTypeName, properties, adapters))
        .addMethod(createReadMethod(className, autoValueTypeName, properties, adapters));

    if (defaultSetters) {
        classBuilder.addMethods(createDefaultMethods(gsonTypeAdapterName, properties))
            .addFields(defaultValueFields.values());
    }

    return classBuilder.build();
  }

  public List<MethodSpec> createDefaultMethods(ClassName gsonTypeAdapterName, List<Property> properties) {
    List<MethodSpec> methodSpecs = new ArrayList<>(properties.size());
    for (Property prop : properties) {
      ParameterSpec valueParam = ParameterSpec.builder(prop.type, "default" + upperCamelizeHumanName(prop)).build();

      methodSpecs.add(MethodSpec.methodBuilder("setDefault" + upperCamelizeHumanName(prop))
              .addModifiers(PUBLIC)
              .addParameter(valueParam)
              .returns(gsonTypeAdapterName)
              .addCode(CodeBlock.builder()
                      .addStatement("this.default$L = $N", upperCamelizeHumanName(prop), valueParam)
                      .addStatement("return this")
                      .build())
              .build());
    }
    return methodSpecs;
  }

  public MethodSpec createWriteMethod(TypeName autoValueClassName,
                                      List<Property> properties,
                                      ImmutableMap<TypeName, FieldSpec> adapters) {
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
    for (Property prop : properties) {
      if (!prop.shouldSerialize()) {
        continue;
      }
      FieldSpec field = adapters.get(prop.type);
      writeMethod.addStatement("$N.name($S)", jsonWriter, prop.serializedName());
      writeMethod.addStatement("$N.write($N, $N.$N())", field, jsonWriter, annotatedParam, prop.methodName);
    }
    writeMethod.addStatement("$N.endObject()", jsonWriter);

    return writeMethod.build();
  }

  public MethodSpec createReadMethod(ClassName className,
                                     TypeName autoValueClassName,
                                     List<Property> properties,
                                     ImmutableMap<TypeName, FieldSpec> adapters) {
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

      if (defaultSetters) {
        readMethod.addStatement("$T $N = this.default$L", field.type, field.name, upperCamelizeHumanName(prop));
      } else {
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
      if (!prop.shouldDeserialize()) {
        continue;
      }
      FieldSpec field = fields.get(prop);

      for (String alternate : prop.serializedNameAlternate()) {
        readMethod.addCode("case $S:\n", alternate);
      }
      readMethod.beginControlFlow("case $S:", prop.serializedName());
      readMethod.addStatement("$N = $N.read($N)", field, adapters.get(prop.type), jsonReader);
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

  /** Returns a default value for initializing well-known types, or else {@code null}. */
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
}
