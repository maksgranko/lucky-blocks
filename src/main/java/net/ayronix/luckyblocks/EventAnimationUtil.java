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
        // 1. Используем armor_stand, который уже стоит у лаки-блока
        // Ищем armor_stand один раз, имя сохраняем, далее не меняем
        final String standName;
        org.bukkit.entity.ArmorStand stand = null;
        if (location != null && location.getWorld() != null)
        {
            var world = location.getWorld();
            org.bukkit.entity.ArmorStand foundStand = null;
            int bx = location.getBlockX();
            int by = location.getBlockY();
            int bz = location.getBlockZ();
            // ищем armorstand строго по координате и PDC
            for (var as : world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class))
            {
                var pdc = as.getPersistentDataContainer();
                Byte tag = pdc.get(net.ayronix.luckyblocks.LuckyBlockPlugin.LUCKY_BLOCK_KEY,
                        org.bukkit.persistence.PersistentDataType.BYTE);
                if (tag != null && tag == (byte) 1)
                {
                    var pos = as.getLocation();
                    if (pos.getBlockX() == bx && pos.getBlockY() == by && pos.getBlockZ() == bz)
                    {
                        foundStand = as;
                        break;
                    }
                }
            }
            if (foundStand != null)
            {
                standName = foundStand.getCustomName();
            } else
            {
                standName = "anim_" + java.util.UUID.randomUUID().toString().replace("-", "");
                Location spawnLocation = new Location(location.getWorld(), location.getBlockX() + 0.5,
                        location.getBlockY(), location.getBlockZ() + 0.5);
                stand = spawnLocation.getWorld().spawn(spawnLocation, org.bukkit.entity.ArmorStand.class, as ->
                {
                    setupArmorStand(as, standName, true);
                });
            }
        } else
        {
            standName = "anim_" + java.util.UUID.randomUUID().toString().replace("-", "");
        }

        // 3. Запустить функцию datapack с привязкой к armor_stand'у по имени
        // (через @n)
        String functionCmd = "execute as @n[type=minecraft:armor_stand,name=" + standName
                + "] at @s run function luckyblocks:animations/" + animation;
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), functionCmd);
        });

        long delayTicks = (long) Math.ceil((delaySec > 0.0 ? delaySec : 2.0) * 20.0);

        // 5. После задержки удалить armor_stand по заранее известному имени
        // standName
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (location != null && location.getWorld() != null && standName != null)
                {
                    var world = location.getWorld();
                    boolean removed = false;
                    for (var ent : world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class))
                    {
                        if (ent.getCustomName() != null && ent.getCustomName().equals(standName))
                        {
                            ent.remove();
                            removed = true;
                        }
                    }
                    if (plugin.getLogger() != null && !removed)
                    {
                        plugin.getLogger()
                                .info("[LuckyBlocks] ArmorStand for animation not removed: name=" + standName);
                    }
                }
                mainAction.run();
            }
        }.runTaskLater(plugin, delayTicks);
    }

    /**
     * Единая настройка параметров armorstand для лаки-блока. Все параметры
     * фиксированные кроме 'small'. Разворот головы всегда (0,0,0).
     */
    public static void setupArmorStand(org.bukkit.entity.ArmorStand as, String standName, boolean small)
    {
        as.setInvisible(true);
        as.setInvulnerable(true);
        as.setGravity(false);
        as.setSilent(true);
        as.setVisualFire(false);
        as.setGlowing(false);
        as.setArms(false);
        as.setSmall(small);
        as.setCustomName(standName);
        as.setCustomNameVisible(false);
        as.setMarker(true);
        as.setDisabledSlots(org.bukkit.inventory.EquipmentSlot.HAND, org.bukkit.inventory.EquipmentSlot.OFF_HAND,
                org.bukkit.inventory.EquipmentSlot.HEAD, org.bukkit.inventory.EquipmentSlot.CHEST,
                org.bukkit.inventory.EquipmentSlot.LEGS, org.bukkit.inventory.EquipmentSlot.FEET);
        as.setHeadPose(new org.bukkit.util.EulerAngle(0.0, 0.0, 0.0));
    }
}
