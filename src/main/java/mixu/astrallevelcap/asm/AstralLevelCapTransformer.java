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
     * Метод, который находит и изменяет поле LEVEL_CAP в классе
     *
     * В AstralSorcery максимальный уровень хранится в статическом поле:
     * private static int LEVEL_CAP = 30;
     *
     * Мы находим это поле и меняем его начальное значение с 30 на наше.
     *
     * @param clazz - узел класса (представление класса в виде дерева)
     */
    private void transformAstral(ClassNode clazz) {
        boolean fieldFound = false;
        int newMaxLevel = AstralLevelCap.AstralLevelCapConfig.maxLevel;
        
        AstralLevelCapPlugin.logger.info("Searching for LEVEL_CAP field in class...");
        
        // Перебираем все поля класса
        for (FieldNode field : clazz.fields) {
            // Ищем поле с именем LEVEL_CAP
            if (field.name.equals(LEVEL_CAP_FIELD)) {
                AstralLevelCapPlugin.logger.info("Found LEVEL_CAP field with value: " + field.value);
                
                // Меняем начальное значение поля
                field.value = newMaxLevel;
                fieldFound = true;
                
                AstralLevelCapPlugin.logger.info("Changed LEVEL_CAP field value from 30 to " + newMaxLevel);
                break;
            }
        }
        
        if (!fieldFound) {
            AstralLevelCapPlugin.logger.error("LEVEL_CAP field not found! The mod will not work.");
            return;
        }
        
        // Дополнительно: ищем и заменяем все прямые использования числа 30 в методах
        // Это нужно на случай, если где-то используется константа напрямую
        int totalReplacedCount = 0;
        
        for (MethodNode method : clazz.methods) {
            int methodReplacedCount = 0;
            
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                // Проверяем, является ли инструкция числом 30
                if (instruction.getOpcode() == BIPUSH) {
                    IntInsnNode intNode = (IntInsnNode) instruction;
                    if (intNode.operand == 30) {
                        AstralLevelCapPlugin.logger.info("Found hardcoded 30 in method: " + method.name);
                        
                        AbstractInsnNode newInstruction;
                        if (newMaxLevel <= 127) {
                            newInstruction = new IntInsnNode(BIPUSH, newMaxLevel);
                        } else {
                            newInstruction = new IntInsnNode(SIPUSH, newMaxLevel);
                        }
                        
                        method.instructions.set(instruction, newInstruction);
                        methodReplacedCount++;
                        totalReplacedCount++;
                        
                        AstralLevelCapPlugin.logger.info("Replaced hardcoded 30 to " + newMaxLevel + " in method: " + method.name);
                    }
                }
            }
            
            if (methodReplacedCount > 0) {
                AstralLevelCapPlugin.logger.info("Method '" + method.name + "': replaced " + methodReplacedCount + " hardcoded constant(s)");
            }
        }
        
        if (totalReplacedCount > 0) {
            AstralLevelCapPlugin.logger.info("Additionally replaced " + totalReplacedCount + " hardcoded constant(s) in methods");
        }
    }
}
