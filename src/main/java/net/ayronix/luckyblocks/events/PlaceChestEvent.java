package net.ayronix.luckyblocks.events;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Collections;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

public class PlaceChestEvent implements ICustomEvent
{
    @Override @SuppressWarnings("LoggerStringConcat")
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        String type = eventConfig.contains("_luckyblock_type") ? eventConfig.getString("_luckyblock_type") : "neutral";
        String itemTable = eventConfig.getString("item-table", "main_loot");

        // table random
        if ("random".equalsIgnoreCase(itemTable))
        {
            File chestsFile = new File(plugin.getDataFolder(), "chests.yml");
            ConfigurationSection catSec = null;
            if (chestsFile.exists())
            {
                YamlConfiguration chestsR = YamlConfiguration.loadConfiguration(chestsFile);
                catSec = chestsR.getConfigurationSection(type);
            }
            if (catSec != null)
            {
                List<String> allTables = new ArrayList<>(catSec.getKeys(false));
                if (!allTables.isEmpty())
                {
                    itemTable = allTables.get(new Random().nextInt(allTables.size()));
                }
            }
        }

        ConfigurationSection rel = eventConfig.getConfigurationSection("relative-coords");
        int dx = rel != null ? rel.getInt("x", 0) : 0;
        int dy = rel != null ? rel.getInt("y", 0) : 0;
        int dz = rel != null ? rel.getInt("z", 0) : 0;

        File chestsFile = new File(plugin.getDataFolder(), "chests.yml");
        if (!chestsFile.exists())
        {
            player.sendMessage("§cНе найден chests.yml.");
            return;
        }
        YamlConfiguration chests = YamlConfiguration.loadConfiguration(chestsFile);
        ConfigurationSection tableSection = chests.getConfigurationSection(type + "." + itemTable);
        if (tableSection == null)
        {
            player.sendMessage("§cВ категории \"" + type + "\" не найдена таблица " + itemTable);
            return;
        }

        int slots = tableSection.getInt("slots", 27);
        List<Map<?, ?>> lootVariants = tableSection.getMapList("loot");
        if (lootVariants == null || lootVariants.isEmpty())
        {
            player.sendMessage("§cПустая таблица лута.");
            return;
        }

        int totalWeight = 0;
        for (Map<?, ?> variant : lootVariants)
        {
            Object weightObj = variant.get("weight");
            if (weightObj instanceof Number)
                totalWeight += ((Number) weightObj).intValue();
        }
        int choose = new Random().nextInt(totalWeight);
        Map<?, ?> selected = null;
        int cumulative = 0;
        for (Map<?, ?> variant : lootVariants)
        {
            Object weightObj = variant.get("weight");
            int w = weightObj instanceof Number ? ((Number) weightObj).intValue() : 1;
            cumulative += w;
            if (choose < cumulative)
            {
                selected = variant;
                break;
            }
        }
        if (selected == null)
            selected = lootVariants.get(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) selected.get("items");
        if (itemsList == null)
            itemsList = new ArrayList<>();
        plugin.getLogger().info("PlaceChestEvent: [DEBUG] Взятый items: Всего=" + itemsList.size());
        for (int ix = 0; ix < Math.min(5, itemsList.size()); ix++)
        {
            Map<String, Object> itemMap = itemsList.get(ix);
            plugin.getLogger().info("PlaceChestEvent: item[" + ix + "] = " + itemMap);
        }

