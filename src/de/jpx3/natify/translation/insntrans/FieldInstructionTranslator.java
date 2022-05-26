package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchRange;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public final class FieldInstructionTranslator extends InstructionTranslator<FieldInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchRange(GETSTATIC, PUTFIELD);

  @Override
  public Class<FieldInsnNode>[] targetInstructions() {
    return new Class[]{FieldInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();
    FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
    int opcode = instruction.getOpcode();
    boolean virtual = opcode == GETFIELD || opcode == PUTFIELD;
    boolean stackOutput = opcode == GETFIELD || opcode == GETSTATIC;
    int stackDepth = (virtual ? 1 : 0) + (stackOutput ? 0 : 1 /* no output -> need input*/);
    int baseStackPosition = executionFrame.stackSize() - stackDepth;
    String fieldOperation = "";
    Type accessType = Type.getType(fieldInsnNode.desc);
    if (stackOutput) {
      fieldOperation = variables.acquireStackVar(baseStackPosition, accessType) + " = ";
    }
    String fieldAccessorDescriptor = stackOutput ? "Get" : "Set";
    String fieldAccessDescriptor = virtual ? "" : "Static";
    String targetTypeDescriptor = firstLetterUppercase(accessType.getSort() == Type.OBJECT || accessType.getSort() == Type.ARRAY ? "object" : accessType.getClassName());
    String fieldDescriptor = "Field";
    String methodName = fieldAccessorDescriptor + fieldAccessDescriptor + targetTypeDescriptor + fieldDescriptor;
    String currentStackInput = variables.acquireStackVar(baseStackPosition, Type.OBJECT); // must be reference
    String targetInput = virtual ? currentStackInput : references.classAccess(fieldInsnNode.owner);
    String targetFieldIdAccess = references.fieldIdAccess(new Handle(0, fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc), !virtual);
    String valueVarAccess = variables.acquireStackVar(baseStackPosition + (virtual ? 1 : 0), accessType);
    fieldOperation += "(*env)->" + methodName + "(env, " + targetInput + ", " + targetFieldIdAccess + (stackOutput ? "" : ", " + valueVarAccess) + ");";
    code.appendLine(fieldOperation);
    return code;
  }

  private String firstLetterUppercase(String input) {
    return input.substring(0, 1).toUpperCase() + input.substring(1);
  }

  @Override
  public boolean requiresExceptionCheck() {
    return true;
  }
}
