package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UnrecognizedFieldsTest {

    @Test
    public void oneUnknownField() {
        Gson gson = createGson();
        String json = "{\"knownField\":9,\"unknownField\":7 }";

        UnrecognizedExample fromJson = gson.fromJson(json, UnrecognizedExample.class);
        
        assertEquals(9, fromJson.knownField());
        assertNotNull(fromJson.unrecognized());
        assertEquals("7", fromJson.unrecognized().get("unknownField"));
    }

    @Test
    public void twoUnknownFields() {
        Gson gson = createGson();
        String json = "{\"knownField\":9,\"unknownField\":7,\"oneMoreUnknown\": true }";

        UnrecognizedExample fromJson = gson.fromJson(json, UnrecognizedExample.class);

        assertEquals(9, fromJson.knownField());
        assertNotNull(fromJson.unrecognized());
        assertEquals("7", fromJson.unrecognized().get("unknownField"));
        assertEquals("true", fromJson.unrecognized().get("oneMoreUnknown"));
    }

    private Gson createGson() {
        return new GsonBuilder()
          .registerTypeAdapterFactory(SampleAdapterFactory.create())
          .create();
    }
}
