package com.ryanharter.auto.value.gson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If present on a property, defines whether the property should be ignored when serializing or
 * deserializing with the generated Gson TypeAdapter.
 * <p>
 * Absence of this annotation is the same as annotating
 * {@code @Ignore(serialization = false, deserialization = false)} (i.e. don't ignore this field).
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Ignore {
  /**
   * If true, the annotated property will be ignored by the generated Gson TypeAdapter when
   * generating a JSON serialization. The default is true.
   */
  boolean serialization() default true;

  /**
   * If true, the annotated property will be ignored by the generated Gson TypeAdapter when
   * creating an object from JSON, and instead the default value for this property will be used.
   * If this field is not marked as nullable, a default value must be supplied to the generated
   * TypeAdapter's constructor.
   * <p>
   * The default is true.
   */
  boolean deserialization() default true;
}
