package de.jpx3.natify.translation.controller;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static de.jpx3.natify.translation.NatifyClassTranslator.SCOPE_SPACING;

/**
 *
 * This class has been abandoned as caching classes would violate omgorgreg
 *
 */
public final class References {
  private final static String CLASS_CACHE_NAME = "classCache";
  private final static String METHOD_CACHE_NAME = "methodCache";
  private final static String FIELD_CACHE_NAME = "fieldCache";

  private final static String CLASS_LOOKUP_CACHE_NAME = "classLookup";
  private final static String METHOD_LOOKUP_CACHE_NAME = "methodLookup";
  private final static String FIELD_LOOKUP_CACHE_NAME = "fieldLookup";

  private final static String CLASS_CACHE_ACCESSOR_NAME = "classOf";//"classAccess";
  private final static String METHOD_CACHE_ACCESSOR_NAME = "methodOf";//"methodAccess";
  private final static String FIELD_CACHE_ACCESSOR_NAME = "fieldOf";//"fieldAccess";

  public final static String INDY_CALLSITE_CACHE_NAME = "indycs";

  private final static int VIRTUAL_TAG = 0;
  private final static int STATIC_TAG = 1;

  private final boolean referenceOptimization;

  private final ClassNode owningClass;
  private final TranslationConfiguration configuration;
  private final List<String> classCache = new ArrayList<>();
  private final List<Handle> methodIdCache = new ArrayList<>();
  private final List<Handle> fieldIdCache = new ArrayList<>();
  private final List<InvokeDynamicInsnNode> invokeDynamics = new ArrayList<>();

  public References(TranslationConfiguration configuration, ClassNode owningClass) {
    this.configuration = configuration;
    this.owningClass = owningClass;
    this.referenceOptimization = configuration.useReferenceOptimization();
  }

  public String classAccess(String className) {
    if(referenceOptimization) {
      if(!classCache.contains(className)) {
        classCache.add(className);
      }
      int index = classCache.indexOf(className);
      return CLASS_CACHE_ACCESSOR_NAME + "(env, " + index + ")";
    } else {
      return "(*env)->FindClass(env, " + stringProcess(className) + ")";
    }
  }

  public String methodIdAccess(Handle handle, boolean isStatic) {
    if(referenceOptimization) {
      handle.tag = isStatic ? STATIC_TAG : VIRTUAL_TAG;
      if(!methodIdCache.contains(handle)) {
        methodIdCache.add(handle);
      }
      int index = methodIdCache.indexOf(handle);
      return METHOD_CACHE_ACCESSOR_NAME + "(env, " + index + ")";
    } else {
      String methodName = isStatic ? "GetStaticMethodID" : "GetMethodID";
      return "(*env)->" + methodName + "(env, " + classAccess(handle.getOwner()) + ", " + stringProcess(handle.getName()) + ", " + stringProcess(handle.getDesc()) + ")";
    }
  }

  public String fieldIdAccess(Handle handle, boolean isStatic) {
    if(referenceOptimization) {
      handle.tag = isStatic ? STATIC_TAG : VIRTUAL_TAG;
      if(!fieldIdCache.contains(handle)) {
        fieldIdCache.add(handle);
      }
      int index = fieldIdCache.indexOf(handle);
      return FIELD_CACHE_ACCESSOR_NAME + "(env, " + index + ")";
    } else {
      String methodName = isStatic ? "GetStaticFieldID" : "GetFieldID";
      return "(*env)->" + methodName + "(env, " + classAccess(handle.getOwner()) + ", " + stringProcess(handle.getName()) + ", " + stringProcess(handle.getDesc()) + ")";
    }
  }

  public int registerIndy(InvokeDynamicInsnNode invokeDynamicInsnNode) {
    if(!invokeDynamics.contains(invokeDynamicInsnNode)) {
      invokeDynamics.add(invokeDynamicInsnNode);
    }
    return invokeDynamics.indexOf(invokeDynamicInsnNode);
  }

