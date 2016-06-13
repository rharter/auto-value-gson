package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.Test;

import com.ryanharter.auto.value.gson.AutoValueGsonTypeAdapterFactory;

import java.util.Arrays;
import java.util.List;

public class Tupe3Test {
    @Test public void testGson() throws Exception {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new AutoValueGsonTypeAdapterFactory())
                .create();

        TypeToken<Tuple3<Integer, String, List<String>>> tuple3TypeToken
                = new TypeToken<Tuple3<Integer, String, List<String>>>() {};

        int expectedA = 1;
        String expectedB = "Testing";
        List<String> expectedC = Arrays.asList("Hello", "World");

        Tuple3<Integer, String, List<String>> tuple3 = Tuple3.<Integer, String, List<String>>builder()
                .a(expectedA)
                .b(expectedB)
                .c(expectedC)
                .build();

        String json = "{\"a\":1,\"b\":\"Testing\",\"c\":[\"Hello\",\"World\"]}";

        String toJson = gson.toJson(tuple3, tuple3TypeToken.getType());
        Assert.assertEquals(json, toJson);

        Tuple3<Integer, String, List<String>> fromJson = gson.fromJson(json, tuple3TypeToken.getType());
        Assert.assertEquals((long) expectedA, (long) fromJson.a());
        Assert.assertEquals(expectedB, fromJson.b());
        Assert.assertEquals(expectedC, fromJson.c());
    }
}
