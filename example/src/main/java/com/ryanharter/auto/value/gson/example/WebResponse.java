package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

@AutoValue public abstract class WebResponse<T> {
  public abstract int status();
  public abstract T data();

  public static TypeAdapter<WebResponse> typeAdapter(Gson gson, TypeToken<? extends WebResponse> typeToken) {
    return new AutoValue_WebResponse.GsonTypeAdapter(gson, typeToken);
  }
}
