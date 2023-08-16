package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.ryanharter.auto.value.gson.OmitDefaults;
import java.util.Date;

@AutoValue
@OmitDefaults
public abstract class Person {
    public abstract String name();

    public abstract int gender();

    public abstract int age();

    public abstract Date birthdate();

    @Nullable
    public abstract Address address();

    public static Builder builder() {
        return new AutoValue_Person.Builder()
            .name("Jane Doe")
            .gender(0);
    }

    public static TypeAdapter<Person> typeAdapter(Gson gson) {
        return new AutoValue_Person.GsonTypeAdapter(gson);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder name(String name);

        public abstract String name();

        public abstract Builder gender(int gender);

        public abstract int gender();

        public abstract Builder age(int age);

        public abstract int age();

        public abstract Builder birthdate(Date birthdate);

        public abstract Builder address(Address address);

        public abstract Person autoBuild();

        public Person build() {
            Person person = autoBuild();
            if (person.age() < 0) {
                throw new IllegalArgumentException("age cannot be negative");
            }
            return person;
        }
    }

    public @interface Nullable {}
}
