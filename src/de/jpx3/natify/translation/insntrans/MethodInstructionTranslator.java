package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public final class MethodInstructionTranslator extends InstructionTranslator<MethodInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchRange(INVOKEVIRTUAL, INVOKEINTERFACE);

  @Override
  public Class<MethodInsnNode>[] targetInstructions() {
    return new Class[]{MethodInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
    String targetMethodDescription = methodInsnNode.desc;
    Type returnType = Type.getReturnType(targetMethodDescription);
    Type[] parameterTypes = Type.getArgumentTypes(targetMethodDescription);
    int opcode = instruction.getOpcode();
    boolean staticCall = opcode == INVOKESTATIC;
    boolean specialCall = opcode == INVOKESPECIAL;
    boolean doesReturnValue = returnType.getSort() != Type.VOID;
    int baseStackPosition = executionFrame.stackSize();
    int parameterLength = parameterTypes.length;
    int stackDepth = parameterLength + (staticCall ? 0 : 1);
    String codeLine = "";
    if (doesReturnValue) {
      int baseStackPositionAfter = baseStackPosition - stackDepth;
      codeLine += variables.acquireStackVar(baseStackPositionAfter, returnType) + " = ";
    }
    Handle handle = new Handle(0, methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
    String methodName = findMethodName(specialCall, doesReturnValue, staticCall, returnType);
    String parameterLoad = buildAccessParameters(variables, parameterTypes, baseStackPosition - parameterLength);
    String preload = preparePreParameters(references, variables, executionFrame, handle, staticCall, specialCall, baseStackPosition, parameterLength);
    codeLine += "(*env)->" + methodName + "(env, " + preload + parameterLoad + ");";
    code.appendLine(codeLine);
    return code;
  }

  private String preparePreParameters(
    References references, Variables variables, Frame<BasicValue> executionFrame,
    Handle methodHandle, boolean staticCall, boolean specialCall,
    int baseStackPosition, int stackDepth
  ) {
    String methodIdAccess = references.methodIdAccess(methodHandle, staticCall);
    String preload = "";
    if (!staticCall) {
      preload += variables.acquireStackVar(baseStackPosition - (stackDepth + 1), executionFrame) + ", ";
    }
    if (specialCall || staticCall) {
      preload += references.classAccess(methodHandle.getOwner()) + ", ";
    }
    preload += methodIdAccess;
    return preload;
  }

  private String findMethodName(boolean specialCall, boolean doesReturnValue, boolean staticCall, Type returnType) {
    String methodAccessDescriptor = specialCall ? "Nonvirtual" : staticCall ? "Static" : "";
    String returnTypeName = firstLetterUppercase(doesReturnValue ? (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY ? "object" : returnType.getClassName()) : "void");
    return "Call" + methodAccessDescriptor + returnTypeName + "Method";
  }

  private String buildAccessParameters(Variables variables, Type[] types, int startIndex) {
    String[] translatedParameterTypes = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      Type parameterType = types[i];
      int stackIndex = i + startIndex;
      String accessor = variables.acquireStackVar(stackIndex, parameterType);
      translatedParameterTypes[i] = accessor;
    }
    StringBuilder parameterLoad = new StringBuilder();
    for (int i = 0; i < translatedParameterTypes.length; i++) {
      String translatedParameterType = translatedParameterTypes[i];
      boolean first = i == 0;
      boolean last = i + 1 == translatedParameterTypes.length;
      if (first) {
        parameterLoad.append(", ");
      }
      parameterLoad.append(translatedParameterType);
      if (!last) {
        parameterLoad.append(", ");
      }
    }
    return parameterLoad.toString();
  }

  private String firstLetterUppercase(String input) {
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }

  @Override
  public boolean requiresExceptionCheck() {
    return true;
  }
}