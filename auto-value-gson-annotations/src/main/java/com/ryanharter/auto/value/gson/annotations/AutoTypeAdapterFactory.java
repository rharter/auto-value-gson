/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
