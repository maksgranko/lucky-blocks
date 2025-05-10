package net.ayronix.luckyblocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BlockPlaceListener implements Listener
{
    private final LuckyBlockPlugin plugin;

    public BlockPlaceListener(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
        var item = event.getItemInHand();
        if (item.getItemMeta() == null)
        {
            return;
        }
        var pdcItem = item.getItemMeta().getPersistentDataContainer();
        Byte flag = pdcItem.get(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.BYTE);
        if (flag == null || flag != (byte) 1)
        {
            return;
        }

        Block block = event.getBlockPlaced();

        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        List<String> existing = pdc.get(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings());
        List<String> coords = (existing != null) ? new ArrayList<>(existing) : new ArrayList<>();

        String type = pdcItem.get(LuckyBlockPlugin.LUCKY_BLOCK_TYPE_KEY, PersistentDataType.STRING);
        Integer levelObj = pdcItem.get(LuckyBlockPlugin.LUCKY_BLOCK_LEVEL_KEY, PersistentDataType.INTEGER);
        if (type == null || levelObj == null)
        {
            return;
        }
        int level = levelObj;

        String coord = block.getX() + "," + block.getY() + "," + block.getZ() + "," + type + "," + level;
        coords.add(coord);
        pdc.set(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings(), coords);

        event.getPlayer().sendMessage("§aLucky Block установлен в чанке!");
    }

}
