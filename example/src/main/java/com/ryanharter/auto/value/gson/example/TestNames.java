package com.ryanharter.auto.value.gson.example;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class TestNames {
    public abstract int lowerCamel();
    public abstract int UpperCamel();
    public abstract int _lowerCamelLeadingUnderscore();
    public abstract int _UpperCamelLeadingUnderscore();
    public abstract int lower_words();
    public abstract int UPPER_WORDS();
    @SerializedName("annotatedName") public abstract int annotated();
    public abstract int lowerId();

    public static Builder builder() {
        return new AutoValue_TestNames.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder lowerCamel(int lowerCamel);
        public abstract Builder UpperCamel(int UpperCamel);
        public abstract Builder _lowerCamelLeadingUnderscore(int _lowerCamelLeadingUnderscore);
        public abstract Builder _UpperCamelLeadingUnderscore(int _UpperCamelLeadingUnderscore);
        public abstract Builder lower_words(int lower_words);
        public abstract Builder UPPER_WORDS(int UPPER_WORDS);
        @SerializedName("annotatedName") public abstract Builder annotated(int annotated);
        public abstract Builder lowerId(int lowerId);
        public abstract TestNames build();
    }

    public static TypeAdapter<TestNames> typeAdapter(Gson gson) {
        return new AutoValue_TestNames.GsonTypeAdapter(gson);
    }
}
