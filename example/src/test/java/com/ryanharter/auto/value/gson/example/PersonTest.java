package com.ryanharter.auto.value.gson.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Test;

public class PersonTest {
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
}
