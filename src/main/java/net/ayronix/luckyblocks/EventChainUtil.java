package net.ayronix.luckyblocks;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Утилита для обработки вложенных execute/CommandExecEvent/PlayerCommandEvent
 * действий для лакиблоков.
 */
public class EventChainUtil
{

    /**
     * Выполнить вложенные командные события из eventConfig["execute"] (если
     * есть).
     * 
     * @param player игрок, для которого выполняется событие
     * @param location локация ивента
     * @param eventConfig конфиг ивента лакиблока
     * @param plugin ссылка на плагин
     */
    public static void executeChained(Player player, Location location, ConfigurationSection eventConfig,
            net.ayronix.luckyblocks.LuckyBlockPlugin plugin)
    {
        if (eventConfig == null)
            return;
        if (!eventConfig.isConfigurationSection("execute"))
            return;
        ConfigurationSection executeSection = eventConfig.getConfigurationSection("execute");
        Set<String> keys = executeSection.getKeys(false);
        for (String key : keys)
        {
            if (key.equalsIgnoreCase("PlayerCommandEvent") || key.equalsIgnoreCase("CommandExecEvent"))
            {
                Object val = executeSection.get(key);
                if (val instanceof List<?>) // Это список команд или вложенных
                                            // структур
                {
                    List<?> cmdList = (List<?>) val;
                    for (Object entry : cmdList)
                    {
                        if (entry instanceof String)
                        {
                            if (key.equalsIgnoreCase("PlayerCommandEvent"))
                                net.ayronix.luckyblocks.events.PlayerCommandEvent.handleAsChain(player, location,
                                        (String) entry, plugin);
                            else
                                net.ayronix.luckyblocks.events.CommandExecEvent.handleAsChain(player, location,
                                        (String) entry, plugin);
                        } else if (entry instanceof Map<?, ?>)
                        {
                            // Вложенный execute для цепочек
                            Map<?, ?> map = (Map<?, ?>) entry;
                            ConfigurationSection fake = new org.bukkit.configuration.MemoryConfiguration();
                            for (Map.Entry<?, ?> e : map.entrySet())
                            {
                                fake.set(e.getKey().toString(), e.getValue());
                            }
                            executeChained(player, location, fake, plugin);
                        }
                    }
                }
            }
        }
    }

}
