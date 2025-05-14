package net.ayronix.luckyblocks;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BlockProtectionListener implements Listener
{

    private final LuckyBlockPlugin plugin;

    public BlockProtectionListener(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
    }

    private boolean isProtectedLuckyBlock(Block block)
    {
        if (block == null)
            return false;
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        if (!pdc.has(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings()))
        {
            return false;
        }
        List<String> entries = pdc.get(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings());
        if (entries == null || entries.isEmpty())
        {
            return false;
        }

        for (String entry : entries)
        {
            String[] parts = entry.split(",");
            if (parts.length >= 3)
            { // x,y,z,type,level
                try
                {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    if (block.getX() == x && block.getY() == y && block.getZ() == z)
                    {
                        if (LuckyBlockPlugin.debug)
                            plugin.getLogger().info("[BlockProtection] Protected lucky block at " + x + "," + y + ","
                                    + z + " from an event.");
                        return true;
                    }
                } catch (NumberFormatException e)
                {
                    plugin.getLogger().warning("[BlockProtection] Error parsing PDC entry: " + entry + " in chunk "
                            + chunk.getX() + "," + chunk.getZ());
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event)
    {
        for (Block block : event.getBlocks())
        {
            if (isProtectedLuckyBlock(block))
            {
                event.setCancelled(true);
                if (LuckyBlockPlugin.debug)
                    plugin.getLogger().info("[BlockProtection] Prevented PistonExtend from moving Lucky Block at "
                            + block.getLocation());
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event)
    {
        for (Block block : event.getBlocks())
        {
            if (isProtectedLuckyBlock(block))
            {
                event.setCancelled(true);
                if (LuckyBlockPlugin.debug)
                    plugin.getLogger().info("[BlockProtection] Prevented PistonRetract from moving Lucky Block at "
                            + block.getLocation());
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event)
    {
        if (isProtectedLuckyBlock(event.getBlock()))
        {
            event.setCancelled(true);
            if (LuckyBlockPlugin.debug)
                plugin.getLogger()
                        .info("[BlockProtection] Prevented Burn for Lucky Block at " + event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event)
    {
        if (isProtectedLuckyBlock(event.getBlock()))
        {
            event.setCancelled(true);
            if (LuckyBlockPlugin.debug)
                plugin.getLogger()
                        .info("[BlockProtection] Prevented Fade for Lucky Block at " + event.getBlock().getLocation());
        }
    }

    // BlockFormEvent - когда блок образуется (например, обсидиан). Если
    // лакиблок был водой/лавой.
    // В этом случае мы проверяем event.getNewState().getBlock() - но это не
    // совсем то.
    // Скорее, если лакиблок - это источник (вода/лава), и он должен был
    // превратиться.
    // Этот случай менее вероятен, если лакиблоки не из жидкостей. Пропустим
    // пока для простоты.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event)
    { // Например, трава на землю, мицелий
        if (event.getSource().getType().isSolid() && isProtectedLuckyBlock(event.getBlock()))
        { // Если источник - твердый блок (например, трава), и он пытается
          // распространиться на лакиблок
            event.setCancelled(true);
            if (LuckyBlockPlugin.debug)
                plugin.getLogger().info("[BlockProtection] Prevented Spread onto Lucky Block at "
                        + event.getBlock().getLocation() + " from " + event.getSource().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event)
    { // Движение жидкостей
        if (isProtectedLuckyBlock(event.getToBlock()))
        { // Если жидкость течет НА место лакиблока
            event.setCancelled(true);
            if (LuckyBlockPlugin.debug)
                plugin.getLogger().info("[BlockProtection] Prevented Liquid flow onto Lucky Block at "
                        + event.getToBlock().getLocation());
        }
        // Также, если сам лакиблок является жидкостью и пытается утечь (менее
        // вероятно)
        // if (isProtectedLuckyBlock(event.getBlock())) {
        // event.setCancelled(true);
        // }
    }

    // BlockPhysicsEvent - очень частое событие. Может сильно нагрузить сервер.
    // Вместо этого, лучше запретить создание лаки-блоков из "падающих"
    // материалов (песок, гравий)
    // или смириться с тем, что они могут упасть, но не разрушатся от падения
    // (если не упадут в пустоту).
    // Если они упадут на другой блок, они останутся лаки-блоками.
    // @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    // public void onBlockPhysics(BlockPhysicsEvent event) {
    // if (isProtectedLuckyBlock(event.getBlock())) {
    // // Дополнительно проверить, действительно ли изменение приведет к
    // разрушению
    // // или это просто обновление состояния.
    // // Это сложная логика, так как BlockPhysicsEvent очень общий.
    // // event.setCancelled(true); // Может сломать много чего
    // }
    // }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event)
    {
        if (isProtectedLuckyBlock(event.getBlock()))
        {
            event.setCancelled(true);
            if (LuckyBlockPlugin.debug)
                plugin.getLogger().info(
                        "[BlockProtection] Prevented LeavesDecay for Lucky Block at " + event.getBlock().getLocation());
        }
    }
}
