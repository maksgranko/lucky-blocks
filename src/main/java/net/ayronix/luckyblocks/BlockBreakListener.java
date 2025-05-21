package net.ayronix.luckyblocks;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.ayronix.luckyblocks.events.ICustomEvent;

public class BlockBreakListener implements Listener
{

    private final LuckyBlockPlugin plugin;

    public BlockBreakListener(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler @SuppressWarnings("LoggerStringConcat")
    public void onBlockBreak(BlockBreakEvent event)
    {
        Block block = event.getBlock();

        // --- стандартная логика лакиблоков ---
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        List<String> existing = pdc.get(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings());
        if (existing == null)
        {
            return;
        }

        String coord = null;
        String type = null;
        int level = 0;
        for (String entry : existing)
        {
            String[] parts = entry.split(",");
            if (parts.length >= 3 && Integer.parseInt(parts[0]) == block.getX()
                    && Integer.parseInt(parts[1]) == block.getY() && Integer.parseInt(parts[2]) == block.getZ())
            {
                coord = entry;
                type = parts[3];
                level = Integer.parseInt(parts[4]);
                break;
            }
        }

        if (coord == null)
        {
            return;
        }
        // --- Удаление armor_stand при ломке лакиблока ---
        try
        {
            String[] parts = coord.split(",");
            if (parts.length >= 6)
            {
                String armorStandName = parts[5];
                var loc = block.getLocation().toCenterLocation();
                var world = block.getWorld();
                var stands = world.getNearbyEntities(loc, 2, 2, 2, ent -> ent instanceof org.bukkit.entity.ArmorStand
                        && armorStandName.equals(ent.getCustomName()));
                for (var ent : stands)
                    ent.remove();
            }
        } catch (Exception ignore)
        {
        }

        event.setDropItems(false);
        event.setExpToDrop(0);
        block.setType(Material.AIR);

        Player player = event.getPlayer();
        ConfigManager configManager = plugin.getConfigManager();
        ConfigurationSection eventSection = ProbabilityCalculator.getRandomEvent(configManager, type, level);

        // Удаляем координату лакиблока из PDC, если НАГРАДА НЕ сундук
        // (PLACE_CHEST)
        // и обработчик не вызовет удаление самостоятельно
        boolean isChestReward = false;
        if (eventSection != null)
        {
            String baseKey = eventSection.getString("__event_key__", "");
            if (!baseKey.isEmpty())
                baseKey = baseKey.toUpperCase();
            isChestReward = baseKey.equals("PLACE_CHEST");
        }
        if (!isChestReward)
        {
            List<String> coords = new java.util.ArrayList<>(existing);
            coords.remove(coord);
            pdc.set(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings(), coords);
            if (plugin.getDebug())
                plugin.getLogger().info("[LuckyBlocks] LuckyBlock удалён из чанка по координате: " + coord);
        }

        if (eventSection != null)
        {
            String baseKey = eventSection.getString("__event_key__", "");
            if (!baseKey.isEmpty())
                baseKey = baseKey.toUpperCase();
            ICustomEvent eventHandler = plugin.getEventRegistry().get(baseKey);
            if (eventHandler != null)
            {
                if (eventSection.getBoolean("enabled", true))
                {
                    // Для PLACE_CHEST и других ивентов — подмешиваем
                    // _luckyblock_type/_level
                    org.bukkit.configuration.MemoryConfiguration enhancedConfig = new org.bukkit.configuration.MemoryConfiguration();
                    for (String key : eventSection.getKeys(false))
                        enhancedConfig.set(key, eventSection.get(key));
                    enhancedConfig.set("_luckyblock_type", type);
                    enhancedConfig.set("_luckyblock_level", level);
                    net.ayronix.luckyblocks.LuckyBlockReplaceManager.allow(block.getLocation());
                    eventHandler.execute(player, block.getLocation(), enhancedConfig, plugin);
                } else
                {
                    net.ayronix.luckyblocks.LuckyBlockReplaceManager.allow(block.getLocation());
                    plugin.getEventRegistry().get("DEFAULT").execute(player, block.getLocation(), null, plugin);
                }
            } else
            {
                plugin.getLogger().warning(
                        "Не найден обработчик для события: " + baseKey + " для типа " + type + " и уровня " + level);
                plugin.getEventRegistry().get("DEFAULT").execute(player, block.getLocation(), null, plugin);
            }
        } else
        {
            plugin.getEventRegistry().get("DEFAULT").execute(player, block.getLocation(), null, plugin);
        }
    }
}
