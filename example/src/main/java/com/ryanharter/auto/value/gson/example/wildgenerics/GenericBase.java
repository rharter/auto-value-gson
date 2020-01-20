package com.ryanharter.auto.value.gson.example.wildgenerics;

import java.util.List;

public abstract class GenericBase<T> {

  public abstract T generic();

  public abstract String notGeneric();

  public abstract List<T> collection();

  public interface Builder<T, B extends Builder<T, B>> {
    B generic(T generic);

    B notGeneric(String notGeneric);

    B collection(List<T> collection);
  }
}
