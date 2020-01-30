package com.ryanharter.auto.value.gson.factory;

import com.google.auto.common.GeneratedAnnotations;
import com.google.auto.common.Visibility;
import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.ryanharter.auto.value.gson.AutoValueGsonExtension;
import com.ryanharter.auto.value.gson.ExposeToGsonTypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;
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
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
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
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;

import static com.google.auto.common.MoreElements.getPackage;
import static com.ryanharter.auto.value.gson.AutoValueGsonExtension.GENERATED_COMMENTS;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.AGGREGATING;

/**
 * Generates a Gson {@link TypeAdapterFactory} that adapts all {@link AutoValue} annotated
 * Gson serializable classes.
 */
@IncrementalAnnotationProcessor(AGGREGATING)
@AutoService(Processor.class)
public class AutoValueGsonAdapterFactoryProcessor extends AbstractProcessor {

  private Types typeUtils;
  private Elements elementUtils;

  @Override public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(
        AutoValue.class.getName(),
        GsonTypeAdapterFactory.class.getName(),
        ExposeToGsonTypeAdapterFactory.class.getName()
    );
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
  }

  private List<TypeElement> extractApplicableElements(Set<? extends Element> elements) {
    return elements.stream()
        .map(e -> (TypeElement) e)
        .filter(e -> AutoValueGsonExtension.isApplicable(e, processingEnv.getMessager()))
        .sorted((o1, o2) -> {
          final String o1Name = classNameOf(o1, ".");
          final String o2Name = classNameOf(o2, ".");
          return o1Name.compareTo(o2Name);
        })
        .collect(Collectors.toList());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Set<? extends Element> adapterFactories = roundEnv.getElementsAnnotatedWith(GsonTypeAdapterFactory.class);
    if (adapterFactories.isEmpty()) {
      return false;
    }
    Set<? extends Element> autoValueElements = roundEnv.getElementsAnnotatedWith(AutoValue.class);
    Set<? extends Element> exposedAdapterElements = roundEnv.getElementsAnnotatedWith(ExposeToGsonTypeAdapterFactory.class);
    List<TypeElement> elements = ImmutableList.<TypeElement>builder()
        .addAll(extractApplicableElements(autoValueElements))
        .addAll(extractApplicableElements(exposedAdapterElements))
        .build();

    if (elements.isEmpty()) {
      Element reportableElement = adapterFactories.iterator().next();
      if (!autoValueElements.isEmpty() || !exposedAdapterElements.isEmpty()) {
        processingEnv.getMessager().printMessage(ERROR,
            "Failed to write TypeAdapterFactory: Cannot generate class for this "
                + "@GsonTypeAdapterFactory-annotated element because while @AutoValue-annotated "
                + "or @ExposeToGsonTypeAdapterFactory elements were found on the compilation "
                + "classpath, none of them contain a requisite public static TypeAdapter-returning "
                + "signature method to opt in to being included in @GsonTypeAdapterFactory-generated "
                + "factories. See the auto-value-gson README for more information on declaring these.",
            reportableElement);
      } else {
        processingEnv.getMessager().printMessage(ERROR,
            "Failed to write TypeAdapterFactory: Cannot generate class for this "
                + "@GsonTypeAdapterFactory-annotated element because no @AutoValue-annotated "
                + "or @ExposeToGsonTypeAdapterFactory elements were found on the compilation classpath.",
            reportableElement);
      }
      return false;
    }

    for (Element element : adapterFactories) {
      if (!element.getModifiers().contains(ABSTRACT)) {
        error(element, "Must be abstract!");
      }
      TypeElement type = (TypeElement) element; // Safe to cast because this is only applicable on types anyway
      if (!implementsTypeAdapterFactory(type)) {
        error(element, "Must implement TypeAdapterFactory!");
      }
      String adapterName = classNameOf(type, "_");
      String qualifiedName = classNameOf(type, ".");
      PackageElement packageElement = packageElementOf(type);
      String packageName = packageElement.getQualifiedName().toString();
      List<TypeElement> applicableElements = elements.stream()
          .filter(e -> {
            Visibility typeVisibility = Visibility.ofElement(e);
            switch (typeVisibility) {
              case PRIVATE:
                return false;
              case DEFAULT:
              case PROTECTED:
                //noinspection UnstableApiUsage
                if (!getPackage(e).equals(packageElement)) {
                  return false;
                }
                break;
            }
            // If we got here, the class is visible. Now check the typeAdapter method
            ExecutableElement adapterMethod = getTypeAdapterMethod(e);
            if (adapterMethod == null) {
              return false;
            }
            Visibility methodVisibility = Visibility.ofElement(adapterMethod);
            switch (methodVisibility) {
              case PRIVATE:
                return false;
              case DEFAULT:
              case PROTECTED:
                //noinspection UnstableApiUsage
                if (!getPackage(adapterMethod).equals(packageElement)) {
                  return false;
                }
                break;
            }
            return true;
          })
          .collect(toList());


      TypeSpec typeAdapterFactory = createTypeAdapterFactory(type, applicableElements, packageName,
              adapterName, qualifiedName);
      JavaFile file = JavaFile.builder(packageName, typeAdapterFactory).build();
      try {
        file.writeTo(processingEnv.getFiler());
      } catch (IOException e) {
        processingEnv.getMessager().printMessage(ERROR, "Failed to write TypeAdapterFactory: " + e.getLocalizedMessage(), element);
      }
    }

    // return false so other processors can consume the @AutoValue annotation
    return false;
  }

  private static AnnotationSpec createGeneratedAnnotationSpec(TypeElement generatedAnnotationTypeElement) {
    return AnnotationSpec.builder(ClassName.get(generatedAnnotationTypeElement))
        .addMember("value", "$S", AutoValueGsonAdapterFactoryProcessor.class.getName())
        .addMember("comments", "$S", GENERATED_COMMENTS)
        .build();
  }

  private TypeSpec createTypeAdapterFactory(
      TypeElement sourceElement,
      List<TypeElement> elements,
      String packageName,
      String adapterName,
      String qualifiedName) {
    TypeSpec.Builder factory = TypeSpec.classBuilder(
        ClassName.get(packageName, "AutoValueGson_" + adapterName));
    Optional<AnnotationSpec> generatedAnnotationSpec =
        GeneratedAnnotations.generatedAnnotation(processingEnv.getElementUtils(),
            processingEnv.getSourceVersion())
            .map(AutoValueGsonAdapterFactoryProcessor::createGeneratedAnnotationSpec);
    generatedAnnotationSpec.ifPresent(factory::addAnnotation);

    factory.addOriginatingElement(sourceElement);
    factory.addModifiers(FINAL);
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
        .addStatement("Class<?> rawType = $N.getRawType()", type);

    List<Pair<TypeElement, ExecutableElement>> properties = elements.stream()
        .peek(factory::addOriginatingElement)
        .map(e -> Pair.create(e, getTypeAdapterMethod(e)))
        .filter(entry -> entry.second != null)
        .collect(Collectors.toList());

    for (int i = 0, elementsSize = properties.size(); i < elementsSize; i++) {
      Pair<TypeElement, ExecutableElement> pair = properties.get(i);
      Element element = pair.first;
      TypeName elementType = rawType(element);
      if (i == 0) {
        create.beginControlFlow("if ($T.class.isAssignableFrom(rawType))", elementType);
      } else {
        create.nextControlFlow("else if ($T.class.isAssignableFrom(rawType))", elementType);
      }
      ExecutableElement typeAdapterMethod = pair.second;
      List<? extends VariableElement> params = typeAdapterMethod.getParameters();
      if (params == null || params.size() == 0) {
        create.addStatement("return (TypeAdapter<$T>) $T." + typeAdapterMethod.getSimpleName() + "()", t,
            elementType);
      } else if (params.size() == 1) {
        create.addStatement("return (TypeAdapter<$T>) $T." + typeAdapterMethod.getSimpleName() + "($N)", t, elementType, gson);
      } else {
        create.addStatement("return (TypeAdapter<$T>) $T." + typeAdapterMethod.getSimpleName() + "($N, (($T) $N.getType()).getActualTypeArguments())",
            t,
            elementType,
            gson,
            ParameterizedType.class,
            type);
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

  private ExecutableElement getTypeAdapterMethod(TypeElement element) {
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
    StringBuilder name = new StringBuilder(type.getSimpleName().toString());
    while (type.getEnclosingElement() instanceof TypeElement) {
      type = (TypeElement) type.getEnclosingElement();
      name.insert(0, type.getSimpleName() + delimiter);
    }
    return name.toString();
  }

  /**
   * Returns the package element that the given type is in. If the type is in the default
   * (unnamed) package then the name is the empty string.
   */
  private static PackageElement packageElementOf(TypeElement type) {
    //noinspection UnstableApiUsage
    return getPackage(type);
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
