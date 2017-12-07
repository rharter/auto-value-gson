package com.ryanharter.auto.value.gson;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotate a given AutoValue-annotated type to indicate to auto-value-gson to generate its
 * TypeAdapter in a separate class rather than an inner class of an intermediate AutoValue-generated
 * class hierarchy.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface GenerateTypeAdapter {
}
