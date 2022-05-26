package de.jpx3.natify.shared;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.objectweb.asm.Opcodes.*;

public final class NatifyClassLinkerClassFactory {

  public static ClassNode generateNatifyClassLinkerClass(LinkageInformation linkageInformation) {
    ClassWriter classWriter = new ClassWriter(0);
    MethodVisitor methodVisitor;

    classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, "sevastopol/natify/" + linkageInformation.uniqueBuildId() + "/NatifyClassLinker", null, "java/lang/Object", null);

//    {
//      methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
//      methodVisitor.visitCode();
//      methodVisitor.visitVarInsn(ALOAD, 0);
//      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
//      methodVisitor.visitInsn(RETURN);
//      methodVisitor.visitMaxs(1, 1);
//      methodVisitor.visitEnd();
//    }

    {
      methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "performLinkage", "(I)V", null, null);
      methodVisitor.visitCode();
      methodVisitor.visitLdcInsn("os.name");
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false);
      methodVisitor.visitVarInsn(ASTORE, 1);
      methodVisitor.visitLdcInsn("os.arch");
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/ lang/String;", false);
      methodVisitor.visitVarInsn(ASTORE, 2);
      methodVisitor.visitVarInsn(ALOAD, 2);
      methodVisitor.visitLdcInsn("64");
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
      Label label0 = new Label();
      methodVisitor.visitJumpInsn(IFNE, label0);
      methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException");
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn("Unsupported environment: Please use a 64-bit version of Java");
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
      methodVisitor.visitInsn(ATHROW);
      methodVisitor.visitLabel(label0);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 3, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String"}, 0, new Object[]{});
//      methodVisitor.visitInsn(ICONST_3);
      String[] supportedOperatingSystems = linkageInformation.supportedOperatingSystems();
      if (supportedOperatingSystems.length <= 5) {
        methodVisitor.visitInsn(ICONST_0 + supportedOperatingSystems.length);
      } else if (supportedOperatingSystems.length <= 127) {
        methodVisitor.visitIntInsn(BIPUSH, supportedOperatingSystems.length);
      } else {
        methodVisitor.visitIntInsn(SIPUSH, supportedOperatingSystems.length);
      }
      methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/String");

      for (int i = 0; i < supportedOperatingSystems.length; i++) {
        String supportedOperatingSystem = supportedOperatingSystems[i];
        methodVisitor.visitInsn(DUP);
        if (i <= 5) {
          methodVisitor.visitInsn(ICONST_0 + i);
        } else if (i <= 127) {
          methodVisitor.visitIntInsn(BIPUSH, i);
        } else {
          methodVisitor.visitIntInsn(SIPUSH, i);
        }
        methodVisitor.visitLdcInsn(supportedOperatingSystem);
        methodVisitor.visitInsn(AASTORE);
      }

//      methodVisitor.visitInsn(DUP);
//      methodVisitor.visitInsn(ICONST_0);
//      methodVisitor.visitLdcInsn("win");
//      methodVisitor.visitInsn(AASTORE);

//      methodVisitor.visitInsn(DUP);
//      methodVisitor.visitInsn(ICONST_1);
//      methodVisitor.visitLdcInsn("lin");
//      methodVisitor.visitInsn(AASTORE);

