package com.ryanharter.auto.value.gson.example;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class SampleAdapterFactory implements TypeAdapterFactory {

    public static SampleAdapterFactory create() {
        return new AutoValueGson_SampleAdapterFactory();
    }
}
