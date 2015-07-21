package com.ryanharter.autogson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.squareup.javapoet.TypeName;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by rharter on 7/20/15.
 */
public class AutoGsonExtensionTest {

  AutoGsonExtension extension = new AutoGsonExtension();

  @Test public void generatesSerializer() throws Exception {
    Map<String, TypeName> properties = new LinkedHashMap<String, TypeName>();
    properties.put("foo", TypeName.get(String.class));
    properties.put("bar", TypeName.get(Double.class));
    properties.put("baz", TypeName.get(Integer.class));
    assertThat(extension.createSerializer("com.test.$Foo", "com.test.Foo", properties).toString())
        .isEqualTo("" +
            "public static final com.google.gson.JsonSerializer<com.test.Foo> SERIALIZER = new com.google.gson.JsonSerializer<com.test.Foo>() {\n" +
            "  @java.lang.Override\n" +
            "  public com.google.gson.JsonElement serialize(com.test.Foo foo, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext context) {\n" +
            "    final com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();\n" +
            "    jsonObject.add(\"foo\", context.serialize(foo.foo()));\n" +
            "    jsonObject.add(\"bar\", context.serialize(foo.bar()));\n" +
            "    jsonObject.add(\"baz\", context.serialize(foo.baz()));\n" +
            "    return jsonObject;\n" +
            "  }\n" +
            "};\n");
  }

  @Test public void generatesDeserializer() throws Exception {
    Map<String, TypeName> properties = new LinkedHashMap<String, TypeName>();
    properties.put("foo", TypeName.get(String.class));
    properties.put("bar", TypeName.get(Double.class));
    properties.put("baz", TypeName.get(Integer.class));
    assertThat(extension.createDeserializer("com.test.$Foo", "com.test.Foo", properties).toString())
        .isEqualTo("" +
            "public static final com.google.gson.JsonDeserializer<com.test.Foo> DESERIALIZER = new com.google.gson.JsonDeserializer<com.test.Foo>() {\n" +
            "  @java.lang.Override\n" +
            "  public com.test.Foo deserialize(com.google.gson.JsonElement jsonElement, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {\n" +
            "    if (jsonElement.isJsonNull()) {\n" +
            "      return null;\n" +
            "    }\n" +
            "    com.google.gson.JsonObject object = jsonElement.getAsJsonObject();\n" +
            "    java.lang.String foo = context.deserialize(object.get(\"foo\"), java.lang.String.class);\n" +
            "    java.lang.Double bar = context.deserialize(object.get(\"bar\"), java.lang.Double.class);\n" +
            "    java.lang.Integer baz = context.deserialize(object.get(\"baz\"), java.lang.Integer.class);\n" +
            "    return new com.test.Foo(foo, bar, baz);\n" +
            "  }\n" +
            "};\n");
  }

  static class Foo {
    Foo(String foo, Double bar, Integer baz) {

    }
    String foo() { return "foo"; }
    Double bar() { return new Double(0); }
    Integer baz() { return 1; }
  }

  private static final JsonSerializer<Foo> FOO_SERIALIZER = new JsonSerializer<Foo>() {
    @Override
    public JsonElement serialize(Foo foo, Type type, JsonSerializationContext context) {
      final JsonObject jsonObj = new JsonObject();
      jsonObj.add("foo", context.serialize(foo.foo()));
      jsonObj.add("bar", context.serialize(foo.bar()));
      jsonObj.add("baz", context.serialize(foo.baz()));
      return jsonObj;
    }
  };

  private static final JsonDeserializer<Foo> FOO_DESERIALIZER = new JsonDeserializer<Foo>() {
    @Override
    public Foo deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
      if (jsonElement.isJsonNull()) {
        return null;
      }
      JsonObject object = jsonElement.getAsJsonObject();
      String foo = context.deserialize(object.get("foo"), String.class);
      Double bar = context.deserialize(object.get("bar"), Double.class);
      Integer baz = context.deserialize(object.get("baz"), Integer.class);
      return new Foo(foo, bar, baz);
    }
  };

}