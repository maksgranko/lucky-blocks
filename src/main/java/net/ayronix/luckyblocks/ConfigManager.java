package net.ayronix.luckyblocks;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.Set;

public class ConfigManager
{

    private final FileConfiguration config;
    private final LuckyBlockPlugin plugin;

    public ConfigManager(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
        this.config = plugin.getConfigFile();
    }

    public Set<String> getAvailableTypes()
    {
        ConfigurationSection typesSection = config.getConfigurationSection("types");
        if (typesSection == null)
        {
            plugin.getLogger().warning("Секция 'types' не найдена в config.yml!");
            return Collections.emptySet();
        }
        return typesSection.getKeys(false);
    }

    @SuppressWarnings("LoggerStringConcat")
    public int getMinLevel(String type)
    {
        ConfigurationSection levelsSection = config.getConfigurationSection("types." + type + ".levels");
        if (levelsSection == null)
        {
            plugin.getLogger().warning("Секция 'types." + type + ".levels' не найдена!");
            return 1;
        }
        return levelsSection.getKeys(false).stream().filter(key -> key.matches("\\d+")).mapToInt(Integer::parseInt)
                .min().orElse(1);
    }

    @SuppressWarnings("LoggerStringConcat")
    public int getMaxLevel(String type)
    {
        ConfigurationSection levelsSection = config.getConfigurationSection("types." + type + ".levels");
        if (levelsSection == null)
        {
            plugin.getLogger().warning("Секция 'types." + type + ".levels' не найдена!");
            return 1;
        }
        return levelsSection.getKeys(false).stream().filter(key -> key.matches("\\d+")).mapToInt(Integer::parseInt)
                .max().orElse(1);
    }

    public boolean isValidLevel(String type, int level)
    {
        ConfigurationSection levelConfig = config.getConfigurationSection("types." + type + ".levels." + level);
        return levelConfig != null;
    }

    @SuppressWarnings("LoggerStringConcat")
    public Material getLuckyBlockMaterial(String type)
    {
        String materialName = config.getString("types." + type + ".item.material", "SPONGE");
        try
        {
            if (materialName != null)
                return Material.valueOf(materialName.toUpperCase());
            else
                return Material.SPONGE;
        } catch (IllegalArgumentException e)
        {
            plugin.getLogger()
                    .warning("Неверный материал '" + materialName + "' для типа '" + type + "'. Используется SPONGE.");
            return Material.SPONGE;
        }
    }

    public int getLuckyBlockCustomModelData(String type)
    {
        return config.getInt("types." + type + ".item.custom-model-data", 0);
    }

    public String getDisplayName(String type, int level)
    {
        return config.getString("types." + type + ".levels." + level + ".display-name", type + " Level " + level);
    }

    public ConfigurationSection getEventsSection(String type, int level)
    {
        return config.getConfigurationSection("types." + type + ".levels." + level + ".events");
    }

    public ConfigurationSection getEventConfig(String type, int level, String eventKey)
    {
        return config.getConfigurationSection("types." + type + ".levels." + level + ".events." + eventKey);
    }

    public FileConfiguration getRawConfig()
    {
        return config;
    }
}
