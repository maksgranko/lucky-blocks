package net.ayronix.luckyblocks.events;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

public class ExplosionEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        net.ayronix.luckyblocks.EventAnimationUtil.handleWithAnimationAndDelay(player, location, eventConfig, plugin,
                () ->
                {
                    // Теперь можно задать мощность взрыва через параметр
                    // "power" (float)
                    float power = (float) eventConfig.getDouble("power", 2.5F);
                    location.getWorld().createExplosion(location, power);

                    // Запуск дополнительных команд (execute: ...)
                    net.ayronix.luckyblocks.EventChainUtil.executeChained(player, location, eventConfig, plugin);
                });
    }
}
