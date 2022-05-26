package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatcher;
import jdk.internal.org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.jpx3.natify.translation.NatifyClassTranslator.SCOPE_SPACING;
import static org.objectweb.asm.Opcodes.LOOKUPSWITCH;
import static org.objectweb.asm.Opcodes.TABLESWITCH;

public final class TableInstructionTranslator extends InstructionTranslator<AbstractInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchRange(TABLESWITCH, LOOKUPSWITCH);

  @Override
  public Class<AbstractInsnNode>[] targetInstructions() {
    return new Class[]{TableSwitchInsnNode.class, LookupSwitchInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    Map<String, LabelNode> tableMappings = new LinkedHashMap<>();
    LabelNode defaultNode;
    if (instruction instanceof TableSwitchInsnNode) {
      TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) instruction;
      int index = 0;
      for (int i = tableSwitchInsnNode.min; i <= tableSwitchInsnNode.max; i++) {
        LabelNode labelNode = tableSwitchInsnNode.labels.get(index);
        tableMappings.put(String.valueOf(i), labelNode);
        index++;
      }
      defaultNode = tableSwitchInsnNode.dflt;
    } else {
      LookupSwitchInsnNode lookupSwitchInsnNode = (LookupSwitchInsnNode) instruction;
      int index = 0;
      for (Integer key : lookupSwitchInsnNode.keys) {
        LabelNode labelNode = lookupSwitchInsnNode.labels.get(index);
        tableMappings.put(String.valueOf(key), labelNode);
        index++;
      }
      defaultNode = lookupSwitchInsnNode.dflt;
    }
    int currentStackPosition = executionFrame.stackSize() - 1;
    String variableToRead = variables.acquireStackVar(currentStackPosition, Type.INT);//DEFAULT_STACK_VAR_PREFIX + currentStackPosition + ".i"; // must be of type int
    code.appendLine("switch (" + variableToRead + ") {");
    TextBlock tableStatements = TextBlock.newEmpty();
    {
      for (Map.Entry<String, LabelNode> tableMapping : tableMappings.entrySet()) {
        String key = tableMapping.getKey();
        tableStatements.appendLine("case " + key + ":");
        TextBlock caseOccurrenceCode = TextBlock.newEmpty();
        {
          LabelNode target = tableMapping.getValue();
          String targetAsString = labelExpressionFrom(target, labelIndices);
          caseOccurrenceCode.appendLine("goto " + targetAsString + ";");
        }
        tableStatements.append(caseOccurrenceCode, SCOPE_SPACING);
      }
      if (defaultNode != null) {
        tableStatements.appendLine("default:");
        TextBlock defaultCaseOccurrenceCode = TextBlock.newEmpty();
        {
          String targetAsString = labelExpressionFrom(defaultNode, labelIndices);
          defaultCaseOccurrenceCode.appendLine("goto " + targetAsString + ";");
        }
        tableStatements.append(defaultCaseOccurrenceCode, SCOPE_SPACING);
      }
    }
    code.append(tableStatements, SCOPE_SPACING);
    code.appendLine("}");
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return false;
  }
}
