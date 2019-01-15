# Annotations are for embedding static analysis information.
-dontwarn org.jetbrains.annotations.**
-dontwarn com.google.errorprone.annotations;.**

# Retain generated TypeAdapters if annotated type is retained.
-if @com.ryanharter.auto.value.gson.GenerateTypeAdapter class *
-keep class <1>_GsonTypeAdapter {
    <init>(...);
    <fields>;
}
-if @com.ryanharter.auto.value.gson.GenerateTypeAdapter class **$*
-keep class <1>_<2>_GsonTypeAdapter {
    <init>(...);
    <fields>;
}
-if @com.ryanharter.auto.value.gson.GenerateTypeAdapter class **$*$*
-keep class <1>_<2>_<3>_GsonTypeAdapter {
    <init>(...);
    <fields>;
}
-if @com.ryanharter.auto.value.gson.GenerateTypeAdapter class **$*$*$*
-keep class <1>_<2>_<3>_<4>_GsonTypeAdapter {
    <init>(...);
    <fields>;
}
-if @com.ryanharter.auto.value.gson.GenerateTypeAdapter class **$*$*$*$*
-keep class <1>_<2>_<3>_<4>_<5>_GsonTypeAdapter {
    <init>(...);
    <fields>;
}
-if @com.ryanharter.auto.value.gson.GenerateTypeAdapter class **$*$*$*$*$*
-keep class <1>_<2>_<3>_<4>_<5>_<6>_GsonTypeAdapter {
    <init>(...);
    <fields>;
}
