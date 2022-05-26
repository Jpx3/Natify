package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static de.jpx3.natify.translation.insntrans.TypeHelper.OPCODES;
import static org.objectweb.asm.Opcodes.JSR;
import static org.objectweb.asm.Opcodes.RET;

public final class RetJsrInstructionTranslator extends InstructionTranslator<AbstractInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchRange(JSR, RET);

  @Override
  public Class<AbstractInsnNode>[] targetInstructions() {
    return new Class[]{JumpInsnNode.class, VarInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    String insnName = OPCODES[instruction.getOpcode()];
    code.appendLine("//Unable to translate " + insnName + " instruction: Instruction not supported");
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }
}
