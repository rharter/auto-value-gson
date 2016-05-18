package com.ryanharter.auto.value.gson;

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
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
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Generates a Gson {@link TypeAdapterFactory} that adapts all {@link AutoValue} annotated
 * Gson serializable classes.
 */
@AutoService(Processor.class)
public class AutoValueGsonAdapterFactoryProcessor extends AbstractProcessor {

  private final AutoValueGsonExtension extension = new AutoValueGsonExtension();

  @Override public Set<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(AutoValue.class.getName());
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
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
      TypeSpec typeAdapterFactory = createTypeAdapterFactory(elements);
      JavaFile file = JavaFile.builder("com.ryanharter.auto.value.gson", typeAdapterFactory).build();
      try {
        file.writeTo(processingEnv.getFiler());
      } catch (IOException e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
            "Failed to write TypeAdapterFactory: " + e.getLocalizedMessage());
      }
    }

    // return false so other processors can consume the @AutoValue annotation
    return false;
  }

  private TypeSpec createTypeAdapterFactory(List<Element> elements) {
    TypeSpec.Builder factory = TypeSpec.classBuilder(
        ClassName.get("com.ryanharter.auto.value.gson", "AutoValueGsonTypeAdapterFactory"));
    factory.addModifiers(PUBLIC, FINAL);
    factory.addSuperinterface(TypeName.get(TypeAdapterFactory.class));

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
        .addParameters(ImmutableSet.of(gson, type))
        .returns(result)
        .addStatement("Class<$T> rawType = (Class<$T>) $N.getRawType()", t, t, type);

    for (int i = 0, elementsSize = elements.size(); i < elementsSize; i++) {
      Element element = elements.get(i);
      if (i == 0) {
        create.beginControlFlow("if ($T.class.isAssignableFrom(rawType))", element);
      } else {
        create.nextControlFlow("else if ($T.class.isAssignableFrom(rawType))", element);
      }
      ExecutableElement typeAdapterMethod = getTypeAdapterMethod(element);
      create.addStatement("return (TypeAdapter<$T>) $T." + typeAdapterMethod.getSimpleName() + "($N)", t, element, gson);
    }
    create.nextControlFlow("else");
    create.addStatement("return null");
    create.endControlFlow();

    factory.addMethod(create.build());
    return factory.build();
  }

  private ExecutableElement getTypeAdapterMethod(Element element) {
    ParameterizedTypeName typeAdapterType = ParameterizedTypeName
        .get(ClassName.get(TypeAdapter.class), TypeName.get(element.asType()));
    for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
      if (method.getModifiers().contains(Modifier.STATIC)
          && method.getModifiers().contains(Modifier.PUBLIC)) {
        TypeName returnType = TypeName.get(method.getReturnType());
        if (returnType.equals(typeAdapterType)) {
          return method;
        }
      }
    }
    return null;
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
  }
}
