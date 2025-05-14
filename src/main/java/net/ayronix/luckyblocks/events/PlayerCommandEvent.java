package net.ayronix.luckyblocks.events;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

public class PlayerCommandEvent implements ICustomEvent
{
    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        if (eventConfig.contains("command"))
        {
            if (eventConfig.isList("command"))
            {
                for (String cmd : eventConfig.getStringList("command"))
                {
                    dispatchWithVars(cmd, player, location);
                }
            } else
            {
                String cmd = eventConfig.getString("command");
                if (cmd != null && !cmd.isEmpty())
                    dispatchWithVars(cmd, player, location);
            }
        }
    }

    private void dispatchWithVars(String cmd, Player player, Location loc)
    {
        cmd = cmd.replace("%player%", player.getName()).replace("@p", player.getName())
                .replace("%x%", String.valueOf(loc.getBlockX())).replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(loc.getBlockZ())).replace("~", String.valueOf(loc.getBlockX()))
                .replace("~~", String.valueOf(loc.getBlockY())).replace("~~~", String.valueOf(loc.getBlockZ()));
        // slash (/) не нужен, player.performCommand требует его без /
        if (cmd.startsWith("/"))
            cmd = cmd.substring(1);
        player.performCommand(cmd);
    }
}
