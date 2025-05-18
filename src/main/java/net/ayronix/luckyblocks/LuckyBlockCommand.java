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
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " give <type> <level> [count]" + ChatColor.GRAY
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
            // --- LuckyBlock Debug Command ---
            if (args[0].equalsIgnoreCase("debug"))
            {
                if (!sender.hasPermission("luckyblocks.debug"))
                {
                    sender.sendMessage(ChatColor.RED + "Нет прав для /" + label + " debug");
                    return true;
                }
                if (args.length == 1)
                {
                    boolean state = plugin.getDebug();
                    sender.sendMessage(ChatColor.AQUA + "LuckyBlocks debug: "
                            + (state ? ChatColor.GREEN + "включён" : ChatColor.RED + "выключен"));
                    return true;
                }
                if (args.length == 2)
                {
                    if (args[1].equalsIgnoreCase("on"))
                    {
                        plugin.setDebug(true);
                        sender.sendMessage(ChatColor.GREEN + "LuckyBlocks debug включён.");
                        return true;
                    } else if (args[1].equalsIgnoreCase("off"))
                    {
                        plugin.setDebug(false);
                        sender.sendMessage(ChatColor.YELLOW + "LuckyBlocks debug выключен.");
                        return true;
                    } else
                    {
                        sender.sendMessage(ChatColor.YELLOW + "Использование: /" + label + " debug [on|off]");
                        return true;
                    }
                }
            }
            // --- LuckyBlock Give Command ---
            if (args[0].equalsIgnoreCase("give"))
            {
                if (!sender.hasPermission("luckyblocks.use"))
                {
                    sender.sendMessage(ChatColor.RED + "Нет прав для /" + label + " give");
                    return true;
                }

                // Если передан player_name (последний аргумент)
                java.util.List<Player> targetPlayers = new java.util.ArrayList<>();
                int argShift = 0;

                if (args.length >= 5)
                {
                    String playerName = args[args.length - 1];
                    if (playerName.equalsIgnoreCase("@a") || playerName.equals("*"))
                    {
                        if (!sender.hasPermission("luckyblocks.give"))
                        {
                            sender.sendMessage(ChatColor.RED + "Нет прав на выдачу лакиблоков другим игрокам.");
                            return true;
                        }
                        targetPlayers.addAll(org.bukkit.Bukkit.getOnlinePlayers());
                        argShift = 1;
                    } else if (playerName.equalsIgnoreCase("@s"))
                    {
                        if (sender instanceof Player player)
                        {
                            targetPlayers.add(player);
                            argShift = 1;
                        } else
                        {
                            sender.sendMessage(ChatColor.RED + "Только игрок может использовать @s.");
                            return true;
                        }
                    } else if (playerName.equalsIgnoreCase("@p"))
                    {
                        if (!(sender instanceof Player senderPlayer))
                        {
                            sender.sendMessage(ChatColor.RED + "Только игрок может использовать @p.");
                            return true;
                        }
                        Player nearest = null;
                        double minDist = Double.MAX_VALUE;
                        org.bukkit.Location loc = senderPlayer.getLocation();
                        for (Player p : org.bukkit.Bukkit.getOnlinePlayers())
                        {
                            if (p == senderPlayer)
                                continue;
                            double dist = loc.getWorld().equals(p.getWorld()) ? loc.distanceSquared(p.getLocation())
                                    : Double.MAX_VALUE;
                            if (dist < minDist)
                            {
                                minDist = dist;
                                nearest = p;
                            }
                        }
                        if (nearest == null)
                        {
                            sender.sendMessage(ChatColor.RED + "Не найден ни один другой игрок для @p.");
                            return true;
                        }
                        if (!sender.hasPermission("luckyblocks.give"))
                        {
                            sender.sendMessage(ChatColor.RED + "Нет прав на выдачу лакиблоков другим игрокам.");
                            return true;
                        }
                        targetPlayers.add(nearest);
                        argShift = 1;
                    } else
                    {
                        Player found = org.bukkit.Bukkit.getPlayerExact(playerName);
                        if (found == null)
                        {
                            sender.sendMessage(ChatColor.RED + "Не удалось найти игрока: " + playerName);
                            return true;
                        }
                        if (!sender.equals(found) && !sender.hasPermission("luckyblocks.give"))
                        {
                            sender.sendMessage(ChatColor.RED + "Нет прав на выдачу лакиблоков другим игрокам.");
                            return true;
                        }
                        targetPlayers.add(found);
                        argShift = 1;
                    }
                } else if (sender instanceof Player player)
                {
                    targetPlayers.add(player);
                } else
                {
                    sender.sendMessage(ChatColor.RED + "Только игрок или команда с указанием игрока!");
                    return true;
                }

                if (args.length < 3 + argShift)
                {
                    sender.sendMessage(ChatColor.YELLOW + "Использование: /" + label
                            + " give <type> <level> [count] [player/@p/@a/*]");
                    return true;
                }

                String type = args[1].toLowerCase();
                Set<String> availableTypes = configManager.getAvailableTypes();
                if (!availableTypes.contains(type))
                {
                    sender.sendMessage(ChatColor.RED + "Тип не найден: " + type);
                    if (!availableTypes.isEmpty())
                    {
                        sender.sendMessage(ChatColor.YELLOW + "Доступные типы: " + String.join(", ", availableTypes));
                    }
                    return true;
                }

                int level;
                int count = 1;
                try
                {
                    level = Integer.parseInt(args[2]);
                    if (args.length >= 4 + argShift)
                    {
                        count = Integer.parseInt(args[3]);
                        if (count <= 0)
                        {
                            sender.sendMessage(ChatColor.RED + "[count] должен быть положительным числом!");
                            return true;
                        }
                    }
                } catch (NumberFormatException e)
                {
                    sender.sendMessage(ChatColor.RED + "<level> и [count] должны быть числами!");
                    return true;
                }

                int minLevel = configManager.getMinLevel(type);
                int maxLevel = configManager.getMaxLevel(type);

                if (!configManager.isValidLevel(type, level) || level < minLevel || level > maxLevel)
                {
                    sender.sendMessage(ChatColor.RED + "Уровень " + level + " невалиден для типа '" + type + "'.");
                    sender.sendMessage(ChatColor.YELLOW + "Доступные уровни: от " + minLevel + " до " + maxLevel + ".");
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

                for (Player target : targetPlayers)
                {
                    for (int i = 0; i < count; i++)
                    {
                        target.getInventory().addItem(luckyBlockItem.clone());
                    }

                    if (sender != target)
                    {
                        target.sendMessage(ChatColor.GREEN + "Вам выдан: "
                                + ChatColor.translateAlternateColorCodes('&', displayName) + ChatColor.GREEN + " (x"
                                + count + ") от " + sender.getName());
                    }
                }
                // Сообщение отправителю
                if (targetPlayers.size() == 1 && sender != targetPlayers.get(0))
                {
                    sender.sendMessage(ChatColor.GREEN + "Вы выдали игроку " + targetPlayers.get(0).getName() + ": "
                            + ChatColor.translateAlternateColorCodes('&', displayName) + ChatColor.GREEN + " (x" + count
                            + ")");
                } else if (targetPlayers.size() > 1)
                {
                    sender.sendMessage(ChatColor.GREEN + "Вы выдали лакиблоки всем онлайн-игрокам: "
                            + ChatColor.translateAlternateColorCodes('&', displayName) + ChatColor.GREEN + " (x" + count
                            + " каждому)");
                } else if (targetPlayers.size() == 1)
                {
                    targetPlayers.get(0)
                            .sendMessage(ChatColor.GREEN + "Вам выдан: "
                                    + ChatColor.translateAlternateColorCodes('&', displayName) + ChatColor.GREEN + " (x"
                                    + count + ")");
                }
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

        return true;
    }
}
