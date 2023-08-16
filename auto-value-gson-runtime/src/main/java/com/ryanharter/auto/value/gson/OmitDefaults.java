package com.ryanharter.auto.value.gson;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * If present, optional properties at their default values will be omitted from the serialized JSON.
 *
 * <p>Requires the class to provide a static builder method returning an instance of the
 * corresponding {@link com.google.auto.value.AutoValue.Builder} with the optional properties set
 * to their default values.
 *
 * <p>A property is considered optional if
 * <ul>
 *   <li>a value is set for it on the builder returned by the static builder method and
 *   <li>the builder defines a getter method for it ({@see
 *   <a href="https://github.com/google/auto/blob/main/value/userguide/builders-howto.md#-normalize-modify-a-property-value-at-build-time">Auto Value documentation</a>}).
 *   Builder getters returning {@link java.util.Optional} wrappers are not (yet) supported.
 * </ul>
 *
 */
@Retention(CLASS)
@Target(TYPE)
public @interface OmitDefaults {
}
