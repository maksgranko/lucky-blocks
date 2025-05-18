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
        // Новый способ: собираем все события с учетом наследия (require)
        List<ConfigurationSection> allEvents = configManager.getEffectiveEventSections(type, level,
                new java.util.HashSet<>());

        if (LuckyBlockPlugin.debug)
            configManager.getPlugin().getLogger().info("getRandomEvent для типа: " + type + ", уровень: " + level
                    + ", собрано событий: " + allEvents.size());

        if (allEvents.isEmpty())
        {
            // Нет событий этого уровня (ни у себя, ни в require)
            return null;
        }

        List<Map.Entry<String, Object>> enabledEventWeights = new ArrayList<>();
        int totalWeight = 0;
        int idx = 0;
        for (ConfigurationSection eventConfig : allEvents)
        {
            String eventType = eventConfig.getName();
            boolean enabled = eventConfig.getBoolean("enabled", true);
            int weight = eventConfig.getInt("weight", 0);
            if (enabled && weight > 0)
            {
                // eventType#idx – уникальный для этого пула выборов
                enabledEventWeights.add(Map.entry(eventType + "#" + idx, weight));
                totalWeight += weight;
            }
            idx++;
        }

        if (LuckyBlockPlugin.debug)
            configManager.getPlugin().getLogger().info("getRandomEvent для типа: " + type + ", уровень: " + level
                    + ", enabledEventWeights: " + enabledEventWeights + ", totalWeight: " + totalWeight);

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
