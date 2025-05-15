package net.ayronix.luckyblocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class LuckyBlockTabCompleter implements TabCompleter
{

    private final LuckyBlockPlugin plugin;

    public LuckyBlockTabCompleter(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        if (!command.getName().equalsIgnoreCase("luckyblock"))
            return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1)
        {
            completions.add("placemode");
            completions.add("list");
            completions.add("reload");
            completions.add("add");
            completions.add("help");
            // Типы лакиблоков
            completions.addAll(plugin.getConfigManager().getAvailableTypes());
            return completions;
        }

        if (args.length == 2)
        {
            if (args[0].equalsIgnoreCase("placemode"))
            {
                completions.add("on");
                completions.add("off");
                completions.add("init");
                completions.add("save");
            } else if (args[0].equalsIgnoreCase("add"))
            {
                completions.add("chest-table");
                completions.add("lucky-table");
            } else
            {
                // Вторая позиция после типа лакиблока — возможные уровни
                String type = args[0].toLowerCase();
                Set<String> availableTypes = plugin.getConfigManager().getAvailableTypes();
                if (availableTypes.contains(type))
                {
                    int min = plugin.getConfigManager().getMinLevel(type);
                    int max = plugin.getConfigManager().getMaxLevel(type);
                    for (int level = min; level <= max; ++level)
                    {
                        completions.add(String.valueOf(level));
                    }
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("chest-table"))
        {
            // chest-table <type> <tableName> — подсказка по типам
            completions.addAll(plugin.getConfigManager().getAvailableTypes());
        }

        return completions;
    }
}
