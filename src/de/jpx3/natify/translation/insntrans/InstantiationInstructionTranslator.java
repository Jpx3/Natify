package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatchValue;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static de.jpx3.natify.translation.NatifyClassTranslator.SCOPE_SPACING;
import static de.jpx3.natify.translation.select.IntegerMatcher.MergeOperation.OR;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.OBJECT;

public final class InstantiationInstructionTranslator extends InstructionTranslator<AbstractInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = IntegerMatcher.merge(OR, new IntegerMatchRange(NEW, ANEWARRAY), new IntegerMatchValue(MULTIANEWARRAY));

  @Override
  public Class<AbstractInsnNode>[] targetInstructions() {
    return new Class[]{TypeInsnNode.class, IntInsnNode.class, MultiANewArrayInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    int opcode = instruction.getOpcode();
    if(opcode == NEW) {
      int stackPosition = executionFrame.stackSize();
      TypeInsnNode typeInsnNode = (TypeInsnNode) instruction;
      Type type = Type.getType("L"+typeInsnNode.desc+";");
      String stackAccess = variables.acquireStackVar(stackPosition, type);
      if(type.getSort() != OBJECT) {
        throw new IllegalStateException();
      }
      code.appendLine(stackAccess + " = (*env)->AllocObject(env, "+ references.classAccess(typeInsnNode.desc)+");");
    } else if(opcode == NEWARRAY) {
      int operand = ((IntInsnNode) instruction).operand;
      int baseStackPosition = executionFrame.stackSize() - 1 /* start index 0 translation*/;
      String targetStackVariable = variables.acquireStackVar(baseStackPosition, OBJECT);
      String sourceStackVariable = variables.acquireStackVar(baseStackPosition, Type.INT);
      // load count from stack
      // prepare class from operand
      Type type = TypeHelper.PRIMITIVE_ARRAY_GENERATION_TYPE_RESOLVE[operand];
      code.appendLine(targetStackVariable + " = (*env)->" + arrayGenerationMethodName(type) + "(env, " + sourceStackVariable + ");");
    } else if(opcode == ANEWARRAY) {
      TypeInsnNode typeInsnNode = (TypeInsnNode) instruction;
      int baseStackPosition = executionFrame.stackSize() - 1 /* start index 0 translation*/;
      String targetStackVariable = variables.acquireStackVar(baseStackPosition, OBJECT);
      String sourceStackVariable = variables.acquireStackVar(baseStackPosition, Type.INT);
      // load count from stack
      // prepare class from operand
      String classAccess = references.classAccess(typeInsnNode.desc); // revise please - can be of other type than object
      code.appendLine(targetStackVariable + " = (*env)->NewObjectArray(env, " + sourceStackVariable + ", "+classAccess+", NULL);");
    } else if(opcode == MULTIANEWARRAY) {
      MultiANewArrayInsnNode arrayInsnNode = (MultiANewArrayInsnNode) instruction;
      int dimensions = arrayInsnNode.dims;
      StringBuilder inputStacks = new StringBuilder();
      for (int i = 0; i < dimensions; i++) {
        int stackSlot = ((executionFrame.stackSize()) - (dimensions)) + i;
        boolean last = i + 1 == dimensions;
        inputStacks.append(variables.acquireStackVar(stackSlot, executionFrame));
        if(!last) {
          inputStacks.append(", ");
        }
      }
      int instructionsBefore = 0;
      AbstractInsnNode beforeNode = instruction;
      while ((beforeNode = beforeNode.getPrevious()) != null) {
        instructionsBefore++;
      }
      String dimensionArrayName = DIMENSION_ARRAY_NAME + instructionsBefore;
      code.appendLine("jsize "+dimensionArrayName+"[] = {" + inputStacks + "};");
      String desc = arrayInsnNode.desc.substring(1);
      Type type = Type.getType(desc);
      int baseStack = (executionFrame.stackSize()) - dimensions;
      code.appendLine(variables.acquireStackVar(baseStack, 'l') + " = (*env)->NewObjectArray(env, "+dimensionArrayName+"[0], " + references.classAccess(desc) + ", NULL);");
      iterateArray(code, dimensionArrayName, 0, dimensions, baseStack, variables, references, type);
    }
    return code;
  }

  private final static String DIMENSION_ARRAY_NAME = "xk";

  private void iterateArray(TextBlock textBlock, String dimensionResolveName, int dimension, int finalDimension, int targetStackIndex, Variables variables, References references, Type parentArrayType) {
    textBlock.appendLine("for (jsize x"+dimension+" = 0; x"+dimension+" < "+dimensionResolveName+"[" + dimension + "]; x"+dimension+"++) {");
    TextBlock forInner = TextBlock.newEmpty();
    {
      Type arrayCore = Type.getType(parentArrayType.getInternalName().substring(1));
      String parentArrayStackVar = variables.acquireStackVar(targetStackIndex + dimension, 'l');
      String newArrayStackVar = variables.acquireStackVar(targetStackIndex + (dimension + 1), 'l');
      String createNewObjectArrayMethodCall;
      boolean isLast = dimension == finalDimension - 2;
      if(isLast && arrayCore.getSort() != OBJECT) {
        createNewObjectArrayMethodCall = "(*env)->"+arrayGenerationMethodName(arrayCore)+"(env, "+dimensionResolveName+"["+(dimension + 1)+"]);";
      } else {
        createNewObjectArrayMethodCall = "(*env)->NewObjectArray(env, "+dimensionResolveName+"["+(dimension + 1)+"], "+ references.classAccess(arrayCore.getInternalName())+", NULL);";
      }
      forInner.appendLine(newArrayStackVar + " = " + createNewObjectArrayMethodCall);
      forInner.appendLine("(*env)->SetObjectArrayElement(env, "+parentArrayStackVar+", x"+dimension+", "+newArrayStackVar+");");
      if(dimension < finalDimension - 2) {
        iterateArray(forInner, dimensionResolveName, dimension + 1, finalDimension, targetStackIndex, variables, references, arrayCore);
      }
    }
    textBlock.append(forInner, SCOPE_SPACING);
    textBlock.appendLine("}");
  }

  private String arrayGenerationMethodName(Type type) {
    String className = type.getClassName();
    return "New" + firstLetterUppercase(className) + "Array";
  }

  private String firstLetterUppercase(String input) {
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }

  @Override
  public boolean requiresExceptionCheck() {
    return true;
  }
}
