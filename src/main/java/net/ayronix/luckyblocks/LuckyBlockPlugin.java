package net.ayronix.luckyblocks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.ayronix.luckyblocks.events.DefaultEvent;
import net.ayronix.luckyblocks.events.ExplosionEvent;
import net.ayronix.luckyblocks.events.HealEvent;
import net.ayronix.luckyblocks.events.ICustomEvent;

/**
 * LuckyBlockPlugin — главный класс плагина Lucky Blocks.
 *
 * Для управления отладкой используйте глобальный флаг debug: Если debug == true
 * — все отладочные сообщения выводятся в server.log. Если debug == false — вся
 * отладка подавляется.
 */
public final class LuckyBlockPlugin extends JavaPlugin
{
    /**
     * Глобальный флаг для управления отладкой. Вся отладочная информация в
     * плагине проходит только через этот флаг. При выключенном debug (по
     * умолчанию) отладка не выводится вообще.
     */
    public static boolean debug = true;
    public static NamespacedKey LUCKY_BLOCK_KEY;
    public static NamespacedKey LUCKY_BLOCK_TYPE_KEY;
    public static NamespacedKey LUCKY_BLOCK_LEVEL_KEY;

    private FileConfiguration luckyblocksConfig;
    private ConfigManager configManager;
    private Map<String, ICustomEvent> eventRegistry;

    public FileConfiguration getConfigFile()
    {
        return luckyblocksConfig;
    }

    public ConfigManager getConfigManager()
    {
        return configManager;
    }

    public Map<String, ICustomEvent> getEventRegistry()
    {
        return eventRegistry;
    }

    @Override @SuppressWarnings("LoggerStringConcat")
    public void onEnable()
    {
        LUCKY_BLOCK_KEY = new NamespacedKey(this, "lucky_block");
        LUCKY_BLOCK_TYPE_KEY = new NamespacedKey(this, "lucky_block_type");
        LUCKY_BLOCK_LEVEL_KEY = new NamespacedKey(this, "lucky_block_level");

        File configFile = new File(getDataFolder(), "luckyblocks.yml");
        if (!configFile.exists())
        {
            saveResource("luckyblocks.yml", false);
            getLogger().info("Не найден luckyblocks.yml. Выгружен шаблон.");
        }
        File chestsFile = new File(getDataFolder(), "chests.yml");
        if (!chestsFile.exists())
        {
            saveResource("chests.yml", false);
            getLogger().info("Не найден chests.yml. Выгружен шаблон.");
        }

        try
        {
            luckyblocksConfig = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e)
        {
            getLogger().severe("Ошибка загрузки luckyblocks.yml: " + e.getMessage());
            luckyblocksConfig = new YamlConfiguration();
        }

        configManager = new ConfigManager(this);
        registerEvents();

        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosionListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockProtectionListener(this), this);
        PluginCommand cmd = getCommand("luckyblock");
        if (cmd != null)
        {
            cmd.setExecutor(new LuckyBlockCommand(this));
            cmd.setTabCompleter(new LuckyBlockTabCompleter(this));
        } else
        {
            getLogger().severe("Команда 'luckyblock' не найдена!");
        }
    }

    private void registerEvents()
    {
        eventRegistry = new HashMap<>();
        eventRegistry.put("EXPLOSION", new ExplosionEvent());
        eventRegistry.put("DROP_ITEM", new net.ayronix.luckyblocks.events.DropItemEvent());
        eventRegistry.put("PLACE_BLOCK", new net.ayronix.luckyblocks.events.PlaceBlockEvent());
        eventRegistry.put("PLACE_CHEST", new net.ayronix.luckyblocks.events.PlaceChestEvent());
        eventRegistry.put("COMMAND_EXEC", new net.ayronix.luckyblocks.events.CommandExecEvent());
        eventRegistry.put("PLAYER_COMMAND", new net.ayronix.luckyblocks.events.PlayerCommandEvent());
        eventRegistry.put("HEAL", new HealEvent());
        eventRegistry.put("DEFAULT", new DefaultEvent());
    }

    public void reloadConfigAndChests()
    {
        File configFile = new File(getDataFolder(), "luckyblocks.yml");
        if (!configFile.exists())
        {
            saveResource("luckyblocks.yml", false);
        }
        File chestsFile = new File(getDataFolder(), "chests.yml");
        if (!chestsFile.exists())
        {
            saveResource("chests.yml", false);
        }
        try
        {
            luckyblocksConfig = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e)
        {
            getLogger().severe("Ошибка загрузки luckyblocks.yml: " + e.getMessage());
            luckyblocksConfig = new YamlConfiguration();
        }
        // Обновить менеджер
        configManager = new ConfigManager(this);
        // В будущем можно также добавить reload для chests.yml или других
        // ресурсов

        getLogger().info("Конфиги успешно перезагружены (config.yml, manager).");
    }

    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
    }

}
