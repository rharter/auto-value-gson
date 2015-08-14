package com.ryanharter.auto.value.gson;

import com.google.gson.Gson;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by rharter on 7/20/15.
 */
public class AutoValueGsonExtensionTest {

  AutoValueGsonExtension extension = new AutoValueGsonExtension();

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
    List<AutoValueGsonExtension.Property> properties = new LinkedList<AutoValueGsonExtension.Property>();
    properties.add(new TestProperty("foo", TypeName.get(String.class)));
    properties.add(new TestProperty("bar", TypeName.get(double.class)));
    properties.add(new TestProperty("baz", "BAZ", TypeName.get(Integer.class)));
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
            "    jsonWriter.name(\"foo\");\n" +
            "    gson.getAdapter(java.lang.String.class).write(jsonWriter, object.foo());\n" +
            "    jsonWriter.name(\"bar\");\n" +
            "    gson.getAdapter(java.lang.Double.class).write(jsonWriter, object.bar());\n" +
            "    jsonWriter.name(\"BAZ\");\n" +
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
            "      } else if (\"BAZ\".equals(_name)) {\n" +
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

    List<AutoValueGsonExtension.Property> properties = new LinkedList<AutoValueGsonExtension.Property>();
    properties.add(new TestProperty("foo", TypeName.get(String.class)));
    properties.add(new TestProperty("bar", TypeName.get(double.class)));
    properties.add(new TestProperty("baz", "BAZ", TypeName.get(Integer.class)));

    assertThat(extension.createWriteMethod(gsonField, "com.test.Foo", properties).toString())
        .isEqualTo("" +
            "@java.lang.Override\n" +
            "public void write(com.google.gson.stream.JsonWriter jsonWriter, com.test.Foo object) throws java.io.IOException {\n" +
            "  jsonWriter.beginObject();\n" +
            "  jsonWriter.name(\"foo\");\n" +
            "  gson.getAdapter(java.lang.String.class).write(jsonWriter, object.foo());\n" +
            "  jsonWriter.name(\"bar\");\n" +
            "  gson.getAdapter(java.lang.Double.class).write(jsonWriter, object.bar());\n" +
            "  jsonWriter.name(\"BAZ\");\n" +
            "  gson.getAdapter(java.lang.Integer.class).write(jsonWriter, object.baz());\n" +
            "  jsonWriter.endObject();\n" +
            "}\n");
  }
  @Test public void createsReadMethod() throws Exception {
    FieldSpec gsonField = FieldSpec.builder(Gson.class, "gson").build();

    List<AutoValueGsonExtension.Property> properties = new LinkedList<AutoValueGsonExtension.Property>();
    properties.add(new TestProperty("foo", TypeName.get(String.class)));
    properties.add(new TestProperty("bar", TypeName.get(double.class)));
    properties.add(new TestProperty("baz", "BAZ", TypeName.get(Integer.class)));

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
            "    } else if (\"BAZ\".equals(_name)) {\n" +
            "      baz = gson.getAdapter(java.lang.Integer.class).read(jsonReader);\n" +
            "    }\n" +
            "  }\n" +
            "  jsonReader.endObject();\n" +
            "  return new com.test.Foo(foo, bar, baz);\n" +
            "}\n");
  }

  public static class TestProperty extends AutoValueGsonExtension.Property {

    private String serializedName;

    public TestProperty(String name, TypeName typeName) {
      this(name, null, typeName);
    }

    public TestProperty(String name, String serializedName, TypeName typeName) {
      this.name = name;
      this.serializedName = serializedName != null ? serializedName : name;
      this.type = typeName;
    }

    @Override
    public String serializedName() {
      return serializedName;
    }
  }

}