        ItemStack[] contents = new ItemStack[slots];
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < slots; i++)
            emptySlots.add(i);
        Random rnd = new Random();

        for (Map<String, Object> info : itemsList)
        {
            ItemStack item = createItemStackFromMap(info);
            plugin.getLogger().info("DEBUG: ItemStack создан: " + item.getType() + " x" + item.getAmount());
            int amount = item.getAmount();
            int maxStack = item.getMaxStackSize();
            int toGive = amount;
            while (toGive > 0 && !emptySlots.isEmpty())
            {
                int stack = Math.min(toGive, maxStack > 0 ? maxStack : 64);
                int slot = emptySlots.remove(rnd.nextInt(emptySlots.size()));
                ItemStack stackItem = item.clone();
                stackItem.setAmount(stack);
                plugin.getLogger()
                        .info("DEBUG: Кладём в сундук slot " + slot + ": " + stackItem.getType() + " x" + stack);
                contents[slot] = stackItem;
                toGive -= stack;
            }
        }
        for (int i = 0; i < slots; i++)
        {
            if (contents[i] == null)
                contents[i] = null;
        }

        // Правильно выбираем блок сундука после смещения!
        Location chestLoc = location.clone().add(dx, dy, dz);
        Block block = chestLoc.getBlock();

        // Проверка наличия лаки-блока по PDC чанка
        org.bukkit.Chunk chunk = block.getChunk();
        org.bukkit.persistence.PersistentDataContainer chunkPDC = chunk.getPersistentDataContainer();
        java.util.List<String> luckyCoords = chunkPDC.get(net.ayronix.luckyblocks.LuckyBlockPlugin.LUCKY_BLOCK_KEY,
                org.bukkit.persistence.PersistentDataType.LIST.strings());

        if (luckyCoords != null && luckyCoords.stream().anyMatch(entry -> matchesLocation(chestLoc, entry)))
        {
            // Разрешаем замену ИСКЛЮЧИТЕЛЬНО если целевая координата -
            // лакиблок, который сейчас активирован (allow выдан для этой coord)
            if (!net.ayronix.luckyblocks.LuckyBlockReplaceManager.mayReplace(chestLoc))
            {
                plugin.getLogger().warning("[PlaceChestEvent] Попытка подменить чужой лакиблок по координате "
                        + chestLoc + " — установка сундука запрещена!");
                return;
            }
            // Если замена разрешена — это именно для "своего" лакиблока (allow
            // выдан только ему)
            net.ayronix.luckyblocks.LuckyBlockReplaceManager.remove(chestLoc);
            // Также удаляем координату luckyblock из списка чанка, если она там
            // есть
            luckyCoords.removeIf(entry -> matchesLocation(chestLoc, entry));
            chunkPDC.set(net.ayronix.luckyblocks.LuckyBlockPlugin.LUCKY_BLOCK_KEY,
                    org.bukkit.persistence.PersistentDataType.LIST.strings(), luckyCoords);
        }
        // Откладываем установку сундука на следующий тик, чтобы была
        // гарантирована возможность заменить лаки-блок!
        new org.bukkit.scheduler.BukkitRunnable()
        {
            @Override
            public void run()
            {
                block.setType(Material.CHEST, false);

                // Гарантируем загрузку чанка
                chestLoc.getChunk().load();

                Block finalBlock = chestLoc.getBlock();
                if (finalBlock.getState() instanceof Chest)
                {
                    // Откладываем манипуляции с инвентарём сундука на следующий
                    // тик!
                    new org.bukkit.scheduler.BukkitRunnable()
                    {
                        @Override
                        public void run()
                        {
                            // Повторно получаем state у блока сундука после
                            // update — только так работает во всех новых
                            // версиях!
                            Block freshBlock = chestLoc.getBlock();
                            if (freshBlock.getState() instanceof Chest)
                            {
                                Chest freshChest = (Chest) freshBlock.getState();
                                plugin.getLogger()
                                        .info("[ChestDebug] freshChest: " + freshChest + " at "
                                                + freshBlock.getLocation() + ", isPlaced=" + freshBlock.getType()
                                                + ", inventorySize=" + freshChest.getInventory().getSize());

                                freshChest.setLootTable(null);
                                freshChest.update(true, true);

                                Inventory inv = freshChest.getInventory();
                                plugin.getLogger().info("[ChestDebug] inventory до setItems: "
                                        + java.util.Arrays.toString(inv.getContents()));
                                plugin.getLogger().info(
                                        "[ChestDebug] contents[] до setItems: " + java.util.Arrays.toString(contents));

                                StringBuilder lootList = new StringBuilder();
                                for (int idx = 0; idx < contents.length; idx++)
                                {
                                    if (contents[idx] != null)
                                    {
                                        plugin.getLogger()
                                                .info("[ChestDebug] inv.setItem(" + idx + ", " + contents[idx] + ")");
                                        inv.setItem(idx, contents[idx]);
                                        lootList.append(contents[idx].getType().name()).append(" x")
                                                .append(contents[idx].getAmount()).append(", ");
                                    }
                                }
                                plugin.getLogger().info("[ChestDebug] inventory после setItems: "
                                        + java.util.Arrays.toString(inv.getContents()));
                                // freshChest.update(true, true); // УБРАНО для
                                // теста причины исчезновения содержимого!

                                plugin.getLogger().info("[ChestDebug] inventory после возможного update: "
                                        + java.util.Arrays.toString(inv.getContents()));
                                plugin.getLogger()
                                        .info("[ChestDebug] contents массив: " + java.util.Arrays.toString(contents));
                                plugin.getLogger().info("[ChestDebug] finalChest=" + freshChest + ", inv=" + inv
                                        + ", invSize=" + inv.getSize());

                                // Сообщение в чат игроку с инфой о луте и
                                // координате сундука
                                String lootInfoStr = lootList.length() > 2
                                        ? lootList.substring(0, lootList.length() - 2)
                                        : "нет лута";
                                player.sendMessage("§e(Dev) Сундук заполнен на " + chestLoc.getBlockX() + ","
                                        + chestLoc.getBlockY() + "," + chestLoc.getBlockZ() + " - " + lootInfoStr);
                            }
                        }
                    }.runTaskLater(plugin, 2);
                }
            }
        }.runTask(plugin);

        player.sendMessage("§aУстановлен сундук: " + itemTable + " (" + type + "), заполнено " + slots + " слотов");
    }

    private ItemStack createItemStackFromMap(Map<String, Object> configMap)
    {
        String materialName;
        if (configMap.containsKey("item"))
            materialName = String.valueOf(configMap.get("item"));
        else if (configMap.containsKey("material"))
            materialName = String.valueOf(configMap.get("material"));
        else
            materialName = "STONE";

        int amount = configMap.containsKey("amount") ? Integer.parseInt(String.valueOf(configMap.get("amount"))) : 1;
        Material material = Material.getMaterial(materialName.toUpperCase());
        String debugAs = "";
        if (material == null)
        {
            material = Material.PAPER;
            debugAs = " as " + materialName.toUpperCase();
        }
        ItemStack item = new ItemStack(material, amount);

        if (configMap.containsKey("custom-model-data"))
        {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null)
            {
                meta.setCustomModelData(Integer.parseInt(String.valueOf(configMap.get("custom-model-data"))));
                item.setItemMeta(meta);
            }
        }

        if (configMap.containsKey("name") || configMap.containsKey("lore") || configMap.containsKey("attributes"))
        {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null)
            {
                if (configMap.containsKey("name"))
                    meta.setDisplayName(String.valueOf(configMap.get("name")).replace("&", "§") + debugAs);
                if (configMap.containsKey("lore"))
                {
                    String loreValue = String.valueOf(configMap.get("lore"));
                    List<String> loreList;
                    if (loreValue != null && loreValue.contains("\n"))
                    {
                        String[] loreLines = loreValue.split("\n");
                        loreList = new ArrayList<>();
                        for (String s : loreLines)
                            loreList.add(s.replace("&", "§"));
                    } else if (loreValue != null)
                    {
                        loreList = Collections.singletonList(loreValue.replace("&", "§"));
                    } else
                    {
                        loreList = Collections.emptyList();
                    }
                    meta.setLore(loreList);
                }
                if (configMap.containsKey("attributes"))
                {
                    Object attrs = configMap.get("attributes");
                    if (attrs instanceof List<?> list)
                    {
                        for (Object obj : list)
                        {
                            if (obj instanceof Map<?, ?> map)
                            {
                                String attrName = String.valueOf(map.get("attribute"));
                                org.bukkit.attribute.Attribute attribute = null;
                                try
                                {
                                    attribute = org.bukkit.attribute.Attribute.valueOf(attrName);
                                } catch (IllegalArgumentException | NullPointerException ignored)
                                {
                                }
                                if (attribute == null)
                                    continue;
                                double amountAttr = 1.0;
                                String operation = "ADD_NUMBER";
                                if (map.containsKey("amount"))
                                    amountAttr = Double.parseDouble(String.valueOf(map.get("amount")));
                                if (map.containsKey("operation"))
                                    operation = String.valueOf(map.get("operation")).toUpperCase();

                                org.bukkit.attribute.AttributeModifier.Operation op = org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER;
                                try
                                {
                                    op = org.bukkit.attribute.AttributeModifier.Operation.valueOf(operation);
                                } catch (IllegalArgumentException ignored)
                                {
                                }
                                meta.addAttributeModifier(attribute, new org.bukkit.attribute.AttributeModifier(
                                        java.util.UUID.randomUUID(), attrName, amountAttr, op));
                            }
                        }
                    }
                }
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    // Сравнение: совпадают ли координаты Location и строки entry из PDC
    // ("x,y,z,type,level")
    private boolean matchesLocation(org.bukkit.Location loc, String entry)
    {
        String[] parts = entry.split(",");
        if (parts.length < 3)
            return false;
        try
        {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z;
        } catch (NumberFormatException e)
        {
            return false;
        }
    }
}
