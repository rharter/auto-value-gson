# AutoValue: Gson Extension

[![Build Status](https://travis-ci.org/rharter/auto-value-gson.svg?branch=master)](https://travis-ci.org/rharter/auto-value-gson)

An extension for Google's [AutoValue](https://github.com/google/auto) that creates a simple [Gson](https://github.com/google/gson) TypeAdapterFactory for each AutoValue annotated object.

## Usage

Simply include auto-value-gson in your project.  You can also annotate your properties using `@SerializedName` to define an alternate name for de/serialization.

```java
@AutoValue
@AutoGson(AutoValue_Foo.GsonTypeAdapter.class)
public abstract class Foo {
  abstract String bar();
  @SerializedName("Baz") abstract String baz();
}

final Gson gson = new GsonBuilder()
  .registerTypeAdapterFactory(new AutoTypeAdapterFactory())
  .create();

// To utilize the generated type adapter, it's important to include the second parameter,
// otherwise, gson will use reflection to do the serialization, which is slow in Android.
String toJson = gson.toJson(foo, Foo.class);
Foo fromJson = gson.fromJson(json, Foo.class);
```

Now build your project and de/serialize your Foo.

## Download

Add a Gradle dependency:

```groovy
compile 'com.ryanharter.auto.value:auto-value-gson-annotations:0.2.5'
apt 'com.ryanharter.auto.value:auto-value-gson-processor:0.2.5'
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
