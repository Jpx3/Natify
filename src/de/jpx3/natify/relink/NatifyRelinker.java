package de.jpx3.natify.relink;

import de.jpx3.natify.NatifyLogger;
import de.jpx3.natify.shared.*;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public final class NatifyRelinker {
  public static void process(File inputJar, File inputDirectory, File outputJar, TranslationConfiguration configuration) {
    NatifyLogger.printInfo("Loading input jar..");
    List<ClassNode> classNodes = new ArrayList<>(ClassIOProvider.matchingClassesIn(inputJar, configuration));
    NatifyLogger.printInfo("Loading linkage data");
    LinkageInformation linkageInformation = LinkageInformation.loadFrom(new File(inputDirectory, LinkageInformation.DEFAULT_FILE_NAME));
    NatifyLogger.printInfo("Loading compiled C libraries..");

    int lastOperatingSystemFiles = -1;
    Map<String, byte[]> natifyNativeLibs = new HashMap<>();
    for (ClassNode classNode : classNodes) {
      Map<OperatingSystem, File> operatingSystemFileMap = classLinkedFiles(classNode, linkageInformation, inputDirectory);
      Set<OperatingSystem> foundOperatingSystems = operatingSystemFileMap.keySet();

      String[] supportedOperatingSystems = linkageInformation.supportedOperatingSystems();
      for (OperatingSystem foundOperatingSystem : foundOperatingSystems) {
        if (!arrayContains(supportedOperatingSystems, foundOperatingSystem.pathName())) {
          NatifyLogger.printInfo("Found .natify library file for " + foundOperatingSystem + " (class " + classNode.name + "), but " + foundOperatingSystem + " isn't supported in your configuration");
        }
      }

      for (String supportedOperatingSystem : supportedOperatingSystems) {
        boolean exists = false;
        for (OperatingSystem foundOperatingSystem : foundOperatingSystems) {
          if (foundOperatingSystem.pathName().equalsIgnoreCase(supportedOperatingSystem)) {
            exists = true;
            break;
          }
        }
        if (!exists) {
          String natifyLibraryName = linkageInformation.lookupBindingOf(classNode.name) + ".natify";
          String errorDescription = "Unable to find .natify library associated with class " + classNode.name + " for " + OperatingSystem.byPathName(supportedOperatingSystem) + ".";
          String expectedFileLocation = "Expected file at " + new File(inputDirectory, OperatingSystem.byPathName(supportedOperatingSystem).pathName() + "/" + natifyLibraryName).getAbsolutePath();
          String errorMessage = errorDescription + " " + expectedFileLocation;
          throw new IllegalStateException(errorMessage);
        }
      }
      if (lastOperatingSystemFiles > 0 && lastOperatingSystemFiles != operatingSystemFileMap.size()) {
        throw new IllegalStateException("Something went wrong processing libraries");
      }

      List<MethodNode> methodNodesToTranslate = configuration.filterActiveMethods(classNode);
      clearAndNatifyMethods(methodNodesToTranslate);
      classInjection(classNode, methodNodesToTranslate, linkageInformation);

      for (OperatingSystem operatingSystem : operatingSystemFileMap.keySet()) {
        String newName = linkageInformation.binaryNameOf(classNode.name, operatingSystem.pathName());
        File file = operatingSystemFileMap.get(operatingSystem);
        byte[] fileBytes = readFileBytes(file);
        natifyNativeLibs.put(newName, fileBytes);
      }
      NatifyLogger.printInfo("Relinked " + classNode.name);
      lastOperatingSystemFiles = operatingSystemFileMap.size();
    }
    NatifyLogger.printInfo("Constructing class linker..");
    ClassNode classLinkerClass = NatifyClassLinkerClassFactory.generateNatifyClassLinkerClass(linkageInformation);
    List<MethodNode> userMethods = classLinkerClass.methods;

    // start comment to ignore linkage translation injection
    clearAndNatifyMethods(userMethods);
    classLinkerClassInjection(classLinkerClass, linkageInformation);
    // end comment
    classNodes.add(classLinkerClass);
    Map<OperatingSystem, File> operatingSystemFileMap = classLinkedFiles(classLinkerClass, linkageInformation, inputDirectory);
    for (OperatingSystem operatingSystem : operatingSystemFileMap.keySet()) {
      String newName = linkageInformation.binaryNameOf(classLinkerClass.name, operatingSystem.pathName());
      File file = operatingSystemFileMap.get(operatingSystem);
      byte[] fileBytes = readFileBytes(file);
      natifyNativeLibs.put(newName, fileBytes);
    }

    NatifyLogger.printInfo("Saving to output jar..");
    ClassIOProvider.selectReplaceClassesAndEntries(inputJar, outputJar, classNodes, natifyNativeLibs);
    NatifyLogger.printInfo("Terminating normally");
  }

  private static <R> boolean arrayContains(R[] array, R value) {
    int hash = value.hashCode();
    return Arrays.stream(array).anyMatch(r -> hash == r.hashCode() && r.equals(value));
  }

  private static void clearAndNatifyMethodsIn(ClassNode classNode, TranslationConfiguration configuration) {
    clearAndNatifyMethods(configuration.filterActiveMethods(classNode));
  }

  private static void clearAndNatifyMethods(List<MethodNode> methodNodes) {
    for (MethodNode methodNode : methodNodes) {
      methodNode.access |= ACC_NATIVE /*| ACC_SYNTHETIC*/;
      methodNode.instructions.clear();
    }
  }

  private static byte[] readFileBytes(File file) {
    try {
      // create FileInputStream object
      FileInputStream fin = new FileInputStream(file);
      byte[] fileContent = new byte[(int) file.length()];
      // Reads up to certain bytes of data from this input stream into an array of bytes.
      fin.read(fileContent);
      //create string from byte array
      return fileContent;
    } catch (IOException ignored) {
    }
    throw new IllegalStateException();
  }

  private static void classLinkerClassInjection(
    ClassNode classNode,
    LinkageInformation linkageInformation
  ) {
    MethodNode method = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
    method.instructions = InjectionBytecodeGenerator.generate(classNode.name, linkageInformation.binariesOf(classNode.name));
    classNode.methods.add(method);
    method.maxStack = method.maxLocals = 10;
  }

  private static void classInjection(
    ClassNode classNode,
    List<MethodNode> methodNodesToTranslate,
//    TranslationConfiguration configuration,
    LinkageInformation linkageInformation
  ) {
    int classIndex = linkageInformation.classIndexOf(classNode.name);
    boolean exportClassInit = exportClassInitRequired(methodNodesToTranslate);
    boolean hasClassInitMethod = false;
    for (MethodNode method : classNode.methods) {
      if (method.name.equalsIgnoreCase("<clinit>")) {
        hasClassInitMethod = true;
        break;
      }
    }
    if (methodNodesToTranslate.isEmpty()) {
      return;
    }
    if (exportClassInit || !hasClassInitMethod) {
      MethodNode method = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
      method.instructions = (generateLinkageInstructions(linkageInformation, classIndex, true));
      if (hasClassInitMethod) {
        for (MethodNode otherMethod : classNode.methods) {
          if (otherMethod.name.equalsIgnoreCase("<clinit>")) {
            otherMethod.access |= ACC_PUBLIC;
            otherMethod.name = "nclinit";
          }
        }
        AbstractInsnNode previous = method.instructions.getLast().getPrevious();
        method.instructions.insert(previous, new MethodInsnNode(INVOKESTATIC, classNode.name, "nclinit", "()V"));
      }
      classNode.methods.add(method);
      method.maxStack = method.maxLocals = 10;
    } else {
      for (MethodNode method : classNode.methods) {
        if (method.name.equalsIgnoreCase("<clinit>")) {
          method.instructions.insert(generateLinkageInstructions(linkageInformation, classIndex, false));
//          method.maxStack += 10;
//          method.maxLocals += 10;
        }
      }
    }
  }

  private static InsnList generateLinkageInstructions(LinkageInformation linkageInformation, int classIndex, boolean returnStatement) {
    InsnList insnNodes = new InsnList();
    if (classIndex <= 5) {
      insnNodes.add(new InsnNode(ICONST_0 + classIndex));
    } else if (classIndex <= 127) {
      insnNodes.add(new IntInsnNode(BIPUSH, classIndex));
    } else {
      insnNodes.add(new IntInsnNode(SIPUSH, classIndex));
    }
    insnNodes.add(new MethodInsnNode(INVOKESTATIC, "sevastopol/natify/" + linkageInformation.uniqueBuildId() + "/NatifyClassLinker", "performLinkage", "(I)V"));
    if (returnStatement) {
      insnNodes.add(new InsnNode(RETURN));
    }
    return insnNodes;
  }

  private static boolean exportClassInitRequired(List<MethodNode> methodNodesToTranslate) {
    // we check if a <clinit> method has to be translated
    for (MethodNode filterActiveMethod : methodNodesToTranslate) {
      if (filterActiveMethod.name.equalsIgnoreCase("<clinit>")) {
        return true;
      }
    }
    return false;
  }

  public static Map<OperatingSystem, File> classLinkedFiles(ClassNode classNode, LinkageInformation linkageInformation, File inputDirectory) {
    String natifyFilePath = linkageInformation.lookupBindingOf(classNode.name) + ".natify";
    Map<OperatingSystem, File> fileMap = new EnumMap<>(OperatingSystem.class);
    for (OperatingSystem value : OperatingSystem.values()) {
      File targetFile = new File(inputDirectory, value.pathName() + "/" + natifyFilePath);
      if (targetFile.exists()) {
        fileMap.put(value, targetFile);
      }
    }
    return fileMap;
  }

}
