package com.ryanharter.auto.value.gson;

import com.google.auto.value.AutoValue;
import com.google.gson.TypeAdapter;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Annotation applied to a field inside an {@link AutoValue} class.
 * Indicates that when an auto-value-gson {@link TypeAdapter} creates an instance of the class the
 * field should have the value contained in this annotation when one is not present.
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface DefaultValue {
  String value();
}
