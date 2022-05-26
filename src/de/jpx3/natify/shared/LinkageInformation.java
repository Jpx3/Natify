package de.jpx3.natify.shared;

import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LinkageInformation implements Serializable {
  private final static String UNIQUE_BUILD_ID = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  public final static String DEFAULT_FILE_NAME = "natify-linkage.dat";

  private final String uniqueBuildId = UNIQUE_BUILD_ID;
  private final String[] supportedOperatingSystem;
  private final List<String> classes = new CopyOnWriteArrayList<>();
  //  private final Map<String, List<String>> methods = new HashMap<>();
  private final Map<String, String> classBinaryBindingsPathMap = new HashMap<>();
  private final Map<String, Map<String, String>> classNameToBinaryMap = new LinkedHashMap<>();

  public LinkageInformation(String[] supportedOperatingSystem) {
    this.supportedOperatingSystem = supportedOperatingSystem;
  }

  public void bindClass(String normalName, String binaryNameOnFS) {
    classBinaryBindingsPathMap.put(normalName, binaryNameOnFS);
  }

  public String lookupBindingOf(String normalName) {
    return classBinaryBindingsPathMap.get(normalName);
  }

  public Collection<String> classBindings() {
    return classBinaryBindingsPathMap.values();
  }

  public void enterClass(String className, String operatingSystem, String binaryName) {
    if (!classes.contains(className)) {
      classes.add(className);
    }
    Map<String, String> stringStringMap = classNameToBinaryMap.computeIfAbsent(className, s -> new LinkedHashMap<>());
    stringStringMap.put(operatingSystem, binaryName);
  }

  public String binaryNameOf(String className, String operatingSystem) {
    return binariesOf(className).get(operatingSystem);
  }

  public Map<String, String> binariesOf(String className) {
    return classNameToBinaryMap.get(className);
  }

  public int classIndexOf(String className) {
    return classes.indexOf(className);
  }

  public List<String> classes() {
    return classes;
  }

  public String[] supportedOperatingSystems() {
    return supportedOperatingSystem;
  }

  public String uniqueBuildId() {
    return uniqueBuildId;
  }

//  public void enterMethod(ClassNode owner, MethodNode method) {
//    String methodKey = method.name + method.desc;
//    List<String> methods = this.methods.computeIfAbsent(owner.name, s -> new ArrayList<>());
//    if(!methods.contains(methodKey)) {
//      methods.add(methodKey);
//    }
//  }

  public void saveTo(File file) {
    try {
      file.delete();
      file.createNewFile();
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
      objectOutputStream.writeObject(this);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static LinkageInformation loadFrom(File file) {
    try {
      FileInputStream fileInputStream = new FileInputStream(file);
      ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
      return (LinkageInformation) objectInputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  public static LinkageInformation constructFrom(
    TranslationConfiguration configuration,
    Collection<ClassNode> classNodes
  ) {
    classNodes = new ArrayList<>(classNodes);
    List<String> binaryFileNames = new ArrayList<>();
    List<OperatingSystem> supportedOperatingSystems = configuration.supportedOperatingSystems();
    String[] supportedOsAsStringArray = new String[supportedOperatingSystems.size()];
    for (int i = 0; i < supportedOperatingSystems.size(); i++) {
      OperatingSystem supportedOperatingSystem = supportedOperatingSystems.get(i);
      supportedOsAsStringArray[i] = supportedOperatingSystem.pathName();
    }
    LinkageInformation linkageInformation = new LinkageInformation(supportedOsAsStringArray);
    for (ClassNode classNode : classNodes) {
      for (OperatingSystem supportedOperatingSystem : supportedOperatingSystems) {
        String newBinaryName = findNewName(binaryFileNames);
        String operatingSystemName = supportedOperatingSystem.pathName();
        linkageInformation.enterClass(classNode.name, operatingSystemName, newBinaryName);
      }
    }
    // keep this
    for (OperatingSystem supportedOperatingSystem : supportedOperatingSystems) {
      String newBinaryName = findNewName(binaryFileNames);
      String operatingSystemName = supportedOperatingSystem.pathName();
      linkageInformation.enterClass("sevastopol/natify/" + UNIQUE_BUILD_ID + "/NatifyClassLinker", operatingSystemName, newBinaryName);
    }
    return linkageInformation;
  }

  private static String findNewName(Collection<String> context) {
    String name;
    do {
      name = findNewName();
    } while (context.contains(name));
    context.add(name);
    return name;
  }

  private final static String PREFIX = "sevastopol/natify/" + UNIQUE_BUILD_ID + "/bin/";

  private final static String SUFFIX = ""; // no suffix

  private static String findNewName() {
    return PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + SUFFIX;
  }
}
