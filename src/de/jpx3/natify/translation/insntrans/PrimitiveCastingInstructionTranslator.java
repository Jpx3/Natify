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

import static de.jpx3.natify.translation.insntrans.TypeHelper.*;
import static org.objectweb.asm.Opcodes.*;

public final class PrimitiveCastingInstructionTranslator extends InstructionTranslator<InsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchRange(I2L, I2S);

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
    int baseStackPosition = executionFrame.stackSize() - 1;
    char typeTo = CASTING_OPCODE_CONVERSION_ACCESS_TYPES[opcode - I2L];
    char typeFrom = CASTING_OPCODE_ORIGIN_ACCESS_TYPES[(opcode - I2L) / 3];
    String variable = variables.acquireStackVar(baseStackPosition);
    String typeToVariable = variable + "." + (opcode >= I2B ? 'i' : typeTo);
    String typeFromVariable = variable + "." + typeFrom;
    String toCastType = ACCESS_TYPE_TO_NATIVE_TYPE.get(typeTo);
    String fromCastType = ACCESS_TYPE_TO_NATIVE_TYPE.get(typeFrom);
    String comment = "cast " + fromCastType + " (" + typeFrom + ") to " + toCastType + " (" + (opcode >= I2B ? "i (override)" : typeTo) + ")";
    String fullCastCode = typeToVariable + " = (" + toCastType + ") " + typeFromVariable + "; // " + comment;
    code.appendLine(fullCastCode);
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return true;
  }
}