  public TextBlock indySetup() {
    TextBlock methodSpace = TextBlock.newEmpty();
    if(!invokeDynamics.isEmpty()) {
      methodSpace.appendLine("jobject "+INDY_CALLSITE_CACHE_NAME+"[" + invokeDynamics.size() + "] = {NULL};");
      methodSpace.emptyLine();
    }
    return methodSpace;
  }

  public TextBlock mcfSetup() {
    TextBlock methodSpace = TextBlock.newEmpty();

    for (Handle handle : methodIdCache) {
      classAccess(handle.getOwner());
    }
    for (Handle handle : fieldIdCache) {
      classAccess(handle.getOwner());
    }

    boolean classCacheRequired = !classCache.isEmpty();
    boolean methodCacheRequired = !methodIdCache.isEmpty();
    boolean fieldCacheRequired = !fieldIdCache.isEmpty();

    if(classCacheRequired) {
      appendClassCacheEnvironment(methodSpace);
      methodSpace.emptyLine();
    }
    if(methodCacheRequired || fieldCacheRequired) {
      createAbstractReferenceStruct(methodSpace);
      methodSpace.emptyLine();
    }
    if(methodCacheRequired) {
      appendMethodCacheEnvironment(methodSpace);
    }
    if(fieldCacheRequired) {
      if(methodCacheRequired) {
        methodSpace.emptyLine();
      }
      appendFieldCacheEnvironment(methodSpace);
    }
    if(methodCacheRequired || fieldCacheRequired) {
      methodSpace.emptyLine();
    }
    return methodSpace;
  }

  private void createAbstractReferenceStruct(TextBlock methodSpace) {
    methodSpace.appendLine("typedef struct {");
    TextBlock structDat = TextBlock.newEmpty();
    {
      structDat.appendLine("int staticAccess;");
      structDat.appendLine("unsigned int owner;");
      structDat.appendLine("char *name;");
      structDat.appendLine("char *desc;");
    }
    methodSpace.append(structDat, SCOPE_SPACING);
    methodSpace.appendLine("} reference_t;");
  }

  private void appendClassCacheEnvironment(TextBlock methodSpace) {
    int classCacheSize = classCache.size();
    List<String> classNames = new ArrayList<>();

    int largestStringSize = 0;
    for (String clazz : classCache) {
      String stringInput = stringProcess(clazz);
      largestStringSize = Math.max(largestStringSize, stringInput.length());
      classNames.add(stringInput);
    }

    methodSpace.appendLine("jclass " + CLASS_CACHE_NAME + "["+classCacheSize+"] = {NULL};");
    methodSpace.appendLine("char *" + CLASS_LOOKUP_CACHE_NAME + "[] = {");
    TextBlock lookupEntrySpace = TextBlock.newEmpty();
    {
      for (int i = 0; i < classNames.size(); i++) {
        String classNameEntry = classNames.get(i);
        boolean last = i + 1 == classNames.size();
        if (!last) {
          classNameEntry += (", ");
        }
        lookupEntrySpace.appendLine(classNameEntry);
      }
    }
    methodSpace.append(lookupEntrySpace, SCOPE_SPACING);
    methodSpace.appendLine("};");
    methodSpace.appendLine("");
    methodSpace.appendLine("static jclass " + CLASS_CACHE_ACCESSOR_NAME + "(JNIEnv *env, unsigned int identifier) {");
    TextBlock methodCacheAccessorMethod = TextBlock.newEmpty();
    {
      methodCacheAccessorMethod.appendLine("jclass clazz = " + CLASS_CACHE_NAME + "[identifier];");
      methodCacheAccessorMethod.appendLine("if(clazz == NULL) {");
      TextBlock lookupMethodCode = TextBlock.newEmpty();
      {
        lookupMethodCode.appendLines(
          "clazz = (*env)->FindClass(env, " + CLASS_LOOKUP_CACHE_NAME +"[identifier]);",
          "clazz = (jclass) (*env)->NewGlobalRef(env, clazz);",
          CLASS_CACHE_NAME + "[identifier] = clazz;"
        );
      }
      methodCacheAccessorMethod.append(lookupMethodCode, SCOPE_SPACING);
      methodCacheAccessorMethod.appendLines("}");
      methodCacheAccessorMethod.appendLines("return clazz;");
    }
    methodSpace.append(methodCacheAccessorMethod, SCOPE_SPACING);
    methodSpace.appendLine("}");
  }

