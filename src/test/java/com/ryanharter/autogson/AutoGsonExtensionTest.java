package com.ryanharter.autogson;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by rharter on 7/20/15.
 */
public class AutoGsonExtensionTest {

  AutoGsonExtension extension = new AutoGsonExtension();

  @Test public void generatesTypeAdapterFactoryMethod() throws Exception {
    TypeSpec typeAdapterFactory = TypeSpec.classBuilder("FooTypeAdapterFactory").build();
    assertThat(extension.createTypeAdapterFactoryMethod(typeAdapterFactory).toString())
        .isEqualTo("" +
            "public static FooTypeAdapterFactory typeAdapterFactory() {\n" +
            "  return new FooTypeAdapterFactory();\n" +
            "}\n");
  }

  @Test public void generatesTypeAdapterFactory() throws Exception {
    ClassName className = ClassName.get("com.test", "$Foo");
    ClassName autoValueClassName = ClassName.get("com.test", "Foo");
    TypeSpec typeAdapter = TypeSpec.classBuilder("FooTypeAdapter").build();
    Map<String, TypeName> properties = new LinkedHashMap<String, TypeName>(3);
    properties.put("foo", TypeName.get(String.class));
    properties.put("bar", TypeName.get(Double.class));
    properties.put("baz", TypeName.get(Integer.class));
    assertThat(extension.createTypeAdapterFactory(className, autoValueClassName, typeAdapter, properties).toString())
        .isEqualTo("" +
            "public static final class FooTypeAdapterFactory implements com.google.gson.TypeAdapterFactory {\n" +
            "  @java.lang.Override\n" +
            "  public <T> com.google.gson.TypeAdapter<T> create(com.google.gson.Gson gson, com.google.gson.reflect.TypeToken<T> typeToken) {\n" +
            "    if (!com.test.Foo.class.isAssignableFrom(typeToken.getRawType())) return null;\n" +
            "    return (com.google.gson.TypeAdapter<T>) new FooTypeAdapter(gson);\n" +
            "  }\n" +
            "}\n");
  }

  @Test public void generatesTypeAdapter() throws Exception {
    Map<String, TypeName> properties = new LinkedHashMap<String, TypeName>(3);
    properties.put("foo", TypeName.get(String.class));
    properties.put("bar", TypeName.get(Double.class));
    properties.put("baz", TypeName.get(Integer.class));
    assertThat(extension.createTypeAdapter("com.test.$Foo", "com.test.Foo", properties).toString())
        .isEqualTo("" +
            "public static final class FooTypeAdapter extends com.google.gson.TypeAdapter<com.test.Foo> {\n" +
            "  com.google.gson.Gson gson;\n" +
            "\n" +
            "  public FooTypeAdapter(com.google.gson.Gson gson) {\n" +
            "    this.gson = gson;\n" +
            "  }\n" +
            "\n" +
            "  @java.lang.Override\n" +
            "  public void write(com.google.gson.stream.JsonWriter jsonWriter, com.test.Foo object) throws java.io.IOException {\n" +
            "    jsonWriter.beginObject();\n" +
            "    gson.getAdapter(java.lang.String.class).write(jsonWriter, object.foo());\n" +
            "    gson.getAdapter(java.lang.Double.class).write(jsonWriter, object.bar());\n" +
            "    gson.getAdapter(java.lang.Integer.class).write(jsonWriter, object.baz());\n" +
            "    jsonWriter.endObject();\n" +
            "  }\n" +
            "\n" +
            "  @java.lang.Override\n" +
            "  public com.test.Foo read(com.google.gson.stream.JsonReader jsonReader) throws java.io.IOException {\n" +
            "    jsonReader.beginObject();\n" +
            "    java.lang.String foo = null;\n" +
            "    java.lang.Double bar = null;\n" +
            "    java.lang.Integer baz = null;\n" +
            "    while (jsonReader.hasNext()) {\n" +
            "      java.lang.String _name = jsonReader.nextName();\n" +
            "      if (\"foo\".equals(_name)) {\n" +
            "        foo = gson.getAdapter(java.lang.String.class).read(jsonReader);\n" +
            "      } else if (\"bar\".equals(_name)) {\n" +
            "        bar = gson.getAdapter(java.lang.Double.class).read(jsonReader);\n" +
            "      } else if (\"baz\".equals(_name)) {\n" +
            "        baz = gson.getAdapter(java.lang.Integer.class).read(jsonReader);\n" +
            "      }\n" +
            "    }\n" +
            "    jsonReader.endObject();\n" +
            "    return new com.test.Foo(foo, bar, baz);\n" +
            "  }\n" +
            "}\n");
  }

