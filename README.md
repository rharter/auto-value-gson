# AutoGson

An extension for Google's [AutoValue](https://github.com/google/auto) that creates a simple [Gson](https://github.com/google/gson) Serializer and Deserializer for each AutoValue annotated object.

**Note**: This is a very early version that won't work with the released AutoValue until a [PR](https://github.com/google/auto/pull/237) has been merged.

## Usage

Simply include AutoGson in your project and add the generated Serializer and Deserializer as a TypeAdapter.

```java
@AutoValue public abstract class Foo {
  abstract String bar();
}

final Gson gson = new GsonBuilder()
  .registerTypeAdapterFactory(Foo.class, AutoValue_Foo.typeAdapterFactory())
  .create();
```

Now build your project and de/serialize your Foo.

## TODO

This wouldn't be quite complete without some added features.

* Automatic registration
* `@SerializedName` support
* Default value support

## Download

Add a Gradle dependency:

**Soon**

```groovy
compile 'com.ryanharter.autogson:auto-gson:0.1-SNAPSHOT'
```

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
