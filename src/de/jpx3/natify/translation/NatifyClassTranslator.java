package de.jpx3.natify.translation;

import de.jpx3.natify.NatifyLogger;
import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.controller.References;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public final class NatifyClassTranslator {
  public final static String[] LIBRARY_HEADERS_TO_IMPORT = new String[]{"jni.h", "string.h"/*, "math.h"*/};
  public final static int SCOPE_SPACING = 2;

  public static TextBlock processClass(TranslationConfiguration configuration, ClassNode classNode, List<MethodNode> methodNodes) {
    TextBlock source = TextBlock.newEmpty();
    // Header
    source.appendLines(
      "/**",
      " * Automatically generated C code for class " + classNode.name,
      " * by the Natify Processor in the Sevastopol Obfuscator",
      " */"
    );
    source.emptyLine();
    // Imports
    for (String libraryHeader : LIBRARY_HEADERS_TO_IMPORT) {
      source.appendLine("#include <" + libraryHeader + ">");
    }
    source.emptyLine();
    if (configuration.shouldObfuscateStrings()) {
      source.appendLine("static inline char* xorDecrypt(char *a, char *b, const size_t len) {");
      TextBlock stringEncryptionCode = TextBlock.newEmpty();
      {
        stringEncryptionCode.appendLines(
          "volatile char c[len]; memcpy((char*) c, b, len); memcpy(b, (char*) c, len);",
          "for(size_t i = 0; i < len; i++) a[i] ^= b[i]; return a;"
        );
      }
      source.append(stringEncryptionCode, SCOPE_SPACING);
      source.appendLine("}");
      source.emptyLine();
    }
    // methods
    Map<String, MethodNode> methodNames = new LinkedHashMap<>();
    References references = new References(configuration, classNode);
    long start = System.nanoTime();
//    int methodIndex = 0;
    TextBlock methods = TextBlock.newEmpty();
    for (MethodNode methodNode : methodNodes) {
//      if((methodNode.access & Opcodes.ACC_ABSTRACT) != 0) { // TODO: 09/22/20 change
//        continue;
//      }
      methods.appendLine("// " + methodNode.name + methodNode.desc);
      String methodName = /*methodNode.name.replace("<", "").replace(">", "") + "_" +*/ findMethodName();
      NatifyMethodTranslator natifyMethodTranslator = new NatifyMethodTranslator(configuration, classNode, references, methodNode, methodName);
      TextBlock codeBlock = natifyMethodTranslator.translateMethod();
      methods.append(codeBlock);
      methods.emptyLine();
      methodNames.put(methodName, methodNode);
//      methodIndex++;
    }
    TextBlock nativeRegistration = buildNativeRegistration(configuration, references, classNode.name, methodNames);
    long duration = System.nanoTime() - start;
    source.append(references.indySetup());
    source.append(references.mcfSetup());
    source.append(methods);
    source.emptyLine();
    // native registration
    source.append(nativeRegistration);
    NatifyLogger.printInfo("Translated class " + classNode.name + " in " + duration + "ns");
    return source;
  }

  private final static List<String> generatedMethodNames = new CopyOnWriteArrayList<>();

  private static String findMethodName() {
    String randomClassName;
    do {
      randomClassName = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase().substring(0, 8);
    } while (generatedMethodNames.contains(randomClassName));
    generatedMethodNames.add(randomClassName);
    return "natifyX" + randomClassName;
  }

  private final static int TABLE_ENTRY_SPACING = 2;

  /**
   * Generates something like
   *
   * <ul>
   * JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
   *     JNIEnv *env; (*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6);
   *
   *     JNINativeMethod table[] = {
   *             {"main", "([Ljava/lang/String;)V", (void *) &method0},
   *     };
   *     jclass thisClass = (*env)->FindClass(env, "path/to/my/Class");
   *     if(clazz == NULL) {
   *         (*env)->ExceptionDescribe(env);
   *         (*env)->ExceptionClear(env);
   *     } else {
   *         (*env)->RegisterNatives(env, clazz, table0, 2);
   *     }
   *     return JNI_VERSION_1_6;
   * }<ul>
   */
  private static TextBlock buildNativeRegistration(TranslationConfiguration configuration, References references, String className, Map<String, MethodNode> methodNodes) {
    TextBlock baseBlock = TextBlock.newEmpty();
    baseBlock.appendLine("// Dynamic registration");
    baseBlock.appendLine("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {");
    // code
    TextBlock codeBlock = TextBlock.newEmpty();
    {
      // load jni environment
      codeBlock.appendLine("JNIEnv *env; (*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6);");
      // load method table
      codeBlock.appendLine("JNINativeMethod table[] = {");
      TextBlock methodTableEntries = TextBlock.newEmpty();
      {
        int index = 0;
        for (Map.Entry<String, MethodNode> methodNodeEntry : methodNodes.entrySet()) {
          String cMethodName = methodNodeEntry.getKey();
          MethodNode methodNode = methodNodeEntry.getValue();
          String originalMethodName = methodNode.name;
          String originalMethodDesc = methodNode.desc;
          boolean last = index + 1 == methodNodes.size();

          if (originalMethodName.equalsIgnoreCase("<clinit>")) {
            originalMethodName = "nclinit";
          }

          methodTableEntries.appendLine("{" + stringProcess(configuration, originalMethodName) + ", " + stringProcess(configuration, originalMethodDesc) + ", (void *) &" + cMethodName + "}" + (last ? "" : ","));
          index++;
        }
      }
      codeBlock.append(methodTableEntries, TABLE_ENTRY_SPACING);
      codeBlock.appendLine("};");
      codeBlock.appendLines(
        "jclass thisClass = " + references.classAccess(className) + ";",
        "if(thisClass == NULL) {"
      );
      TextBlock invalidClassTarget = TextBlock.newEmpty();
      {
        invalidClassTarget.appendLine("(*env)->ExceptionDescribe(env);");
        invalidClassTarget.appendLine("(*env)->ExceptionClear(env);");
      }
      codeBlock.append(invalidClassTarget, SCOPE_SPACING);
      codeBlock.appendLine("} else {");
      TextBlock validClassTarget = TextBlock.newEmpty();
      {
        validClassTarget.appendLine("(*env)->RegisterNatives(env, thisClass, table, " + methodNodes.size() + ");");
      }
      codeBlock.append(validClassTarget, SCOPE_SPACING);
      codeBlock.appendLine("}");
      codeBlock.appendLine("return JNI_VERSION_1_6;");
    }
    // code end
    baseBlock.append(codeBlock, 2);
    baseBlock.appendLine("}");
    return baseBlock;
  }

  private static String stringProcess(TranslationConfiguration configuration, String input) {
    if (configuration.shouldObfuscateStrings()) {
      input += "\0";
      char[] chars = input.toCharArray();
      int[] key = new int[chars.length];
      int[] value = new int[chars.length];
      for (int i = 0; i < key.length; i++) {
        key[i] = ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
        value[i] = ((byte) chars[i]) ^ key[i];
      }
      return "/*" + input.substring(0, input.length() - 1) + "*/xorDecrypt(" + charArrayToC(value) + ", " + charArrayToC(key) + ", " + value.length + ")";
    } else {
      return "\"" + input + "\"";
    }
  }

  private static String charArrayToC(int[] chars) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < chars.length; i++) {
      int aChar = chars[i];
      boolean last = i + 1 == chars.length;
      stringBuilder.append(aChar);
      if (!last) {
        stringBuilder.append(", ");
      }
    }
    return "(char[]) {" + stringBuilder.toString() + "}";
  }
}
