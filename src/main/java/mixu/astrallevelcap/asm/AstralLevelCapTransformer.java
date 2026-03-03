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
     * Метод, который находит и изменяет все использования LEVEL_CAP в классе
     *
     * В AstralSorcery максимальный уровень хранится в поле:
     * private static int LEVEL_CAP = 30;
     *
     * Мы заменяем:
     * 1. Инициализацию поля (BIPUSH 30 -> PUTSTATIC LEVEL_CAP) в <clinit>
     * 2. Все обращения к полю (GETSTATIC LEVEL_CAP) на константу с нашим значением
     *
     * @param clazz - узел класса (представление класса в виде дерева)
     */
    private void transformAstral(ClassNode clazz) {
        int newMaxLevel = AstralLevelCap.AstralLevelCapConfig.maxLevel;
        int constantsReplaced = 0;
        int fieldAccessReplaced = 0;
        
        AstralLevelCapPlugin.logger.info("Patching LEVEL_CAP usage in class...");
        
        // Перебираем все методы класса
        for (MethodNode method : clazz.methods) {
            // Перебираем все инструкции в методе
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                
                // 1. Заменяем константы 30 (инициализация и прямое использование)
                if (instruction.getOpcode() == BIPUSH || instruction.getOpcode() == SIPUSH) {
                    IntInsnNode intNode = (IntInsnNode) instruction;
                    if (intNode.operand == 30) {
                        AbstractInsnNode newInstruction;
                        if (newMaxLevel <= 127) {
                            newInstruction = new IntInsnNode(BIPUSH, newMaxLevel);
                        } else {
                            newInstruction = new IntInsnNode(SIPUSH, newMaxLevel);
                        }
                        
                        method.instructions.set(instruction, newInstruction);
                        constantsReplaced++;
                        AstralLevelCapPlugin.logger.info("Replaced constant 30 -> " + newMaxLevel + " in method: " + method.name);
                    }
                }
                
                // 2. Заменяем обращения к полю LEVEL_CAP (GETSTATIC) на константу
                if (instruction.getOpcode() == GETSTATIC) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
                    if (fieldInsn.name.equals(LEVEL_CAP_FIELD)) {
                        AbstractInsnNode newInstruction;
                        if (newMaxLevel <= 127) {
                            newInstruction = new IntInsnNode(BIPUSH, newMaxLevel);
                        } else {
                            newInstruction = new IntInsnNode(SIPUSH, newMaxLevel);
                        }
                        
                        method.instructions.set(instruction, newInstruction);
                        fieldAccessReplaced++;
                        AstralLevelCapPlugin.logger.info("Replaced GETSTATIC LEVEL_CAP -> " + newMaxLevel + " in method: " + method.name);
                    }
                }
            }
        }
        
        AstralLevelCapPlugin.logger.info("Patching complete:");
        AstralLevelCapPlugin.logger.info("  - Replaced " + constantsReplaced + " constant(s) 30");
        AstralLevelCapPlugin.logger.info("  - Replaced " + fieldAccessReplaced + " field access(es) to LEVEL_CAP");
        AstralLevelCapPlugin.logger.info("  - Total: " + (constantsReplaced + fieldAccessReplaced) + " replacement(s)");
        
        if (constantsReplaced + fieldAccessReplaced == 0) {
            AstralLevelCapPlugin.logger.error("No LEVEL_CAP usage found! The mod will not work.");
        }
    }
}
