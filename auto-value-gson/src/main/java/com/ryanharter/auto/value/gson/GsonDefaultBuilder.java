package com.ryanharter.auto.value.gson;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * If present, indicates that the annotated method should be used for retrieving an instance of the
 * AutoValue.Builder. This provides a mechanism for supplying default values.
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface GsonDefaultBuilder {
}