  private void appendMethodCacheEnvironment(TextBlock methodSpace) {
    int methodCacheSize = methodIdCache.size();
    methodSpace.appendLine("jmethodID " + METHOD_CACHE_NAME + "["+methodCacheSize+"] = {NULL};");
    methodSpace.appendLine("reference_t " + METHOD_LOOKUP_CACHE_NAME + "[] = {");
    TextBlock lookupEntrySpace = TextBlock.newEmpty();
    {
      for (int i = 0; i < methodIdCache.size(); i++) {
        Handle handle = methodIdCache.get(i);
        boolean targetStatic = handle.tag == STATIC_TAG;
        int owner = classCache.indexOf(handle.getOwner());//stringProcess(handle.getOwner());
        String name = stringProcess(handle.getName());
        String desc = stringProcess(handle.getDesc());
        boolean last = i + 1 == methodIdCache.size();
        String staticAccess = targetStatic ? "1" : "0";
        String line = "{" + staticAccess + ", " + owner + ", " + name + ", " + desc + "}";
        if(!last) {
          line += ",";
        }
        lookupEntrySpace.appendLine(line);
      }
    }
    methodSpace.append(lookupEntrySpace, SCOPE_SPACING);
    methodSpace.appendLine("};");
    methodSpace.appendLine("");
    methodSpace.appendLine("static jmethodID " + METHOD_CACHE_ACCESSOR_NAME + "(JNIEnv *env, unsigned int identifier) {");
    TextBlock methodCacheAccessorMethod = TextBlock.newEmpty();
    {
      methodCacheAccessorMethod.appendLine("jmethodID methodid = " + METHOD_CACHE_NAME + "[identifier];");
      methodCacheAccessorMethod.appendLine("if(methodid == NULL) {");
      TextBlock lookupMethodCode = TextBlock.newEmpty();
      {
        lookupMethodCode.appendLines(
          "reference_t methodRef = " + METHOD_LOOKUP_CACHE_NAME + "[identifier];",
          "jclass owningClass = "+CLASS_CACHE_ACCESSOR_NAME+"(env, methodRef.owner);",
          "if(methodRef.staticAccess) {"
        );
        TextBlock staticHandle = TextBlock.newEmpty();
        {
          staticHandle.appendLine("methodid = (*env)->GetStaticMethodID(env, owningClass, methodRef.name, methodRef.desc);");
        }
        lookupMethodCode.append(staticHandle, SCOPE_SPACING);
        lookupMethodCode.appendLine("} else {");
        TextBlock nonstaticHandle = TextBlock.newEmpty();
        {
          nonstaticHandle.appendLine("methodid = (*env)->GetMethodID(env, owningClass, methodRef.name, methodRef.desc);");
        }
        lookupMethodCode.append(nonstaticHandle, SCOPE_SPACING);
        lookupMethodCode.appendLine("}");
        lookupMethodCode.appendLine(METHOD_CACHE_NAME + "[identifier] = methodid;");
      }
      methodCacheAccessorMethod.append(lookupMethodCode, SCOPE_SPACING);
      methodCacheAccessorMethod.appendLines("}");
      methodCacheAccessorMethod.appendLines("return methodid;");
    }
    methodSpace.append(methodCacheAccessorMethod, SCOPE_SPACING);
    methodSpace.appendLine("}");
  }

