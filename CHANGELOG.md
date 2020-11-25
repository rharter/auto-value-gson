# Change Log

## Version 1.3.1 (2020-11-25)

* Fix generated proguard names for inner classes (#246)
* Use reflection name in proguardNameOf() (#247)
* Recognize @Nullable as a TYPE_USE annotation. (#254)
* fix read method for builder pattern. (#256)

## Verson 1.3.0 (2020-01-19)

* Resolve materialized types in subclasses of generic types (#241)
* Extract factory to its own artifact (`auto-value-gson-factory`) (#238)
* Rename extension artifact to `auto-value-gson-extension` and add legacy artifact with the original name (`auto-value-gson`) for existing users. The legacy artifact is empty and just depends on both the extension and factory artifacts. See README ["Download"](README.md#download) section for why you should move away from the legacy artifact! (#239)
* Generate proguard rules on-demand (#236)

## Version 1.2.0 (2020-01-10)

* Make generated class + constructor package private (#228)
* Make field name policy support opt-in (#230)
* Generate human readable helper toString() methods (#231)
* Optimize hierarchy lookups for GenerateTypeAdapter (#232)
* Make generated factories package private (#229)

## Version 1.1.1 (2019-10-18)

* Fix factory method with generics #226 (#227)

## Version 1.1.0 (2019-10-15)

* Adds support for property defaults, normalization, and validation via Builders (#224)
* Migrate off deprecated generatedAnnotation method (#215)

## Version 1.0.0 (2019-02-08)

* Adds support for wildcard types.
* Respect FieldNamingStrategy when set on the Gson builder (#124)
* Use correct "Generated"-annotation based on JDK version. (#186)
* Add support for incremental annotaiton processing. (#188, #189)
* Allow no-arg static typeAdapter() methods (#195)
* Remove support for @GsonTypeAdapter, empty collection defaults, and mutable adapters (#201)
* Refactor annotations artifact to runtime artifact for consistency with other libraries (#202)
* Embed Proguard rules (#203)
* Switch transient annotation to shared AutoTransient library (#207)
* Add support for non-public typeAdapter() methods (#210)

## Version 0.8.0 (2018-05-31)

* Fixes an issue causing TypeAdapters to be generated non-deterministically.
* Updates TypeAdapters to be lazily initialized.
* Suppress unchecked for generated adapter read & write methods.
* Fixes usage of Java 8 API in GenerateTypeAdapter (#166)
* Switch from ConcurrentHashMap to Collections.synchronizedMap()

## Version 0.7.0 (2017-12-20)

* Implement `@GeneratedTypeAdapter` support (#160)
* Generated code optimizations (#156)
* Make annotations java 7 compatible (#157)

## Version 0.6.0 (2017-10-03)

* Standalone annotations artifact (#118) (#126)
* Generate Nullable annotation at constructor arguments. (#144)
* Remove unused arguments to CodeBlock.of() (#146)
* Support inner Factory class generation (#117)

## Version 0.5.0 (2017-07-07)

* Allow ignoring fields for serialization/deserialization (#91)
* Support non-public TypeAdapter factory methods (#121) (#122)
* Make defaults off by default (#133)
* Add @Generated annotation when available (#135)

## Version 0.4.6 (2017-02-03)

* Serializes collections to null by default, adding compiler option to serialize to empty variants. (#103)
* Skip null values in JSON (#100)
* Use TypeToken.getParameterized for all parameterized fields. (#99)

## Version 0.4.5 (2016-12-08)

* Adds support for custom field adapters (#83)
* Implements support for default values. (#84)
* Handle parameterized generic fields. (#66)

## Version 0.4.4 (2016-10-25)

#### Supports: AutoValue 1.3

* Adds better null support. (fixes #23, #50, #67)

## Version 0.4.3 (2016-10-17)

#### Supports: AutoValue 1.3

* Updates AutoValue dependency to 1.3
* Initializes known non-nullable collection types to empty collections. (#71)
* Use `getDeclaredClass()` instead of `getClass()` on enums. (#72)

## Version 0.4.2 (2016-08-05)

#### Supports: AutoValue: 1.3-rc1

* Bumps version number to fix release.

## Version 0.4.1 (2016-08-01)

#### Supports: AutoValue: 1.3-rc1

* Adds support for generic AutoValue types!!

## Version 0.4.0 (2016-07-28)

#### Supports: AutoValue: 1.3-rc1

* Adds alternate serialization names
* Changes to AutoValue-esque factory generation.

## Version 0.3.2-rc1 (2016-06-13)

#### Supports: AutoValue: 1.3-rc1

* Updates extension to support AutoValue 1.3-rc1
* Removes final modifier from AutoValueGsonAdapterFactory.

## Version 0.3.1 (2016-05-17)

#### Supports: AutoValue 1.2
 
* Replaces equals check with `isAssignableFrom` in generated type adapter factory.

## Version 0.3.0 (2016-05-11)

#### Supports: AutoValue 1.2
 
* Adds support for single AutoValueGsonTypeAdapterFactory for all AutoValue gson types.
* Fixes generation of TypeAdapter.read() for 'char' values
* Adds sample project.

## Version 0.2.5 (2016-04-13)

#### Supports: AutoValue 1.2
 
* Fixes issue causing failure for non-standard class and package names.

## Version 0.2.3 (2016-04-13)

#### Supports: AutoValue 1.2
 
* Adds selective generation based on public static method returning TypeAdapter<Foo>

## Version 0.2.2 (2016-04-05)

#### Supports: AutoValue 1.2-rc1
 
* Fixes issue causing method prefixes (`get`, `is`) to be ignored.

## Version 0.2.1 (2016-03-22)

Fixes snapshot issues with 0.2.0 release. Only guaranteed to support AutoValue 1.2-rc1

## Version 0.2.0 (2016-03-21)

Initial release. Only guaranteed to support AutoValue 1.2-rc1
