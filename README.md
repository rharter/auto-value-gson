# AutoValue: Gson Extension

[![Build Status](https://travis-ci.org/rharter/auto-value-gson.svg?branch=master)](https://travis-ci.org/rharter/auto-value-gson)

An extension for Google's [AutoValue](https://github.com/google/auto) that creates a simple [Gson](https://github.com/google/gson) TypeAdapterFactory for each AutoValue annotated object.

## Usage

Simply include auto-value-gson in your project and add a public static method to your `@AutoValue` 
annotated class returning a TypeAdapter.  You can also annotate your properties using 
`@SerializedName` to define an alternate name for de/serialization.

```java
@AutoValue public abstract class Foo {
  abstract String bar();
  @SerializedName("Baz") abstract String baz();

  // The public static method returning a TypeAdapter<Foo> is what
  // tells auto-value-gson to create a TypeAdapter for Foo.
  public static TypeAdapter<Foo> typeAdapter(Gson gson) {
    return new AutoValue_Foo.GsonTypeAdapter(gson);
  }
}

final Gson gson = new GsonBuilder()
  .registerTypeAdapterFactory(new AutoValueGsonTypeAdapterFactory())
  .create();
```

Now build your project and de/serialize your Foo.

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
    .build();
```

## Download

Add a Gradle dependency:

```groovy
apt 'com.ryanharter.auto.value:auto-value-gson:0.4.0'
```

(Using the [android-apt](https://bitbucket.org/hvisser/android-apt) plugin)

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
