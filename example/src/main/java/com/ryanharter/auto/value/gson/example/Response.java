package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

@AutoValue
public abstract class Response<T> {
    public abstract T data();

    public static TypeAdapter<Response> typeAdapter(Gson gson, TypeToken<? extends Response> typeToken) {
        return new AutoValue_Response.GsonTypeAdapter(gson, typeToken);
    }

    public static <T> Builder<T> builder() {
        return new AutoValue_Response.Builder<>();
    }

    @AutoValue.Builder
    public static abstract class Builder<T> {
        public abstract Builder<T> data(T data);
        public abstract Response<T> build();
    }
}
