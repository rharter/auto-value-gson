package com.ryanharter.auto.value.gson.annotations;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Create {@link TypeAdapter} for type T, using its {@link AutoGson} annotation value.
 */
public final class AutoTypeAdapterFactory implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        final Class<T> rawType = (Class<T>) type.getRawType();
        final AutoGson annotation = rawType.getAnnotation(AutoGson.class);
        TypeAdapter<T> adapter;
        if (annotation == null) {
            adapter = null;
        } else {
            try {
                Constructor<TypeAdapter<T>> constructor =
                        annotation.value().getConstructor(Gson.class);
                adapter = constructor.newInstance(gson);
            } catch (NoSuchMethodException e) {
                adapter = null;
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                adapter = null;
                e.printStackTrace();
            } catch (InstantiationException e) {
                adapter = null;
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                adapter = null;
                e.printStackTrace();
            }
        }

        return adapter;
    }
}
