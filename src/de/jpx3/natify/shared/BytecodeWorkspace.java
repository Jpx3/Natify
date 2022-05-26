package de.jpx3.natify.shared;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BytecodeWorkspace {

  public static void x() {
    final boolean arch64 = System.getProperty("os.arch").contains("64");
    final String os = System.getProperty("os.name").toLowerCase();
    String s = null;
    if (os.contains("win") && arch64) {
      s = "<target path>";
    }
    if (os.contains("lin") && arch64) {
      s = "<target path>";
    }
    if (s == null) {
      throw new RuntimeException("Unknown operating system or architecture");
    }
    File tempFile;
    try {
      tempFile = File.createTempFile("richy", null);
      tempFile.deleteOnExit();
      if (!tempFile.exists()) {
        throw new IOException();
      }
    } catch (IOException ex2) {
      throw new UnsatisfiedLinkError("Failed to create temp file");
    }
    final byte[] array = new byte[2048];
    try {
      InputStream resourceAsStream = BytecodeWorkspace.class.getResourceAsStream(s);
      FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
      int read;
      while ((read = resourceAsStream.read(array)) != -1) {
        fileOutputStream.write(array, 0, read);
      }
    } catch (IOException ex) {
      throw new UnsatisfiedLinkError("Failed to copy file: " + ex.getMessage());
    }
    System.load(tempFile.getAbsolutePath());
  }

  public static void performLinkage(int classIndex) throws IOException, ClassNotFoundException {
    String osName = System.getProperty("os.name").toLowerCase();
    String osArch = System.getProperty("os.arch");
    if (!osArch.contains("64")) {
      throw new RuntimeException("Unsupported environment: Please use a 64-bit version of Java");
    }
    String[] supportedOs = {
      "win", "lin", "mac"
    };
    int osIndex = -1;
    for (int i = 0; i < supportedOs.length; i++) {
      String os = supportedOs[i];
      if (osName.contains(os)) {
        osIndex = i;
      }
    }
    if (osIndex < 0) {
      throw new RuntimeException("Unsupported environment: Your operating system is unsupported");
    }

    String className;
    String[] resourceBundle;

    switch (classIndex) {
      case 0:
        className = "de/jpx3/sevastopol/Test";
        resourceBundle = new String[]{
          "/natify/bin/aec658b3",
          "/natify/bin/aec658b3",
          "/natify/bin/aec658b3",
        };
        break;
      case 1:
        className = "de/jpx3/sevastopol/Test2";
        resourceBundle = new String[]{
          "/natify/bin/aec658b",
          "/natify/bin/aec658b",
          "/natify/bin/aec658b",
        };
        break;
      default:
        throw new IllegalStateException("Unknown index " + classIndex);
    }

    String resourceName = resourceBundle[osIndex];
    File tempFile = File.createTempFile("natify", null);
    tempFile.deleteOnExit();
    if (!tempFile.exists()) {
      throw new IOException("Unable to access temporary directory");
    }
    byte[] array = new byte[2048];
    Class<?> targetClass = Class.forName(className);
    InputStream resourceAsStream = targetClass.getResourceAsStream(resourceName);
    FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
    int read;
    while ((read = resourceAsStream.read(array)) != -1) {
      fileOutputStream.write(array, 0, read);
    }
    fileOutputStream.close();
    resourceAsStream.close();
    System.load(tempFile.getAbsolutePath());
  }
}
