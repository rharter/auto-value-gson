package com.ryanharter.auto.value.gson.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an <i>AutoValue</i> annotated type for proper Gson serialization.
 * <p>
 * This annotation is needed because the {@linkplain Retention retention} of
 * <i>AutoValue</i> does not allow reflection at runtime.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoGson {

    /**
     * A reference to the Auto*-generated {@link com.google.gson.TypeAdapter} class
     * (e.g. AutoValue_Person.GsonTypeAdapter.class). This is necessary to handle obfuscation
     * of the class names.
     *
     * @return the annotated class's TypeAdapter type.
     */
    Class value();
}
