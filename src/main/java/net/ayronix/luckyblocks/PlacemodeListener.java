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
    // Anti-double-click throttle: Храним последнюю координату установки для
    // каждого игрока (timestamp)
    private final java.util.Map<java.util.UUID, String> recentPlacements = new java.util.HashMap<>();
    private final long blockPlaceLimitMs = 250L; // защита от даблклика 0.25с
    private final java.util.Map<java.util.UUID, Long> lastPlacementTime = new java.util.HashMap<>();

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

            // Показать в offhand материал лакиблока для текущего типа
            org.bukkit.Material mat = org.bukkit.Material.GOLD_BLOCK;
            org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LuckyBlocks");
            if (plugin instanceof net.ayronix.luckyblocks.LuckyBlockPlugin lbp)
            {
                org.bukkit.Material m = lbp.getConfigManager().getLuckyBlockMaterial(typeName);
                if (m != null)
                    mat = m;
            }
            player.getInventory().setItemInOffHand(new org.bukkit.inventory.ItemStack(mat));

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
                // Новая логика позиционирования:
                // Если клик по блоку: если сверху блок SNOW, заменяем его. В
                // остальных случаях — ищем сверху первый AIR, но если клик
                // сбоку, ставим по нормали, на блоке (не заменяем сам блок!)
                var baseLoc = event.getClickedBlock().getLocation();
                var face = event.getBlockFace();
                var targetBlock = event.getClickedBlock().getRelative(face);

                // Если это верх и это SNOW — заменяем, иначе ищем AIR выше
                boolean isSnowLayer = targetBlock.getType() == org.bukkit.Material.SNOW;
                if (isSnowLayer)
                {
                    loc = targetBlock.getLocation();
                } else
                {
                    // Всегда "ставим" на соседний по нормали блок, если он не
                    // занят (AIR или SNOW — заменить SNOW)
                    if (targetBlock.getType() == org.bukkit.Material.AIR
                            || targetBlock.getType() == org.bukkit.Material.SNOW)
                    {
                        loc = targetBlock.getLocation();
                    } else
                    {
                        // Если все занято — ничего не делать
                        player.sendActionBar("§cНет свободного пространства для установки лакиблока!");
                        event.setCancelled(true);
                        return;
                    }
                }
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR)
            {
                org.bukkit.block.Block rayBlock = player.getTargetBlock(null, 30);
                if (rayBlock == null || rayBlock.getType() == Material.AIR)
                {
                    player.sendActionBar("§cПоверхность слишком далеко для установки лакиблока!");
                    event.setCancelled(true);
                    return;
                }
                loc = rayBlock.getLocation();
            }
            if (loc == null)
                return;

            String blockType = session.getType();
            int blockLevel = session.getLevel();

            java.util.List<BlockData> worldBlocks = PlacemodeManager.getInstance().getBlocksForWorld(player.getWorld());

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
            {
                // Throttle: если координата совпадает с прошлым действием
                // игрока, и прошло < blockPlaceLimitMs — игнорируем
                String coordKey = loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":"
                        + loc.getBlockZ() + ":" + blockType + ":" + blockLevel;
                long now = System.currentTimeMillis();
                boolean recentlyClicked = coordKey.equals(recentPlacements.get(player.getUniqueId()))
                        && now - lastPlacementTime.getOrDefault(player.getUniqueId(), 0L) < blockPlaceLimitMs;

                if (recentlyClicked)
                {
                    event.setCancelled(true);
                    return;
                }
                recentPlacements.put(player.getUniqueId(), coordKey);
                lastPlacementTime.put(player.getUniqueId(), now);

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
