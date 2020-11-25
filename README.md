# AutoValue: Gson Extension

An extension for Google's [AutoValue](https://github.com/google/auto) that creates a simple [Gson](https://github.com/google/gson) TypeAdapterFactory for each AutoValue annotated object.

## Usage

Simply include auto-value-gson in your project and add a public static method to your `@AutoValue` 
annotated class returning a TypeAdapter.  You can also annotate your properties using 
`@SerializedName` to define an alternate name for de/serialization.

```java
@AutoValue public abstract class Foo {
  abstract String bar();
  @SerializedName("Baz") abstract String baz();
  abstract int quux();
  abstract String with_underscores();

  // The public static method returning a TypeAdapter<Foo> is what
  // tells auto-value-gson to create a TypeAdapter for Foo.
  public static TypeAdapter<Foo> typeAdapter(Gson gson) {
    return new AutoValue_Foo.GsonTypeAdapter(gson);
  }
}
```

Now build your project and de/serialize your Foo.

## The TypeAdapter

To trigger TypeAdapter generation, you need include a non-private static factory method that accepts
a `Gson` parameter and returns a `TypeAdapter` for your AutoValue type. From within this method you
can instantiate a new `GsonTypeAdapter` which will have been generated as an inner class of your
AutoValue generated implementation. 

```java
@AutoValue public abstract class Foo {
  // properties...
  
  public static TypeAdapter<Foo> typeAdapter(Gson gson) {
    return new AutoValue_Foo.GsonTypeAdapter(gson);
  }
}
```

## Generics support

If your annotated class uses generics, you'll have to modify your static method a little so
AutoValue will know how to generate an appropriate adapter. Simply add a `TypeToken<?>` parameter
and pass it to the generated `GsonTypeAdapter` class.

