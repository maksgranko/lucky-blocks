package net.ayronix.luckyblocks;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

        // Фильтруем события, которые выключены (enabled: false)
        List<Map.Entry<String, Object>> enabledEventWeights = new ArrayList<>();
        int totalWeight = 0;

        for (String eventKey : eventsSection.getKeys(false))
        {
            ConfigurationSection eventConfig = eventsSection.getConfigurationSection(eventKey);
            if (eventConfig != null && eventConfig.getBoolean("enabled", true))
            { // По умолчанию событие включено
                int weight = eventConfig.getInt("weight", 0);
                if (weight > 0)
                {
                    // Вместо getValues(false) которое возвращает Map<String,
                    // Object> где Object это ConfigurationSection,
                    // нам нужен сам вес. Мы уже получили eventConfig.
                    enabledEventWeights.add(Map.entry(eventKey, weight));
                    totalWeight += weight;
                }
            }
        }

        if (totalWeight <= 0 || enabledEventWeights.isEmpty())
        {
            return null; // Нет доступных событий или все веса нулевые
        }

        int randomValue = new Random().nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (Map.Entry<String, Object> entry : enabledEventWeights)
        {
            // Object здесь это Integer (вес), так как мы его так добавили
            cumulativeWeight += (Integer) entry.getValue();
            if (randomValue < cumulativeWeight)
            {
                return entry.getKey(); // Возвращаем ключ события (его имя)
            }
        }
        return null; // Не должно произойти, если totalWeight > 0
    }
}
