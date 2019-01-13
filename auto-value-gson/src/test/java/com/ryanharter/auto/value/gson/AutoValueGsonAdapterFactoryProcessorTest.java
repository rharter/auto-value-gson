package com.ryanharter.auto.value.gson;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class AutoValueGsonAdapterFactoryProcessorTest {

  @Test public void generatesTypeAdapterFactory() {
    JavaFileObject fooSource = JavaFileObjects.forSourceString("test.Foo", ""
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
    // BarSource's typeAdapter method accepts no parameter, which is valid for the processor factory
    JavaFileObject barSource = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.google.gson.TypeAdapter;\n"
        + "@AutoValue public abstract class Bar {\n"
        + "  public static TypeAdapter<Bar> jsonAdapter() {\n"
        + "    return null;\n"
        + "  }\n"
        + "  public abstract String getName();\n"
        + "}");
    JavaFileObject bazSource = JavaFileObjects.forSourceString("test.Baz", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "import com.google.gson.TypeAdapter;\n"
            + "import com.google.gson.Gson;\n"
            + "@AutoValue abstract class Baz {\n"
            + "  static TypeAdapter<Baz> jsonAdapter(Gson gson) {\n"
            + "    return null;\n"
            + "  }\n"
            + "  abstract String getName();\n"
            + "}");
    JavaFileObject factorySource = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.google.gson.TypeAdapterFactory;\n"
        + "import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;\n"
        + "@GsonTypeAdapterFactory\n"
        + "public abstract class MyAdapterFactory implements TypeAdapterFactory {\n"
        + "  public static TypeAdapterFactory create() {\n"
        + "    return new AutoValueGson_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValueGson_MyAdapterFactory", ""
        + "package test;\n"
        + "import com.google.gson.Gson;\n"
        + "import com.google.gson.TypeAdapter;\n"
        + "import com.google.gson.reflect.TypeToken;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.SuppressWarnings;\n"
        + "\n"
        + "public final class AutoValueGson_MyAdapterFactory extends MyAdapterFactory {\n"
        + "  @Override\n"
        + "  @SuppressWarnings(\"unchecked\")\n"
        + "  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {\n"
        + "    Class<?> rawType = type.getRawType();\n"
        + "    if (Bar.class.isAssignableFrom(rawType)) {\n"
        + "      return (TypeAdapter<T>) Bar.jsonAdapter();\n"
        + "    } else if (Baz.class.isAssignableFrom(rawType)) {\n"
        + "      return (TypeAdapter<T>) Baz.jsonAdapter(gson);\n"
        + "    } else if (Foo.class.isAssignableFrom(rawType)) {\n"
        + "      return (TypeAdapter<T>) Foo.typeAdapter(gson);\n"
        + "    } else {\n"
        + "      return null;\n"
        + "    }\n"
        + "  }\n"
        + "}");

    assertAbout(javaSources())
        .that(ImmutableSet.of(fooSource, barSource, bazSource, factorySource))
        .processedWith(new AutoValueGsonAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void generatesTypeAdapterFactory_notAbstract_shouldFail() {
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
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.google.gson.TypeAdapterFactory;\n"
        + "import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;\n"
        + "@GsonTypeAdapterFactory\n"
        + "public class MyAdapterFactory implements TypeAdapterFactory {\n"
        + "  public static TypeAdapterFactory create() {\n"
        + "    return new AutoValueGson_MyAdapterFactory();\n"
        + "  }\n"
        + "}");

    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source2))
        .processedWith(new AutoValueGsonAdapterFactoryProcessor())
        .failsToCompile()
        .withErrorContaining("Must be abstract!");
  }

  @Test public void generatesTypeAdapterFactory_doesNotImplementTypeAdapterFactory_shouldFail() {
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
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.google.gson.TypeAdapterFactory;\n"
        + "import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;\n"
        + "@GsonTypeAdapterFactory\n"
        + "public abstract class MyAdapterFactory {\n"
        + "  public static TypeAdapterFactory create() {\n"
        + "    return new AutoValueGson_MyAdapterFactory();\n"
        + "  }\n"
        + "}");

    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source2, source3))
        .processedWith(new AutoValueGsonAdapterFactoryProcessor())
        .failsToCompile()
        .withErrorContaining("Must implement TypeAdapterFactory!");
  }

  @Test public void generatesTypeAdapterFactory_shouldSearchUpComplexAncestry() {
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
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.IMyAdapterFactoryBase", ""
        + "package test;\n"
        + "import com.google.gson.TypeAdapterFactory;\n"
        + "public interface IMyAdapterFactoryBase extends TypeAdapterFactory {\n"
        + "}");
    JavaFileObject source4 = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
        + "package test;\n"
        + "import com.google.gson.TypeAdapterFactory;\n"
        + "import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;\n"
        + "@GsonTypeAdapterFactory\n"
        + "public abstract class MyAdapterFactory implements IMyAdapterFactoryBase {\n"
        + "  public static TypeAdapterFactory create() {\n"
        + "    return new AutoValueGson_MyAdapterFactory();\n"
        + "  }\n"
        + "}");
    JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValueGson_MyAdapterFactory", ""
        + "package test;\n"
        + "import com.google.gson.Gson;\n"
        + "import com.google.gson.TypeAdapter;\n"
        + "import com.google.gson.reflect.TypeToken;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.SuppressWarnings;\n"
        + "\n"
        + "public final class AutoValueGson_MyAdapterFactory extends MyAdapterFactory {\n"
        + "  @Override\n"
        + "  @SuppressWarnings(\"unchecked\")\n"
        + "  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {\n"
        + "    Class<?> rawType = type.getRawType();\n"
        + "    if (Bar.class.isAssignableFrom(rawType)) {\n"
        + "      return (TypeAdapter<T>) Bar.jsonAdapter(gson);\n"
        + "    } else if (Foo.class.isAssignableFrom(rawType)) {\n"
        + "      return (TypeAdapter<T>) Foo.typeAdapter(gson);\n"
        + "    } else {\n"
        + "      return null;\n"
        + "    }\n"
        + "  }\n"
        + "}");

    assertAbout(javaSources())
        .that(ImmutableSet.of(source1, source2, source3, source4))
        .processedWith(new AutoValueGsonAdapterFactoryProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void generatesInnerClassTypeAdapterFactory() {
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
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.InnerClassWrapper", ""
            + "package test;\n"
            + "import com.google.gson.TypeAdapterFactory;\n"
            + "import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;\n"
            + "public abstract class InnerClassWrapper {\n"
                + "@GsonTypeAdapterFactory\n"
                + "public abstract static class MyAdapterFactory implements TypeAdapterFactory {\n"
                + "  public static TypeAdapterFactory create() {\n"
                + "    return new AutoValueGson_InnerClassWrapper_MyAdapterFactory();\n"
                + "  }\n"
                + "}\n"
            + "}");
    JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValueGson_MyAdapterFactory", ""
            + "package test;\n"
            + "import com.google.gson.Gson;\n"
            + "import com.google.gson.TypeAdapter;\n"
            + "import com.google.gson.reflect.TypeToken;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.SuppressWarnings;\n"
            + "\n"
            + "public final class AutoValueGson_InnerClassWrapper_MyAdapterFactory extends InnerClassWrapper.MyAdapterFactory {\n"
            + "  @Override\n"
            + "  @SuppressWarnings(\"unchecked\")\n"
            + "  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {\n"
            + "    Class<?> rawType = type.getRawType();\n"
            + "    if (Bar.class.isAssignableFrom(rawType)) {\n"
            + "      return (TypeAdapter<T>) Bar.jsonAdapter(gson);\n"
            + "    } else if (Foo.class.isAssignableFrom(rawType)) {\n"
            + "      return (TypeAdapter<T>) Foo.typeAdapter(gson);\n"
            + "    } else {\n"
            + "      return null;\n"
            + "    }\n"
            + "  }\n"
            + "}");
    assertAbout(javaSources())
            .that(ImmutableSet.of(source1, source2, source3))
            .processedWith(new AutoValueGsonAdapterFactoryProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expected);
  }

  @Test public void generatesTypeAdapterFactory_shouldHaveOrder() {
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
    JavaFileObject factorySource = JavaFileObjects.forSourceString("test.MyAdapterFactory", ""
      + "package test;\n"
      + "import com.google.gson.TypeAdapterFactory;\n"
      + "import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;\n"
      + "@GsonTypeAdapterFactory\n"
      + "public abstract class MyAdapterFactory implements TypeAdapterFactory {\n"
      + "  public static TypeAdapterFactory create() {\n"
      + "    return new AutoValueGson_MyAdapterFactory();\n"
      + "  }\n"
      + "}");
    JavaFileObject expected = JavaFileObjects.forSourceString("test.AutoValueGson_MyAdapterFactory", ""
      + "package test;\n"
      + "import com.google.gson.Gson;\n"
      + "import com.google.gson.TypeAdapter;\n"
      + "import com.google.gson.reflect.TypeToken;\n"
      + "import java.lang.Override;\n"
      + "import java.lang.SuppressWarnings;\n"
      + "\n"
      + "public final class AutoValueGson_MyAdapterFactory extends MyAdapterFactory {\n"
      + "  @Override\n"
      + "  @SuppressWarnings(\"unchecked\")\n"
      + "  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {\n"
      + "    Class<?> rawType = type.getRawType();\n"
      + "    if (Bar.class.isAssignableFrom(rawType)) {\n"
      + "      return (TypeAdapter<T>) Bar.jsonAdapter(gson);\n"
      + "    } else if (Foo.class.isAssignableFrom(rawType)) {\n"
      + "      return (TypeAdapter<T>) Foo.typeAdapter(gson);\n"
      + "    } else {\n"
      + "      return null;\n"
      + "    }\n"
      + "  }\n"
      + "}");

    assertAbout(javaSources())
      .that(ImmutableSet.of(source1, source2, factorySource))
      .processedWith(new AutoValueGsonAdapterFactoryProcessor())
      .compilesWithoutError()
      .and()
      .generatesSources(expected);

    assertAbout(javaSources())
      .that(ImmutableSet.of(source2, source1, factorySource))
      .processedWith(new AutoValueGsonAdapterFactoryProcessor())
      .compilesWithoutError()
      .and()
      .generatesSources(expected);
  }
}
