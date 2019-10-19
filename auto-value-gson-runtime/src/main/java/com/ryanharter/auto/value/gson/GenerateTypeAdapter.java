package com.ryanharter.auto.value.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    private final Class<?> typeArrayClass = Array.newInstance(Type.class, 0).getClass();
    private final Map<Class<?>, Constructor<? extends TypeAdapter>> adapters =
        Collections.synchronizedMap(new LinkedHashMap<>());

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
        if (constructor.getParameterTypes().length == 1) {
          return constructor.newInstance(gson);
        } else {
          return constructor.newInstance(gson, ((ParameterizedType) type.getType()).getActualTypeArguments());
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
              (Constructor<? extends TypeAdapter>) bindingClass.getDeclaredConstructor(Gson.class);
          adapterCtor.setAccessible(true);
        } catch (NoSuchMethodException e) {
          // Try the gson + type[] constructor
          //noinspection unchecked
          adapterCtor =
              (Constructor<? extends TypeAdapter>) bindingClass.getDeclaredConstructor(Gson.class,
                  typeArrayClass);
          adapterCtor.setAccessible(true);
        }
      } catch (ClassNotFoundException e) {
        Constructor<? extends TypeAdapter> superClassAdapter = findConstructorForClass(cls.getSuperclass());
        if (superClassAdapter != null) {
          superClassAdapter.setAccessible(true);
        }
        adapterCtor = superClassAdapter;
      } catch (NoSuchMethodException e) {
        throw new RuntimeException("Unable to find binding constructor for " + clsName, e);
      }
      adapters.put(cls, adapterCtor);
      return adapterCtor;
    }
  };
}
