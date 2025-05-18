package net.ayronix.luckyblocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Утилита для обработки animation + delay для событий лакиблока.
 */
public class EventAnimationUtil
{

    /**
     * Проверяет наличие animation и delay в конфиге. Если заданы, сначала
     * проигрывает анимацию, затем с задержкой вызывает основной runnable. Если
     * поля отсутствуют — сразу вызывает действие.
     *
     * @param player игрок, для которого проигрывать анимацию (optional, может
     * быть null для console)
     * @param location локация события (не используется для animation)
     * @param config конфиг эвента
     * @param plugin инстанс плагина (для scheduler и dispatchCommand)
     * @param mainAction действие, которое будет выполнено после
     * анимации/задержки
     */
    public static void handleWithAnimationAndDelay(Player player, Location location, ConfigurationSection config,
            Plugin plugin, Runnable mainAction)
    {
        // Проверяем наличие полей в конфиге
        String animation = null;
        double delaySec = 0.0;

        if (config != null)
        {
            if (config.contains("animation"))
            {
                animation = config.getString("animation");
                if (animation != null && animation.trim().isEmpty())
                    animation = null;
            }
            if (config.contains("delay"))
            {
                // могут передать int или double
                try
                {
                    delaySec = config.getDouble("delay");
                } catch (Exception ignore)
                {
                    delaySec = 0;
                }
            }
        }

        // Если animation не задана ИЛИ delay <= 0 — сразу выполнять событие
        if ((animation == null && delaySec <= 0) || (delaySec <= 0))
        {
            mainAction.run();
            return;
        }

        // Если есть animation, выполняем ее через /function
        // LuckyBlocks:animations/<animation>
        if (animation != null)
        {
            String command = "function LuckyBlocks:animations/" + animation;
            // Выполняем через консоль
            Bukkit.getScheduler().runTask(plugin, () ->
            {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
        }

        // Выполняем основное действие с задержкой delay сек, если delay > 0
        long delayTicks = (long) Math.ceil(delaySec * 20.0);
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                mainAction.run();
            }
        }.runTaskLater(plugin, delayTicks);
    }
}
