package net.ayronix.luckyblocks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlacemodeListener implements Listener
{

    // Основной инструмент для размещения лакиблоков (пока blaze rod)
    private static final Material PLACER_TOOL = Material.BLAZE_ROD;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();

        // Не в placemode — не реагируем
        if (!PlacemodeManager.getInstance().isActive(player))
            return;

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null)
            return;

        PlacemodeSession session = PlacemodeManager.getInstance().getSession(player);
        if (session == null)
            return;

        Material type = handItem.getType();

        // --- Смена уровня/типа ---
        if (type == Material.SOUL_TORCH
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
        {
            // Уровень вниз
            int lvl = session.getLevel();
            int min = session.getMinLevel();
            if (lvl > min)
            {
                session.setLevel(lvl - 1);
                player.setLevel(lvl - 1);
                player.sendActionBar("§bУровень лакиблока: §e" + (lvl - 1));
            } else
            {
                player.sendActionBar("§cМинимальный уровень (" + min + ")");
            }
            event.setCancelled(true);
            return;
        }
        if (type == Material.REDSTONE_TORCH
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
        {
            // Уровень вверх
            int lvl = session.getLevel();
            int max = session.getMaxLevel();
            if (lvl < max)
            {
                session.setLevel(lvl + 1);
                player.setLevel(lvl + 1);
                player.sendActionBar("§bУровень лакиблока: §e" + (lvl + 1));
            } else
            {
                player.sendActionBar("§cМаксимальный уровень (" + max + ")");
            }
            event.setCancelled(true);
            return;
        }
        if (type == Material.BLUE_WOOL
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
        {
            // Прототип: просто тип classic/super — в реальной версии перебор
            // типов лакиблоков из конфига
            String prevType = session.getType();
            String newType = prevType.equals("classic") ? "super" : "classic";
            session.setType(newType);
            player.sendActionBar("§aТип лакиблока: §b" + newType);
            event.setCancelled(true);
            return;
        }

        // --- Основной инструмент размещения ---
        if (type == PLACER_TOOL
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
        {
            // Куда ставить блок? Если клик по блоку — берем соседнюю позицию по
            // нормали. Если по воздуху — raytrace до 30 блоков
            Location loc = null;
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null)
            {
                loc = event.getClickedBlock().getLocation().clone().add(event.getBlockFace().getDirection());
            } else
            {
                // Raytrace по направлению взгляда
                loc = player.getTargetBlock(null, 30).getLocation();
            }
            if (loc == null)
                return;

            // Добавить в кэш — записать в PlacemodeManager
            String blockType = session.getType();
            int blockLevel = session.getLevel();
            PlacemodeManager.getInstance().addBlock(loc.getWorld(),
                    new BlockData(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), blockType, blockLevel));
            player.sendActionBar("§aЛакиблок сохраняется по координатам: §e" + loc.getBlockX() + ", " + loc.getBlockY()
                    + ", " + loc.getBlockZ() + " §bТип: §f" + blockType + " §bУровень: §e" + blockLevel);
            // Не ставим физически сам блок — только запись точки!
            event.setCancelled(true);
        }
    }
}
