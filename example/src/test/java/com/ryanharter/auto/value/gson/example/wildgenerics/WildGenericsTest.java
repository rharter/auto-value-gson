package com.ryanharter.auto.value.gson.example.wildgenerics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import com.ryanharter.auto.value.gson.example.Address;
import com.ryanharter.auto.value.gson.example.SampleAdapterFactory;
import java.io.IOException;
import org.junit.Test;

import static com.google.gson.reflect.TypeToken.getParameterized;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public final class WildGenericsTest {

  @Test public void addressGenericBase() throws IOException {
    //language=JSON
    String json = "{\"generic\":{\"street-name\":\"genericStreet\",\"city\":\"genericCity\"},"
        + "\"notGeneric\":\"notGeneric\",\"collection\":[{\"street-name\":\"fooStreet\","
        + "\"city\":\"barCity\"}],\"topLevelNonGeneric\":\"topLevelNonGeneric\"}";

    Gson gson = new GsonBuilder().registerTypeAdapterFactory(SampleAdapterFactory.create())
        .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY)
        .create();
    TypeAdapter<AddressGenericBase> adapter = gson.getAdapter(AddressGenericBase.class);
    AddressGenericBase instance = adapter.fromJson(json);

    AddressGenericBase testInstance = AddressGenericBase.builder()
        .topLevelNonGeneric("topLevelNonGeneric")
        .collection(singletonList(Address.create("fooStreet", "barCity")))
        .generic(Address.create("genericStreet", "genericCity"))
        .notGeneric("notGeneric")
        .build();

    assertEquals(instance, testInstance);
    assertEquals(json, adapter.toJson(testInstance));
  }

  @Test public void doubleGeneric() throws IOException {
    //language=JSON
    String json = "{\"generic\":{\"street-name\":\"genericStreet\",\"city\":\"genericCity\"},"
        + "\"notGeneric\":\"notGeneric\",\"collection\":[{\"street-name\":\"fooStreet\","
        + "\"city\":\"barCity\"}],\"topLevelNonGeneric\":\"topLevelNonGeneric\","
        + "\"topLevelGeneric\":{\"street-name\":\"topStreet\",\"city\":\"topCity\"},"
        + "\"topLevelGenericCollection\":[{\"street-name\":\"topCollectionStreet\","
        + "\"city\":\"topCollectionCity\"}]}";

    Gson gson = new GsonBuilder().registerTypeAdapterFactory(SampleAdapterFactory.create())
        .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY)
        .create();
    //noinspection unchecked
    TypeToken<DoubleGeneric<Address>> token =
        (TypeToken<DoubleGeneric<Address>>) getParameterized(DoubleGeneric.class, Address.class);
    TypeAdapter<DoubleGeneric<Address>> adapter = gson.getAdapter(token);
    DoubleGeneric<Address> instance = adapter.fromJson(json);

    DoubleGeneric<Address> testInstance =
        DoubleGeneric.<Address>builder().topLevelNonGeneric("topLevelNonGeneric")
            .topLevelGeneric(Address.create("topStreet", "topCity"))
            .topLevelGenericCollection(singletonList(Address.create("topCollectionStreet",
                "topCollectionCity")))
            .collection(singletonList(Address.create("fooStreet", "barCity")))
            .generic(Address.create("genericStreet", "genericCity"))
            .notGeneric("notGeneric")
            .build();

    assertEquals(instance, testInstance);
    assertEquals(json, adapter.toJson(testInstance));
  }
}
