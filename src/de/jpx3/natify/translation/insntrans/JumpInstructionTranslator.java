package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchValue;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static org.objectweb.asm.Opcodes.GOTO;

public final class JumpInstructionTranslator extends InstructionTranslator<JumpInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchValue(GOTO);

  @Override
  public Class<JumpInsnNode>[] targetInstructions() {
    return new Class[]{JumpInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    TextBlock textBlock = TextBlock.newEmpty();
    JumpInsnNode jumpInsnNode = (JumpInsnNode) instruction;
    String labelexpr = labelExpressionFrom(jumpInsnNode.label, labelIndices);
    code.appendLine("goto " + labelexpr + ";");
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }
}