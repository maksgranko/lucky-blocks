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
            var loc = location.toCenterLocation();
            var world = location.getWorld();
            String found = null;
            for (var ent : world.getNearbyEntities(loc, 2, 2, 2))
            {
                if (!(ent instanceof org.bukkit.entity.ArmorStand))
                    continue;
                var as = (org.bukkit.entity.ArmorStand) ent;
                String name = as.getCustomName();
                if (name != null && name.length() == 12 && name.chars().allMatch(Character::isLetterOrDigit))
                {
                    found = name;
                    break;
                }
            }
            if (found != null)
            {
                standName = found;
            } else
            {
                standName = "anim_" + java.util.UUID.randomUUID().toString().replace("-", "");
                Location spawnLocation = new Location(location.getWorld(), location.getBlockX() + 0.5,
                        location.getBlockY(), location.getBlockZ() + 0.5);
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
                    as.setCustomName(standName);
                    as.setCustomNameVisible(false);
                    as.setMarker(false);
                    as.setDisabledSlots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD,
                            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
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
