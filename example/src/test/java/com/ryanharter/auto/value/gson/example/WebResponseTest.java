package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.lang.reflect.Type;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebResponseTest {

  @Test public void handlesBasicTypes() {
    String json = "{\"status\":200,\"data\":\"string\"," +
        "\"dataList\":[\"string\"]," +
        "\"dataMap\":{\"key\":[\"string\"]}}";
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(SampleAdapterFactory.create())
        .create();
    Type responseType = new TypeToken<WebResponse<String>>(){}.getType();
    WebResponse<String> response = gson.fromJson(json, responseType);

    assertEquals("string", response.data());
    assertEquals("string", response.dataList().get(0));
    assertEquals("string", response.dataMap().get("key").get(0));
    assertEquals(200, response.status());
  }

  @Test public void handlesComplexTypes() {
    String json = "{\"status\":200,\"data\":{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}, " +
        "\"dataList\":[{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}]," +
        "\"dataMap\":{\"key\":[{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}]}}";
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(SampleAdapterFactory.create())
        .create();
    Type responseType = new TypeToken<WebResponse<User>>(){}.getType();
    WebResponse<User> response = gson.fromJson(json, responseType);

    User expected = User.with("Ryan", "Harter");
    assertEquals(expected, response.data());
    assertEquals(expected, response.dataList().get(0));
    assertEquals(expected, response.dataMap().get("key").get(0));
  }

  @Test public void handlesBasicTypesExternalAdapter() {
    String json = "{\"status\":200,\"data\":\"string\"," +
        "\"dataList\":[\"string\"]," +
        "\"dataMap\":{\"key\":[\"string\"]}}";
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY)
        .create();
    Type responseType = new TypeToken<WebResponse<String>>(){}.getType();
    WebResponse<String> response = gson.fromJson(json, responseType);

    assertEquals("string", response.data());
    assertEquals("string", response.dataList().get(0));
    assertEquals("string", response.dataMap().get("key").get(0));
    assertEquals(200, response.status());
  }

  @Test public void handlesComplexTypesExternalAdapter() {
    String json = "{\"status\":200,\"data\":{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}, " +
        "\"dataList\":[{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}]," +
        "\"dataMap\":{\"key\":[{\"firstname\":\"Ryan\",\"lastname\":\"Harter\"}]}}";
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY)
        .create();
    Type responseType = new TypeToken<WebResponse<User>>(){}.getType();
    WebResponse<User> response = gson.fromJson(json, responseType);

    User expected = User.with("Ryan", "Harter");
    assertEquals(expected, response.data());
    assertEquals(expected, response.dataList().get(0));
    assertEquals(expected, response.dataMap().get("key").get(0));
  }

}
