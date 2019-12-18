package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class GenerateTypeAdapterTest {

  @Test
  public void smokeTest() throws IOException {
    //language=JSON
    String json = "{\"seasoning\":\"spicy\"}";
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY)
        .create();

    Taco expectedTaco = Taco.create("spicy");

    // Basic read
    Taco taco = gson.fromJson(json, Taco.class);
    assertEquals(expectedTaco, taco);

    // Basic write
    String encoded = gson.toJson(taco);
    assertEquals(json, encoded);

    // The same as above, but with the adapter looked up first
    TypeAdapter<Taco> adapter = gson.getAdapter(Taco.class);

    // Basic read
    Taco taco2 = adapter.fromJson(json);
    assertEquals(expectedTaco, taco2);

    // Basic write
    String encoded2 = adapter.toJson(taco);
    assertEquals(json, encoded2);

    // Ensure the generated AutoValue_Taco class still gets translated back into a taco adapter
    // Assert they're the same to ensure we've properly delegated up the gson chain and avoid
    // duplicating adapter instances.
    TypeAdapter<AutoValue_GenerateTypeAdapterTest_Taco> generatedClassAdapter = gson.getAdapter(AutoValue_GenerateTypeAdapterTest_Taco.class);
    assertSame(adapter, generatedClassAdapter);
  }

  @GenerateTypeAdapter
  @AutoValue
  public static abstract class Taco {

    public abstract String seasoning();

    public static Taco create(String seasoning) {
      return new AutoValue_GenerateTypeAdapterTest_Taco(seasoning);
    }

  }
}
