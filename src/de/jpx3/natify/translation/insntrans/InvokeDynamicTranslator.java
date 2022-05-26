package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchValue;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static de.jpx3.natify.translation.NatifyClassTranslator.SCOPE_SPACING;
import static de.jpx3.natify.translation.controller.References.INDY_CALLSITE_CACHE_NAME;
import static org.objectweb.asm.Opcodes.*;

public final class InvokeDynamicTranslator extends InstructionTranslator<InvokeDynamicInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchValue(INVOKEDYNAMIC);

  private final static Handle FIND_CONSTRUCTOR_MH_LOOKUP_HANDLE = new Handle(0, "java/lang/invoke/MethodHandles$Lookup", "findConstructor", "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;");
  private final static Handle FIND_SPECIAL_MH_LOOKUP_HANDLE = new Handle(0, "java/lang/invoke/MethodHandles$Lookup", "findSpecial", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
  private final static Handle FIND_STATIC_MH_LOOKUP_HANDLE = new Handle(0, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;");
  private final static Handle FIND_VIRTUAL_MH_LOOKUP_HANDLE = new Handle(0, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;");

  private final static Handle TYPE_LOOKUP_HANDLE = new Handle(0, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;");
  private final static Handle INVOKE_MH_WITH_ARGUMENT_ARRAY_HANDLE = new Handle(0, "java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;");
  private final static Handle RESOLVE_HM_FROM_CS_HANDLE = new Handle(0, "java/lang/invoke/CallSite", "getTarget", "()Ljava/lang/invoke/MethodHandle;");
  private final static Handle LOOKUP_METHOD_HANDLE = new Handle(0, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;");
  private final static Handle BOX_LONG_HANDLE = new Handle(0, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
  private final static Handle BOX_INT_HANDLE = new Handle(0, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
  private final static Handle BOX_DOUBLE_HANDLE = new Handle(0, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
  private final static Handle BOX_FLOAT_HANDLE = new Handle(0, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
  private final static Handle BOX_BOOLEAN_HANDLE = new Handle(0, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
  private final static Handle BOX_SHORT_HANDLE = new Handle(0, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
  private final static Handle BOX_BYTE_HANDLE = new Handle(0, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
  private final static Handle BOX_CHAR_HANDLE = new Handle(0, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");

  @Override
  public Class<InvokeDynamicInsnNode>[] targetInstructions() {
    return new Class[]{InvokeDynamicInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    InvokeDynamicInsnNode invokeDynamicInsnNode = (InvokeDynamicInsnNode) instruction;
    Handle bsm = invokeDynamicInsnNode.bsm;
    Object[] bsmArgs = invokeDynamicInsnNode.bsmArgs;
    int indyConstantPoolId = references.registerIndy(invokeDynamicInsnNode);
    String indyCacheAccess = INDY_CALLSITE_CACHE_NAME + "[" + indyConstantPoolId + "]";
    code.appendLine("if (" + indyCacheAccess + " == NULL) {");
    TextBlock boostrapInitiate = TextBlock.newEmpty();
    {
      boolean bootstrapIsStatic = bsm.tag == H_INVOKESTATIC;
      String targetTempVar = variables.acquireTempVar(0, 'l');
      String access;
      if (bootstrapIsStatic) {
        access = "(*env)->CallStaticObjectMethod(";
      } else {
        access = "(*env)->CallObjectMethod(";
      }
      boostrapInitiate.appendLine(targetTempVar + " = " + access);
      TextBlock boostrapCaller = TextBlock.newEmpty();
      {
        boostrapCaller.appendLines(
          "env,",
          references.classAccess(bsm.getOwner()) + ",",
          references.methodIdAccess(bsm, bootstrapIsStatic) + ","
        );
        boostrapCaller.append(constructLookupAccess(references));
        boostrapCaller.appendToLastLine(",");
        boostrapCaller.appendLine(transformStringToJNIString(invokeDynamicInsnNode.name));
        boostrapCaller.appendToLastLine(",");
        boostrapCaller.append(constructMethodType(references, invokeDynamicInsnNode.desc));
        if (bsmArgs.length > 0) {
          boostrapCaller.appendToLastLine(",");
          boostrapCaller.append(processBSMArgs(references, bsmArgs));
        }
      }
      boostrapInitiate.append(boostrapCaller, SCOPE_SPACING);
      boostrapInitiate.appendLine(");");
      boostrapInitiate.appendLine(indyCacheAccess + " = (*env)->NewGlobalRef(env, " + targetTempVar + ");");
    }
    code.append(boostrapInitiate, SCOPE_SPACING);
    code.appendLine("}");
    Type[] argumentTypes = Type.getArgumentTypes(invokeDynamicInsnNode.desc);
    String mhTempAccessVar = variables.acquireTempVar(0, 'l');
    code.appendLine(mhTempAccessVar + " = (*env)->CallObjectMethod(env, " + indyCacheAccess + ", " + references.methodIdAccess(RESOLVE_HM_FROM_CS_HANDLE, false) + ");");
    String paramArrTempAccessVar = variables.acquireTempVar(1, 'l');
    code.appendLine(paramArrTempAccessVar + " = (*env)->NewObjectArray(env, " + argumentTypes.length + ", " + references.classAccess("java/lang/Object") + ", NULL);");
    int baseStackPosition = executionFrame.stackSize() - argumentTypes.length;
    for (int i = 0; i < argumentTypes.length; i++) {
      Type argumentType = argumentTypes[i];
      String stackAccess = variables.acquireStackVar(baseStackPosition + i, argumentType);
      String argumentOnRt = prepareValueForArrayInsertion(references, argumentType, stackAccess);
      code.appendLine("(*env)->SetObjectArrayElement(env, " + paramArrTempAccessVar + ", " + i + ", " + argumentOnRt + ");");
    }
    String outputStackVar = variables.acquireStackVar(baseStackPosition, Type.getReturnType(invokeDynamicInsnNode.desc));
    code.appendLine(outputStackVar + " = (*env)->CallObjectMethod(env, " + mhTempAccessVar + ", " + references.methodIdAccess(INVOKE_MH_WITH_ARGUMENT_ARRAY_HANDLE, false) + ", " + paramArrTempAccessVar + ");");
    return code;
  }

  private TextBlock processBSMArgs(References references, Object[] bsmArgs) {
    TextBlock boostrapCaller = TextBlock.newEmpty();
    int index = 0;
    for (Object bsmArg : bsmArgs) {
      if (bsmArg instanceof Handle) {
        boostrapCaller.append(constructMethodHandleRef(references, (Handle) bsmArg));
      } else if (bsmArg instanceof Type) {
        Type arg = (Type) bsmArg;
        boostrapCaller.append(constructMethodType(references, arg.getDescriptor()));
      } else if (bsmArg instanceof String) {
        boostrapCaller.appendLine(transformStringToJNIString((String) bsmArg));
      } else {// number process
        String suffix = "";
        if (bsmArg instanceof Float) {
          suffix = "f";
        } else if (bsmArg instanceof Long) {
          suffix = "L";
        }
        boostrapCaller.appendLine(bsmArg + suffix);
      }
      boolean last = (index) + 1 == bsmArgs.length;
      if (!last) {
        boostrapCaller.appendToLastLine(",");
      }
      index++;
    }
    return boostrapCaller;
  }

  private String prepareValueForArrayInsertion(References references, Type type, String stackAccessVar) {
    int typeSort = type.getSort();
    if (typeSort == Type.ARRAY || typeSort == Type.OBJECT) {
      return stackAccessVar;
    } else {
      Handle methodHandle;
      if (typeSort == Type.LONG) {
        methodHandle = BOX_LONG_HANDLE;
      } else if (typeSort == Type.INT) {
        methodHandle = BOX_INT_HANDLE;
      } else if (typeSort == Type.DOUBLE) {
        methodHandle = BOX_DOUBLE_HANDLE;
      } else if (typeSort == Type.FLOAT) {
        methodHandle = BOX_FLOAT_HANDLE;
      } else if (typeSort == Type.BOOLEAN) {
        methodHandle = BOX_BOOLEAN_HANDLE;
      } else if (typeSort == Type.SHORT) {
        methodHandle = BOX_SHORT_HANDLE;
      } else if (typeSort == Type.BYTE) {
        methodHandle = BOX_BYTE_HANDLE;
      } else if (typeSort == Type.CHAR) {
        methodHandle = BOX_CHAR_HANDLE;
      } else {
        throw new IllegalStateException("Invalid type: " + type);
      }
      return "(*env)->CallStaticObjectMethod(env, " + references.classAccess(methodHandle.getOwner()) + ", " + references.methodIdAccess(methodHandle, true) + ", " + stackAccessVar + ")";
    }
  }

  private TextBlock constructLookupAccess(References references) {
    TextBlock code = TextBlock.newEmpty();
    code.appendLine("(*env)->CallStaticObjectMethod(");
    TextBlock methodHandleLookupBody = TextBlock.newEmpty();
    methodHandleLookupBody.appendLine("env,");
    methodHandleLookupBody.appendLine(references.classAccess("java/lang/invoke/MethodHandles") + ",");
    methodHandleLookupBody.appendLine(references.methodIdAccess(LOOKUP_METHOD_HANDLE, true));
    code.append(methodHandleLookupBody, SCOPE_SPACING);
    code.appendLine(")");
    return code;
  }

  private TextBlock constructMethodHandleRef(References references, Handle originalHandle) {
    String owner = originalHandle.getOwner();
    String name = originalHandle.getName();
    String desc = originalHandle.getDesc();
    TextBlock firstArgument = TextBlock.ofLines(references.classAccess(owner));
    TextBlock secondArgument = TextBlock.ofLines(transformStringToJNIString(name));
    TextBlock thirdArgument;
    TextBlock forthArgument = TextBlock.newEmpty();
    Handle lookupTargetProcessor;
    int tag = originalHandle.tag;
    boolean handleIsField = tag <= H_PUTSTATIC;
    if (handleIsField) {
      thirdArgument = TextBlock.ofLines(references.classAccess(Type.getType(desc).getInternalName()));
      String targetName = "";
      switch (tag) {
        case H_PUTFIELD:
          targetName = "findSetter";
          break;
        case H_PUTSTATIC:
          targetName = "findStaticSetter";
          break;
        case H_GETFIELD:
          targetName = "findGetter";
          break;
        case H_GETSTATIC:
          targetName = "findStaticGetter";
          break;
      }
      lookupTargetProcessor = new Handle(
        0,
        "java/lang/invoke/MethodHandles$Lookup",
        targetName,
        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"
      );
    } else {
      thirdArgument = constructMethodType(references, desc);
      if (tag == H_INVOKESTATIC) {
        lookupTargetProcessor = FIND_STATIC_MH_LOOKUP_HANDLE;
      } else if (tag == H_INVOKEVIRTUAL || tag == H_INVOKEINTERFACE) {
        lookupTargetProcessor = FIND_VIRTUAL_MH_LOOKUP_HANDLE;
      } else if (name.equalsIgnoreCase("<init>")) {
        secondArgument = thirdArgument;
        thirdArgument = TextBlock.newEmpty();
        lookupTargetProcessor = FIND_CONSTRUCTOR_MH_LOOKUP_HANDLE;
      } else { // invoke special
        forthArgument = TextBlock.ofLines(references.classAccess(owner));
        lookupTargetProcessor = FIND_SPECIAL_MH_LOOKUP_HANDLE;
      }
    }
    TextBlock code = TextBlock.newEmpty();
    code.appendLine("(*env)->CallObjectMethod(");
    TextBlock methodHandleRefArguments = TextBlock.newEmpty();
    {
      methodHandleRefArguments.appendLine("env,");
      methodHandleRefArguments.append(constructLookupAccess(references));
      methodHandleRefArguments.appendToLastLine(",");
      methodHandleRefArguments.appendLine(references.methodIdAccess(lookupTargetProcessor, false));
      methodHandleRefArguments.appendToLastLine(",");
      methodHandleRefArguments.append(firstArgument);
      methodHandleRefArguments.appendToLastLine(",");
      methodHandleRefArguments.append(secondArgument);
      if (!thirdArgument.isEmpty()) {
        methodHandleRefArguments.appendToLastLine(",");
        methodHandleRefArguments.append(thirdArgument);
      }
      if (!forthArgument.isEmpty()) {
        methodHandleRefArguments.appendToLastLine(",");
        methodHandleRefArguments.append(forthArgument);
      }
    }
    code.append(methodHandleRefArguments, SCOPE_SPACING);
    code.appendLine(")");
    return code;
  }

  private TextBlock constructMethodType(References references, String targetDescriptor) {
    TextBlock code = TextBlock.newEmpty();
    code.appendLine("(*env)->CallStaticObjectMethod(");
    TextBlock methodHandleLookupBody = TextBlock.newEmpty();
    {
      methodHandleLookupBody.appendLine("env,");
      methodHandleLookupBody.appendLine(references.classAccess("java/lang/invoke/MethodType") + ",");
      methodHandleLookupBody.appendLine(references.methodIdAccess(TYPE_LOOKUP_HANDLE, true) + ",");
      methodHandleLookupBody.appendLine(transformStringToJNIString(targetDescriptor) + ",");
      methodHandleLookupBody.appendLine("classloader");
    }
    code.append(methodHandleLookupBody, SCOPE_SPACING);
    code.appendLine(")");
    return code;
  }

  private String transformStringToJNIString(String constantEntryAsString) {
    char[] chars = constantEntryAsString.toCharArray();
    StringBuilder arrayEntryCharWrapper = new StringBuilder();
    for (int i = 0; i < chars.length; i++) {
      char aChar = chars[i];
      boolean last = i + 1 == chars.length;
      arrayEntryCharWrapper.append((int) aChar);
      if (!last) {
        arrayEntryCharWrapper.append(", ");
      }
    }
    return "(*env)->NewString(env, (unsigned short[]) {" + arrayEntryCharWrapper + "}, " + chars.length + ")";
  }

  @Override
  public boolean requiresExceptionCheck() {
    return true;
  }
}