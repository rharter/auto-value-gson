package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public class PersonTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (rawType.equals(Person.class)) {
            return (TypeAdapter<T>) Person.typeAdapter(gson);
        }
        return null;
    }
}
