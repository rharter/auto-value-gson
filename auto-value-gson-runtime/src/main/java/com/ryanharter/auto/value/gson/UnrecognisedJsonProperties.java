// modified by mapbox
package com.ryanharter.auto.value.gson;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * If present, indicates that the annotated method should be used for retrieving an instance of the
 * AutoValue.Builder. Only necessary if there is more than one builder method.
 */
@Retention(CLASS)
@Target(METHOD)
public @interface UnrecognisedJsonProperties {
}