//      methodVisitor.visitInsn(DUP);
//      methodVisitor.visitInsn(ICONST_2);
//      methodVisitor.visitLdcInsn("mac");
//      methodVisitor.visitInsn(AASTORE);

      methodVisitor.visitVarInsn(ASTORE, 3);
      methodVisitor.visitInsn(ICONST_M1);
      methodVisitor.visitVarInsn(ISTORE, 4);
      methodVisitor.visitInsn(ICONST_0);
      methodVisitor.visitVarInsn(ISTORE, 5);
      Label label1 = new Label();
      methodVisitor.visitLabel(label1);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 6, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String", "[Ljava/lang/String;", Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[]{});
      methodVisitor.visitVarInsn(ILOAD, 5);
      methodVisitor.visitVarInsn(ALOAD, 3);
      methodVisitor.visitInsn(ARRAYLENGTH);
      Label label2 = new Label();
      methodVisitor.visitJumpInsn(IF_ICMPGE, label2);
      methodVisitor.visitVarInsn(ALOAD, 3);
      methodVisitor.visitVarInsn(ILOAD, 5);
      methodVisitor.visitInsn(AALOAD);
      methodVisitor.visitVarInsn(ASTORE, 6);
      methodVisitor.visitVarInsn(ALOAD, 1);
      methodVisitor.visitVarInsn(ALOAD, 6);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
      Label label3 = new Label();
      methodVisitor.visitJumpInsn(IFEQ, label3);
      methodVisitor.visitVarInsn(ILOAD, 5);
      methodVisitor.visitVarInsn(ISTORE, 4);
      methodVisitor.visitLabel(label3);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 6, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String", "[Ljava/lang/String;", Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[]{});
      methodVisitor.visitIincInsn(5, 1);
      methodVisitor.visitJumpInsn(GOTO, label1);
      methodVisitor.visitLabel(label2);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 5, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String", "[Ljava/lang/String;", Opcodes.INTEGER}, 0, new Object[]{});
      methodVisitor.visitVarInsn(ILOAD, 4);
      Label label4 = new Label();
      methodVisitor.visitJumpInsn(IFGE, label4);
      methodVisitor.visitTypeInsn(NEW, "java/lang/RuntimeException");
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn("Unsupported environment: Your operating system is unsupported");
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
      methodVisitor.visitInsn(ATHROW);
      methodVisitor.visitLabel(label4);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 5, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String", "[Ljava/lang/String;", Opcodes.INTEGER}, 0, new Object[]{});
      methodVisitor.visitVarInsn(ILOAD, 0);
//      Label label5 = new Label();
//      Label label6 = new Label();
      List<Label> labels = new ArrayList<>();

      int availableClasses = linkageInformation.classes().size();
      for (int i = 0; i < availableClasses; i++) {
        labels.add(new Label());
      }

      Label escape = new Label();
      methodVisitor.visitLookupSwitchInsn(escape, IntStream.range(0, availableClasses).toArray(), labels.toArray(new Label[0])/*new Label[]{label5, label6}*/);

      Label proceed = new Label();

      for (int i = 0; i < availableClasses; i++) {
        String className = linkageInformation.classes().get(i);
        methodVisitor.visitLabel(labels.get(i));
        methodVisitor.visitLdcInsn(className.replace("/", "."));
        methodVisitor.visitVarInsn(ASTORE, 5);
        String[] operatingSystems = linkageInformation.supportedOperatingSystems();
        if (operatingSystems.length <= 5) {
          methodVisitor.visitInsn(ICONST_0 + operatingSystems.length);
        } else if (operatingSystems.length <= 127) {
          methodVisitor.visitIntInsn(BIPUSH, operatingSystems.length);
        } else {
          methodVisitor.visitIntInsn(SIPUSH, operatingSystems.length);
        }
        methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/String");
        for (int j = 0; j < operatingSystems.length; j++) {
          String operatingSystem = operatingSystems[j];
          methodVisitor.visitInsn(DUP);
          if (j <= 5) {
            methodVisitor.visitInsn(ICONST_0 + j);
          } else if (i <= 127) {
            methodVisitor.visitIntInsn(BIPUSH, j);
          } else {
            methodVisitor.visitIntInsn(SIPUSH, j);
          }
          methodVisitor.visitLdcInsn("/" + linkageInformation.binaryNameOf(className, operatingSystem));
          methodVisitor.visitInsn(AASTORE);
        }
        methodVisitor.visitVarInsn(ASTORE, 6);
        methodVisitor.visitJumpInsn(GOTO, proceed);
      }

//      methodVisitor.visitInsn(ICONST_3);
//      methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/String");
//      methodVisitor.visitInsn(DUP);
//      methodVisitor.visitInsn(ICONST_0);
//      methodVisitor.visitLdcInsn("/natify/bin/aec658b3");
//      methodVisitor.visitInsn(AASTORE);
//      methodVisitor.visitInsn(DUP);
//      methodVisitor.visitInsn(ICONST_1);
//      methodVisitor.visitLdcInsn("/natify/bin/aec658b3");
//      methodVisitor.visitInsn(AASTORE);
//      methodVisitor.visitInsn(DUP);
//      methodVisitor.visitInsn(ICONST_2);
//      methodVisitor.visitLdcInsn("/natify/bin/aec658b3");
//      methodVisitor.visitInsn(AASTORE);
//      methodVisitor.visitVarInsn(ASTORE, 6);
//      methodVisitor.visitJumpInsn(GOTO, proceed);

//      methodVisitor.visitLabel(label6);
//      methodVisitor.visitLdcInsn("de/jpx3/sevastopol/Test2");
//      methodVisitor.visitVarInsn(ASTORE, 5);
//      methodVisitor.visitInsn(ICONST_3);
//      methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/String");
//      methodVisitor.visitInsn(DUP);
//      methodVisitor.visitInsn(ICONST_0);
//      methodVisitor.visitLdcInsn("/natify/bin/aec658b");
//      methodVisitor.visitInsn(AASTORE);
//      methodVisitor.visitInsn(DUP);
//      methodVisitor.visitInsn(ICONST_1);
//      methodVisitor.visitLdcInsn("/natify/bin/aec658b");
//      methodVisitor.visitInsn(AASTORE);
//      methodVisitor.visitInsn(DUP);
//      methodVisitor.visitInsn(ICONST_2);
//      methodVisitor.visitLdcInsn("/natify/bin/aec658b");
//      methodVisitor.visitInsn(AASTORE);
//      methodVisitor.visitVarInsn(ASTORE, 6);
//      methodVisitor.visitJumpInsn(GOTO, proceed);

      methodVisitor.visitLabel(escape);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 5, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String", "[Ljava/lang/String;", Opcodes.INTEGER}, 0, new Object[]{});
      methodVisitor.visitTypeInsn(NEW, "java/lang/IllegalStateException");
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
      methodVisitor.visitLdcInsn("Unknown index ");
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
      methodVisitor.visitVarInsn(ILOAD, 0);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V", false);
      methodVisitor.visitInsn(ATHROW);
      methodVisitor.visitLabel(proceed);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 7, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String", "[Ljava/lang/String;", Opcodes.INTEGER, "java/lang/String", "[Ljava/lang/String;"}, 0, new Object[]{});
      methodVisitor.visitVarInsn(ALOAD, 6);
      methodVisitor.visitVarInsn(ILOAD, 4);
      methodVisitor.visitInsn(AALOAD);
      methodVisitor.visitVarInsn(ASTORE, 7);
      methodVisitor.visitLdcInsn("natify");
      methodVisitor.visitInsn(ACONST_NULL);
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/io/File", "createTempFile", "(Ljava/lang/String;Ljava/lang/String;)Ljava/io/File;", false);
      methodVisitor.visitVarInsn(ASTORE, 8);
      methodVisitor.visitVarInsn(ALOAD, 8);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/File", "deleteOnExit", "()V", false);
      methodVisitor.visitVarInsn(ALOAD, 8);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/File", "exists", "()Z", false);
      Label label9 = new Label();
      methodVisitor.visitJumpInsn(IFNE, label9);
      methodVisitor.visitTypeInsn(NEW, "java/io/IOException");
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn("Unable to access temporary directory");
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/io/IOException", "<init>", "(Ljava/lang/String;)V", false);
      methodVisitor.visitInsn(ATHROW);
      methodVisitor.visitLabel(label9);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 9, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String", "[Ljava/lang/String;", Opcodes.INTEGER, "java/lang/String", "[Ljava/lang/String;", "java/lang/String", "java/io/File"}, 0, new Object[]{});
      methodVisitor.visitIntInsn(SIPUSH, 2048);
      methodVisitor.visitIntInsn(NEWARRAY, T_BYTE);
      methodVisitor.visitVarInsn(ASTORE, 9);
      methodVisitor.visitVarInsn(ALOAD, 5);
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
      methodVisitor.visitVarInsn(ASTORE, 10);
      methodVisitor.visitVarInsn(ALOAD, 10);
      methodVisitor.visitVarInsn(ALOAD, 7);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
      methodVisitor.visitVarInsn(ASTORE, 11);
      methodVisitor.visitTypeInsn(NEW, "java/io/FileOutputStream");
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitVarInsn(ALOAD, 8);
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/io/FileOutputStream", "<init>", "(Ljava/io/File;)V", false);
      methodVisitor.visitVarInsn(ASTORE, 12);
      Label label10 = new Label();
      methodVisitor.visitLabel(label10);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 13, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String", "[Ljava/lang/String;", Opcodes.INTEGER, "java/lang/String", "[Ljava/lang/String;", "java/lang/String", "java/io/File", "[B", "java/lang/Class", "java/io/InputStream", "java/io/FileOutputStream"}, 0, new Object[]{});
      methodVisitor.visitVarInsn(ALOAD, 11);
      methodVisitor.visitVarInsn(ALOAD, 9);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "read", "([B)I", false);
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitVarInsn(ISTORE, 13);
      methodVisitor.visitInsn(ICONST_M1);
      Label label11 = new Label();
      methodVisitor.visitJumpInsn(IF_ICMPEQ, label11);
      methodVisitor.visitVarInsn(ALOAD, 12);
      methodVisitor.visitVarInsn(ALOAD, 9);
      methodVisitor.visitInsn(ICONST_0);
      methodVisitor.visitVarInsn(ILOAD, 13);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/FileOutputStream", "write", "([BII)V", false);
      methodVisitor.visitJumpInsn(GOTO, label10);
      methodVisitor.visitLabel(label11);
      //methodVisitor.visitFrame(Opcodes.F_NEW, 14, new Object[]{Opcodes.INTEGER, "java/lang/String", "java/lang/String", "[Ljava/lang/String;", Opcodes.INTEGER, "java/lang/String", "[Ljava/lang/String;", "java/lang/String", "java/io/File", "[B", "java/lang/Class", "java/io/InputStream", "java/io/FileOutputStream", Opcodes.INTEGER}, 0, new Object[]{});
      methodVisitor.visitVarInsn(ALOAD, 12);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/FileOutputStream", "close", "()V", false);
      methodVisitor.visitVarInsn(ALOAD, 11);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
      methodVisitor.visitVarInsn(ALOAD, 8);
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;", false);
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "load", "(Ljava/lang/String;)V", false);
      methodVisitor.visitInsn(RETURN);
      methodVisitor.visitMaxs(4, 14);
      methodVisitor.visitEnd();
    }
    classWriter.visitEnd();
    ClassReader cr = new ClassReader(classWriter.toByteArray());
    ClassNode cn = new ClassNode();
    try {
      cr.accept(cn, 0);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return cn;
  }
}
