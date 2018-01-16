package com.ryanharter.auto.value.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotate a given AutoValue-annotated type to indicate to auto-value-gson to generate its
 * TypeAdapter in a separate class rather than an inner class of an intermediate AutoValue-generated
 * class hierarchy.
 */
@Inherited
@Retention(RUNTIME)
@Target(TYPE)
public @interface GenerateTypeAdapter {

  TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
    private final Map<Class<?>, Constructor<? extends TypeAdapter>> adapters =
        Collections.synchronizedMap(
            new LinkedHashMap<Class<?>, Constructor<? extends TypeAdapter>>());

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      Class<? super T> rawType = type.getRawType();
      if (!rawType.isAnnotationPresent(GenerateTypeAdapter.class)) {
        return null;
      }

      Constructor<? extends TypeAdapter> constructor = findConstructorForClass(rawType);
      if (constructor == null) {
        return null;
      }
      //noinspection TryWithIdenticalCatches Resolves to API 19+ only type.
      try {
        if (constructor.getParameterCount() == 1) {
          return constructor.newInstance(gson);
        } else {
          return constructor.newInstance(gson, type);
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Unable to invoke " + constructor, e);
      } catch (InstantiationException e) {
        throw new RuntimeException("Unable to invoke " + constructor, e);
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
          throw (Error) cause;
        }
        throw new RuntimeException(
            "Could not create generated TypeAdapter instance for type " + rawType, cause);
      }
    }

    private Constructor<? extends TypeAdapter> findConstructorForClass(Class<?> cls) {
      Constructor<? extends TypeAdapter> adapterCtor = adapters.get(cls);
      if (adapterCtor != null) {
        return adapterCtor;
      }
      String clsName = cls.getName();
      if (clsName.startsWith("android.")
          || clsName.startsWith("java.")
          || clsName.startsWith("kotlin.")) {
        return null;
      }
      try {
        Class<?> bindingClass = cls.getClassLoader()
            .loadClass(clsName + "_GsonTypeAdapter");
        try {
          // Try the gson constructor
          //noinspection unchecked
          adapterCtor =
              (Constructor<? extends TypeAdapter>) bindingClass.getConstructor(Gson.class);
        } catch (NoSuchMethodException e) {
          // Try the gson + typetoken constructor
          //noinspection unchecked
          adapterCtor =
              (Constructor<? extends TypeAdapter>) bindingClass.getConstructor(Gson.class,
                  TypeToken.class);
        }
      } catch (ClassNotFoundException e) {
        adapterCtor = findConstructorForClass(cls.getSuperclass());
      } catch (NoSuchMethodException e) {
        throw new RuntimeException("Unable to find binding constructor for " + clsName, e);
      }
      adapters.put(cls, adapterCtor);
      return adapterCtor;
    }
  };
}
