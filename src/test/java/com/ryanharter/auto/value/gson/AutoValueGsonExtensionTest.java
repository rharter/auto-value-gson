package com.ryanharter.auto.value.gson;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.gson.Gson;
import com.google.testing.compile.JavaFileObjects;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.junit.Before;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

/**
 * Created by rharter on 7/20/15.
 */
public class AutoValueGsonExtensionTest {

  private JavaFileObject serializedName, nullable;

  @Before
  public void setup() {
    serializedName = JavaFileObjects.forSourceString("com.ryanharter.auto.value.gson.SerializedName", ""
        + "package com.ryanharter.auto.value.gson;\n"
        + "import java.lang.annotation.Retention;\n"
        + "import java.lang.annotation.Target;\n"
        + "import static java.lang.annotation.ElementType.METHOD;\n"
        + "import static java.lang.annotation.ElementType.PARAMETER;\n"
        + "import static java.lang.annotation.ElementType.FIELD;\n"
        + "import static java.lang.annotation.RetentionPolicy.SOURCE;\n"
        + "@Retention(SOURCE)\n"
        + "@Target({METHOD, PARAMETER, FIELD})\n"
        + "public @interface SerializedName {\n"
        + "  String value();\n"
        + "}");
    nullable = JavaFileObjects.forSourceString("com.ryanharter.auto.value.gson.Nullable", ""
        + "package com.ryanharter.auto.value.gson;\n"
        + "import java.lang.annotation.Retention;\n"
        + "import java.lang.annotation.Target;\n"
        + "import static java.lang.annotation.ElementType.METHOD;\n"
        + "import static java.lang.annotation.ElementType.PARAMETER;\n"
        + "import static java.lang.annotation.ElementType.FIELD;\n"
        + "import static java.lang.annotation.RetentionPolicy.SOURCE;\n"
        + "@Retention(SOURCE)\n"
        + "@Target({METHOD, PARAMETER, FIELD})\n"
        + "public @interface Nullable {\n"
        + "}");
  }

  @Test
  public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import com.ryanharter.auto.value.gson.SerializedName;\n"
        + "import com.ryanharter.auto.value.gson.Nullable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test {\n"
        // Reference type
        + "public abstract String a();\n"
        // Array type
        + "public abstract int[] b();\n"
        // Primitive type
        + "public abstract int c();\n"
        // SerializedName
        + "@SerializedName(\"_D\") public abstract String d();\n"
        // Nullable type
        + "@Nullable abstract String e();\n"
        + "}\n"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n"
        + "\n"
        + "import com.google.gson.Gson;\n"
        + "import com.google.gson.TypeAdapter;\n"
        + "import com.google.gson.TypeAdapterFactory;\n"
        + "import com.google.gson.reflect.TypeToken;\n"
        + "import com.google.gson.stream.JsonReader;\n"
        + "import com.google.gson.stream.JsonToken;\n"
        + "import com.google.gson.stream.JsonWriter;\n"
        + "import java.io.IOException;\n"
        + "import java.lang.Integer;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "\n"
        + "final class AutoValue_Test extends $AutoValue_Test {\n"
        + "  AutoValue_Test(String a, int[] b, int c, String d, String e) {\n"
        + "    super(a, b, c, d, e);\n"
        + "  }\n"
        + "\n"
        + "  public static TestTypeAdapterFactory typeAdapterFactory() {\n"
        + "    return new TestTypeAdapterFactory();\n"
        + "  }\n"
        + "\n"
        + "  public static final class TestTypeAdapterFactory implements TypeAdapterFactory {\n"
        + "    @Override\n"
        + "    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {\n"
        + "      if (!Test.class.isAssignableFrom(typeToken.getRawType())) return null;\n"
        + "      return (TypeAdapter<T>) new TestTypeAdapter(gson);\n"
        + "    }\n"
        + "  }\n"
        + "\n"
        + "  public static final class TestTypeAdapter extends TypeAdapter<Test> {\n"
        + "    Gson gson;\n"
        + "    public TestTypeAdapter(Gson gson) {\n"
        + "      this.gson = gson;\n"
        + "    }\n"
        + "    @Override\n"
        + "    public void write(JsonWriter jsonWriter, Test object) throws IOException {\n"
        + "      jsonWriter.beginObject();\n"
        + "      jsonWriter.name(\"a\");\n"
        + "      gson.getAdapter(String.class).write(jsonWriter, object.a());\n"
        + "      jsonWriter.name(\"b\");\n"
        + "      gson.getAdapter(int[].class).write(jsonWriter, object.b());\n"
        + "      jsonWriter.name(\"c\");\n"
        + "      gson.getAdapter(Integer.class).write(jsonWriter, object.c());\n"
        + "      jsonWriter.name(\"_D\");\n"
        + "      gson.getAdapter(String.class).write(jsonWriter, object.d());\n"
        + "      if (object.e() != null) {\n"
        + "        jsonWriter.name(\"e\");\n"
        + "        gson.getAdapter(String.class).write(jsonWriter, object.e());\n"
        + "      }\n"
        + "      jsonWriter.endObject();\n"
        + "    }\n"
        + "    @Override\n"
        + "    public Test read(JsonReader jsonReader) throws IOException {\n"
        + "      jsonReader.beginObject();\n"
        + "      String a = null;\n"
        + "      int[] b = null;\n"
        + "      Integer c = null;\n"
        + "      String d = null;\n"
        + "      String e = null;\n"
        + "      while (jsonReader.hasNext()) {\n"
        + "        String _name = jsonReader.nextName();\n"
        + "        if (jsonReader.peek() == JsonToken.NULL) {\n"
        + "          jsonReader.skipValue();\n"
        + "          continue;"
        + "        }"
        + "        switch (_name) {\n"
        + "          case \"a\": {\n"
        + "            a = gson.getAdapter(String.class).read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"b\": {\n"
        + "            b = gson.getAdapter(int[].class).read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"c\": {\n"
        + "            c = gson.getAdapter(Integer.class).read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"_D\": {\n"
        + "            d = gson.getAdapter(String.class).read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"e\": {\n"
        + "            e = gson.getAdapter(String.class).read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          default: {\n"
        + "            jsonReader.skipValue();\n"
        + "          }\n"
        + "        }\n"
        + "      }\n"
        + "      jsonReader.endObject();\n"
        + "      return new AutoValue_Test(a, b, c, d, e);\n"
        + "    }\n"
        + "  }\n"
        + "}"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(serializedName, nullable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }
}