package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public final class LoadIntConstInstructionTranslator extends InstructionTranslator<InsnNode> {
  private final static IntegerMatchRange OPCODE_RANGE = new IntegerMatchRange(ACONST_NULL, DCONST_1);

  @Override
  public Class<InsnNode>[] targetInstructions() {
    return new Class[]{InsnNode.class};
  }

  @Override
  public IntegerMatchRange opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelNodes) {
    int insertionStackIndex = executionFrame.stackSize() + 1 /* one more than stack */ - 1 /* start index 0 translation*/;
    int opcode = instruction.getOpcode();
    int typeSort;
    TextBlock code = TextBlock.newEmpty();
    String value;
    switch (opcode) {
      case ACONST_NULL:
        typeSort = Type.OBJECT;
        value = "NULL";
        break;
      case ICONST_M1:
        typeSort = Type.INT;
        value = "-1";
        break;
      case ICONST_0:
      case ICONST_1:
      case ICONST_2:
      case ICONST_3:
      case ICONST_4:
      case ICONST_5:
        typeSort = Type.INT;
        value = (opcode - ICONST_0) + "";
        break;
      case LCONST_0:
      case LCONST_1:
        typeSort = Type.LONG;
        value = (opcode - LCONST_0) + "L";
        break;
      case FCONST_0:
      case FCONST_1:
      case FCONST_2:
        typeSort = Type.FLOAT;
        value = (opcode - FCONST_0) + ".0f";
        break;
      case DCONST_0:
      case DCONST_1:
        typeSort = Type.DOUBLE;
        value = (opcode - DCONST_0) + ".0";//d
        break;
      default:
        throw new IllegalStateException("Unexpected value " + opcode + " within " + opcodeRange());
    }
    code.appendLine(variables.acquireStackVar(insertionStackIndex, typeSort) + " = " + value + "; // push value " + value + " on top of stack");
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }
}