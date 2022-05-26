package de.jpx3.natify.shared;

import java.util.Arrays;

public enum OperatingSystem {
  WINDOWS("win"),
  LINUX("lin"),
  MAC("mac");

  final String pathName;

  OperatingSystem(String pathName) {
    this.pathName = pathName;
  }

  public String pathName() {
    return pathName;
  }

  public static OperatingSystem byPathName(String pathName) {
    return Arrays.stream(OperatingSystem.values())
      .filter(value -> value.pathName.equalsIgnoreCase(pathName))
      .findFirst()
      .orElse(null);
  }
}
