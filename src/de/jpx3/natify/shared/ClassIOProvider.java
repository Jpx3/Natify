package de.jpx3.natify.shared;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

public final class ClassIOProvider {
  public static List<ClassNode> matchingClassesIn(File jarFile, TranslationConfiguration configuration) {
    return configuration.filterUsedClasses(classesIn(jarFile));
  }

  public static Map<ClassNode, List<MethodNode>> matchingMethodsIn(File jarFile, TranslationConfiguration configuration) {
    return configuration.filterActiveMethods(classesIn(jarFile));
  }

  public static Map<String, byte[]> compileClasses(List<ClassNode> classNodes) {
    Map<String, byte[]> map = new HashMap<>();
    for (ClassNode replacement : classNodes) {
      map.put(replacement.name + ".class", bytesFromClassNode(replacement));
    }
    return map;
  }

  public static void selectReplaceClasses(File inputFile, File outputFile, List<ClassNode> replacements) {
    selectReplaceEntries(inputFile, outputFile, compileClasses(replacements));
  }

  public static void selectReplaceClassesAndEntries(File inputFile, File outputFile, List<ClassNode> replacements, Map<String, byte[]> byteReplacements) {
    Map<String, byte[]> overallByteReplacement = new HashMap<>(compileClasses(replacements));
    overallByteReplacement.putAll(byteReplacements);
    selectReplaceEntries(inputFile, outputFile, overallByteReplacement);
  }

  public static void selectReplaceEntries(File inputFile, File outputFile, Map<String, byte[]> replacements) {
    Map<String, byte[]> jarEntries = findJarEntriesIn(inputFile);
    replacements.forEach(jarEntries::put);
    ZipOutputStream zipOutputStream = prepareOutputFile(outputFile);
    jarEntries.forEach((fileName, bytes) -> {
      ZipEntry newEntry = new ZipEntry(fileName);
      newEntry.setTime(0);
      try {
        zipOutputStream.putNextEntry(newEntry);
        zipOutputStream.write(bytes);
        zipOutputStream.closeEntry();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    try {
      zipOutputStream.close();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static ZipOutputStream prepareOutputFile(File output) {
    if (output.exists()) {
      throw new IllegalStateException("Output file already exists");
    }
    try {
      output.createNewFile();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not create new output file", exception);
    }
    try {
      ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
      zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);
      return zipOutputStream;
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  public static List<ClassNode> classesIn(File jarFile) {
    return Collections.unmodifiableList(new ArrayList<>(namedClassesIn(jarFile).values()));
  }

  public static Map<String, ClassNode> namedClassesIn(File jarFile) {
    Map<String, byte[]> jarEntries = findJarEntriesIn(jarFile);
    Map<String, ClassNode> classNodeMap = new HashMap<>();

    for (Map.Entry<String, byte[]> jarEntry : jarEntries.entrySet()) {
      String name = jarEntry.getKey();
      byte[] bytes = jarEntry.getValue();
      if (name.endsWith(".class") && hasJarHeader(bytes)) {
        classNodeMap.put(name, classNodeFromBytes(bytes));
      }
    }
    return classNodeMap;
  }

  private static boolean hasJarHeader(byte[] bytes) {
    return String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).toLowerCase().equals("cafebabe");
  }

  private static ClassNode classNodeFromBytes(byte[] bytes) {
    ClassReader cr = new ClassReader(bytes);
    ClassNode cn = new ClassNode();
    try {
      cr.accept(cn, EXPAND_FRAMES);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return cn;
  }

  private static byte[] bytesFromClassNode(ClassNode classNode) {
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    classNode.accept(classWriter);
    return classWriter.toByteArray();
  }

  private static Map<String, byte[]> findJarEntriesIn(File file) {
    try {
      JarFile jarFile = new JarFile(file);
      Map<String, byte[]> entryMap = new HashMap<>();
      Enumeration<JarEntry> jarEntries = jarFile.entries();
      while (jarEntries.hasMoreElements()) {
        JarEntry jarEntry = jarEntries.nextElement();
        String name = jarEntry.getName();
        byte[] bytes = toByteArray(jarFile.getInputStream(jarEntry));
        entryMap.put(name, bytes);
      }
      return entryMap;
    } catch (IOException e) {
      return new HashMap<>();
    }
  }

  private static byte[] toByteArray(InputStream inputStream) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int remaining;
    while (-1 != (remaining = inputStream.read(buffer))) {
      outputStream.write(buffer, 0, remaining);
    }
    return outputStream.toByteArray();
  }
}
