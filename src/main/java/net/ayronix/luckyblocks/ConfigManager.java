package net.ayronix.luckyblocks;

import java.util.Collections;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

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
            plugin.getLogger().warning("Секция 'types' не найдена в luckyblocks.yml!");
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
        String[] split = eventKey.split("#");
        String key = split[0];
        int index = (split.length > 1) ? Integer.parseInt(split[1]) : -1;
        String path = "types." + type + ".levels." + level + ".events." + key;
        Object eventSectionObj = config.get(path);
        if (index >= 0 && eventSectionObj instanceof java.util.List<?> list)
        {
            Object entryObj = (index < list.size()) ? list.get(index) : null;
            if (entryObj instanceof org.bukkit.configuration.ConfigurationSection cs)
            {
                return cs;
            } else if (entryObj instanceof java.util.Map<?, ?> map)
            {
                // workaround для случаев, когда yaml парсит не как
                // ConfigurationSection
                // а как Map (сплошь и рядом при list-структуре)
                // создаем временную секцию, запихиваем туда map
                org.bukkit.configuration.MemoryConfiguration tempSection = new org.bukkit.configuration.MemoryConfiguration();
                map.forEach((k, v) -> tempSection.set(String.valueOf(k), v));
                return tempSection;
            }
            // если нет такого элемента, вернем null
            return null;
        } else
        {
            return config.getConfigurationSection(path);
        }
    }

    public FileConfiguration getRawConfig()
    {
        return config;
    }
}
