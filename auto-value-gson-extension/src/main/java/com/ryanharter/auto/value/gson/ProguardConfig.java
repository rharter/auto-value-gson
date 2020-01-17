package com.ryanharter.auto.value.gson;

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.ClassName;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

/**
 * Represents a proguard configuration for a given spec. This covers two main areas:
 * <ul>
 * <li>Keeping the target class name to GenerateTypeAdapter.FACTORY's reflective lookup of the adapter.</li>
 * <li>Keeping the generated adapter class name + public constructor for reflective lookup.</li>
 * </ul>
 * <p>
 * Each rule is intended to be as specific and targeted as possible to reduce footprint, and each is
 * conditioned on usage of the original target type.
 * <p>
 * To keep this processor as an {@code ISOLATING} incremental processor, we generate one file per
 * target class with a deterministic name (see {@link #outputFile}) with an appropriate originating
 * element.
 */
@AutoValue
abstract class ProguardConfig {
  abstract ClassName targetClass();
  abstract ClassName adapterName();
  abstract List<String> adapterConstructorParams();
  abstract String outputFile();

  static ProguardConfig create(
      ClassName targetClass,
      ClassName adapterName,
      List<String> adapterConstructorParams) {
    String outputFile = "META-INF/proguard/avg-" + targetClass.canonicalName() + ".pro";
    return new AutoValue_ProguardConfig(targetClass,
        adapterName,
        adapterConstructorParams,
        outputFile);
  }

  /** Writes this to {@code filer}. */
  final void writeTo(Filer filer, Element... originatingElements) throws IOException {
    try (Writer writer = filer.createResource(CLASS_OUTPUT, "", outputFile(), originatingElements)
        .openWriter()) {
      writeTo(writer);
    }
  }

  private void writeTo(Appendable out) throws IOException {
    //
    // -if class {the target class}
    // -keepnames class {the target class}
    // -if class {the target class}
    // -keep class {the generated adapter} {
    //    <init>(...);
    // }
    //
    String targetName = targetClass().canonicalName();
    String adapterCanonicalName = adapterName().canonicalName();
    // Keep the class name for GenerateTypeAdapter.FACTORY's reflective lookup based on it
    out.append("-if class ")
        .append(targetName)
        .append("\n");
    out.append("-keepnames class ")
        .append(targetName)
        .append("\n");
    out.append("-if class ")
        .append(targetName)
        .append("\n");
    out.append("-keep class ")
        .append(adapterCanonicalName)
        .append(" {\n");

    // Keep the constructor for GenerateTypeAdapter.FACTORY's reflective lookup
    String constructorArgs = String.join(",", adapterConstructorParams());
    out.append("    <init>(")
        .append(constructorArgs)
        .append(");\n");
    out.append("}\n");
  }
}
