package net.ayronix.luckyblocks.events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SummonEntityEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection config,
            net.ayronix.luckyblocks.LuckyBlockPlugin plugin)
    {
        if (config == null)
            return;

        String entityType = config.getString("entity", "sheep");
        int count = config.getInt("count", 1);

        Location spawnLoc = location != null ? location : (player != null ? player.getLocation() : null);
        if (spawnLoc == null)
            return;

        String baseCommand = String.format("summon %s %f %f %f", entityType, spawnLoc.getX(), spawnLoc.getY(),
                spawnLoc.getZ());

        for (int i = 0; i < count; i++)
        {
            Bukkit.getScheduler().runTask(plugin, () ->
            {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), baseCommand);
            });
        }
    }
}
