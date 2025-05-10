package net.ayronix.luckyblocks.events;

import net.ayronix.luckyblocks.LuckyBlockPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ExplosionEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        // В будущем можно добавить параметры из eventConfig, например, силу
        // взрыва
        // float power = (float) eventConfig.getDouble("power", 2.5F);
        // boolean setFire = eventConfig.getBoolean("setFire", false);
        // location.getWorld().createExplosion(location, power, setFire);
        location.getWorld().createExplosion(location, 2.5F);
    }
}
