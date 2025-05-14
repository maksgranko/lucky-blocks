package net.ayronix.luckyblocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.configuration.ConfigurationSection;

public class ProbabilityCalculator
{

    public static String getRandomEvent(ConfigManager configManager, String type, int level)
    {
        ConfigurationSection eventsSection = configManager.getEventsSection(type, level);
        if (eventsSection == null)
        {
            // Логируем, если нужно, или просто возвращаем null
            // plugin.getLogger().warning("Секция событий не найдена для " +
            // type + " Lvl " + level);
            return null;
        }

        List<Map.Entry<String, Object>> enabledEventWeights = new ArrayList<>();
        int totalWeight = 0;

        for (String eventKey : eventsSection.getKeys(false))
        {
            Object value = eventsSection.get(eventKey);
            if (value instanceof List<?> cfgList)
            {
                int idx = 0;
                for (Object obj : cfgList)
                {
                    if (obj instanceof Map<?, ?> map)
                    {
                        boolean enabled = true;
                        if (map.containsKey("enabled"))
                            enabled = Boolean.parseBoolean(String.valueOf(map.get("enabled")));
                        int weight = 0;
                        if (map.containsKey("weight"))
                            weight = Integer.parseInt(String.valueOf(map.get("weight")));
                        if (enabled && weight > 0)
                        {
                            // гарантия уникальности: eventKey#N
                            enabledEventWeights.add(Map.entry(eventKey + "#" + idx, weight));
                            totalWeight += weight;
                        }
                    }
                    idx++;
                }
            } else
            {
                ConfigurationSection eventConfig = eventsSection.getConfigurationSection(eventKey);
                if (eventConfig != null && (eventConfig.getBoolean("enabled", true)))
                {
                    int weight = eventConfig.getInt("weight", 0);
                    if (weight > 0)
                    {
                        enabledEventWeights.add(Map.entry(eventKey, weight));
                        totalWeight += weight;
                    }
                }
            }
        }

        if (totalWeight <= 0 || enabledEventWeights.isEmpty())
        {
            return null;
        }

        int randomValue = new Random().nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (Map.Entry<String, Object> entry : enabledEventWeights)
        {
            cumulativeWeight += (Integer) entry.getValue();
            if (randomValue < cumulativeWeight)
            {
                return entry.getKey();
            }
        }
        return null;
    }
}
