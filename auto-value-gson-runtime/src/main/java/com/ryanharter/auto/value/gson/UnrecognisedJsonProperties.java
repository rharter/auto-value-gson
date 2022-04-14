// modified by mapbox
package com.ryanharter.auto.value.gson;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Indicates that a filed of type Map<String, Object> is a container for unrecognised JSON properties.
 */
@Retention(CLASS)
@Target(METHOD)
public @interface UnrecognisedJsonProperties {
}
