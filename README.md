# AutoValue: Gson Extension

[![Build Status](https://travis-ci.org/rharter/auto-value-gson.svg?branch=master)](https://travis-ci.org/rharter/auto-value-gson)

An extension for Google's [AutoValue](https://github.com/google/auto) that creates a simple [Gson](https://github.com/google/gson) TypeAdapterFactory for each AutoValue annotated object.

**Note**: This is a very early version that won't work with the released AutoValue until a [PR](https://github.com/google/auto/pull/237) has been merged.

## Usage

Simply include AutoGson in your project and add the generated Serializer and Deserializer as a TypeAdapter.  You can also annotate your properties using `@SerializedName` to define an alternate name for de/serialization.

```java
@AutoValue public abstract class Foo {
  abstract String bar();
  @SerializedName("Baz") abstract String baz();

  public static TypeAdapterFactory typeAdapterFactory() {
    return AutoValue_Foo.typeAdapterFactory();
  }
}

final Gson gson = new GsonBuilder()
  .registerTypeAdapterFactory(Foo.class, Foo.typeAdapterFactory())
  .create();
```

Now build your project and de/serialize your Foo.

## TODO

This wouldn't be quite complete without some added features.

* Automatic registration
* Default value support

## Download

Add a Gradle dependency:

```groovy
apt 'com.ryanharter.auto.value:auto-value-gson:0.1-SNAPSHOT'
```

(Using the [android-apt](https://bitbucket.org/hvisser/android-apt) plugin)

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
