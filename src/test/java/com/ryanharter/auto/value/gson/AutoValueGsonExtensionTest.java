package com.ryanharter.auto.value.gson;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
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
        + "import java.util.Map;\n"
        + "import java.util.Set;\n"
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
        // Parametrized type, multiple parameters
        + "public abstract Map<String, Number> f();\n"
        // Parametrized type, single parameter
        + "public abstract Set<String> g();\n"
        // Nested parameterized type
        + "public abstract Map<String, Set<String>> h();\n"
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
        + "import java.lang.Number;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "import java.util.Map;\n"
        + "import java.util.Set;\n"
        + "\n"
        + "final class AutoValue_Test extends $AutoValue_Test {\n"
        + "  AutoValue_Test(String a, int[] b, int c, String d, String e, Map<String, Number> f, Set<String> g, Map<String, Set<String>> h) {\n"
        + "    super(a, b, c, d, e, f, g, h);\n"
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
        + "    private final TypeAdapter<String> aAdapter;\n"
        + "    private final TypeAdapter<int[]> bAdapter;\n"
        + "    private final TypeAdapter<Integer> cAdapter;\n"
        + "    private final TypeAdapter<String> dAdapter;\n"
        + "    private final TypeAdapter<String> eAdapter;\n"
        + "    private final TypeAdapter<Map<String, Number>> fAdapter;\n"
        + "    private final TypeAdapter<Set<String>> gAdapter;\n"
        + "    private final TypeAdapter<Map<String, Set<String>>> hAdapter;\n"
        + "    public TestTypeAdapter(Gson gson) {\n"
        + "      this.aAdapter = gson.getAdapter(String.class);\n"
        + "      this.bAdapter = gson.getAdapter(int[].class);\n"
        + "      this.cAdapter = gson.getAdapter(Integer.class);\n"
        + "      this.dAdapter = gson.getAdapter(String.class);\n"
        + "      this.eAdapter = gson.getAdapter(String.class);\n"
        + "      this.fAdapter = gson.getAdapter(new TypeToken<Map<String, Number>>(){});\n"
        + "      this.gAdapter = gson.getAdapter(new TypeToken<Set<String>>(){});\n"
        + "      this.hAdapter = gson.getAdapter(new TypeToken<Map<String, Set<String>>>(){});\n"
        + "    }\n"
        + "    @Override\n"
        + "    public void write(JsonWriter jsonWriter, Test object) throws IOException {\n"
        + "      jsonWriter.beginObject();\n"
        + "      jsonWriter.name(\"a\");\n"
        + "      aAdapter.write(jsonWriter, object.a());\n"
        + "      jsonWriter.name(\"b\");\n"
        + "      bAdapter.write(jsonWriter, object.b());\n"
        + "      jsonWriter.name(\"c\");\n"
        + "      cAdapter.write(jsonWriter, object.c());\n"
        + "      jsonWriter.name(\"_D\");\n"
        + "      dAdapter.write(jsonWriter, object.d());\n"
        + "      if (object.e() != null) {\n"
        + "        jsonWriter.name(\"e\");\n"
        + "        eAdapter.write(jsonWriter, object.e());\n"
        + "      }\n"
        + "      jsonWriter.name(\"f\");\n"
        + "      fAdapter.write(jsonWriter, object.f());\n"
        + "      jsonWriter.name(\"g\");\n"
        + "      gAdapter.write(jsonWriter, object.g());\n"
        + "      jsonWriter.name(\"h\");\n"
        + "      hAdapter.write(jsonWriter, object.h());\n"
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
        + "      Map<String, Number> f = null;\n"
        + "      Set<String> g = null;\n"
        + "      Map<String, Set<String>> h = null;\n"
        + "      while (jsonReader.hasNext()) {\n"
        + "        String _name = jsonReader.nextName();\n"
        + "        if (jsonReader.peek() == JsonToken.NULL) {\n"
        + "          jsonReader.skipValue();\n"
        + "          continue;\n"
        + "        }\n"
        + "        switch (_name) {\n"
        + "          case \"a\": {\n"
        + "            a = aAdapter.read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"b\": {\n"
        + "            b = bAdapter.read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"c\": {\n"
        + "            c = cAdapter.read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"_D\": {\n"
        + "            d = dAdapter.read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"e\": {\n"
        + "            e = eAdapter.read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"f\": {\n"
        + "            f = fAdapter.read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"g\": {\n"
        + "            g = gAdapter.read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          case \"h\": {\n"
        + "            h = hAdapter.read(jsonReader);\n"
        + "            break;\n"
        + "          }\n"
        + "          default: {\n"
        + "            jsonReader.skipValue();\n"
        + "          }\n"
        + "        }\n"
        + "      }\n"
        + "      jsonReader.endObject();\n"
        + "      return new AutoValue_Test(a, b, c, d, e, f, g, h);\n"
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