To have support for fields with generic parameters (eg. `List<B>`) you need to upgrade your Gson
dependency to at least **2.8.0**, which introduces the helper `TypeToken.getParameterized()`
see [Gson Changelog](https://github.com/google/gson/blob/master/CHANGELOG.md#version-28).

```java
@AutoValue public abstract class Foo<A, B, C> {

  abstract A data();
  abstract List<B> dataList();
  abstract Map<String, List<C>> dataMap();

  public static <A, B, C> TypeAdapter<Foo<A, B, C>> typeAdapter(Gson gson,
      Type[] types) {
    return new AutoValue_Foo.GsonTypeAdapter<>(gson, types);
  }
}
```

Note that the `types` is an array of the `Type` representations of the given type's generics. If 
`Foo` is parameterized as `Foo<String, Integer, Boolean>`, then the `Type` array passed in should be
an array of `{String.class, Integer.class, Boolean.class}`.

## Transient types

To ignore certain properties from serialization, you can use the `@AutoTransient` annotation. This comes from a 
shared transience annotations library and is an `api` dependency of the runtime artifact. You can annotate
a property and it will be treated as `transient` for both serialization and deserialization. Note that
this should only be applied to nullable properties.

## Builder Support
If your `@AutoValue` class has a builder, auto-value-gson will use the builder to 
instantiate the class. If the `@AutoValue` class has a static no-argument factory method for its builder, it will be used. If there are multiple factory methods, the one annotated `@AutoValueGsonBuilder` will be used. This can be 
useful for setting default values.

```java
@AutoValue public abstract class Foo {
  abstract int bar();
  abstract String quux();

  public static Builder builder() {
    return new AutoValue_Foo.Builder();
  }

  @AutoValueGsonBuilder
  public static Builder builderWithDefaults() {
    return new builder().quux("QUUX");
  }
}
```

## Field name policy

If you want the generated adapter classes to use the input `Gson` instance's field name policy, you can 
enable this via `autovaluegson.useFieldNamePolicy` processor option. This acts as a flag (any value is ignored)
and can be set like any other annotation processor option.

In Gradle, this could look like this:

```gradle
tasks.withType(JavaCompile) {
    options.compilerArgs += "-Aautovaluegson.useFieldNamePolicy"
}
```

## Factory

Optionally, auto-value-gson can create a single [TypeAdapterFactory](https://google.github.io/gson/apidocs/com/google/gson/TypeAdapterFactory.html) so
that you don't have to add each generated TypeAdapter to your Gson instance manually.

To generate a `TypeAdapterFactory` for all of your auto-value-gson classes, simply create
an abstract class that implements `TypeAdapterFactory` and annotate it with `@GsonTypeAdapterFactory`,
and auto-value-gson will create an implementation for you.  You simply need to provide a static
factory method, just like your AutoValue classes, and you can use the generated `TypeAdapterFactory`
to help Gson de/serialize your types.

```java
@GsonTypeAdapterFactory
public abstract class MyAdapterFactory implements TypeAdapterFactory {

  // Static factory method to access the package
  // private generated implementation
  public static TypeAdapterFactory create() {
    return new AutoValueGson_MyAdapterFactory();
  }
  
}
```

Then you simply need to register the Factory with Gson.

```java
Gson gson = new GsonBuilder()
    .registerTypeAdapterFactory(MyAdapterFactory.create())
    .create();
```

## @GenerateTypeAdapter

There is an annotation in the `auto-value-gson-runtime` artifact called `@GenerateTypeAdapter`. This annotation
can be set on types to indicate to the extension that you want the generated adapter to be a top level class in the same
package. The name of this class will be the AutoValue class's name plus `_GsonTypeAdapter` suffix.

Types annotated with this can also be (de)serialized dynamically at runtime with a provided runtime `TypeAdapterFactory`
implementation in the annotation called `FACTORY`. The type name and generated typeadapter class's name *must not be obfuscated*
for this to work. The extension that runs during annotation processing will automatically generate custom
.pro rules for Proguard/R8 for this, so it should require no extra configuration.

When this annotation is used, there will be no intermediate AutoValue class generated (as opposed to the default logic, 
which generates an intermediate class and generates the `TypeAdapter` as a static inner class of it). There is no need
to declare a static `TypeAdapter<...> typeAdapter()` method anymore for this case, though you can optionally define
one if you still want to use the `@GsonTypeAdapterFactory` generator for them.

`@GenerateTypeAdapter` is compatible with the factory approach above, just make your static method's implementation
point to it. It can also be an alternative to it if you use the runtime factory, particularly if you 
have a multimodule project and are willing to accept a small amount of (heavily cached) reflection.

The generated class will have the same parameters as if it were the inner class. If it's generic, its constructor
accepts a `Gson` instance and `TypeToken` of the generics. If it's not generic, it's just a `Gson` instance.

Example usage:

```java
@GenerateTypeAdapter
@AutoValue
public class Foo {
  // ...
}

// Generates
public final class Foo_GsonTypeAdapter extends TypeAdapter<Foo> {
  public Foo_GsonTypeAdapter(Gson gson) {
    //...
  }
}

// Or with generics
@GenerateTypeAdapter
@AutoValue
public class Foo<T> {
  // ...
}

// Generates
public final class Foo_GsonTypeAdapter extends TypeAdapter<Foo> {
  public Foo_GsonTypeAdapter(Gson gson, TypeToken<? extends Foo<T>> typeToken) {
    //...
  }
}

// Using the runtime FACTORY
new GsonBuilder()
    .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY)
    .create()
    .toJson(myFooInstance);
```

## Download

Add a Gradle dependency to the `annotationProcessor`/`kapt` and `implementation`/`api` configuration.

```kotlin
annotationProcessor("com.ryanharter.auto.value:auto-value-gson-extension:1.3.1")
implementation("com.ryanharter.auto.value:auto-value-gson-runtime:1.3.1")

// Optional @GsonTypeAdapterFactory support
annotationProcessor("com.ryanharter.auto.value:auto-value-gson-factory:1.3.1")

// Legacy generic artifact that includes both -extension and -factory above. This exists to not
// break existing users, but shouldn't be used because it includes both the -factory artifact as
// well as the -extension artifact. This can have a negative impact on build times if you don't
// actually use the factory support, as it is an aggregating incremental processor thus slower
// compared to just using the isolating incremental behavior of the extension.
annotationProcessor("com.ryanharter.auto.value:auto-value-gson:1.3.1")
```

Snapshots of the latest development version are available in [Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/).

## License

```
Copyright 2015 Ryan Harter.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
