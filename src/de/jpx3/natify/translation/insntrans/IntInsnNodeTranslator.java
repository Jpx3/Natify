package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.SIPUSH;

public final class IntInsnNodeTranslator extends InstructionTranslator<IntInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchRange(BIPUSH, SIPUSH);

  @Override
  public Class<IntInsnNode>[] targetInstructions() {
    return new Class[]{IntInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    IntInsnNode intInsnNode = (IntInsnNode) instruction;
    int operand = intInsnNode.operand;
    int insertionStackIndex = executionFrame.stackSize() + 1 /* one more than stack */ - 1 /* start index 0 translation*/;
    String targetStackVariable = variables.acquireStackVar(insertionStackIndex, Type.INT);
    String value = String.valueOf(operand);
    code.appendLine(targetStackVariable + " = " + value + "; // push tiny integer " + value + " on top of stack");
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }
}
