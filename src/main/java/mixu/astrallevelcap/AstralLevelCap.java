package mixu.astrallevelcap;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

/**
 * Главный класс мода AstralLevelCap
 *
 * Этот мод увеличивает максимальный уровень перков в Astral Sorcery.
 * По умолчанию в Astral Sorcery максимум 30 уровней, этот мод позволяет увеличить до 100 (или другого значения).
 */
@Mod(
        modid = AstralLevelCap.MODID,           // ID мода (уникальный идентификатор)
        name = AstralLevelCap.MODNAME,          // Название мода
        version = AstralLevelCap.VERSION,       // Версия мода
        dependencies = "required-after:astralsorcery",  // Зависимости: требуется Astral Sorcery
        useMetadata = true                      // Использовать метаданные из mcmod.info
)
public class AstralLevelCap {

    // Константы мода
    public static final String MODID = "astral-level-cap";
    public static final String MODNAME = "Astral Level Cap";
    public static final String VERSION = "1.0.0";

    // Логгер для вывода сообщений в консоль
    public static Logger logger;

    /**
     * Экземпляр мода, создается Forge автоматически
     */
    @Mod.Instance(MODID)
    public static AstralLevelCap INSTANCE;

    /**
     * Метод вызывается на этапе пре-инициализации мода
     * Здесь регистрируем обработчики событий и инициализируем логгер
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Регистрируем этот класс для получения событий
        MinecraftForge.EVENT_BUS.register(this);
        // Получаем логгер для вывода сообщений
        logger = event.getModLog();
    }

    /**
     * Обработчик события изменения конфигурации
     * Вызывается когда игрок меняет настройки мода в игре
     */
    @SubscribeEvent
    public void onConfigChanged(OnConfigChangedEvent event) {
        // Проверяем, что изменилась конфигурация именно нашего мода
        if (event.getModID().equals(MODID)) {
            // Синхронизируем конфигурацию (сохраняем изменения)
            ConfigManager.sync(MODID, Type.INSTANCE);
        }
    }

    /**
     * Класс конфигурации мода
     * Здесь хранятся все настройки, которые можно изменить
     */
    @Config(modid = AstralLevelCap.MODID, name = MODID)
    public static class AstralLevelCapConfig {
        
        // Настройка максимального уровня
        @Config.Name("max_level")  // Имя параметра в конфиг-файле
        @Config.Comment("Maximum level for Astral Sorcery perks (default: 100)")  // Комментарий в конфиг-файле
        @Config.RangeInt(min = 30, max = 200)  // Диапазон допустимых значений: от 30 до 200
        public static int maxLevel = 100;  // Значение по умолчанию: 100
    }
}
