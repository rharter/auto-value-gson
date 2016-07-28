package com.ryanharter.auto.value.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 * Created by rharter on 6/24/16.
 */
public final class AutoValueGsonTypeAdapterFactory implements TypeAdapterFactory {

  private static final String AUTOVALUE_PREFIX = "AutoValue_";

  @SuppressWarnings("unchecked")
  @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Class<? super T> rawType = type.getRawType();
    if (rawType.getSimpleName().startsWith(AUTOVALUE_PREFIX)) {
      // This is an AutoValue generated class, delegate to the generated type adapter
      return getTypeAdapter(rawType, gson);
    } else {
      // This isn't an AutoValue generated class, but it could be a class with the @AutoValue annotation.
      if (rawType.isPrimitive()) {
        return null;
      } else {
        // The AutoValue_ClassName class was found, now we must find the type adapter
        String packageName = ((Class) type.getRawType()).getPackage().getName();
        String autoValueClassSimpleName = AUTOVALUE_PREFIX +
            rawType.getName().substring(packageName.length() + 1).replace('$', '_');
        String autoValueClassName = packageName + "." + autoValueClassSimpleName;
        try {
          Class<?> autoValueClass = Class.forName(autoValueClassName);
          return getTypeAdapter(autoValueClass, gson);
        } catch (ClassNotFoundException ignored) {
          // There is no AutoValue generated class for this class, return null
          return null;
        }
      }
    }
  }

  /**
   * Gets the type adapter for the given AutoValue class. Recursively searches through AutoValue generated
   * classes. It starts by looking for AutoValue_Class$ClassTypeAdapter, then $AutoValue_Class$ClassTypeAdapter
   * until it finds the type adapter or crashes because something went terribly wrong.
   * @param type the type, must be an AutoValue generated class, e.g. AutoValue_MyClass
   * @param gson the gson instance
   * @return the type
   */
  TypeAdapter getTypeAdapter(Class type, Gson gson) {
    if (!type.getSimpleName().startsWith(AUTOVALUE_PREFIX)) {
      throw new IllegalArgumentException("This method can only retrieve the TypeAdapter for AutoValue classes");
    }
    return getTypeAdapter(type, gson, 0);
  }

  /**
   * Recursive helper method for {@link #getTypeAdapter(Class, Gson)}
   * @param type the type, must be an AutoValue generated class, e.g. AutoValue_MyClass
   * @param gson the gson instance
   * @param depth the type
   * @return the type adapter
   */
  private TypeAdapter getTypeAdapter(Class type, Gson gson, int depth) {
    String autoValueClassName = getAutoValueClassNameWithExtensionDepth(type, depth);
    int lastUnderscorePosition = type.getSimpleName().lastIndexOf("_");
    String annotatedSimpleName = type.getSimpleName().substring(lastUnderscorePosition + 1);
    String typeAdapterClassName = autoValueClassName + "$" + annotatedSimpleName + "TypeAdapter";
    if (getClass(autoValueClassName) != null) {
      Class typeAdapterClass = getClass(typeAdapterClassName);
      if (typeAdapterClass != null) {
        try {
          //noinspection unchecked
          TypeAdapter typeAdapter =
              (TypeAdapter) typeAdapterClass.getDeclaredConstructor(Gson.class).newInstance(gson);
          return typeAdapter.nullSafe();
        } catch (Exception e) {
          throw new RuntimeException("Failed to invoke the type adapter's constructor", e);
        }
      } else {
        return getTypeAdapter(type, gson, depth + 1);
      }
    } else {
      throw new RuntimeException("Reached the end fo the AutoValue extension chain for " +
          autoValueClassName + " without finding a type adapter. Is autovalue-gson installed?");
    }
  }
  /**
   * Gets the AutoValue generated class with the given extension depth. Eg, for a class called AutoValue_MyClass
   * and extension depth 2, this method would return $$AutoValue_MyClass.
   * @param type the type, must be an AutoValue generated class, e.g. AutoValue_MyClass
   * @param extensionDepth the extension depth
   * @return the class name for the specified extension depth
   */
  private String getAutoValueClassNameWithExtensionDepth(Class type, int extensionDepth) {
    StringBuilder classPrefixBuilder = new StringBuilder();
    for (int i = 1; i < extensionDepth; i++) {
      classPrefixBuilder.append('$');
    }
    return type.getPackage().getName() + "." + classPrefixBuilder.toString() + type.getSimpleName();
  }
  /**
   * Helper method to get a class, or return null if it doesn't exist rather than throwing an exception
   * @param className the class name
   * @return the class, or null if it didn't exist
   */
  private Class getClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException ignored) {
      return null;
    }
  }
}