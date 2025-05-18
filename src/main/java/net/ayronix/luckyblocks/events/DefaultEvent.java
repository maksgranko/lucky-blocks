package net.ayronix.luckyblocks.events;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

public class DefaultEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        net.ayronix.luckyblocks.EventAnimationUtil.handleWithAnimationAndDelay(player, location, eventConfig, plugin,
                () ->
                {
                    // Это событие может быть настроено в config.yml для типа
                    // "DEFAULT" или
                    // использоваться,
                    // если другое событие не найдено или отключено.
                    // String message = eventConfig != null ?
                    // eventConfig.getString("message", "§bПусто...") :
                    // "§bПусто...";
                    player.sendMessage("§bПусто...");
                    // Запуск дополнительных команд (execute: ...)
                    net.ayronix.luckyblocks.EventChainUtil.executeChained(player, location, eventConfig, plugin);
                });
    }
}
