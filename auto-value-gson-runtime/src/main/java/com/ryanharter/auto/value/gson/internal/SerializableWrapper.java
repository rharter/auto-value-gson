// modified by mapbox
package com.ryanharter.auto.value.gson.internal;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

public class SerializableWrapper implements Serializable {

  private JsonElement element;

  public SerializableWrapper(JsonElement element) {
    this.element = element;
  }

  public JsonElement getElement() {
    return this.element;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SerializableWrapper that = (SerializableWrapper) o;
    return Objects.equals(element, that.element);
  }

  @Override public int hashCode() {
    return Objects.hash(element);
  }

  private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException
  {
    String json = aInputStream.readUTF();
    element = new GsonBuilder().create().fromJson(json, JsonElement.class);
  }

  private void writeObject(ObjectOutputStream aOutputStream) throws IOException
  {
    String json = element.toString();
    aOutputStream.writeUTF(json);
  }
}
