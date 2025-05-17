package net.ayronix.luckyblocks.events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.ayronix.luckyblocks.LuckyBlockPlugin;
import static net.ayronix.luckyblocks.Replacer.apply;

public class CommandExecEvent implements ICustomEvent
{
    /**
     * Статическая обработка одиночной команды — от сервера, с подстановкой
     * %player%
     */
    public static void handleAsChain(Player player, Location location, String command, LuckyBlockPlugin plugin)
    {
        String cmd = apply(command, player, location);
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
        cmd = apply(cmd, player, loc);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
