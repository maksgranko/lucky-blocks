package net.ayronix.luckyblocks.events;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.ayronix.luckyblocks.LuckyBlockPlugin;
import net.ayronix.luckyblocks.EventChainUtil;

public class HealEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        net.ayronix.luckyblocks.EventAnimationUtil.handleWithAnimationAndDelay(player, location, eventConfig, plugin,
                () ->
                {
                    AttributeInstance max_health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (max_health != null)
                    {
                        double max = max_health.getValue();
                        double healPercent = 100.0;
                        if (eventConfig != null && eventConfig.contains("heal_percent"))
                        {
                            healPercent = eventConfig.getDouble("heal_percent", 100.0);
                        }
                        healPercent = Math.max(0.0, Math.min(healPercent, 100.0));
                        double healAmount = max * healPercent / 100.0;
                        double newHealth = Math.min(player.getHealth() + healAmount, max);
                        player.setHealth(newHealth);
                        if (healPercent >= 99.99)
                        {
                            player.sendMessage("§cВы полностью исцелены!");
                        } else
                        {
                            player.sendMessage("§cВы исцелены на " + (int) healPercent + "% здоровья!");
                        }
                    }
                    // Запуск дополнительных команд (execute: ...)
                    net.ayronix.luckyblocks.EventChainUtil.executeChained(player, location, eventConfig, plugin);
                });
    }
}
