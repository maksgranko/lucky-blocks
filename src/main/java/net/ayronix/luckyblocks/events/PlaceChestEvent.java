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
    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        String type = eventConfig.contains("_luckyblock_type") ? eventConfig.getString("_luckyblock_type") : "neutral";
        String itemTable = eventConfig.getString("item-table", "main_loot");

        // Выбор таблицы при random
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

        // Выбор содержимого
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

        ItemStack[] contents = new ItemStack[slots];
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < slots; i++)
            emptySlots.add(i);
        Random rnd = new Random();

        // Заполняем инвентарь с полной кастомизацией
        for (Map<String, Object> info : itemsList)
        {
            ItemStack item = createItemStackFromMap(info);
            int amount = item.getAmount();
            int maxStack = item.getMaxStackSize();
            int toGive = amount;
            while (toGive > 0 && !emptySlots.isEmpty())
            {
                int stack = Math.min(toGive, maxStack > 0 ? maxStack : 64);
                int slot = emptySlots.remove(rnd.nextInt(emptySlots.size()));
                ItemStack stackItem = item.clone();
                stackItem.setAmount(stack);
                contents[slot] = stackItem;
                toGive -= stack;
            }
        }

        // Ставим сундук
        Location chestLoc = location.clone().add(dx, dy, dz);
        Block block = chestLoc.getBlock();
        block.setType(Material.CHEST);
        if (block.getState() instanceof Chest)
        {
            Chest chest = (Chest) block.getState();
            Inventory inv = chest.getBlockInventory();
            inv.setContents(contents);
            chest.update();
        }
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
        if (material == null)
            material = Material.STONE;
        ItemStack item = new ItemStack(material, amount);

        // Custom model data
        if (configMap.containsKey("custom-model-data"))
        {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null)
            {
                meta.setCustomModelData(Integer.parseInt(String.valueOf(configMap.get("custom-model-data"))));
                item.setItemMeta(meta);
            }
        }

        // Name and lore
        if (configMap.containsKey("name") || configMap.containsKey("lore") || configMap.containsKey("attributes"))
        {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null)
            {
                if (configMap.containsKey("name"))
                    meta.setDisplayName(String.valueOf(configMap.get("name")).replace("&", "§"));
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

                // Атрибуты предмета
                if (configMap.containsKey("attributes"))
                {
                    Object attrs = configMap.get("attributes");
                    if (attrs instanceof List<?>)
                    {
                        for (Object obj : (List<?>) attrs)
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
}
