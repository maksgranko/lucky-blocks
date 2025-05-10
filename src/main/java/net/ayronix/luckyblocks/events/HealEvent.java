package net.ayronix.luckyblocks.events;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class HealEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        // Можно добавить параметр для количества исцеления или эффектов из
        // eventConfig
        // double healAmount = eventConfig.getDouble("amount",
        // player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        // player.setHealth(Math.min(player.getHealth() + healAmount,
        // player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
        AttributeInstance max_health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (max_health != null)
        {
            player.setHealth(max_health.getValue());
        }
        player.sendMessage("§cВы исцелены!");
    }
}
