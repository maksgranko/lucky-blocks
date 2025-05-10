package net.ayronix.luckyblocks.events;

import net.ayronix.luckyblocks.LuckyBlockPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class DefaultEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        // Это событие может быть настроено в config.yml для типа "DEFAULT" или
        // использоваться,
        // если другое событие не найдено или отключено.
        // String message = eventConfig != null ?
        // eventConfig.getString("message", "§bПусто...") : "§bПусто...";
        player.sendMessage("§bПусто...");
    }
}