  @Test public void createsWriteMethod() throws Exception {
    FieldSpec gsonField = FieldSpec.builder(Gson.class, "gson").build();

    Map<String, TypeName> properties = new LinkedHashMap<String, TypeName>(3);
    properties.put("foo", TypeName.get(String.class));
    properties.put("bar", TypeName.get(Double.class));
    properties.put("baz", TypeName.get(Integer.class));

    assertThat(extension.createWriteMethod(gsonField, "com.test.Foo", properties).toString())
        .isEqualTo("" +
            "@java.lang.Override\n" +
            "public void write(com.google.gson.stream.JsonWriter jsonWriter, com.test.Foo object) throws java.io.IOException {\n" +
            "  jsonWriter.beginObject();\n" +
            "  gson.getAdapter(java.lang.String.class).write(jsonWriter, object.foo());\n" +
            "  gson.getAdapter(java.lang.Double.class).write(jsonWriter, object.bar());\n" +
            "  gson.getAdapter(java.lang.Integer.class).write(jsonWriter, object.baz());\n" +
            "  jsonWriter.endObject();\n" +
            "}\n");
  }

  @Test public void createsReadMethod() throws Exception {
    FieldSpec gsonField = FieldSpec.builder(Gson.class, "gson").build();

    Map<String, TypeName> properties = new LinkedHashMap<String, TypeName>(3);
    properties.put("foo", TypeName.get(String.class));
    properties.put("bar", TypeName.get(Double.class));
    properties.put("baz", TypeName.get(Integer.class));

    assertThat(extension.createReadMethod(gsonField, "com.test.$Foo", "com.test.Foo", properties).toString())
        .isEqualTo("" +
            "@java.lang.Override\n" +
            "public com.test.Foo read(com.google.gson.stream.JsonReader jsonReader) throws java.io.IOException {\n" +
            "  jsonReader.beginObject();\n" +
            "  java.lang.String foo = null;\n" +
            "  java.lang.Double bar = null;\n" +
            "  java.lang.Integer baz = null;\n" +
            "  while (jsonReader.hasNext()) {\n" +
            "    java.lang.String _name = jsonReader.nextName();\n" +
            "    if (\"foo\".equals(_name)) {\n" +
            "      foo = gson.getAdapter(java.lang.String.class).read(jsonReader);\n" +
            "    } else if (\"bar\".equals(_name)) {\n" +
            "      bar = gson.getAdapter(java.lang.Double.class).read(jsonReader);\n" +
            "    } else if (\"baz\".equals(_name)) {\n" +
            "      baz = gson.getAdapter(java.lang.Integer.class).read(jsonReader);\n" +
            "    }\n" +
            "  }\n" +
            "  jsonReader.endObject();\n" +
            "  return new com.test.Foo(foo, bar, baz);\n" +
            "}\n");
  }

  /*@AutoValue*/ static class Foo {
    Foo(String foo, Double bar, Integer baz) {

    }
    String foo() { return "foo"; }
    Double bar() { return new Double(0); }
    Integer baz() { return 1; }
  }

  public static FooTypeAdapterFactory typeAdapterFactory() {
    return new FooTypeAdapterFactory();
  }

  public static final class FooTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
      if (!Foo.class.isAssignableFrom(typeToken.getRawType())) return null;
      return (TypeAdapter<T>) new FooTypeAdapter(gson);
    }
  }

  public static final class FooTypeAdapter extends TypeAdapter<Foo> {

    Gson gson;

    public FooTypeAdapter(Gson gson) {
      this.gson = gson;
    }

    @Override
    public void write(JsonWriter jsonWriter, Foo foo) throws IOException {
      jsonWriter.beginObject();
      gson.getAdapter(String.class).write(jsonWriter, foo.foo());
      gson.getAdapter(Double.class).write(jsonWriter, foo.bar());
      gson.getAdapter(Integer.class).write(jsonWriter, foo.baz());
      jsonWriter.endObject();
    }

    @Override
    public Foo read(JsonReader jsonReader) throws IOException {
      jsonReader.beginObject();
      String foo = null;
      Double bar = null;
      Integer baz = null;
      while (jsonReader.hasNext()) {
        String name = jsonReader.nextName();
        if ("foo".equals(name)) {
          foo = gson.getAdapter(String.class).read(jsonReader);
        } else if ("bar".equals(name)) {
          bar = gson.getAdapter(Double.class).read(jsonReader);
        } else if ("baz".equals(name)) {
          baz = gson.getAdapter(Integer.class).read(jsonReader);
        }
      }
      jsonReader.endObject();
      return new Foo(foo, bar, baz);
    }
  }

}