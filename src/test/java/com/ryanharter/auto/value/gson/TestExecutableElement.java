package com.ryanharter.auto.value.gson;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Created by rharter on 7/22/15.
 */
public class TestExecutableElement implements ExecutableElement {

  private List<? extends TypeParameterElement> typeParameters;
  private TypeMirror returnType;
  private List<? extends VariableElement> parameters;
  private TypeMirror receiverType;
  private boolean isVarArgs;
  private boolean isDefault;
  private List<? extends TypeMirror> thrownTypes;
  private AnnotationValue defaultValue;
  private ElementKind kind;
  private TypeMirror type;
  private Set<Modifier> modifiers;
  private Name simpleName;
  private Element enclosingElement;
  private List<? extends Element> enclosedElements;
  private List<? extends AnnotationMirror> annotationMirrors;
  private Annotation annotation;

  public TestExecutableElement(List<? extends TypeParameterElement> typeParameters, TypeMirror returnType, List<? extends VariableElement> parameters, TypeMirror receiverType, boolean isVarArgs, boolean isDefault, List<? extends TypeMirror> thrownTypes, AnnotationValue defaultValue, ElementKind kind, TypeMirror type, Set<Modifier> modifiers, Name simpleName, Element enclosingElement, List<? extends Element> enclosedElements, List<? extends AnnotationMirror> annotationMirrors, Annotation annotation) {
    this.typeParameters = typeParameters;
    this.returnType = returnType;
    this.parameters = parameters;
    this.receiverType = receiverType;
    this.isVarArgs = isVarArgs;
    this.isDefault = isDefault;
    this.thrownTypes = thrownTypes;
    this.defaultValue = defaultValue;
    this.kind = kind;
    this.type = type;
    this.modifiers = modifiers;
    this.simpleName = simpleName;
    this.enclosingElement = enclosingElement;
    this.enclosedElements = enclosedElements;
    this.annotationMirrors = annotationMirrors;
    this.annotation = annotation;
  }

  @Override
  public List<? extends TypeParameterElement> getTypeParameters() {
    return typeParameters;
  }

  @Override
  public TypeMirror getReturnType() {
    return returnType;
  }

  @Override
  public List<? extends VariableElement> getParameters() {
    return parameters;
  }

  @Override
  public TypeMirror getReceiverType() {
    return receiverType;
  }

  @Override
  public boolean isVarArgs() {
    return isVarArgs;
  }

  @Override
  public boolean isDefault() {
    return isDefault;
  }

  @Override
  public List<? extends TypeMirror> getThrownTypes() {
    return thrownTypes;
  }

  @Override
  public AnnotationValue getDefaultValue() {
    return defaultValue;
  }

  @Override
  public TypeMirror asType() {
    return type;
  }

  @Override
  public ElementKind getKind() {
    return kind;
  }

  @Override
  public Set<Modifier> getModifiers() {
    return modifiers;
  }

  @Override
  public Name getSimpleName() {
    return simpleName;
  }

  @Override
  public Element getEnclosingElement() {
    return enclosingElement;
  }

  @Override
  public List<? extends Element> getEnclosedElements() {
    return enclosedElements;
  }

  @Override
  public List<? extends AnnotationMirror> getAnnotationMirrors() {
    return (List<? extends AnnotationMirror>) annotationMirrors;
  }

  @Override
  public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
    return (A) annotation;
  }

  @Override
  public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
    return null;
  }

  @Override
  public <R, P> R accept(ElementVisitor<R, P> v, P p) {
    return null;
  }

  public static class Builder {
    private List<? extends TypeParameterElement> typeParameters;
    private TypeMirror returnType;
    private List<? extends VariableElement> parameters;
    private TypeMirror receiverType;
    private boolean isVarArgs;
    private boolean isDefault;
    private List<? extends TypeMirror> thrownTypes;
    private AnnotationValue defaultValue;
    private ElementKind kind;
    private TypeMirror type;
    private Set<Modifier> modifiers;
    private Name simpleName;
    private Element enclosingElement;
    private List<? extends Element> enclosedElements;
    private List<? extends AnnotationMirror> annotationMirrors;
    private Annotation annotation;

    public Builder typeParameters(List<? extends TypeParameterElement> typeParameters) {
      this.typeParameters = typeParameters;
      return this;
    }

    public Builder returnType(TypeMirror returnType) {
      this.returnType = returnType;
      return this;
    }

    public Builder parameters(List<? extends VariableElement> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder receiverType(TypeMirror receiverType) {
      this.receiverType = receiverType;
      return this;
    }

    public Builder isVarArgs(boolean isVarArgs) {
      this.isVarArgs = isVarArgs;
      return this;
    }

    public Builder isDefault(boolean isDefault) {
      this.isDefault = isDefault;
      return this;
    }

    public Builder thrownTypes(List<? extends TypeMirror> thrownTypes) {
      this.thrownTypes = thrownTypes;
      return this;
    }

    public Builder defaultValue(AnnotationValue defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public Builder kind(ElementKind kind) {
      this.kind = kind;
      return this;
    }

    public Builder type(TypeMirror type) {
      this.type = type;
      return this;
    }

    public Builder modifiers(Set<Modifier> modifiers) {
      this.modifiers = modifiers;
      return this;
    }

    public Builder simpleName(Name simpleName) {
      this.simpleName = simpleName;
      return this;
    }

    public Builder enclosingElement(Element enclosingElement) {
      this.enclosingElement = enclosingElement;
      return this;
    }

    public Builder enclosedElements(List<? extends Element> enclosedElements) {
      this.enclosedElements = enclosedElements;
      return this;
    }

    public Builder annotationMirrors(List<? extends AnnotationMirror> annotationMirrors) {
      this.annotationMirrors = annotationMirrors;
      return this;
    }

    public Builder annotation(Annotation annotation) {
      this.annotation = annotation;
      return this;
    }

    public TestExecutableElement build() {
      return new TestExecutableElement(typeParameters, returnType, parameters, receiverType, isVarArgs, isDefault, thrownTypes, defaultValue, kind, type, modifiers, simpleName, enclosingElement, enclosedElements, annotationMirrors, annotation);
    }
  }

}