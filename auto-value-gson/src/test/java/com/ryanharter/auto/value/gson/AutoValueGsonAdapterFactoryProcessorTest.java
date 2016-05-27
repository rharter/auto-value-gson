package com.ryanharter.auto.value.gson;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class AutoValueGsonAdapterFactoryProcessorTest {

  @Test public void generatesTypeAdapterFactory() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.google.gson.TypeAdapter;\n"
        + "import com.google.gson.Gson;\n"
        + "@AutoValue public abstract class Foo {\n"
        + "  public static TypeAdapter<Foo> typeAdapter(Gson gson) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "  public abstract boolean isAwesome();\n"
        + "}");
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.google.gson.TypeAdapter;\n"
        + "import com.google.gson.Gson;\n"
        + "@AutoValue public abstract class Bar {\n"
        + "  public static TypeAdapter<Bar> jsonAdapter(Gson gson) {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "}");
    JavaFileObject expected = JavaFileObjects.forSourceString("com.ryanharter.auto.value.gson.AutoValueGsonTypeAdapterFactory", ""
        + "package com.ryanharter.auto.value.gson;\n"
        + "import com.google.gson.Gson;\n"
        + "import com.google.gson.TypeAdapter;\n"
        + "import com.google.gson.TypeAdapterFactory;\n"
        + "import com.google.gson.reflect.TypeToken;\n"
        + "import java.lang.Override;\n"
        + "import test.Bar;\n"
        + "import test.Foo;\n"
        + "public class AutoValueGsonTypeAdapterFactory implements TypeAdapterFactory {\n"
        + "  @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {\n"
        + "    Class<T> rawType = (Class<T>) type.getRawType();\n"
        + "    if (Foo.class.isAssignableFrom(rawType)) {\n"
        + "      return (TypeAdapter<T>) Foo.typeAdapter(gson);\n"
        + "    } else if (Bar.class.isAssignableFrom(rawType)) {\n"
        + "      return (TypeAdapter<T>) Bar.jsonAdapter(gson);\n"
        + "    } else {\n"
        + "      return null;\n"
        + "    }\n"
        + "  }\n"
        + "}");

    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source2))
        .processedWith(new AutoValueGsonAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

}