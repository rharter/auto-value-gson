package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

@AutoValue public abstract class WebResponse<T> {
  public abstract int status();
  public abstract T data();
  public abstract List<T> dataList();
  public abstract Map<String, List<T>> dataMap();

  public static <T> TypeAdapter<WebResponse<T>> typeAdapter(Gson gson, TypeToken<? extends WebResponse<T>> typeToken) {
    return new AutoValue_WebResponse.GsonTypeAdapter(gson, typeToken);
  }
}
