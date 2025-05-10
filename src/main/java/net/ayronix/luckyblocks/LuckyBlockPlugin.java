package net.ayronix.luckyblocks;

import net.ayronix.luckyblocks.events.*;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class LuckyBlockPlugin extends JavaPlugin
{
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

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists())
        {
            saveResource("config.yml", false);
            getLogger().info("Создан новый конфиг config.yml");
        }

        try
        {
            luckyblocksConfig = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e)
        {
            getLogger().severe("Ошибка загрузки конфига: " + e.getMessage());
            luckyblocksConfig = new YamlConfiguration();
        }

        configManager = new ConfigManager(this);
        registerEvents();

        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosionListener(), this);

        if (getCommand("luckyblock") != null)
        {
            getCommand("luckyblock").setExecutor(new LuckyBlockCommand(this));

        } else
        {
            getLogger().severe("Команда 'luckyblock' не найдена!");
        }
    }

    private void registerEvents()
    {
        eventRegistry = new HashMap<>();
        eventRegistry.put("EXPLOSION", new ExplosionEvent());
        eventRegistry.put("DROP_FOOD", new DropFoodEvent());
        eventRegistry.put("PORTAL", new PortalEvent());
        eventRegistry.put("HEAL", new HealEvent());
        eventRegistry.put("DROP_WEAPON", new DropWeaponEvent());
        eventRegistry.put("DEFAULT", new DefaultEvent());
    }

    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
    }

}
