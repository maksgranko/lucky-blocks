package net.ayronix.luckyblocks.events;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

public class GiveEffectEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection config, LuckyBlockPlugin plugin)
    {
        if (player == null || config == null)
            return;

        net.ayronix.luckyblocks.EventAnimationUtil.handleWithAnimationAndDelay(player, location, config, plugin, () ->
        {
            String effectName = config.getString("effect", "LUCK");
            int amplifier = config.getInt("amplifier", 1);
            int duration = config.getInt("duration", 100);

            try
            {
                org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType
                        .getByName(effectName.toUpperCase());
                if (type != null)
                {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(type, duration, amplifier));
                    if (plugin.getDebug())
                        plugin.getLogger().info("[GiveEffectEvent] Выдан эффект " + effectName + " x" + amplifier + " ("
                                + duration + " тиков) игроку " + player.getName());
                } else if (plugin.getDebug())
                {
                    plugin.getLogger().warning("[GiveEffectEvent] Неизвестный эффект: " + effectName);
                }
            } catch (Exception e)
            {
                if (plugin.getDebug())
                {
                    plugin.getLogger().warning(
                            "[GiveEffectEvent] Ошибка применения эффекта " + effectName + ": " + e.getMessage());
                }
            }

            // Универсальная поддержка команд через executeChained:
            net.ayronix.luckyblocks.EventChainUtil.executeChained(player, location, config, plugin);
        });
    }
}
