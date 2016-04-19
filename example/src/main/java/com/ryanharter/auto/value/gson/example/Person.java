package com.ryanharter.auto.value.gson.example;

import android.os.Parcelable;
import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.annotations.AutoGson;

@AutoValue
@AutoGson(AutoValue_Person.GsonTypeAdapter.class)
public abstract class Person implements Parcelable {
    public abstract String name();

    public abstract int gender();

    public abstract int age();

    public static Builder builder() {
        return new AutoValue_Person.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder name(String name);

        public abstract Builder gender(int gender);

        public abstract Builder age(int age);

        public abstract Person build();
    }
}
