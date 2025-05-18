package net.ayronix.luckyblocks;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ExplosionListener implements Listener
{
    private final LuckyBlockPlugin plugin;

    public ExplosionListener(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event)
    {
        if (plugin.getDebug())
            plugin.getLogger().info("[ExplosionListener] EntityExplodeEvent triggered. Entity: " + event.getEntityType()
                    + ", Location: " + event.getLocation());
        if (preventLuckyBlockExplosion(event.blockList(), event.getLocation()))
        {
            if (plugin.getDebug())
                plugin.getLogger().info("[ExplosionListener] Modified block list for EntityExplodeEvent.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event)
    {
        if (plugin.getDebug())
            plugin.getLogger().info("[ExplosionListener] BlockExplodeEvent triggered. Block: "
                    + event.getBlock().getType() + ", Location: " + event.getBlock().getLocation());
        if (preventLuckyBlockExplosion(event.blockList(), event.getBlock().getLocation()))
        {
            if (plugin.getDebug())
                plugin.getLogger().info("[ExplosionListener] Modified block list for BlockExplodeEvent.");
        }
    }

    /**
     * Итерирует по списку блоков, предназначенных для взрыва, и удаляет из него
     * лаки-блоки.
     * 
     * @param blockList Список блоков из события взрыва.
     * @param explosionOrigin Локация источника взрыва (для возможного
     * логирования или специфичной логики).
     * @return true, если список был изменен (хотя бы один лаки-блок был
     * удален).
     */
    private boolean preventLuckyBlockExplosion(List<Block> blockList, Location explosionOrigin)
    {
        boolean modified = false;
        int initialBlockCount = blockList.size();
        int removedLuckyBlocks = 0;

        Iterator<Block> iterator = blockList.iterator();
        while (iterator.hasNext())
        {
            Block block = iterator.next();
            Chunk chunk = block.getChunk();
            PersistentDataContainer pdc = chunk.getPersistentDataContainer();

            if (!pdc.has(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings()))
            {
                continue;
            }

            List<String> luckyBlockEntries = pdc.get(LuckyBlockPlugin.LUCKY_BLOCK_KEY,
                    PersistentDataType.LIST.strings());
            if (luckyBlockEntries == null || luckyBlockEntries.isEmpty())
            {
                continue;
            }

            for (String entry : luckyBlockEntries)
            {
                String[] parts = entry.split(",");
                if (parts.length >= 3)
                {
                    try
                    {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);

                        if (block.getX() == x && block.getY() == y && block.getZ() == z)
                        {
                            if (plugin.getDebug())
                                plugin.getLogger().info("[ExplosionListener] Lucky Block found at " + x + "," + y + ","
                                        + z + ". Preventing its explosion (Source: " + explosionOrigin + ").");
                            iterator.remove(); // Удаляем лаки-блок из списка
                                               // разрушаемых
                            modified = true;
                            removedLuckyBlocks++;
                            break;
                        }
                    } catch (NumberFormatException e)
                    {
                        plugin.getLogger().severe("[ExplosionListener] Error parsing lucky block entry: " + entry
                                + " - " + e.getMessage());
                    }
                }
            }
        }
        if (removedLuckyBlocks > 0)
        {
            if (plugin.getDebug())
                plugin.getLogger()
                        .info("[ExplosionListener] Prevented " + removedLuckyBlocks
                                + " lucky blocks from exploding. Initial list size: " + initialBlockCount + ", final: "
                                + blockList.size() + " (Source: " + explosionOrigin + ")");
        }
        return modified;
    }
}
