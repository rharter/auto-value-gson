package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class Address {

    public static Address create(String streetName, String city) {
        return new AutoValue_Address(streetName, city);
    }

    public static TypeAdapter<Address> typeAdapter(Gson gson) {
        return new AutoValue_Address.GsonTypeAdapter(gson);
    }

    @SerializedName("street-name")
    public abstract String streetName();

    public abstract String city();
}
