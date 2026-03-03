package mixu.astrallevelcap.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Плагин для загрузки ASM трансформера
 *
 * Это специальный класс, который загружается ДО загрузки всех модов.
 * Он нужен для того, чтобы изменить байткод (скомпилированный код) других модов
 * ДО того, как они будут загружены в память.
 *
 * В нашем случае - мы изменяем код Astral Sorcery, чтобы увеличить максимальный уровень.
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")  // Указываем версию Minecraft
@IFMLLoadingPlugin.Name("AstralLevelCapPlugin")  // Имя плагина
public class AstralLevelCapPlugin implements IFMLLoadingPlugin {
    
    // Логгер для вывода сообщений в консоль (используется в трансформере)
    public static Logger logger = LogManager.getLogger("AstralLevelCap Transformer");

    /**
     * Возвращает список классов-трансформеров, которые нужно загрузить
     * Трансформер - это класс, который изменяет байткод других классов
     */
    @Override
    public String[] getASMTransformerClass() {
        // Возвращаем наш трансформер AstralLevelCapTransformer
        return new String[]{AstralLevelCapTransformer.class.getName()};
    }

    /**
     * Возвращает класс контейнера мода (не используется в нашем случае)
     */
    @Override
    public String getModContainerClass() {
        return null;  // Не используем
    }

    /**
     * Возвращает класс для настройки (не используется в нашем случае)
     */
    @Nullable
    @Override
    public String getSetupClass() {
        return null;  // Не используем
    }

    /**
     * Метод для получения дополнительных данных от Forge (не используется в нашем случае)
     */
    @Override
    public void injectData(Map<String, Object> data) {
        // Ничего не делаем, данные нам не нужны
    }

    /**
     * Возвращает класс Access Transformer (не используется в нашем случае)
     * Access Transformer нужен для изменения модификаторов доступа (private/public)
     */
    @Override
    public String getAccessTransformerClass() {
        return null;  // Не используем
    }
}
