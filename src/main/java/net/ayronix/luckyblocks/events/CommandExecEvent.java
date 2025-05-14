package net.ayronix.luckyblocks.events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

public class CommandExecEvent implements ICustomEvent
{
    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        // Поддержка как одиночной команды, так и списка
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

    // Подстановка координат, ника, etc
    private void dispatchWithVars(String cmd, Player player, Location loc)
    {
        cmd = cmd.replace("%player%", player.getName()).replace("@p", player.getName())
                .replace("%x%", String.valueOf(loc.getBlockX())).replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(loc.getBlockZ())).replace("~", String.valueOf(loc.getBlockX()))
                .replace("~~", String.valueOf(loc.getBlockY())).replace("~~~", String.valueOf(loc.getBlockZ()));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
