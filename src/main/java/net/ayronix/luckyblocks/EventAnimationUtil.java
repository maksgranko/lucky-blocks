package net.ayronix.luckyblocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
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

        // Если animation не задана — сразу выполнять событие
        if (animation == null)
        {
            mainAction.run();
            return;
        }

        // --- Новый блок: реализация armor_stand-анимации ---
        // 1. Сгенерировать уникальное имя для armor_stand
        String armorStandName = "anim_" + java.util.UUID.randomUUID().toString().replace("-", "");

        // 2. Спавнить armor_stand через API с нужными параметрами
        org.bukkit.entity.ArmorStand stand = null;
        if (location != null && location.getWorld() != null)
        {
            // Спавним armor_stand строго в центре блока (X.5, Y, Z.5)
            Location spawnLocation = new Location(location.getWorld(), location.getBlockX() + 0.5, location.getBlockY(),
                    location.getBlockZ() + 0.5);
            stand = spawnLocation.getWorld().spawn(spawnLocation, org.bukkit.entity.ArmorStand.class, as ->
            {
                as.setInvisible(true);
                as.setInvulnerable(true);
                as.setGravity(false);
                as.setSilent(true);
                as.setVisualFire(false);
                as.setGlowing(false);
                as.setArms(true);
                as.setSmall(true);
                as.setCustomName(armorStandName);
                as.setCustomNameVisible(false);
                as.setMarker(false);
                as.setDisabledSlots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS, EquipmentSlot.FEET);
            });
        }

        // 3. Запустить функцию datapack с привязкой к armor_stand'у по имени
        // (через @n)
        String functionCmd = "execute as @n[type=minecraft:armor_stand,name=" + armorStandName
                + "] at @s run function luckyblocks:animations/" + animation;
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), functionCmd);
        });

        // 4. Длительность анимации = delaySec (если не задано — дефолт 2.0 сек)
        long delayTicks = (long) Math.ceil((delaySec > 0.0 ? delaySec : 2.0) * 20.0);

        // 5. После задержки: удалить armor_stand и выполнить основное действие
        org.bukkit.entity.ArmorStand armorStandForRemove = stand;
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (armorStandForRemove != null && !armorStandForRemove.isDead())
                {
                    armorStandForRemove.remove();
                }
                mainAction.run();
            }
        }.runTaskLater(plugin, delayTicks);
    }
}
