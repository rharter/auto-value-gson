package com.ryanharter.auto.value.gson;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * If present, will indicate which builder build method to use. Only necessary if the build method
 * is not named "build".
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface GsonBuild {

}
