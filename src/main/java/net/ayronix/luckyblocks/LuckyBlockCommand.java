package net.ayronix.luckyblocks;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

public class LuckyBlockCommand implements CommandExecutor
{

    private final ConfigManager configManager;

    public LuckyBlockCommand(LuckyBlockPlugin plugin)
    {
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!sender.hasPermission("luckyblocks.use"))
        {
            sender.sendMessage(ChatColor.RED + "Нет прав для /" + label);
            return true;
        }
        if (!(sender instanceof Player player))
        {
            sender.sendMessage(ChatColor.RED + "Только игрок может выполнить команду.");
            return true;
        }
        if (args.length < 2)
        {
            player.sendMessage(ChatColor.YELLOW + "Использование: /" + label + " <type> <level> [count]");
            return true;
        }

        String type = args[0].toLowerCase();
        Set<String> availableTypes = configManager.getAvailableTypes();
        if (!availableTypes.contains(type))
        {
            player.sendMessage(ChatColor.RED + "Тип не найден: " + type);
            if (!availableTypes.isEmpty())
            {
                player.sendMessage(ChatColor.YELLOW + "Доступные типы: " + String.join(", ", availableTypes));
            }
            return true;
        }

        int level;
        int count = 1;
        try
        {
            level = Integer.parseInt(args[1]);
            if (args.length >= 3)
            {
                count = Integer.parseInt(args[2]);
                if (count <= 0)
                {
                    player.sendMessage(ChatColor.RED + "[count] должен быть положительным числом!");
                    return true;
                }
            }
        } catch (NumberFormatException e)
        {
            player.sendMessage(ChatColor.RED + "<level> и [count] должны быть числами!");
            return true;
        }

        int minLevel = configManager.getMinLevel(type);
        int maxLevel = configManager.getMaxLevel(type);

        if (!configManager.isValidLevel(type, level) || level < minLevel || level > maxLevel)
        {
            player.sendMessage(ChatColor.RED + "Уровень " + level + " невалиден для типа '" + type + "'.");
            player.sendMessage(ChatColor.YELLOW + "Доступные уровни: от " + minLevel + " до " + maxLevel + ".");
            return true;
        }

        Material material = configManager.getLuckyBlockMaterial(type);
        int customModelData = configManager.getLuckyBlockCustomModelData(type);
        String displayName = configManager.getDisplayName(type, level);

        ItemStack luckyBlockItem = new ItemStack(material, 1);
        ItemMeta meta = luckyBlockItem.getItemMeta();

        if (meta != null)
        {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            if (customModelData > 0)
            {
                meta.setCustomModelData(customModelData);
            }
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.BYTE, (byte) 1);
            pdc.set(LuckyBlockPlugin.LUCKY_BLOCK_TYPE_KEY, PersistentDataType.STRING, type);
            pdc.set(LuckyBlockPlugin.LUCKY_BLOCK_LEVEL_KEY, PersistentDataType.INTEGER, level);
            luckyBlockItem.setItemMeta(meta);
        }

        for (int i = 0; i < count; i++)
        {
            player.getInventory().addItem(luckyBlockItem.clone());
        }

        player.sendMessage(ChatColor.GREEN + "Вам выдан: " + ChatColor.translateAlternateColorCodes('&', displayName)
                + ChatColor.GREEN + " (x" + count + ")");
        return true;
    }
}
