package net.ayronix.luckyblocks.events;

import net.ayronix.luckyblocks.LuckyBlockPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public interface ICustomEvent
{
    /**
     * Выполняет логику события.
     * 
     * @param player Игрок, сломавший лаки блок.
     * @param location Местоположение лаки блока.
     * @param eventConfig Секция конфигурации для этого конкретного события из
     * config.yml.
     * @param plugin Экземпляр главного класса плагина.
     */
    void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin);
}
