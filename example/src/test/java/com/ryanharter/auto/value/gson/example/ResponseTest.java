package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.Test;
import com.ryanharter.auto.value.gson.AutoValueGsonTypeAdapterFactory;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class ResponseTest {
    @Test
    public void testGson() throws Exception {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new AutoValueGsonTypeAdapterFactory())
                .create();


        List<String> expected = Arrays.asList("Hello", "World");
        Type responseType = new TypeToken<Response<List<String>>>(){}.getType();


        Response<List<String>> response = Response.<List<String>>builder()
                .data(expected)
                .build();
        String json = "{\"data\":[\"Hello\",\"World\"]}";

        String toJson = gson.toJson(response, responseType);
        Assert.assertEquals(json, toJson);

        Response<List<String>> fromJson = gson.fromJson(json, responseType);
        Assert.assertEquals(expected, fromJson.data());
    }
}
