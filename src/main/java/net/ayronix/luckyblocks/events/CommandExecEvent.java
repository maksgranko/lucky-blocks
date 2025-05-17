package net.ayronix.luckyblocks.events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

public class CommandExecEvent implements ICustomEvent
{
    /**
     * Статическая обработка одиночной команды — от сервера, с подстановкой
     * %player%
     */
    public static void handleAsChain(Player player, Location location, String command, LuckyBlockPlugin plugin)
    {
        String cmd = command.replace("%player%", player.getName()).replace("@p", player.getName())
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ())).replace("~", String.valueOf(location.getBlockX()))
                .replace("~~", String.valueOf(location.getBlockY()))
                .replace("~~~", String.valueOf(location.getBlockZ()));
        if (cmd.startsWith("/"))
            cmd = cmd.substring(1);
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
    }

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
