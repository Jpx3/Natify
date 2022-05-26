package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatchValue;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static de.jpx3.natify.translation.insntrans.TypeHelper.ARRAY_ACCESS_OPCODE_TYPE_MAPPING;
import static de.jpx3.natify.translation.select.IntegerMatcher.MergeOperation.OR;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.DOUBLE;
import static org.objectweb.asm.Type.FLOAT;
import static org.objectweb.asm.Type.LONG;
import static org.objectweb.asm.Type.*;


// load/save array element(s)
public final class LSAEInstructionTranslator extends InstructionTranslator<InsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = IntegerMatcher.merge(OR, new IntegerMatchRange(IALOAD, SALOAD), new IntegerMatchRange(IASTORE, SASTORE), new IntegerMatchValue(ARRAYLENGTH));

  @Override
  public Class<InsnNode>[] targetInstructions() {
    return new Class[]{InsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @SuppressWarnings("UnnecessaryLocalVariable")
  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    int baseStackPosition = executionFrame.stackSize() - 1;
    int opcode = instruction.getOpcode();
    if(opcode >= IALOAD && opcode <= SALOAD) {
      Type type = ARRAY_ACCESS_OPCODE_TYPE_MAPPING[opcode - IALOAD];
      if(type != null) { // primitive array access
        String typeInMethodName = firstLetterCapitalized(type.getClassName());
        String methodName = "Get" + typeInMethodName + "ArrayRegion";
        int arrayStackIndex = baseStackPosition - 1;
        int startIndexStackIndex = baseStackPosition;
        String arrayStackDescriptor = variables.acquireStackVar(arrayStackIndex, Type.OBJECT);
        String accessArrayIndexStackDescriptor = variables.acquireStackVar(startIndexStackIndex, Type.INT);
        String nonPaddedReturnValueStackDescriptor = variables.acquireStackVar(arrayStackIndex, nonPaddedTypeAccess(type));
        String paddedReturnValueStackDescriptor = variables.acquireStackVar(arrayStackIndex, type);
        String comment = "Load an element from array and put it on top of stack";
        code.appendLine("(*env)->" + methodName + "(env, " + arrayStackDescriptor + ", " + accessArrayIndexStackDescriptor + ", 1, &" + nonPaddedReturnValueStackDescriptor + "); // " + comment);
        if(!paddedReturnValueStackDescriptor.equals(nonPaddedReturnValueStackDescriptor)) {
          code.appendLine(paddedReturnValueStackDescriptor + " = " + nonPaddedReturnValueStackDescriptor + "; // type translation");
        }
      } else {
        int arrayStackIndex = baseStackPosition - 1;
        int startIndexStackIndex = baseStackPosition;
        String arrayStackDescriptor = variables.acquireStackVar(arrayStackIndex, Type.OBJECT);
        String accessArrayIndexStackDescriptor = variables.acquireStackVar(startIndexStackIndex, Type.INT);
        String returnValueStackDescriptor = variables.acquireStackVar(arrayStackIndex, Type.OBJECT);
        code.appendLine(returnValueStackDescriptor + " = (*env)->GetObjectArrayElement(env, " + arrayStackDescriptor + ", " + accessArrayIndexStackDescriptor + ");");
      }
    } else if (opcode >= IASTORE && opcode <= SASTORE) {
      Type arrayType = ARRAY_ACCESS_OPCODE_TYPE_MAPPING[opcode - IASTORE];
      if(arrayType != null) {
        String typeInMethodName = firstLetterCapitalized(arrayType.getClassName());
        String methodName = "Set" + typeInMethodName + "ArrayRegion";
        int arrayStackIndex = baseStackPosition - 2;
        int startIndexStackIndex = baseStackPosition - 1;
        int valueStackIndex = baseStackPosition;
        String arrayStackDescriptor = variables.acquireStackVar(arrayStackIndex, Type.OBJECT);
        String accessArrayIndexStackDescriptor = variables.acquireStackVar(startIndexStackIndex, Type.INT);
        String tempVar = variables.acquireTempVar(0, nonPaddedTypeAccess(arrayType));
        String originStackVar = variables.acquireStackVar(valueStackIndex, arrayType);
        code.appendLine(tempVar + " = " + originStackVar + ";");
        String comment = "Store top of stack (stack address: "+baseStackPosition +") to array (stack address: "+arrayStackIndex+") at an unknown index (stack address: " + startIndexStackIndex+")";
        code.appendLine("(*env)->" + methodName + "(env, " + arrayStackDescriptor + ", " + accessArrayIndexStackDescriptor + ", 1, &" + tempVar + "); // " + comment);
      } else { // reference array access
        int arrayStackIndex = baseStackPosition - 2;
        int startIndexStackIndex = baseStackPosition - 1;
        int valueStackIndex = baseStackPosition;
        String arrayStackDescriptor = variables.acquireStackVar(arrayStackIndex, Type.OBJECT);
        String accessArrayIndexStackDescriptor = variables.acquireStackVar(startIndexStackIndex, Type.INT);
        String valueDescriptor = variables.acquireStackVar(valueStackIndex, Type.OBJECT);
        code.appendLine("(*env)->SetObjectArrayElement(env, " + arrayStackDescriptor + ", " + accessArrayIndexStackDescriptor + ", " + valueDescriptor + ");");
      }
    } else { // array length check
      String arrayStackVariable = variables.acquireStackVar(baseStackPosition, 'l');
      code.appendLine(variables.acquireStackVar(baseStackPosition, 'i') + " = "+arrayStackVariable+" == NULL ? 0 : (*env)->GetArrayLength(env, "+ arrayStackVariable +");");
    }
    return code;
  }

  private char nonPaddedTypeAccess(Type type) {
    switch (type.getSort()) {
      case BOOLEAN:
        return 'z';
      case BYTE:
        return 'b';
      case CHAR:
        return 'c';
      case SHORT:
        return 's';
      case INT:
        return 'i';
      case LONG:
        return 'j';
      case FLOAT:
        return 'f';
      case DOUBLE:
        return 'd';
      default:
        return 'l';
    }
  }

  private String firstLetterCapitalized(String input) {
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }

  @Override
  public boolean requiresExceptionCheck() {
    return true;
  }
}
