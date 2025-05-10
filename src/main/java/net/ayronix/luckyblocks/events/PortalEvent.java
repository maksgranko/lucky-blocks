package net.ayronix.luckyblocks.events;

import net.ayronix.luckyblocks.LuckyBlockPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class PortalEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        // Логика создания портала или телепортации может быть добавлена здесь.
        // Параметры, такие как тип портала или место назначения, могут быть
        // взяты из eventConfig.
        player.sendMessage("§5Активирован портал!"); // Сообщение тоже можно
                                                     // будет настраивать
    }
}
