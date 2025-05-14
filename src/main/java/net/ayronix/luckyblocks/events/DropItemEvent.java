package net.ayronix.luckyblocks.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

import java.util.UUID;

public class DropItemEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        // Множественная поддержка (items: [...]) и обратная совместимость
        if (eventConfig.contains("items") && eventConfig.get("items") instanceof List<?> itemListRaw)
        {
            for (Object obj : itemListRaw)
            {
                if (obj instanceof Map<?, ?> map)
                {
                    dropItemFromMap(player, location, map, plugin);
                }
            }
        } else
        {
            // старый вариант — один предмет из параметров
            dropItemFromMap(player, location, eventConfig.getValues(false), plugin);
        }

    }

    // Поддержка логики дропа одного предмета по параметрам
    private void dropItemFromMap(Player player, Location location, Map<?, ?> configMap, LuckyBlockPlugin plugin)
    {
        Object matObj = configMap.get("item");
        String materialName = matObj != null ? String.valueOf(matObj) : "APPLE";
        int amount = configMap.containsKey("amount") ? Integer.parseInt(String.valueOf(configMap.get("amount"))) : 1;

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null)
            material = Material.APPLE;

        ItemStack item = new ItemStack(material, amount);

        // Custom model data
        if (configMap.containsKey("custom-model-data"))
        {
            ItemMeta meta = item.getItemMeta();
            if (meta != null)
            {
                meta.setCustomModelData(Integer.parseInt(String.valueOf(configMap.get("custom-model-data"))));
                item.setItemMeta(meta);
            }
        }

        // Name and lore
        if (configMap.containsKey("name") || configMap.containsKey("lore") || configMap.containsKey("attributes"))
        {
            ItemMeta meta = item.getItemMeta();
            if (meta != null)
            {
                if (configMap.containsKey("name"))
                {
                    meta.setDisplayName(String.valueOf(configMap.get("name")).replace("&", "§"));
                }
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
                                Attribute attribute = null;
                                try
                                {
                                    attribute = Attribute.valueOf(attrName);
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

                                AttributeModifier.Operation op = AttributeModifier.Operation.ADD_NUMBER;
                                try
                                {
                                    op = AttributeModifier.Operation.valueOf(operation);
                                } catch (IllegalArgumentException ignored)
                                {
                                }

                                meta.addAttributeModifier(attribute,
                                        new AttributeModifier(UUID.randomUUID(), attrName, amountAttr, op));
                            }
                        }
                    }
                }

                item.setItemMeta(meta);
            }
        }

        location.getWorld().dropItemNaturally(location, item);

        // Проверяем PDC (лакиблок) в координате дропа
        org.bukkit.Chunk chunk = location.getBlock().getChunk();
        org.bukkit.persistence.PersistentDataContainer chunkPDC = chunk.getPersistentDataContainer();
        java.util.List<String> luckyCoords = chunkPDC.get(net.ayronix.luckyblocks.LuckyBlockPlugin.LUCKY_BLOCK_KEY,
                org.bukkit.persistence.PersistentDataType.LIST.strings());
        boolean isLuckyBlockPDC = luckyCoords != null
                && luckyCoords.stream().anyMatch(entry -> matchesLocation(location, entry));
        String pdcState = isLuckyBlockPDC ? "В PDC присутствует лакиблок" : "В PDC НЕТ лакиблока";

        String itemDisp = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : material.name();

        String msg = "§e(Dev) Дроп: " + itemDisp + " x" + item.getAmount() + " на " + location.getBlockX() + ","
                + location.getBlockY() + "," + location.getBlockZ() + " | " + pdcState;
        player.sendMessage(msg);

        plugin.getLogger().info("[DropItemEvent] " + itemDisp + " x" + item.getAmount() + " на " + location.getBlockX()
                + "," + location.getBlockY() + "," + location.getBlockZ() + " | " + pdcState);
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
