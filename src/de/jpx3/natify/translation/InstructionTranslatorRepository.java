package de.jpx3.natify.translation;

import de.jpx3.natify.translation.insntrans.*;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.NOP;

public final class InstructionTranslatorRepository {
  private final static InstructionTranslator<?> DUMMY_TRANSLATOR = new NopInsnNodeTranslator();
  private final static List<InstructionTranslator<?>> translators = new CopyOnWriteArrayList<>();
  private final static Map<Class<?>, List<InstructionTranslator<?>>> fastTranslatorClassAccess = new HashMap<>();
  private final static Map<Integer, List<InstructionTranslator<?>>> fastTranslatorOpcodeAccess = new HashMap<>();
  private final static Map<InstructionTranslator<?>, Class<?>[]> translatorTargetInstructionCache = new ConcurrentHashMap<>();

  public static InstructionTranslator<?> suitableTranslator(AbstractInsnNode instruction) {
    List<InstructionTranslator<?>> availableTranslators = fastTranslatorOpcodeAccess.get(instruction.getOpcode());
    if(availableTranslators == null) {
      availableTranslators = fastTranslatorClassAccess.get(instruction.getClass());
      if(availableTranslators == null) {
        availableTranslators = translators;
      }
    }
    for (InstructionTranslator<?> translator : availableTranslators) {
      if (matches(translator, instruction)) {
        return translator;
      }
    }
    return DUMMY_TRANSLATOR;
  }

  private static boolean matches(InstructionTranslator<?> translator, AbstractInsnNode instructionNode) {
    boolean matchesOpcodeRange = translator.opcodeRange().matches(instructionNode.getOpcode());
    boolean matchesTargetInstructionNode = arrayContains(targetInstructionNodesOf(translator), instructionNode.getClass());
    return matchesOpcodeRange && matchesTargetInstructionNode;
  }

  private static Class<?>[] targetInstructionNodesOf(InstructionTranslator<?> translator) {
    return translatorTargetInstructionCache.computeIfAbsent(translator, InstructionTranslator::targetInstructions);
  }

  private static <R> boolean arrayContains(R[] array, R value) {
    int hash = value.hashCode();
    return Arrays.stream(array).anyMatch(r -> hash == r.hashCode() && r.equals(value));
  }

  static {
    loadTranslator(new LabelNodeTranslator());
    loadTranslator(new LineNumberTranslator());
    loadTranslator(new NopInsnNodeTranslator());
    loadTranslator(new LoadIntConstInstructionTranslator());
    loadTranslator(new IntInsnNodeTranslator());
    loadTranslator(new LdcInsnNodeTranslator());
    loadTranslator(new VarInsnInstructionTranslator());
    loadTranslator(new LSAEInstructionTranslator());
    loadTranslator(new PopDupSwapInstructionTranslator());
    loadTranslator(new MathInstructionTranslator());
    loadTranslator(new PrimitiveCastingInstructionTranslator());
    loadTranslator(new ConditionalJumpInstructionTranslator());
    loadTranslator(new JumpInstructionTranslator());
    loadTranslator(new RetJsrInstructionTranslator());
    loadTranslator(new TableInstructionTranslator());
    loadTranslator(new ReturnInstructionTranslator());
    loadTranslator(new FieldInstructionTranslator());
    loadTranslator(new MethodInstructionTranslator());
    loadTranslator(new InvokeDynamicTranslator());
    loadTranslator(new InstantiationInstructionTranslator());
    loadTranslator(new ExceptionThrowInstructionTranslator());
    loadTranslator(new MonitorInstructionTranslator());
    loadTranslator(new InstanceOfInstructionTranslator());
    loadTranslator(new CheckCastTypeInstructionTranslator());
    compile();
  }

  private static void compile() {
    for (InstructionTranslator<?> translator : translators) {
      for (Class<?> aClass : translator.targetInstructions()) {
        List<InstructionTranslator<?>> instructionTranslators = fastTranslatorClassAccess.computeIfAbsent(aClass, aClass1 -> new ArrayList<>());
        if(!instructionTranslators.contains(translator)) {
          instructionTranslators.add(translator);
        }
      }
    }
    for (int i = NOP; i < IFNONNULL; i++) {
      for (InstructionTranslator<?> translator : translators) {
        if(translator.opcodeRange().matches(i)) {
          List<InstructionTranslator<?>> instructionTranslators = fastTranslatorOpcodeAccess.computeIfAbsent(i, x -> new ArrayList<>());
          if(!instructionTranslators.contains(translator)) {
            instructionTranslators.add(translator);
          }
        }
      }
    }
  }

  private static void loadTranslator(InstructionTranslator<?> translator) {
    translators.add(translator);
  }
}
