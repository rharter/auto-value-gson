package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue public abstract class User {
  abstract String firstname();
  abstract String lastname();

  public static TypeAdapter<User> typeAdapter(Gson gson) {
    return new AutoValue_User.GsonTypeAdapter(gson);
  }

  public static User with(String firstname, String lastname) {
    return new AutoValue_User(firstname, lastname);
  }
}
