package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import org.junit.Test;

import static org.junit.Assert.*;

public class WebResponseTest {

  @Test public void handlesBasicTypes() {
    String json = "{\"status\":200,\"data\":\"string\"}";
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(SampleAdapterFactory.create())
        .create();
    Type responseType = new TypeToken<WebResponse<String>>(){}.getType();
    WebResponse<String> response = gson.fromJson(json, responseType);

    assertEquals("string", response.data());
    assertEquals(200, response.status());
  }

  @Test public void handlesComplexTypes() {
    String json = "{\"status\":200,\"data\":{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}}";
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(SampleAdapterFactory.create())
        .create();
    Type responseType = new TypeToken<WebResponse<User>>(){}.getType();
    WebResponse<User> response = gson.fromJson(json, responseType);

    User expected = User.with("Ryan", "Harter");
    assertEquals(expected, response.data());
  }

}