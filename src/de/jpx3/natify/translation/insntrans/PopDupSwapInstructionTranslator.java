package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static de.jpx3.natify.translation.controller.Variables.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.DOUBLE;
import static org.objectweb.asm.Type.FLOAT;
import static org.objectweb.asm.Type.LONG;
import static org.objectweb.asm.Type.*;

public final class PopDupSwapInstructionTranslator extends InstructionTranslator<InsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchRange(POP, SWAP);

  @Override
  public Class<InsnNode>[] targetInstructions() {
    return new Class[]{InsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    int opcode = instruction.getOpcode();
    int stackRemovalLength = -1;
    int[] tempToStackMapping = {};
    int stackSize = executionFrame.stackSize();
    switch (opcode) {
      case POP:
      case POP2:
        // let's ignore stack discards
        break;
      case SWAP:
        stackRemovalLength = 2;
        tempToStackMapping = new int[]{1, 0};
        break;
      case DUP:
        stackRemovalLength = 1;
        if (!oneDimensionalStackVal(executionFrame, stackSize, 0)) {
          throw error("Unable to process stack context: Expected 32-bit value at " + stackSize);
        }
        tempToStackMapping = new int[]{0, 0};
        break;
      case DUP_X1:
        stackRemovalLength = 2;
        if(!oneDimensionalStackVal(executionFrame, stackSize, 0) || !oneDimensionalStackVal(executionFrame, stackSize, 1)) {
          throw error("Unable to process stack context: Expected two 32-bit values as input");
        }
        tempToStackMapping = new int[]{0, 1, 0};
        break;
      case DUP_X2:
        if (!oneDimensionalStackVal(executionFrame, stackSize, 0)) {
          throw error("Unable to process stack context: Expected 32-bit value at " + stackSize);
        }
        if (oneDimensionalStackVal(executionFrame, stackSize, 1)) {
          if (oneDimensionalStackVal(executionFrame, stackSize, 2)) {
            stackRemovalLength = 3;
            tempToStackMapping = new int[]{0, 2, 1, 0};
          } else {
            throw error("Unable to process stack context: Expected 32-bit value at " + (stackSize - 2));
          }
        } else {
          stackRemovalLength = 2;
          tempToStackMapping = new int[]{0, 1, 0};
        }
        break;
      case DUP2:
        if (oneDimensionalStackVal(executionFrame, stackSize, 0)) {
          if (oneDimensionalStackVal(executionFrame, stackSize, 1)) {
            stackRemovalLength = 2;
            tempToStackMapping = new int[]{1, 0, 1, 0};
          } else {
            throw error("Unable to process stack context: Expected 32-bit value at " + (stackSize - 1));
          }
        } else {
          stackRemovalLength = 1;
          tempToStackMapping = new int[]{0, 0};
        }
        break;
      case DUP2_X1:
        if (oneDimensionalStackVal(executionFrame, stackSize, 0)) {
          if (oneDimensionalStackVal(executionFrame, stackSize, 1)) {
            if (oneDimensionalStackVal(executionFrame, stackSize, 2)) {
              stackRemovalLength = 3;
              tempToStackMapping = new int[]{1, 0, 2, 1, 0};
            }
          }
        } else {
          if (oneDimensionalStackVal(executionFrame, stackSize, 1)) {
            stackRemovalLength = 2;
            tempToStackMapping = new int[]{0, 1, 0};
          }
        }
        break;
      case DUP2_X2:
        if (oneDimensionalStackVal(executionFrame, stackSize, 0)) {
          if (oneDimensionalStackVal(executionFrame, stackSize, 1)) {
            if (oneDimensionalStackVal(executionFrame, stackSize, 2)) {
              if (oneDimensionalStackVal(executionFrame, stackSize, 3)) {
                stackRemovalLength = 4;
                tempToStackMapping = new int[]{1, 0, 3, 2, 1, 0};
              } else {
                throw error("Unable to process stack context: Expected 32-bit value at " + (stackSize - 3));
              }
            } else {
              stackRemovalLength = 3;
              tempToStackMapping = new int[]{1, 0, 2, 1, 0};
            }
          }
        } else {
          if (oneDimensionalStackVal(executionFrame, stackSize, 1)) {
            if (oneDimensionalStackVal(executionFrame, stackSize, 2)) {
              stackRemovalLength = 3;
              tempToStackMapping = new int[]{0, 2, 1, 0};
            } else {
              throw error("Unable to process stack context: Expected 32-bit value at " + (stackSize - 2));
            }
          } else {
            stackRemovalLength = 2;
            tempToStackMapping = new int[]{0, 1, 0};
          }
        }
        break;
      default:
        throw new IllegalStateException("Unknown opcode " + opcode);
    }
    if (stackRemovalLength < 0) {
      return code;
    }
    String[] typeAccess = prepareAccessTypes(stackSize, stackRemovalLength, executionFrame);
    TextBlock saveStackToTempCode = generateSaveStackToTempCode(variables, stackSize, stackRemovalLength, typeAccess);
    TextBlock loadStackFromTempCode = generateLoadStackFromTempCode(variables, stackSize - stackRemovalLength, tempToStackMapping, typeAccess);
    code.append(saveStackToTempCode);
    code.append(loadStackFromTempCode);
    return code;
  }

  private IllegalStateException error(String message) {
    return new IllegalStateException(message);
  }

  private boolean oneDimensionalStackVal(Frame<BasicValue> executionFrame, int stackSize, int negPos) {
    return executionFrame.getStack(stackSize - negPos - 1).getSize() == 1;
  }

  private String[] prepareAccessTypes(int stackStart, int size, Frame<BasicValue> executionFrame) {
    String[] types = new String[size];
    for (int i = 0; i < size; i++) {
      int stackPos = stackStart - size + i;
      int sort = executionFrame.getStack(stackPos).getType().getSort();
      types[i] = accessTypeOf(sort);
    }
    return types;
  }

  protected final String accessTypeOf(int typeSort) {
    switch (typeSort) {
      case BOOLEAN:
      case BYTE:
      case CHAR:
      case SHORT:
      case INT:
        return "i";
      case LONG:
        return "j";
      case FLOAT:
        return "f";
      case DOUBLE:
        return "d";
      default:
        return "l";
    }
  }

  private TextBlock generateSaveStackToTempCode(Variables variables, int stackStart, int size, String[] types) {
    int[] mappingsFromTempPerspective = new int[size];
    for (int i = 0; i < size; i++) {
      int stackPos = stackStart - (i + 1);
      mappingsFromTempPerspective[i] = stackPos;
    }
    return swapVariables(variables, mappingsFromTempPerspective, types, 0, DEFAULT_STACK_VAR_PREFIX, DEFAULT_TEMP_VAR_PREFIX);
  }

  private TextBlock generateLoadStackFromTempCode(Variables variables, int stackStart, int[] mapping, String[] types) {
    int size = mapping.length;
    int[] shiftedMappings = new int[stackStart + size];
    String[] shiftedTypes = new String[stackStart + size];
    for (int currentLocalStackIndex = 0; currentLocalStackIndex < size; currentLocalStackIndex++) {
      int tempTarget = mapping[currentLocalStackIndex];
      int exactStackIndex = stackStart + currentLocalStackIndex;
      shiftedMappings[exactStackIndex] = tempTarget;
      shiftedTypes[exactStackIndex] = types[tempTarget];
    }
    return swapVariables(variables, shiftedMappings, shiftedTypes, stackStart, DEFAULT_TEMP_VAR_PREFIX, DEFAULT_STACK_VAR_PREFIX);
  }

  private TextBlock swapVariables(Variables variables, int[] mappings, String[] accessTypes, int start, String fromAddress, String toAddress) {
    TextBlock code = TextBlock.newEmpty();
    for (int index = 0, mappingLength = mappings.length; index < mappingLength; index++) {
      int targetIndex = mappings[index];
      if (index >= start) {
        code.appendLine(swapSingleVariable(variables, targetIndex, index, fromAddress, toAddress, accessTypes[index]));
      }
    }
    return code;
  }

  private String swapSingleVariable(Variables variables, int fromIndex, int toIndex, String fromAddress, String toAddress, String type) {
    String fromVariable = acquireVariableFor(variables, fromAddress, fromIndex);
    String toVariable = acquireVariableFor(variables, toAddress, toIndex);
    return toVariable + "." + type + " = " + fromVariable + "." + type + ";";
  }

  private String acquireVariableFor(Variables variables, String address, int index) {
    switch (address) {
      case DEFAULT_STACK_VAR_PREFIX:
        return variables.acquireStackVar(index);
      case DEFAULT_LOCAL_VAR_PREFIX:
        return variables.acquireLocalVar(index);
      case DEFAULT_TEMP_VAR_PREFIX:
        return variables.acquireTempVar(index);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }
}
