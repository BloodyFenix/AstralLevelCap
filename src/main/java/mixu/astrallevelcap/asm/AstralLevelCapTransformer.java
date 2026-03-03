package mixu.astrallevelcap.asm;

import mixu.astrallevelcap.AstralLevelCap;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * ASM Трансформер - это класс, который изменяет байткод (скомпилированный код) других классов
 *
 * ВАЖНО: Этот код работает с байткодом - низкоуровневым представлением Java-кода.
 * Байткод состоит из инструкций (opcodes), которые выполняет виртуальная машина Java.
 *
 * Наша задача: найти в коде Astral Sorcery число 30 (максимальный уровень) и заменить его на 100.
 */
public class AstralLevelCapTransformer implements IClassTransformer {
    
    // Полное имя класса, который мы хотим изменить (класс из мода Astral Sorcery)
    private static final String classToTransform = "hellfirepvp.astralsorcery.common.constellation.perk.PerkLevelManager";
    
    // Имя поля, которое хранит максимальный уровень
    private static final String LEVEL_CAP_FIELD = "LEVEL_CAP";

    /**
     * Главный метод трансформера, вызывается для КАЖДОГО класса, который загружается в игру
     *
     * @param name - внутреннее имя класса
     * @param transformedName - преобразованное имя класса
     * @param classBeingTransformed - байткод класса (массив байтов)
     * @return измененный или неизмененный байткод класса
     */
    @Override
    public byte[] transform(String name, String transformedName, byte[] classBeingTransformed) {
        // Проверяем, является ли загружаемый класс тем, который нам нужен
        if (transformedName.equals(classToTransform)) {
            // Если да - трансформируем его
            return transform(transformedName, classBeingTransformed);
        }
        // Если нет - возвращаем без изменений
        return classBeingTransformed;
    }

    /**
     * Метод, который выполняет саму трансформацию класса
     *
     * @param className - имя класса
     * @param classBeingTransformed - байткод класса
     * @return измененный байткод класса
     */
    private byte[] transform(String className, byte[] classBeingTransformed) {
        boolean ok = true;  // Флаг успешности трансформации
        
        try {
            // Шаг 1: Читаем байткод и преобразуем его в удобную структуру (ClassNode)
            ClassNode classNode = new ClassNode();  // Создаем узел класса (представление класса в виде дерева)
            ClassReader classReader = new ClassReader(classBeingTransformed);  // Создаем читатель байткода
            classReader.accept(classNode, 0);  // Читаем байткод в classNode

            // Шаг 2: Изменяем структуру класса (находим и заменяем число 30 на наше значение)
            transformAstral(classNode);
            
            // Шаг 3: Преобразуем измененную структуру обратно в байткод
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);  // Записываем classNode в classWriter
            
            // Возвращаем измененный байткод
            return classWriter.toByteArray();
            
        } catch (Exception e) {
            // Если произошла ошибка - логируем её
            ok = false;
            AstralLevelCapPlugin.logger.error("Failed to transform astral level cap!");
            AstralLevelCapPlugin.logger.error(e);
        } finally {
            // В любом случае выводим результат
            if (ok)
                AstralLevelCapPlugin.logger.info("Transformed astral level cap successfully to " + AstralLevelCap.AstralLevelCapConfig.maxLevel);
        }
        
        // Если произошла ошибка - возвращаем оригинальный байткод
        return classBeingTransformed;
    }

    /**
     * Метод, который находит и изменяет все использования числа 30 в классе
     *
     * В AstralSorcery максимальный уровень инициализируется в статическом блоке:
     * private static int LEVEL_CAP = 30;
     *
     * Значение 30 присваивается в методе <clinit> (статический инициализатор).
     * Мы находим все инструкции BIPUSH 30 и заменяем их на наше значение.
     *
     * @param clazz - узел класса (представление класса в виде дерева)
     */
    private void transformAstral(ClassNode clazz) {
        int newMaxLevel = AstralLevelCap.AstralLevelCapConfig.maxLevel;
        int totalReplacedCount = 0;
        boolean foundInClinit = false;
        
        AstralLevelCapPlugin.logger.info("Searching for LEVEL_CAP initialization (value 30) in class methods...");
        
        // Перебираем все методы класса
        for (MethodNode method : clazz.methods) {
            int methodReplacedCount = 0;
            
            // Перебираем все инструкции в методе
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                // Ищем инструкции BIPUSH или SIPUSH с операндом 30
                if (instruction.getOpcode() == BIPUSH || instruction.getOpcode() == SIPUSH) {
                    IntInsnNode intNode = (IntInsnNode) instruction;
                    if (intNode.operand == 30) {
                        // Проверяем, что следующая инструкция - это присваивание в LEVEL_CAP
                        // или это используется в цикле/сравнении
                        AbstractInsnNode nextInsn = instruction.getNext();
                        boolean isLevelCapAssignment = false;
                        
                        // Проверяем, является ли это присваиванием в статическое поле
                        if (nextInsn != null && nextInsn.getOpcode() == PUTSTATIC) {
                            FieldInsnNode fieldInsn = (FieldInsnNode) nextInsn;
                            if (fieldInsn.name.equals(LEVEL_CAP_FIELD)) {
                                isLevelCapAssignment = true;
                                foundInClinit = true;
                                AstralLevelCapPlugin.logger.info("Found LEVEL_CAP initialization in method: " + method.name);
                            }
                        }
                        
                        // Заменяем значение независимо от того, где оно используется
                        AstralLevelCapPlugin.logger.info("Found value 30 in method: " + method.name +
                            (isLevelCapAssignment ? " (LEVEL_CAP assignment)" : " (other usage)"));
                        
                        AbstractInsnNode newInstruction;
                        if (newMaxLevel <= 127) {
                            newInstruction = new IntInsnNode(BIPUSH, newMaxLevel);
                        } else {
                            newInstruction = new IntInsnNode(SIPUSH, newMaxLevel);
                        }
                        
                        method.instructions.set(instruction, newInstruction);
                        methodReplacedCount++;
                        totalReplacedCount++;
                        
                        AstralLevelCapPlugin.logger.info("Replaced value 30 to " + newMaxLevel + " in method: " + method.name);
                    }
                }
            }
            
            if (methodReplacedCount > 0) {
                AstralLevelCapPlugin.logger.info("Method '" + method.name + "': replaced " + methodReplacedCount + " constant(s)");
            }
        }
        
        if (totalReplacedCount == 0) {
            AstralLevelCapPlugin.logger.error("No value 30 found in any method! The mod will not work.");
        } else {
            AstralLevelCapPlugin.logger.info("Total: Successfully replaced " + totalReplacedCount + " constant(s)");
            if (foundInClinit) {
                AstralLevelCapPlugin.logger.info("Successfully patched LEVEL_CAP initialization!");
            }
        }
    }
}
