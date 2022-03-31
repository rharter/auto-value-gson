package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.Map;

@AutoValue
public abstract class UnrecognizedExample {

  public abstract int knownField();

  public static UnrecognizedExample.Builder builder() {
    return new AutoValue_UnrecognizedExample.Builder();
  }

  public static TypeAdapter<UnrecognizedExample> typeAdapter(Gson gson) {
    return new AutoValue_UnrecognizedExample.GsonTypeAdapter(gson);
  }

  @Nullable
  abstract Map<String, Object> unrecognized();

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder knownField(int value);
    abstract Builder unrecognized(@Nullable Map<String, Object> value);
    public abstract UnrecognizedExample build();
  }

  public @interface Nullable {}
}
