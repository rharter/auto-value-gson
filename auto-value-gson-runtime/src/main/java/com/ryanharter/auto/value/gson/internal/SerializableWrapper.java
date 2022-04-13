// modified by mapbox
package com.ryanharter.auto.value.gson.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializableWrapper {

  private JsonElement element;

  public SerializableWrapper(JsonElement element) {
    this.element = element;
  }

  public JsonElement getElement() {
    return this.element;
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
