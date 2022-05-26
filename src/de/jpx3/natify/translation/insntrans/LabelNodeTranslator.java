package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatcher;
import de.jpx3.natify.translation.select.MatchAnything;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

public final class LabelNodeTranslator extends InstructionTranslator<LabelNode> {
  @Override
  public Class<LabelNode>[] targetInstructions() {
    return new Class[]{LabelNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return new MatchAnything();
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelNodes) {
    TextBlock code = TextBlock.newEmpty();
    LabelNode labelNode = (LabelNode) instruction;
    if (executionFrame != null && labelNodes.contains(labelNode)) {
      String labelExpression = labelExpressionFrom(labelNode, labelNodes);
      code.appendLine(labelExpression + ": // label " + labelExpression);
    }
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }
}
