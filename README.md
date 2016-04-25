# AutoValue: Gson Extension

[![Build Status](https://travis-ci.org/rharter/auto-value-gson.svg?branch=master)](https://travis-ci.org/rharter/auto-value-gson)

An extension for Google's [AutoValue](https://github.com/google/auto) that creates a simple [Gson](https://github.com/google/gson) TypeAdapterFactory for each AutoValue annotated object.

## Usage

Simply include auto-value-gson in your project and add a public static method to your `@AutoValue` annotated class returning a TypeAdapter.  You can also annotate your properties using `@SerializedName` to define an alternate name for de/serialization.

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

public class AutoValueTypeAdapterFactory implements TypeAdapterFactory {
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Class<? super T> rawType = type.getRawType();
    if (rawType == Foo.class) {
      return (TypeAdapter<T>) new Foo.typeAdapter(gson);
    } else if (rawType == Bar.class) {
      return (TypeAdapter<T>) new Bar.typeAdapter(gson);
    }
    return null;
  }
}

final Gson gson = new GsonBuilder()
  .registerTypeAdapterFactory(new AutoValueTypeAdapterFactory())
  .create();
```

Now build your project and de/serialize your Foo.

## Download

Add a Gradle dependency:

```groovy
apt 'com.ryanharter.auto.value:auto-value-gson:0.2.5'
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
