package com.ryanharter.auto.value.gson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If present on a property, defines whether the property should be ignored when serializing and/or
 * deserializing with the generated Gson TypeAdapter.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Ignore {
  /**
   * Returns a {@link Type} indicating whether this field should be ignored for serialization only,
   * deserialization only, or both. The default is {@link Type#BOTH}.
   *
   * @see Type
   */
  Type value() default Type.BOTH;

  enum Type {
    /**
     * Ignore this field for serialization only. The annotated property will be ignored by the
     * generated Gson TypeAdapter when generating a JSON serialization.
     */
    SERIALIZATION,
    /**
     * Ignore this field for deserialization only. The annotated property will be ignored by the
     * generated Gson TypeAdapter when creating an object from JSON, and instead the default value
     * for this property will be used.
     * <p>
     * If this field is not marked as nullable, a default value must be supplied to the generated
     * TypeAdapter's constructor.
     */
    DESERIALIZATION,
    /**
     * Ignore this field for both serialization and deserialization.
     * <p>
     * If this field is not marked as nullable, a default value must be supplied to the generated
     * TypeAdapter's constructor.
     * <p>
     * This is the default value for the Ignore annotation.
     */
    BOTH
  }
}
