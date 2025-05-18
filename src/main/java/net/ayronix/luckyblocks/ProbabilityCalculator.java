package net.ayronix.luckyblocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.configuration.ConfigurationSection;

public class ProbabilityCalculator
{

    /**
     * Выбирает случайное событие для данного типа и уровня лаки-блока, учитывая
     * require-наследование и веса событий.
     *
     * @param configManager Менеджер конфигурации.
     * @param type Тип лаки-блока.
     * @param level Уровень лаки-блока.
     * @return ConfigurationSection выбранного события, или null, если событий
     * нет.
     */
    public static ConfigurationSection getRandomEvent(ConfigManager configManager, String type, int level)
    {
        // Новый способ: собираем все события с учетом наследия (require)
        List<ConfigurationSection> allEvents = configManager.getEffectiveEventSections(type, level,
                new java.util.HashSet<>());

        if (configManager.getPlugin().getDebug())
            configManager.getPlugin().getLogger().info("getRandomEvent для типа: " + type + ", уровень: " + level
                    + ", собрано событий: " + allEvents.size());

        if (allEvents.isEmpty())
        {
            // Нет событий этого уровня (ни у себя, ни в require)
            return null;
        }

        List<Integer> enabledEventIndices = new ArrayList<>();
        List<Integer> enabledWeights = new ArrayList<>();
        int totalWeight = 0;
        int idx = 0;
        for (ConfigurationSection eventConfig : allEvents)
        {
            boolean enabled = eventConfig.getBoolean("enabled", true);
            int weight = eventConfig.getInt("weight", 0);
            if (enabled && weight > 0)
            {
                enabledEventIndices.add(idx);
                enabledWeights.add(weight);
                totalWeight += weight;
            }
            idx++;
        }

        if (configManager.getPlugin().getDebug())
            configManager.getPlugin().getLogger()
                    .info("getRandomEvent для типа: " + type + ", уровень: " + level + ", enabledEventIndices: "
                            + enabledEventIndices + ", enabledWeights: " + enabledWeights + ", totalWeight: "
                            + totalWeight);

        if (totalWeight <= 0 || enabledEventIndices.isEmpty())
        {
            return null;
        }

        int randomValue = new Random().nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (int i = 0; i < enabledEventIndices.size(); i++)
        {
            cumulativeWeight += enabledWeights.get(i);
            if (randomValue < cumulativeWeight)
            {
                return allEvents.get(enabledEventIndices.get(i));
            }
        }
        return null;
    }
}
