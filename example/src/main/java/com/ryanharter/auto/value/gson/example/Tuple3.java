package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

@AutoValue
public abstract class Tuple3<A, B, C> {

    public static TypeAdapter<Tuple3> typeAdapter(Gson gson, TypeToken<? extends Tuple3> typeToken) {
        return new AutoValue_Tuple3.GsonTypeAdapter(gson, typeToken);
    }

    public static <A, B, C> Builder<A, B, C> builder() {
        return new AutoValue_Tuple3.Builder<>();
    }

    public abstract A a();
    public abstract B b();
    public abstract C c();

    @AutoValue.Builder
    public static abstract class Builder<A, B, C> {
        public abstract Builder<A, B, C> a(A a);
        public abstract Builder<A, B, C> b(B b);
        public abstract Builder<A, B, C> c(C c);
        public abstract Tuple3<A, B, C> build();
    }
}
