package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UnrecognizedFieldsTest {

    @Test
    public void readWriteFullyRecognizedJson() {
        Gson gson = createGson();
        String sourceJson = "{\"knownField\":9}";

        UnrecognizedExample object = gson.fromJson(sourceJson, UnrecognizedExample.class);
        String json = gson.toJson(object);

        assertEquals(sourceJson, json);
    }

    @Test
    public void readWriteTwoUnknownFields() {
        Gson gson = createGson();
        String sourceJson = "{\"knownField\":9,\"unknownField\":7,\"oneMoreUnknown\":true}";

        UnrecognizedExample object = gson.fromJson(sourceJson, UnrecognizedExample.class);
        String json = gson.toJson(object);

        assertEquals(sourceJson, json);
    }

    @Test
    public void readUnknownObject() {
        Gson gson = createGson();
        String json = "{\"knownField\":9,\"unknownObject\":{\"unknownField\":\"test\"}}";

        UnrecognizedExample fromJson = gson.fromJson(json, UnrecognizedExample.class);

        assertEquals(9, fromJson.knownField());
        assertNotNull(fromJson.unrecognized());
        assertEquals("{\"unknownField\":\"test\"}", fromJson.unrecognized().get("unknownObject").toString());
    }

    @Test
    public void readUnknownArray() {
        Gson gson = createGson();
        String json = "{\"knownField\":9,\"unknownArray\":[1,2,true,{\"a\": \"b\"}]}";

        UnrecognizedExample fromJson = gson.fromJson(json, UnrecognizedExample.class);

        assertEquals(9, fromJson.knownField());
        assertNotNull(fromJson.unrecognized());
        assertEquals("[1,2,true,{\"a\":\"b\"}]", fromJson.unrecognized().get("unknownArray").toString());
    }

    private Gson createGson() {
        return new GsonBuilder()
          .registerTypeAdapterFactory(SampleAdapterFactory.create())
          .create();
    }
}
