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

        Material toolType = handItem.getType();

        // --- Смена уровня/типа ---
        if (toolType == Material.SOUL_TORCH
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
        {
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
        if (toolType == Material.REDSTONE_TORCH
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
        {
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

        // --- Смена типа (BLUE_WOOL) — листаем из конфига! ---
        if (toolType == Material.BLUE_WOOL
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
        {
            java.util.List<String> allTypes = new java.util.ArrayList<>(net.ayronix.luckyblocks.LuckyBlockPlugin
                    .getPlugin(net.ayronix.luckyblocks.LuckyBlockPlugin.class).getConfigManager().getAvailableTypes());
            if (allTypes.isEmpty())
            {
                player.sendActionBar("§cНет доступных типов лакиблоков!");
                event.setCancelled(true);
                return;
            }
            int cur = session.getTypeIndex();
            int next = (cur + 1) % allTypes.size();
            String typeName = allTypes.get(next);
            session.setType(typeName);
            session.setTypeIndex(next);
            player.sendActionBar("§aТип лакиблока: §b" + typeName);
            event.setCancelled(true);
            return;
        }

        // --- ВЫХОД из режима через Barrier (8-й слот) ЛКМ/ПКМ ---
        if (toolType == Material.BARRIER
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK
                        || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK))
        {
            PlacemodeManager.getInstance().disableFor(player);
            event.setCancelled(true);
            return;
        }

        // --- Blaze Rod: ПКМ — поставить лакиблок как в config, ЛКМ — убрать,
        // оба действия всегда обновляют worldBlocks ---
        if (toolType == PLACER_TOOL)
        {
            Location loc = null;
            if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)
                    && event.getClickedBlock() != null)
            {
                loc = event.getClickedBlock().getLocation().clone().add(event.getBlockFace().getDirection());
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR)
            {
                loc = player.getTargetBlock(null, 30).getLocation();
            }
            if (loc == null)
                return;

            String blockType = session.getType();
            int blockLevel = session.getLevel();

            java.util.List<BlockData> worldBlocks = PlacemodeManager.getInstance().getBlocksForWorld(player.getWorld());

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
            {
                // Установка лакиблока (и запись в worldBlocks, и физически в
                // мир)
                BlockData add = new BlockData(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), blockType, blockLevel);
                // Проверим, есть ли такой уже — не дублируем
                boolean already = worldBlocks.stream().anyMatch(b -> b.x == add.x && b.y == add.y && b.z == add.z
                        && b.type.equals(add.type) && b.level == add.level);
                if (!already)
                {
                    worldBlocks.add(add);
                    // Ставим блок-материал из конфига
                    org.bukkit.Material mat = org.bukkit.Material.GOLD_BLOCK;
                    org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LuckyBlocks");
                    if (plugin instanceof net.ayronix.luckyblocks.LuckyBlockPlugin lbp)
                    {
                        org.bukkit.Material m = lbp.getConfigManager().getLuckyBlockMaterial(blockType);
                        if (m != null)
                            mat = m;
                    }
                    player.getWorld().getBlockAt(loc).setType(mat);
                }
                player.sendActionBar("§aЛакиблок установлен: §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", "
                        + loc.getBlockZ() + " §bТип: §f" + blockType + " §bУровень: §e" + blockLevel);
                event.setCancelled(true);
                return;
            }

            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
            {
                // Поиск и удаление лакиблока по этим координатам
                BlockData match = null;
                for (BlockData b : new java.util.ArrayList<>(worldBlocks))
                {
                    if (b.x == loc.getBlockX() && b.y == loc.getBlockY() && b.z == loc.getBlockZ()
                            && b.type.equals(blockType) && b.level == blockLevel)
                    {
                        match = b;
                        break;
                    }
                }
                if (match != null)
                {
                    worldBlocks.remove(match);
                    player.getWorld().getBlockAt(loc).setType(org.bukkit.Material.AIR);
                    player.sendActionBar("§cЛакиблок удалён с точки: §e" + loc.getBlockX() + ", " + loc.getBlockY()
                            + ", " + loc.getBlockZ() + " §bТип: §f" + blockType + " §bУровень: §e" + blockLevel);
                } else
                {
                    player.sendActionBar("§cНет такого лакиблока для удаления.");
                }
                event.setCancelled(true);
                return;
            }
        }
    }
}
