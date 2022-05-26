package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

public abstract class InstructionTranslator<I extends AbstractInsnNode> {

  // don't delete - not all asm instructions have opcodes..
  public abstract Class<I>[] targetInstructions();

  public abstract IntegerMatcher opcodeRange();

  public abstract TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices);

  public abstract boolean requiresExceptionCheck();

  protected final String labelExpressionFrom(LabelNode labelNode, List<LabelNode> labelNodes) {
    int index = labelNodes.indexOf(labelNode);
    return "L" + index;
  }
}
