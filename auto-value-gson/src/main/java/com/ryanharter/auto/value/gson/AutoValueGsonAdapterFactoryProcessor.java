package com.ryanharter.auto.value.gson;

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * Generates a Gson {@link TypeAdapterFactory} that adapts all {@link AutoValue} annotated
 * Gson serializable classes.
 */
@AutoService(Processor.class)
public class AutoValueGsonAdapterFactoryProcessor extends AbstractProcessor {

  private final AutoValueGsonExtension extension = new AutoValueGsonExtension();
  private Types typeUtils;
  private Elements elementUtils;

  @Override public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(AutoValue.class.getName(), GsonTypeAdapterFactory.class.getName());
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<Element> elements = new LinkedList<>();
    for (Element element : roundEnv.getElementsAnnotatedWith(AutoValue.class)) {
      AutoValueExtension.Context context = new LimitedContext(processingEnv, (TypeElement) element);
      if (extension.applicable(context)) {
        elements.add(element);
      }
    }

    if (!elements.isEmpty()) {
      Set<? extends Element> adaptorFactories = roundEnv.getElementsAnnotatedWith(GsonTypeAdapterFactory.class);
      for (Element element : adaptorFactories) {
        if (!element.getModifiers().contains(ABSTRACT)) {
          error(element, "Must be abstract!");
        }
        TypeElement type = (TypeElement) element; // Safe to cast because this is only applicable on types anyway
        if (!implementsTypeAdapterFactory(type)) {
          error(element, "Must implement TypeAdapterFactory!");
        }
        String adapterName = classNameOf(type, "_");
        String qualifiedName = classNameOf(type, ".");
        String packageName = packageNameOf(type);
        TypeSpec typeAdapterFactory = createTypeAdapterFactory(elements, packageName,
                adapterName, qualifiedName);
        JavaFile file = JavaFile.builder(packageName, typeAdapterFactory).build();
        try {
          file.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
          processingEnv.getMessager().printMessage(ERROR, "Failed to write TypeAdapterFactory: " + e.getLocalizedMessage());
        }
      }
    }

