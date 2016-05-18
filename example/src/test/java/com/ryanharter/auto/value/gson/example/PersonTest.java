package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import com.ryanharter.auto.value.gson.AutoValueGsonTypeAdapterFactory;

public class PersonTest {
    @Test
    public void testGson() throws Exception {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new AutoValueGsonTypeAdapterFactory())
                .create();
        Person person = Person.builder()
                .name("Piasy")
                .gender(1)
                .age(23)
                .address(Address.create("street", "city"))
                .build();
        String json = "{\"name\":\"Piasy\",\"gender\":1,\"age\":23,\"address\":{\"street-name\":\"street\",\"city\":\"city\"}}";

        String toJson = gson.toJson(person, Person.class);
        Assert.assertEquals(json, toJson);

        Person fromJson = gson.fromJson(json, Person.class);
        Assert.assertEquals("Piasy", fromJson.name());
        Assert.assertEquals(23, fromJson.age());
        Assert.assertEquals(1, fromJson.gender());
    }
}
