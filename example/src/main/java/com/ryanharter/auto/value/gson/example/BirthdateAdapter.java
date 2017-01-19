package com.ryanharter.auto.value.gson.example;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BirthdateAdapter extends TypeAdapter<Date> {
  private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

  @Override public void write(JsonWriter out, Date value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      String date = df.format(value);
      out.value(date);
    }
  }

  @Override public Date read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    String stringDate = in.nextString();
    try {
      return df.parse(stringDate);
    } catch (ParseException e) {
      throw new DateTypeParseException(stringDate);
    }
  }

  public class DateTypeParseException extends RuntimeException {
    public DateTypeParseException(String stringDate) {
      super("Unparseable date: " + stringDate + ". It must be in \"yyyy-MM-dd\" format");
    }
  }
}
