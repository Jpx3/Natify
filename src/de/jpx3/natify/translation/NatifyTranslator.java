package de.jpx3.natify.translation;

import de.jpx3.natify.NatifyControl;
import de.jpx3.natify.NatifyLogger;
import de.jpx3.natify.shared.ClassIOProvider;
import de.jpx3.natify.shared.LinkageInformation;
import de.jpx3.natify.shared.NatifyClassLinkerClassFactory;
import de.jpx3.natify.shared.TranslationConfiguration;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NatifyTranslator {
  public static void process(
    File inputJarFile,
    File outputDirectory,
    TranslationConfiguration configuration
  ) {
    NatifyLogger.printInfo("Input jar: " + inputJarFile.getAbsolutePath());
    NatifyLogger.printInfo("Output directory: " + outputDirectory.getAbsolutePath());

    NatifyLogger.printInfo("Clearing output directory..");
    File[] files = outputDirectory.listFiles((dir, name) -> name.endsWith(".c"));
    if (files != null) {
      for (File file : files) {
        NatifyLogger.printInfo("Deleted " + file);
        file.delete();
      }
    }

    NatifyLogger.printInfo("Loading classes..");
    Map<ClassNode, List<MethodNode>> targetMethods = ClassIOProvider.matchingMethodsIn(inputJarFile, configuration);

    // generate NatifyClassLinker class
    NatifyLogger.printInfo("Constructing class linker..");
    LinkageInformation linkageInformation = generateLinkageInformationOf(configuration, targetMethods.keySet());

    ClassNode classLinkerClass = NatifyClassLinkerClassFactory.generateNatifyClassLinkerClass(linkageInformation);
    targetMethods.put(classLinkerClass, classLinkerClass.methods);

    NatifyLogger.printInfo("Translating classes..");
    performTranslation(outputDirectory, configuration, linkageInformation, targetMethods);
    linkageInformation.saveTo(new File(outputDirectory, LinkageInformation.DEFAULT_FILE_NAME));

    NatifyLogger.printInfo("Terminating normally");
  }

  private static LinkageInformation generateLinkageInformationOf(
    TranslationConfiguration configuration,
    Collection<ClassNode> classNodes
  ) {
    return LinkageInformation.constructFrom(configuration, classNodes);
  }

  private static void performTranslation(
    File outputDirectory,
    TranslationConfiguration configuration,
    LinkageInformation linkageInformation,
    Map<ClassNode, List<MethodNode>> targetMethods
  ) {
    if (configuration.useParallelProcessing()) {
      AsyncTaskProcessor processor = new AsyncTaskProcessor();
      processor.aquire(targetMethods.size());
      targetMethods.forEach((classNode, methods) -> processor.push(() -> {
        TextBlock sourcecode = NatifyClassTranslator.processClass(configuration, classNode, methods);
        saveSourceTo(classNode, linkageInformation, outputDirectory, sourcecode);
      }));
      processor.await();
    } else {
      for (Map.Entry<ClassNode, List<MethodNode>> classNodeListEntry : targetMethods.entrySet()) {
        ClassNode classNode = classNodeListEntry.getKey();
        List<MethodNode> methods = classNodeListEntry.getValue();
        TextBlock sourcecode = NatifyClassTranslator.processClass(configuration, classNode, methods);
        saveSourceTo(classNode, linkageInformation, outputDirectory, sourcecode);
      }
    }
  }

  private synchronized static void saveSourceTo(ClassNode classNode, LinkageInformation linkageInformation, File outputDirectory, TextBlock sourcecode) {
    String fileName;
    if (NatifyControl.USE_CLASS_NAME_AS_C_TARGET_NAME) {
      fileName = classNode.name.replace("/", "_");
//    } else {
//      fileName = findNewName(linkageInformation.classBindings());
//    }
    } else {
      fileName = "natify-" + findNewCleanName(linkageInformation.classBindings());
    }
    linkageInformation.bindClass(classNode.name, fileName);
    fileName += ".c";
    File file = new File(outputDirectory, fileName);
    if (file.exists()) {
      file.delete();
    }
    try {
      file.createNewFile();
      BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
      writer.write(sourcecode.stringifyWith(0));
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String findNewName(Collection<String> context) {
    String name;
    do {
      name = findNewName();
    } while (context.contains(name));
    return name;
  }

  private static String findNewCleanName(Collection<String> context) {
    return newLowercaseNameByIndex(context.size());
  }

  private final static String AVAILABLE_DIGITS_LOWERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789".toLowerCase();

  public static String newLowercaseNameByIndex(int index) {
    if (index <= 0) {
      return String.valueOf(AVAILABLE_DIGITS_LOWERCASE.charAt(0));
    }
    int base = AVAILABLE_DIGITS_LOWERCASE.length();
    StringBuilder output = new StringBuilder();
    while (index > 0) {
      int digit = index % base;
      output.insert(0, AVAILABLE_DIGITS_LOWERCASE.charAt(digit));
      index = index / base;
    }
    return output.toString();
  }

  private static String findNewName() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  }
}
