package com.ryanharter.auto.value.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Annotation to indicate that a given class should be included in a {@link GsonTypeAdapterFactory GsonTypeAdapterFactory-annotated}
 * factory in this compilation unit if it doesn't use the Auto-Value-Gson extension. This can be
 * useful for classes that use custom adapters but you still want included in the generated factory.
 * <p>
 * Like classes that do use the Auto-Value-Gson extension, these classes must implement a static
 * {@link TypeAdapter TypeAdapter-returning} method that contains one of the following parameters
 * combinations:
 * <p>
 * <ul>
 *   <li>public static TypeAdapter&lt;T&gt; typeAdapter() // No parameters</li>
 *   <li>public static TypeAdapter&lt;T&gt; typeAdapter({@link Gson})</li>
 *   <li>public static TypeAdapter&lt;T&gt; typeAdapter({@link Gson}, {@link Type}[])</li>
 * </ul>
 */
@Retention(CLASS)
@Target(TYPE)
public @interface ExposeToGsonTypeAdapterFactory {
}
