package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatcher;
import de.jpx3.natify.translation.select.MatchAnything;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

public final class LineNumberTranslator extends InstructionTranslator<LineNumberNode> {
  @Override
  public Class<LineNumberNode>[] targetInstructions() {
    return new Class[]{LineNumberNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return new MatchAnything();
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    LineNumberNode lineNumberNode = (LineNumberNode) instruction;
    TextBlock code = TextBlock.newEmpty();
    code.appendLine("// source line " + lineNumberNode.line);
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }
}
