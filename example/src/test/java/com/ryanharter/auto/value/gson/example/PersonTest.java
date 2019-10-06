package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PersonTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testGson() throws Exception {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String birthdate = "2007-11-11";
        Date date = df.parse(birthdate);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new BirthdateAdapter())
                .registerTypeAdapterFactory(SampleAdapterFactory.create())
                .create();
        Person person = Person.builder()
                .name("Piasy")
                .gender(1)
                .age(23)
                .birthdate(date)
                .address(Address.create("street", "city"))
                .build();
        String json = "{\"name\":\"Piasy\",\"gender\":1,\"age\":23,\"birthdate\":\"" + birthdate + "\",\"address\":{\"street-name\":\"street\",\"city\":\"city\"}}";

        String toJson = gson.toJson(person, Person.class);
        Assert.assertEquals(json, toJson);

        Person fromJson = gson.fromJson(json, Person.class);
        Assert.assertEquals("Piasy", fromJson.name());
        Assert.assertEquals(23, fromJson.age());
        Assert.assertEquals(1, fromJson.gender());
        Assert.assertEquals(date, fromJson.birthdate());
    }

    @Test
    public void testGsonWithValidation() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("age cannot be negative");

        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(SampleAdapterFactory.create())
            .create();

        String json = "{\"name\":\"Piasy\",\"gender\":1,\"age\":-1,\"birthdate\":\"2007-11-11\",\"address\":{\"street-name\":\"street\",\"city\":\"city\"}}";
        gson.fromJson(json, Person.class);
    }

    @Test
    public void testGsonWithDefaults() {
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(SampleAdapterFactory.create())
            .create();

        // "name" and "gender" are unspecified. Should default to "Jane Doe" and 23
        String json = "{\"age\":23,\"birthdate\":\"2007-11-11\",\"address\":{\"street-name\":\"street\",\"city\":\"city\"}}";
        Person fromJson = gson.fromJson(json, Person.class);
        Assert.assertEquals("Jane Doe", fromJson.name());
        Assert.assertEquals(23, fromJson.age());
        Assert.assertEquals(0, fromJson.gender());
    }
}
