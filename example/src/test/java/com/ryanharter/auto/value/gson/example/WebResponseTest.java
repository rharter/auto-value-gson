package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    User expected = new User();
    expected.firstname = "Ryan";
    expected.lastname = "Harter";
    assertEquals(expected, response.data());
  }

  private static class User {
    String firstname;
    String lastname;

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      User user = (User) o;

      if (firstname != null ? !firstname.equals(user.firstname) : user.firstname != null)
        return false;
      return lastname != null ? lastname.equals(user.lastname) : user.lastname == null;

    }

    @Override public int hashCode() {
      int result = firstname != null ? firstname.hashCode() : 0;
      result = 31 * result + (lastname != null ? lastname.hashCode() : 0);
      return result;
    }
  }

}