    // return false so other processors can consume the @AutoValue annotation
    return false;
  }

  private TypeSpec createTypeAdapterFactory(
      List<Element> elements,
      String packageName,
      String adapterName,
      String qualifiedName) {
    TypeSpec.Builder factory = TypeSpec.classBuilder(
        ClassName.get(packageName, "AutoValueGson_" + adapterName));
    factory.addModifiers(PUBLIC, FINAL);
    factory.superclass(ClassName.get(packageName, qualifiedName));

    ParameterSpec gson = ParameterSpec.builder(Gson.class, "gson").build();
    TypeVariableName t = TypeVariableName.get("T");
    ParameterSpec type = ParameterSpec
        .builder(ParameterizedTypeName.get(ClassName.get(TypeToken.class), t), "type")
        .build();
    ParameterizedTypeName result = ParameterizedTypeName.get(ClassName.get(TypeAdapter.class), t);
    MethodSpec.Builder create = MethodSpec.methodBuilder("create")
        .addModifiers(PUBLIC)
        .addTypeVariable(t)
        .addAnnotation(Override.class)
        .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
            .addMember("value", "\"unchecked\"")
            .build())
        .addParameters(ImmutableSet.of(gson, type))
        .returns(result)
        .addStatement("Class<$T> rawType = (Class<$T>) $N.getRawType()", t, t, type);

    List<Pair<Element, ExecutableElement>> properties = elements.stream()
        .map(e -> Pair.create(e, getTypeAdapterMethod(e)))
        .filter(entry -> entry.second != null)
        .collect(Collectors.toList());

    for (int i = 0, elementsSize = properties.size(); i < elementsSize; i++) {
      Pair<Element, ExecutableElement> pair = properties.get(i);
      Element element = pair.first;
      TypeName elementType = rawType(element);
      if (i == 0) {
        create.beginControlFlow("if ($T.class.isAssignableFrom(rawType))", elementType);
      } else {
        create.nextControlFlow("else if ($T.class.isAssignableFrom(rawType))", elementType);
      }
      //noinspection ConstantConditions We've filtered absent ones
      ExecutableElement typeAdapterMethod = pair.second;
      List<? extends VariableElement> params = typeAdapterMethod.getParameters();
      if (params != null && params.size() == 1) {
        create.addStatement("return (TypeAdapter<$T>) $T." + typeAdapterMethod.getSimpleName() + "($N)", t, elementType, gson);
      } else {
        create.addStatement("return (TypeAdapter<$T>) $T." + typeAdapterMethod.getSimpleName() + "($N, ($T) $N)", t, elementType, gson, params.get(1), type);
      }
    }
    create.nextControlFlow("else");
    create.addStatement("return null");
    create.endControlFlow();

    factory.addMethod(create.build());
    return factory.build();
  }

  private TypeName rawType(Element element) {
    TypeName type = TypeName.get(element.asType());
    if (type instanceof ParameterizedTypeName) {
      type = ((ParameterizedTypeName) type).rawType;
    }
    return type;
  }

  private ExecutableElement getTypeAdapterMethod(Element element) {
    TypeName type = TypeName.get(element.asType());
    ParameterizedTypeName typeAdapterType = ParameterizedTypeName
        .get(ClassName.get(TypeAdapter.class), type);
    for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
      if (method.getModifiers().contains(STATIC) && !method.getModifiers().contains(PRIVATE)) {
        TypeName returnType = TypeName.get(method.getReturnType());
        if (returnType.equals(typeAdapterType)) {
          return method;
        } else if (returnType instanceof ParameterizedTypeName) {
          ParameterizedTypeName paramReturnType = (ParameterizedTypeName) returnType;
          TypeName argument = paramReturnType.typeArguments.get(0);

          // If the original type uses generics, user's don't have to nest the generic type args
          if (type instanceof ParameterizedTypeName) {
            ParameterizedTypeName pTypeName = (ParameterizedTypeName) type;
            if (pTypeName.rawType.equals(argument)) {
              return method;
            }
          }
        }
      }
    }
    return null;
  }

  private void error(Element element, String message, Object... args) {
    if (args.length > 0) {
      message = String.format(message, args);
    }
    processingEnv.getMessager().printMessage(ERROR, message, element);
  }

  private boolean implementsTypeAdapterFactory(TypeElement type) {
    TypeMirror typeAdapterFactoryType
        = elementUtils.getTypeElement(TypeAdapterFactory.class.getCanonicalName()).asType();
    TypeMirror typeMirror = type.asType();
    if (!type.getInterfaces().isEmpty() || typeMirror.getKind() != TypeKind.NONE) {
      while (typeMirror.getKind() != TypeKind.NONE) {
        if (searchInterfacesAncestry(typeMirror, typeAdapterFactoryType)) {
          return true;
        }
        type = (TypeElement) typeUtils.asElement(typeMirror);
        typeMirror = type.getSuperclass();
      }
    }
    return false;
  }

  private boolean searchInterfacesAncestry(TypeMirror rootIface, TypeMirror target) {
    TypeElement rootIfaceElement = (TypeElement) typeUtils.asElement(rootIface);
    // check if it implements valid interfaces
    for (TypeMirror iface : rootIfaceElement.getInterfaces()) {
      TypeElement ifaceElement = (TypeElement) typeUtils.asElement(rootIface);
      while (iface.getKind() != TypeKind.NONE) {
        if (typeUtils.isSameType(iface, target)) {
          return true;
        }
        // go up
        if (searchInterfacesAncestry(iface, target)) {
          return true;
        }
        // then move on
        iface = ifaceElement.getSuperclass();
      }
    }
    return false;
  }

  /**
   * Returns the name of the given type, including any enclosing types but not the package, separated
   * by a delimiter.
   */
  private static String classNameOf(TypeElement type, String delimiter) {
    String name = type.getSimpleName().toString();
    while (type.getEnclosingElement() instanceof TypeElement) {
      type = (TypeElement) type.getEnclosingElement();
      name = type.getSimpleName() + delimiter + name;
    }
    return name;
  }

  /**
   * Returns the name of the package that the given type is in. If the type is in the default
   * (unnamed) package then the name is the empty string.
   */
  private static String packageNameOf(TypeElement type) {
    while (true) {
      Element enclosing = type.getEnclosingElement();
      if (enclosing instanceof PackageElement) {
        return ((PackageElement) enclosing).getQualifiedName().toString();
      }
      type = (TypeElement) enclosing;
    }
  }

  private static class LimitedContext implements AutoValueExtension.Context {
    private final ProcessingEnvironment processingEnvironment;
    private final TypeElement autoValueClass;

    public LimitedContext(ProcessingEnvironment processingEnvironment, TypeElement autoValueClass) {
      this.processingEnvironment = processingEnvironment;
      this.autoValueClass = autoValueClass;
    }

    @Override public ProcessingEnvironment processingEnvironment() {
      return processingEnvironment;
    }

    @Override public String packageName() {
      return processingEnvironment().getElementUtils()
          .getPackageOf(autoValueClass).getQualifiedName().toString();
    }

    @Override public TypeElement autoValueClass() {
      return autoValueClass;
    }

    @Override public Map<String, ExecutableElement> properties() {
      return null;
    }

    @Override public Set<ExecutableElement> abstractMethods() {
      return null;
    }
  }

  private static class Pair<F, S> {
    private final F first;
    private final S second;

    private Pair(F first, S second) {
      this.first = first;
      this.second = second;
    }

    static <F, S> Pair<F, S> create(F first, S second) {
      return new Pair<>(first, second);
    }
  }
}
