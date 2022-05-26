package de.jpx3.natify.translation.insntrans;

import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.TextBlock;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.select.IntegerMatchValue;
import de.jpx3.natify.translation.select.IntegerMatcher;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

import static de.jpx3.natify.translation.NatifyClassTranslator.SCOPE_SPACING;
import static org.objectweb.asm.Opcodes.CHECKCAST;

public final class CheckCastTypeInstructionTranslator extends InstructionTranslator<TypeInsnNode> {
  private final static IntegerMatcher OPCODE_RANGE = new IntegerMatchValue(CHECKCAST);

  @Override
  public Class<TypeInsnNode>[] targetInstructions() {
    return new Class[]{TypeInsnNode.class};
  }

  @Override
  public IntegerMatcher opcodeRange() {
    return OPCODE_RANGE;
  }

  /*
  stack0.l = local1.l; // Load local variable from slot 1 to top of stack
  // MethodInsnNode  INVOKEVIRTUAL    RR.... R
  stack0.l = (*env)->CallObjectMethod(env, stack0.l, (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/Object"), "getClass", "()Ljava/lang/Class;"));
  if ((*env)->ExceptionCheck(env)) {
    return;
  }
  // MethodInsnNode  INVOKEVIRTUAL    RR.... R
  stack0.l = (*env)->CallObjectMethod(env, stack0.l, (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/Class"), "getName", "()Ljava/lang/String;"));
  if ((*env)->ExceptionCheck(env)) {
    return;
  }
  L2:
  local2.l = stack0.l; // Store top of stack to local variables slot 2
  stack0.l = (*env)->AllocObject(env, (*env)->FindClass(env, "java/lang/ClassCastException"));
  temp0.l = stack0.l;
  stack0.l = temp0.l;
  stack1.l = temp0.l;
  stack2.l = (*env)->AllocObject(env, (*env)->FindClass(env, "java/lang/StringBuilder"));
  temp0.l = stack2.l;
  stack2.l = temp0.l;
  stack3.l = temp0.l;
  (*env)->CallNonvirtualVoidMethod(env, stack3.l, (*env)->FindClass(env, "java/lang/StringBuilder"), (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/StringBuilder"), "<init>", "()V"));
  stack3.l = (*env)->NewString(env, (unsigned short[]) {67, 97, 110, 32, 110, 111, 116, 32, 99, 97, 115, 116, 32}, 13); // ldc string
  stack2.l = (*env)->CallObjectMethod(env, stack2.l, (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/StringBuilder"), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"), stack3.l);
  stack3.l = local2.l;
  stack2.l = (*env)->CallObjectMethod(env, stack2.l, (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/StringBuilder"), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"), stack3.l);
  stack3.l = (*env)->NewString(env, (unsigned short[]) {32, 116, 111, 32, 106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 83, 116, 114, 105, 110, 103}, 20); // ldc string
  stack2.l = (*env)->CallObjectMethod(env, stack2.l, (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/StringBuilder"), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"), stack3.l);
  stack2.l = (*env)->CallObjectMethod(env, stack2.l, (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/StringBuilder"), "toString", "()Ljava/lang/String;"));
  (*env)->CallNonvirtualVoidMethod(env, stack1.l, (*env)->FindClass(env, "java/lang/ClassCastException"), (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/ClassCastException"), "<init>", "(Ljava/lang/String;)V"), stack2.l);
  (*env)->Throw(env, stack0.l);
   */
  @Override
  public TextBlock translate(AbstractInsnNode instruction, Variables variables, References references, TranslationConfiguration configuration, Frame<BasicValue> executionFrame, List<LabelNode> labelIndices) {
    TextBlock code = TextBlock.newEmpty();

    TypeInsnNode typeInsnNode = (TypeInsnNode) instruction;
    Type classCast = Type.getObjectType(typeInsnNode.desc);
    String classCastAccess = references.classAccess(classCast.getInternalName());

    /*

      jobject objectClassName; // = temp0

      if(object == null) {
        objectClassName = new string("null");
      } else {
        jclass objectClass = GetObjectClass(env, object);
        objectClassName = CallObjectMethod(env, objectClass, "getName", "()Ljava/lang/String;");
      }

      jobject classCastException = AllocObject("ClassCastException");// = temp1
      jobject stringBuilder = AllocObject("StringBuilder");

      // init stringbuilder

     */

    String stackVarAccess = variables.acquireStackVar(executionFrame.stackSize() - 1, 'l');
    code.appendLine("if (" + stackVarAccess + " != NULL && !(*env)->IsInstanceOf(env, "+stackVarAccess+", "+ classCastAccess +")) {");
    TextBlock throwClassCastException = TextBlock.newEmpty();
    {
//      throwClassCastException.appendLines(
//        ""
//      );
      throwClassCastException.appendLine("(*env)->ThrowNew(env, "+ references.classAccess("java/lang/ClassCastException")+", \"Cannot cast <unknown> to "+classCast.getClassName()+"\");");
    }
    code.append(throwClassCastException, SCOPE_SPACING);
    code.appendLine("}");
    return code;
  }

  @Override
  public boolean requiresExceptionCheck() {
    return true;
  }
}