  private void appendFieldCacheEnvironment(TextBlock methodSpace) {
    int methodCacheSize = fieldIdCache.size();
    methodSpace.appendLine("jfieldID " + FIELD_CACHE_NAME + "["+methodCacheSize+"] = {NULL};");
    methodSpace.appendLine("reference_t " + FIELD_LOOKUP_CACHE_NAME + "[] = {");
    TextBlock lookupEntrySpace = TextBlock.newEmpty();
    {
      for (int i = 0; i < fieldIdCache.size(); i++) {
        Handle handle = fieldIdCache.get(i);
        boolean targetStatic = handle.tag == STATIC_TAG;
        int owner = classCache.indexOf(handle.getOwner());//stringProcess(handle.getOwner());
        String name = stringProcess(handle.getName());
        String desc = stringProcess(handle.getDesc());
        boolean last = i + 1 == fieldIdCache.size();
        String staticAccess = targetStatic ? "1" : "0";
        String line = "{" + staticAccess + ", " + owner + ", " + name + ", " + desc + "}";
        if(!last) {
          line += ",";
        }
        lookupEntrySpace.appendLine(line);
      }
    }
    methodSpace.append(lookupEntrySpace, SCOPE_SPACING);
    methodSpace.appendLine("};");
    methodSpace.appendLine("static jfieldID " + FIELD_CACHE_ACCESSOR_NAME + "(JNIEnv *env, unsigned int identifier) {");
    TextBlock methodCacheAccessorMethod = TextBlock.newEmpty();
    {
      methodCacheAccessorMethod.appendLine("jfieldID fieldid = " + FIELD_CACHE_NAME + "[identifier];");
      methodCacheAccessorMethod.appendLine("if(fieldid == NULL) {");
      TextBlock lookupMethodCode = TextBlock.newEmpty();
      {
        lookupMethodCode.appendLines(
          "reference_t fieldRef = " + FIELD_LOOKUP_CACHE_NAME + "[identifier];",
          "jclass owningClass = "+CLASS_CACHE_ACCESSOR_NAME+"(env, fieldRef.owner);",
          "if(fieldRef.staticAccess) {"
        );
        TextBlock staticHandle = TextBlock.newEmpty();
        {
          staticHandle.appendLine("fieldid = (*env)->GetStaticFieldID(env, owningClass, fieldRef.name, fieldRef.desc);");
        }
        lookupMethodCode.append(staticHandle, SCOPE_SPACING);
        lookupMethodCode.appendLine("} else {");
        TextBlock nonstaticHandle = TextBlock.newEmpty();
        {
          nonstaticHandle.appendLine("fieldid = (*env)->GetFieldID(env, owningClass, fieldRef.name, fieldRef.desc);");
        }
        lookupMethodCode.append(nonstaticHandle, SCOPE_SPACING);
        lookupMethodCode.appendLine("}");

        lookupMethodCode.appendLine(FIELD_CACHE_NAME + "[identifier] = fieldid;");
      }
      methodCacheAccessorMethod.append(lookupMethodCode, SCOPE_SPACING);
      methodCacheAccessorMethod.appendLines("}");
      methodCacheAccessorMethod.appendLines("return fieldid;");
    }
    methodSpace.append(methodCacheAccessorMethod, SCOPE_SPACING);
    methodSpace.appendLine("}");
  }

  private String stringProcess(String input) {
    if(false/*configuration.shouldObfuscateStrings()*/) {
      input += "\0";
      char[] chars = input.toCharArray();
      int[] key = new int[chars.length];
      int[] value = new int[chars.length];
      for (int i = 0; i < key.length; i++) {
        key[i] = ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE);
        value[i] = ((byte) chars[i]) ^ key[i];
      }
      return "/*"+input.substring(0, input.length() - 1)+"*/xorDecrypt(" + charArrayToC(value) + ", " + charArrayToC(key) + ", " + value.length + ")";
    } else {
      return "\"" + input + "\"";
    }
  }

  private String charArrayToC(int[] chars) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < chars.length; i++) {
      int aChar = chars[i];
      boolean last = i + 1 == chars.length;
      stringBuilder.append(aChar);
      if(!last) {
        stringBuilder.append(", ");
      }
    }
    return "(char[]) {" + stringBuilder + "}";
  }
}
