package net.ayronix.luckyblocks;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ExplosionListener implements Listener
{

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event)
    {
        Chunk chunk = null;
        List<String> coords = null;
        PersistentDataContainer pdc;

        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext())
        {
            Block block = it.next();
            if (chunk == null || !block.getChunk().equals(chunk))
            {
                chunk = block.getChunk();
                pdc = chunk.getPersistentDataContainer();
                coords = pdc.get(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings());
            }
            if (coords != null)
            {
                String coord = block.getX() + "," + block.getY() + "," + block.getZ();
                if (coords.contains(coord))
                {
                    it.remove();
                }
            }
        }
    }

}
