package de.jpx3.natify.translation;

import de.jpx3.natify.NatifyControl;
import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.controller.References;
import de.jpx3.natify.translation.controller.Variables;
import de.jpx3.natify.translation.insntrans.ExceptionThrowInstructionTranslator;
import de.jpx3.natify.translation.insntrans.InstructionTranslator;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;
import java.util.stream.IntStream;

import static de.jpx3.natify.translation.NatifyClassTranslator.SCOPE_SPACING;
import static de.jpx3.natify.translation.controller.Variables.*;
import static de.jpx3.natify.translation.insntrans.TypeHelper.OPCODES;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public final class NatifyMethodTranslator {
  private final static Handle CLASSLOADER_ACCESS_HANDLE = new Handle(0, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");

  private final TranslationConfiguration configuration;
  private final ClassNode owner;
  private final MethodNode targetMethodNode;
  private final String methodName;
  private final References references;

  public NatifyMethodTranslator(TranslationConfiguration configuration, ClassNode owner, References references, MethodNode targetMethodNode, String methodName) {
    this.configuration = configuration;
    this.owner = owner;
    this.references = references;
    this.targetMethodNode = targetMethodNode;
    this.methodName = methodName;
  }

  public TextBlock translateMethod() {
    TextBlock methodBlock = TextBlock.newEmpty();

    // load method parameters
    boolean staticMethod = (targetMethodNode.access & ACC_STATIC) != 0;
    String selfParameter = (staticMethod ? "jclass" : "jobject") + " self";
    StringBuilder argumentTypes = new StringBuilder();
    Type[] rawArgumentTypes = Type.getArgumentTypes(targetMethodNode.desc);
    for (int i = 0; i < rawArgumentTypes.length; i++) {
      Type rawArgumentType = rawArgumentTypes[i];
      argumentTypes
        .append(", ")
        .append(rawArgumentType.jniTargetName())
        .append(" ")
        .append("arg")
        .append(i);
    }

    // load return type
    Type returnType = Type.getReturnType(targetMethodNode.desc);

    // base method assignment
    methodBlock.appendLine("JNIEXPORT "+returnType.jniTargetName()+" JNICALL " + methodName + "(JNIEnv *env, " + selfParameter + argumentTypes + ") {");

    // load variable name telemetry
    Variables variables = new Variables(targetMethodNode);

    TextBlock codeBlock = TextBlock.newEmpty();
    {
      // translate instructions
      TextBlock instructionSet = translateEquivalentInstructionCode(variables);
      // prepare locals from instructions
      TextBlock prerequisites = generatePrerequisitesFor(variables, instructionSet);

      // insert prerequisites
      codeBlock.append(prerequisites);

      // insert translated instructions
      codeBlock.append(instructionSet);
    }

    // put code block to method body
    methodBlock.append(codeBlock, SCOPE_SPACING);

    // build code block end
    methodBlock.appendLine("}");
    return methodBlock;
  }

  private TextBlock generatePrerequisitesFor(Variables variableNameTelemetry, TextBlock instructionSet) {
    TextBlock code = TextBlock.newEmpty();
    int locals = findMaxUsedVariableSize(instructionSet, DEFAULT_LOCAL_VAR_PREFIX, variableNameTelemetry.locals(), targetMethodNode.maxLocals);
    int stack = findMaxUsedVariableSize(instructionSet, DEFAULT_STACK_VAR_PREFIX, variableNameTelemetry.stack(), targetMethodNode.maxStack);
    int temp = findMaxUsedVariableSize(instructionSet, DEFAULT_TEMP_VAR_PREFIX, variableNameTelemetry.temp(), 4);

    boolean virtualMethod = (targetMethodNode.access & ACC_STATIC) == 0;

    if(virtualMethod) {
      // reserve local var
      variableNameTelemetry.acquireLocalVar(0, 'l');
    }

    if(NatifyControl.OBFUSCATE_VARIABLE_NAMES) {
      List<String> variableNames = new ArrayList<>();
      code.appendLine("//temp: " + variableNameTelemetry.temp() + " | access: " + temp);
      variableNames.addAll(variableNameTelemetry.temp().subList(0, temp));
      code.appendLine("//local: " + variableNameTelemetry.locals() + " | access: " + locals);
      variableNames.addAll(variableNameTelemetry.locals().subList(0, locals));
      code.appendLine("//stack: " + variableNameTelemetry.stack() + " | access: " + stack);
      variableNames.addAll(variableNameTelemetry.stack().subList(0, stack));
      Collections.sort(variableNames);

      StringBuilder variables = new StringBuilder();
      int index = 0;
      for (String variableName : variableNames) {
        boolean last = index + 1 == variableNames.size();
        variables.append(variableName);
        if(!last) {
          variables.append(", ");
        }
        index++;
      }
      if(index > 0) {
        code.appendLine("jvalue " + variables + ";");
      }
    } else {
      code.appendLine("// " + locals + " locals");
      for (int i = 0; i < locals; i++) {
        code.appendLine("jvalue "+ DEFAULT_LOCAL_VAR_PREFIX + i + ";");
      }
      if(stack > 0 && locals > 0 && !(stack == 1 && locals == 1)) {
        code.emptyLine();
      }
      code.appendLine("// " + stack + " stacks");
      for (int i = 0; i < stack; i++) {
        code.appendLine("jvalue "+ DEFAULT_STACK_VAR_PREFIX + i + ";");
      }
      if(temp > 0 && !(stack == 1 && locals == 1)) {
        code.emptyLine();
      }
      code.appendLine("// " + temp + " temps");
      for (int i = 0; i < temp; i++) {
        code.appendLine("jvalue "+ DEFAULT_TEMP_VAR_PREFIX + i + ";");
      }
    }
    if(locals > 0 || stack > 0 || temp > 0) {
      code.emptyLine();
    }
    // load classloader if required
    boolean needsClassLoader = false;
    for (AbstractInsnNode instruction : targetMethodNode.instructions) {
      if (instruction instanceof InvokeDynamicInsnNode) {
        needsClassLoader = true;
        break;
      }
    }
    if(needsClassLoader) {
      code.appendLine("jobject classloader = (*env)->CallObjectMethod(env, " + references.classAccess(owner.name) + ", " + references.methodIdAccess(CLASSLOADER_ACCESS_HANDLE, false) + ");");
      code.emptyLine();
    }
    boolean localUsed = instructionSet.contains(variableNameTelemetry.acquireLocalVar(0) + ".l");
    // load this ref to locals
    if(virtualMethod && localUsed) {
      code.appendLine(variableNameTelemetry.acquireLocalVar(0, Type.OBJECT) + " = self;");
      code.emptyLine();
    }
    boolean addedArgsToLocalSwitch = false;
    Type[] argumentTypes = Type.getArgumentTypes(targetMethodNode.desc);
    for (int i = 0; i < argumentTypes.length; i++) {
      Type argumentType = argumentTypes[i];
      String localVariable = variableNameTelemetry.acquireLocalVar(i + (virtualMethod ? 1 : 0), argumentType);
      if(instructionSet.contains(localVariable)) {
        code.appendLine(localVariable + " = arg" + i + ";");
        addedArgsToLocalSwitch = true;
      }
    }
    if(addedArgsToLocalSwitch) {
      code.emptyLine();
    }
    return code;
  }

  private int findMaxUsedVariableSize(TextBlock instructionSet, String defaultName, List<String> availableVars, int max) {
    if(NatifyControl.OBFUSCATE_VARIABLE_NAMES) {
      int last = 0;
      // this is the correct way - don't change it
      for (int i = 1; i <= (Math.min(max, availableVars.size())); i++) {
        String generatedTempNode = availableVars.get(i - 1);
        if(instructionSet.contains(generatedTempNode + ".")) {
          last = i;
        }
      }
      return last;
    } else {
      int last = 0;
      for (int i = 1; i <= max; i++) {
        String generatedTempNode = defaultName + (i - 1);
        if(instructionSet.contains(generatedTempNode + ".")) {
          last = i;
        }
      }
      return last;
    }
  }

  private TextBlock translateEquivalentInstructionCode(Variables variables) {
    TextBlock codeBlock = TextBlock.newEmpty();
    // load frames for each instruction
    Frame<BasicValue>[] frames = tryAssignFrames();
    // pick used labels from all labels (to ignore unused labels later)
    List<LabelNode> labelNodeList = findUsedLabelNodes();

    List<LabelNode> passedLabelNodes = new ArrayList<>();

//    System.out.println(owner.name + "." + targetMethodNode.name);

    int index = 0;
    // iterate through instructions
    for (AbstractInsnNode instruction : targetMethodNode.instructions) {
      // load frame for instruction
      Frame<BasicValue> frame = frames[index];

//      try {
//        System.out.println(frame);
//      } catch (NullPointerException nullPointerException) {
//        nullPointerException.printStackTrace();
//        System.out.println(frame.getLocals() + " locals, " + frame.getMaxStackSize() + " stack. unable to identify types");
//      }

      // print instruction debug
      if(instruction.getOpcode() >= 0) {
        String insnName = OPCODES[instruction.getOpcode()];
        codeBlock.appendLine("// " + formatToSize(instruction.getClass().getSimpleName(), 16) + formatToSize(insnName, 16) + " " + frame);
      } else {
        codeBlock.appendLine("// " + formatToSize(instruction.getClass().getSimpleName(), 16) + formatToSize("NONE", 16) + " " + frame);
      }

      // find translator for instruction
      InstructionTranslator<?> instructionTranslator = InstructionTranslatorRepository.suitableTranslator(instruction);
      // translate instruction
      TextBlock translation = TextBlock.newEmpty();
      try {
        translation = instructionTranslator.translate(instruction, variables, references, configuration, frame, labelNodeList);
      } catch (Exception exception) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        System.out.println("An exception occurred while processing an " + OPCODES[instruction.getOpcode()] + " instruction (index " + index + ")");
        System.out.println("In: " + owner.name + "." + targetMethodNode.name + targetMethodNode.desc);
        System.out.println("Frame: " + frame);
        try {
          Thread.sleep(250);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        exception.printStackTrace();
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      // perform exception check if instruction might throw exception
      if(instructionTranslator.requiresExceptionCheck()) {
        List<TryCatchBlockNode> tryCatchBlocks = new ArrayList<>();
        for (TryCatchBlockNode tryCatchBlock : targetMethodNode.tryCatchBlocks) {
          LabelNode start = tryCatchBlock.start;
          LabelNode end = tryCatchBlock.end;
          if(passedLabelNodes.contains(start) && !passedLabelNodes.contains(end)) {
            tryCatchBlocks.add(tryCatchBlock);
          }
        }

        boolean exceptionInstruction = instructionTranslator instanceof ExceptionThrowInstructionTranslator;

        TextBlock exceptionCheck = TextBlock.newEmpty();
        if(!exceptionInstruction) {
          exceptionCheck.appendLine("if ((*env)->ExceptionCheck(env)) {");
        }
        TextBlock whenExceptionOccurredBlock = TextBlock.newEmpty();
        {
          boolean hasReturnType = Type.getReturnType(targetMethodNode.desc).getSort() != Type.VOID;
          boolean specificExceptionCheck = !tryCatchBlocks.isEmpty();

          if(specificExceptionCheck) {
            String exceptionAccessVariable = variables.acquireStackVar(0, 'l');
            whenExceptionOccurredBlock.appendLine(exceptionAccessVariable + " = (*env)->ExceptionOccurred(env);");
            for (TryCatchBlockNode tryCatchBlock : tryCatchBlocks) {
              if(tryCatchBlock.type == null) {
                LabelNode targetLabel = tryCatchBlock.handler;
                int targetLabelIndex = labelNodeList.indexOf(targetLabel);
                String targetLabelName = "L" + targetLabelIndex;

                whenExceptionOccurredBlock.appendLine("(*env)->ExceptionClear(env);");
                whenExceptionOccurredBlock.appendLine("goto " + targetLabelName + ";");
              } else {
                whenExceptionOccurredBlock.appendLine("if ((*env)->IsInstanceOf(env, "+ exceptionAccessVariable+", "+ references.classAccess(tryCatchBlock.type)+")) {");

                TextBlock matchingExceptionBlock = TextBlock.newEmpty();
                {
                  LabelNode targetLabel = tryCatchBlock.handler;
                  int targetLabelIndex = labelNodeList.indexOf(targetLabel);
                  String targetLabelName = "L" + targetLabelIndex;

                  matchingExceptionBlock.appendLine("(*env)->ExceptionClear(env);");
                  matchingExceptionBlock.appendLine("goto " + targetLabelName + ";");
                }
                whenExceptionOccurredBlock.append(matchingExceptionBlock, SCOPE_SPACING);
                whenExceptionOccurredBlock.appendLine("}");
              }
            }
          } else if(!exceptionInstruction) {
            if(hasReturnType) {
              whenExceptionOccurredBlock.appendLine("return "+ variables.acquireTempVar(0, Type.getReturnType(targetMethodNode.desc)) + ";");
            } else {
              whenExceptionOccurredBlock.appendLine("return;");
            }
          }
        }
        exceptionCheck.append(whenExceptionOccurredBlock, exceptionInstruction ? 0 : SCOPE_SPACING);
        if(!exceptionInstruction) {
          exceptionCheck.appendLine("}");
        }
        translation.append(exceptionCheck);
      }

      // put translated instruction to code
      if(translation != null && !translation.isEmpty()) {
        codeBlock.append(translation);
      }

      if(instruction instanceof LabelNode) {
        passedLabelNodes.add((LabelNode) instruction);
      }
      index++;
    }
    return codeBlock;
  }

  private Frame<BasicValue>[] tryAssignFrames() {
    try {
      // no singleton instance of class Analyzer in order to allow concurrency
      return new Analyzer<>(new BasicInterpreter()).analyze(owner.name, targetMethodNode);
    } catch (AnalyzerException e) {
      throw new IllegalStateException("Unable to analyze code correctly", e);
    }
  }

  private List<LabelNode> findUsedLabelNodes() {
    // resolve used and all label nodes
    List<LabelNode> allLabelNodes = new ArrayList<>();
    Set<LabelNode> usedLabelNodes = new HashSet<>();
    for (AbstractInsnNode instruction : targetMethodNode.instructions) {
      if (instruction instanceof LabelNode) {
        allLabelNodes.add((LabelNode) instruction);
      } else if (instruction instanceof JumpInsnNode) {
        usedLabelNodes.add(((JumpInsnNode) instruction).label);
      } else if(instruction instanceof TableSwitchInsnNode) {
        TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) instruction;
        usedLabelNodes.add(tableSwitchInsnNode.dflt);
        usedLabelNodes.addAll(tableSwitchInsnNode.labels);
      } else if(instruction instanceof LookupSwitchInsnNode) {
        usedLabelNodes.add(((LookupSwitchInsnNode) instruction).dflt);
        usedLabelNodes.addAll(((LookupSwitchInsnNode) instruction).labels);
      }
    }
    for (TryCatchBlockNode tryCatchBlock : targetMethodNode.tryCatchBlocks) {
      usedLabelNodes.add(tryCatchBlock.start);
      usedLabelNodes.add(tryCatchBlock.end);
      usedLabelNodes.add(tryCatchBlock.handler);
    }
    // remove unused nodes
    allLabelNodes.removeIf(node -> !usedLabelNodes.contains(node));
    return allLabelNodes;
  }

  private String formatToSize(String input, int requiredSize) {
    if(input.length() >= requiredSize) {
      return input;
    }
    return input + buildSpacing(requiredSize - input.length());
  }

  private String buildSpacing(int size) {
    StringBuilder space = new StringBuilder();
    IntStream.range(0, size).mapToObj(i -> " ").forEach(space::append);
    return space.toString();
  }
}
