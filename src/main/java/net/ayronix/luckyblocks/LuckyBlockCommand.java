package net.ayronix.luckyblocks;

import java.util.Set;

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

public class LuckyBlockCommand implements CommandExecutor
{

    private final ConfigManager configManager;
    private final LuckyBlockPlugin plugin;

    public LuckyBlockCommand(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help")))
        {
            sender.sendMessage(ChatColor.GOLD + "LuckyBlock — команды:");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " placemode on" + ChatColor.GRAY
                    + " — режим расстановки лакиблоков");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " placemode off" + ChatColor.GRAY
                    + " — выключить placemode и вернуть хотбар");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " placemode init" + ChatColor.GRAY
                    + " — инициализировать лакиблоки на карте");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " placemode save" + ChatColor.GRAY
                    + " — сохранить точки лакиблоков карты");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " <type> <level> [count]" + ChatColor.GRAY
                    + " — получить лакиблок в инвентарь");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " add chest-table <type> <имя>" + ChatColor.GRAY
                    + " — экспортировать содержимое сундука как таблицу лута для лакиблока");
            sender.sendMessage(
                    ChatColor.YELLOW + "/" + label + " list" + ChatColor.GRAY + " — список всех типов лакиблоков");
            sender.sendMessage(
                    ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " — перезагрузить конфиги");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " help" + ChatColor.GRAY + " — эта справка");
            return true;
        }
        if (args.length >= 1)
        {
            // --- Placemode commands ---
            if (args[0].equalsIgnoreCase("placemode"))
            {
                if (!(sender instanceof Player player))
                {
                    sender.sendMessage(ChatColor.RED + "Только игрок может использовать placemode.");
                    return true;
                }
                if (!player.hasPermission("luckyblocks.placemode"))
                {
                    player.sendMessage(ChatColor.RED + "Нет прав для /" + label + " placemode.");
                    return true;
                }
                if (args.length == 2 && args[1].equalsIgnoreCase("on"))
                {
                    // Включить режим placemode, выдача hotbar инициализация
                    // сессии
                    PlacemodeManager.getInstance().enableFor(player);
                    return true;
                }
                if (args.length == 2 && args[1].equalsIgnoreCase("off"))
                {
                    // Отключить placemode, вернуть предметы
                    PlacemodeManager.getInstance().disableFor(player);
                    return true;
                }
                if (args.length == 2 && args[1].equalsIgnoreCase("init"))
                {
                    // Установить лакиблоки в сохранённых местах, загрузить из
                    // мира
                    PlacemodeManager.getInstance().loadBlocks(player.getWorld());
                    PlacemodeManager.getInstance().initBlocks(player.getWorld());
                    player.sendMessage(ChatColor.AQUA + "[LuckyBlock] Инициализация лакиблоков из карты завершена.");
                    return true;
                }
                if (args.length == 2 && args[1].equalsIgnoreCase("save"))
                {
                    // Сохранить точки лакиблоков карты
                    PlacemodeManager.getInstance().saveBlocks(player.getWorld());
                    player.sendMessage(ChatColor.GREEN + "[LuckyBlock] Позиции лакиблоков для карты сохранены.");
                    return true;
                }
                player.sendMessage(ChatColor.YELLOW + "Использование: /" + label + " placemode <on|off|init|save>");
                return true;
            }
            if (args[0].equalsIgnoreCase("reload"))
            {
                if (!sender.hasPermission("luckyblocks.reload"))
                {
                    sender.sendMessage(ChatColor.RED + "Нет прав для /" + label + " reload");
                    return true;
                }
                plugin.reloadConfigAndChests();
                sender.sendMessage(ChatColor.GREEN + "LuckyBlocks: Конфиги успешно перезагружены!");
                return true;
            }
            if (args[0].equalsIgnoreCase("list"))
            {
                Set<String> types = configManager.getAvailableTypes();
                if (types.isEmpty())
                {
                    sender.sendMessage(ChatColor.YELLOW + "Типы лакиблоков не найдены в config.yml.");
                } else
                {
                    sender.sendMessage(ChatColor.GREEN + "Типы Lucky Block:");
                    sender.sendMessage(
                            types.stream().sorted().map(t -> ChatColor.AQUA + " * " + t).toArray(String[]::new));
                }
                return true;
            }
            // /luckyblock add lucky-table <type>
            if (args.length >= 3 && args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("lucky-table"))
            {
                if (!(sender instanceof Player player))
                {
                    sender.sendMessage(ChatColor.RED + "Только игрок может использовать эту команду.");
                    return true;
                }
                if (!player.hasPermission("luckyblocks.edit"))
                {
                    player.sendMessage(ChatColor.RED + "Нет прав для /" + label + " add lucky-table ...");
                    return true;
                }
                String type = args[2].toLowerCase();
                org.bukkit.block.Block targetBlock = player.getTargetBlockExact(6);
                if (targetBlock == null || !(targetBlock.getState() instanceof org.bukkit.block.Chest chest))
                {
                    player.sendMessage(ChatColor.RED + "Посмотрите на сундук для экспорта лута!");
                    return true;
                }
                org.bukkit.inventory.Inventory inv = chest.getBlockInventory();
                java.util.List<java.util.Map<String, Object>> itemsOut = new java.util.ArrayList<>();
                for (org.bukkit.inventory.ItemStack stack : inv.getContents())
                {
                    if (stack == null || stack.getType().isAir())
                        continue;
                    java.util.Map<String, Object> itemData = new java.util.HashMap<>();
                    itemData.put("item", stack.getType().name());
                    itemData.put("amount", stack.getAmount());
                    itemsOut.add(itemData);
                }
                // Открываем luckyblocks.yml
                java.io.File luckyFile = new java.io.File(plugin.getDataFolder(), "luckyblocks.yml");
                org.bukkit.configuration.file.YamlConfiguration lucky = org.bukkit.configuration.file.YamlConfiguration
                        .loadConfiguration(luckyFile);
                // Ищем первый available type.level (в примере — всегда
                // levels.1, либо спрашивать уровень?)
                String level = "1";
                String dropPath = "types." + type + ".levels." + level + ".events.DROP_ITEM";
                Object dropSection = lucky.get(dropPath);
                java.util.Map<String, Object> newDrop = new java.util.HashMap<>();
                newDrop.put("enabled", true);
                newDrop.put("weight", 100);
                newDrop.put("items", itemsOut);
                java.util.List<Object> list;
                if (dropSection instanceof java.util.List<?> l)
                {
                    list = new java.util.ArrayList<>(l);
                    list.add(newDrop);
                } else if (dropSection != null)
                {
                    // если был 1 объект, преобразуем в массив
                    list = new java.util.ArrayList<>();
                    list.add(dropSection);
                    list.add(newDrop);
                } else
                {
                    list = new java.util.ArrayList<>();
                    list.add(newDrop);
                }
                lucky.set(dropPath, list);
                try
                {
                    lucky.save(luckyFile);
                } catch (Exception ex)
                {
                    player.sendMessage(ChatColor.RED + "Ошибка сохранения luckyblocks.yml: " + ex.getMessage());
                    return true;
                }
                player.sendMessage(ChatColor.GREEN + "Этот лут добавлен в DROP_ITEM лаки-блока типа " + type
                        + " (level 1) как отдельный вариант!");
                return true;
            }
            // /luckyblock add chest-table <type> <tableName>
            if (args.length >= 4 && args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("chest-table"))
            {
                if (!(sender instanceof Player player))
                {
                    sender.sendMessage(ChatColor.RED + "Только игрок может использовать эту команду.");
                    return true;
                }
                if (!player.hasPermission("luckyblocks.edit"))
                {
                    player.sendMessage(ChatColor.RED + "Нет прав для /" + label + " add chest-table ...");
                    return true;
                }
                String type = args[2].toLowerCase();
                String tableName = args[3];
                org.bukkit.block.Block targetBlock = player.getTargetBlockExact(6);
                if (targetBlock == null || !(targetBlock.getState() instanceof org.bukkit.block.Chest chest))
                {
                    player.sendMessage(ChatColor.RED + "Посмотрите на сундук для экспорта лута!");
                    return true;
                }
                org.bukkit.inventory.Inventory inv = chest.getBlockInventory();
                java.util.List<java.util.Map<String, Object>> itemsOut = new java.util.ArrayList<>();
                for (org.bukkit.inventory.ItemStack stack : inv.getContents())
                {
                    if (stack == null || stack.getType().isAir())
                        continue;
                    java.util.Map<String, Object> itemData = new java.util.HashMap<>();
                    itemData.put("material", stack.getType().name());
                    itemData.put("amount", stack.getAmount());
                    // В дальнейшем: поддержка имени, лора, атрибутов — оставить
                    // задел
                    itemsOut.add(itemData);
                }
                // Формируем структуру loot-таблицы: type -> tableName
                org.bukkit.configuration.file.YamlConfiguration chests = org.bukkit.configuration.file.YamlConfiguration
                        .loadConfiguration(new java.io.File(plugin.getDataFolder(), "chests.yml"));
                String path = type + "." + tableName + ".slots";
                chests.set(type + "." + tableName + ".slots", inv.getSize());
                java.util.List<java.util.Map<?, ?>> lootList = chests.getMapList(type + "." + tableName + ".loot");
                if (lootList == null)
                    lootList = new java.util.ArrayList<>();
                // Добавляем новый вариант лута с весом 100 (можно задать, либо
                // как next)
                java.util.Map<String, Object> lootVariant = new java.util.HashMap<>();
                lootVariant.put("items", itemsOut);
                lootVariant.put("weight", 100);
                lootList.add(lootVariant);
                chests.set(type + "." + tableName + ".loot", lootList);
                try
                {
                    chests.save(new java.io.File(plugin.getDataFolder(), "chests.yml"));
                } catch (Exception ex)
                {
                    player.sendMessage(ChatColor.RED + "Ошибка сохранения chests.yml: " + ex.getMessage());
                    return true;
                }
                player.sendMessage(
                        ChatColor.GREEN + "Таблица " + tableName + " для типа " + type + " добавлена из сундука!");
                return true;
            }
        }

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
