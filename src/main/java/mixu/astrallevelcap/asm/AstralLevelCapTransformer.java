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
    
    // Имя метода в этом классе, который мы хотим изменить
    // В этом методе создается цикл for от 1 до 30 (максимальный уровень)
    private static final String astralFunction = "ensureLevels";

    /**
     * Проверяет, является ли данная инструкция байткода числом 30
     *
     * В байткоде числа представлены инструкциями:
     * - BIPUSH - для чисел от -128 до 127 (1 байт)
     * - SIPUSH - для чисел от -32768 до 32767 (2 байта)
     *
     * Мы ищем именно число 30, потому что это максимальный уровень в Astral Sorcery
     */
    private static boolean isMaxLevelNode(AbstractInsnNode node) {
        if (node == null) return false;
        
        // Проверяем, является ли инструкция BIPUSH с операндом 30
        if (node.getOpcode() == BIPUSH) {
            IntInsnNode intNode = (IntInsnNode) node;  // Приводим к типу IntInsnNode (инструкция с целым числом)
            return intNode.operand == 30;  // Проверяем, что число равно 30
        }
        
        // Проверяем, является ли инструкция SIPUSH с операндом 30
        if (node.getOpcode() == SIPUSH) {
            IntInsnNode intNode = (IntInsnNode) node;
            return intNode.operand == 30;
        }
        
        return false;  // Это не число 30
    }

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
     * Метод, который находит и изменяет нужные методы в классе
     *
     * ВАЖНО: Мы заменяем число 30 во ВСЕХ методах класса, потому что:
     * 1. В методе ensureLevels() - создается таблица опыта (строка 44: for (int i = 1; i <= 30; i++))
     * 2. В методе getNextLevelPercent() - проверяется максимальный уровень (строка 90: if (level >= 30))
     * 3. Могут быть другие места, где используется это ограничение
     *
     * @param clazz - узел класса (представление класса в виде дерева)
     */
    private void transformAstral(ClassNode clazz) {
        int totalReplacedCount = 0;  // Общий счетчик замененных инструкций
        
        // Перебираем ВСЕ методы в классе (не только ensureLevels)
        for (MethodNode method : clazz.methods) {
            int methodReplacedCount = 0;  // Счетчик для текущего метода
            
            // Перебираем все инструкции в методе
            // method.instructions.toArray() - преобразуем список инструкций в массив
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                
                // Проверяем, является ли текущая инструкция числом 30
                if (isMaxLevelNode(instruction)) {
                    AstralLevelCapPlugin.logger.info("Found max level constant (30) in method: " + method.name);
                    
                    // Получаем новое значение максимального уровня из конфигурации
                    int newMaxLevel = AstralLevelCap.AstralLevelCapConfig.maxLevel;
                    AbstractInsnNode newInstruction;  // Новая инструкция
                    
                    // Выбираем тип инструкции в зависимости от размера числа
                    if (newMaxLevel <= 127) {
                        // Для чисел <= 127 используем BIPUSH (занимает меньше места)
                        newInstruction = new IntInsnNode(BIPUSH, newMaxLevel);
                    } else {
                        // Для больших чисел используем SIPUSH
                        newInstruction = new IntInsnNode(SIPUSH, newMaxLevel);
                    }
                    
                    // ГЛАВНОЕ: Заменяем старую инструкцию (BIPUSH 30) на новую (BIPUSH 100 или другое значение)
                    method.instructions.set(instruction, newInstruction);
                    methodReplacedCount++;  // Увеличиваем счетчик для метода
                    totalReplacedCount++;   // Увеличиваем общий счетчик
                    
                    AstralLevelCapPlugin.logger.info("Replaced max level from 30 to " + newMaxLevel + " in method: " + method.name);
                }
            }
            
            // Если в методе были замены - выводим информацию
            if (methodReplacedCount > 0) {
                AstralLevelCapPlugin.logger.info("Method '" + method.name + "': replaced " + methodReplacedCount + " constant(s)");
            }
        }
        
        // Проверяем, нашли ли мы хотя бы одну инструкцию во всем классе
        if (totalReplacedCount == 0) {
            AstralLevelCapPlugin.logger.warn("No max level constant found in any method! The mod might not work correctly.");
        } else {
            AstralLevelCapPlugin.logger.info("Total: Successfully replaced " + totalReplacedCount + " max level constant(s) across all methods");
        }
    }
}
