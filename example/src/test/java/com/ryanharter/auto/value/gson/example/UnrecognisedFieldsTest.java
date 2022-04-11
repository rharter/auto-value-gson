// modified by mapbox
package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    public void readAndWriteUnknownObject() {
        Gson gson = createGson();
        String originalJson = "{\"knownField\":9,\"unknownObject\":{\"unknownField\":\"test\"}}";

        UnrecognisedExample object = gson.fromJson(originalJson, UnrecognisedExample.class);
        String json = gson.toJson(object);

        assertEquals(originalJson, json);
    }

    @Test
    public void readAndWriteUnknownArray() {
        Gson gson = createGson();
        String originalJson = "{\"knownField\":9,\"unknownArray\":[1,2,true,{\"a\":\"b\"}]}";

        UnrecognisedExample object = gson.fromJson(originalJson, UnrecognisedExample.class);
        String json = gson.toJson(object);

        assertEquals(originalJson, json);
    }

    private Gson createGson() {
        return new GsonBuilder()
          .registerTypeAdapterFactory(SampleAdapterFactory.create())
          .create();
    }
}
