package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;

public class UnrecognizedFieldsTest {

    @Test
    public void testGsonWithDefaults() {
        Gson gson = createGson();

        String json = "{\"knownField\":9,\"unknownField\":9 }";
        UnrecognizedExample fromJson = gson.fromJson(json, UnrecognizedExample.class);
        Assert.assertEquals(9, fromJson.knownField());
    }

    private Gson createGson() {
        return new GsonBuilder()
          .registerTypeAdapterFactory(SampleAdapterFactory.create())
          .create();
    }
}
