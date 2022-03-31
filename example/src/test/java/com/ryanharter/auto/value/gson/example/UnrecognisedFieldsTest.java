package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UnrecognisedFieldsTest {

    @Test
    public void readWriteFullyRecognisedJson() {
        Gson gson = createGson();
        String sourceJson = "{\"knownField\":9}";

        UnrecognisedExample object = gson.fromJson(sourceJson, UnrecognisedExample.class);
        String json = gson.toJson(object);

        assertEquals(sourceJson, json);
    }

    @Test
    public void readWriteTwoUnknownFields() {
        Gson gson = createGson();
        String sourceJson = "{\"knownField\":9,\"unknownField\":7,\"oneMoreUnknown\":true}";

        UnrecognisedExample object = gson.fromJson(sourceJson, UnrecognisedExample.class);
        String json = gson.toJson(object);

        assertEquals(sourceJson, json);
    }

    @Test
    public void readUnknownObject() {
        Gson gson = createGson();
        String json = "{\"knownField\":9,\"unknownObject\":{\"unknownField\":\"test\"}}";

        UnrecognisedExample fromJson = gson.fromJson(json, UnrecognisedExample.class);

        assertEquals(9, fromJson.knownField());
        assertNotNull(fromJson.unrecognised());
        assertEquals("{\"unknownField\":\"test\"}", fromJson.unrecognised().get("unknownObject").toString());
    }

    @Test
    public void readUnknownArray() {
        Gson gson = createGson();
        String json = "{\"knownField\":9,\"unknownArray\":[1,2,true,{\"a\": \"b\"}]}";

        UnrecognisedExample fromJson = gson.fromJson(json, UnrecognisedExample.class);

        assertEquals(9, fromJson.knownField());
        assertNotNull(fromJson.unrecognised());
        assertEquals("[1,2,true,{\"a\":\"b\"}]", fromJson.unrecognised().get("unknownArray").toString());
    }

    private Gson createGson() {
        return new GsonBuilder()
          .registerTypeAdapterFactory(SampleAdapterFactory.create())
          .create();
    }
}
