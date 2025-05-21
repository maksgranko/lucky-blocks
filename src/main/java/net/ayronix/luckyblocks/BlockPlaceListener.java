package net.ayronix.luckyblocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BlockPlaceListener implements Listener
{
    @SuppressWarnings("unused")
    private final LuckyBlockPlugin plugin;

    public BlockPlaceListener(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
        var item = event.getItemInHand();
        if (item.getItemMeta() == null)
        {
            return;
        }
        var pdcItem = item.getItemMeta().getPersistentDataContainer();
        Byte flag = pdcItem.get(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.BYTE);
        if (flag == null || flag != (byte) 1)
        {
            return;
        }

        Block block = event.getBlockPlaced();

        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        List<String> existing = pdc.get(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings());
        List<String> coords = (existing != null) ? new ArrayList<>(existing) : new ArrayList<>();

        String type = pdcItem.get(LuckyBlockPlugin.LUCKY_BLOCK_TYPE_KEY, PersistentDataType.STRING);
        Integer levelObj = pdcItem.get(LuckyBlockPlugin.LUCKY_BLOCK_LEVEL_KEY, PersistentDataType.INTEGER);
        if (type == null || levelObj == null)
        {
            return;
        }
        int level = levelObj;

        // Через 1 тик заменяем установленный блок на материал из конфига
        var material = plugin.getConfigManager().getLuckyBlockMaterial(type);
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
        {
            block.setType(material);
        });

        // Генерация имени armor_stand (12 символов, только буквы и цифры)
        String armorStandName = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // Получение offset и головы из конфига лакиблока
        var config = plugin.getConfigManager().getRawConfig();
        String head = config.getString("types." + type + ".item.armorstand-head", "");
        List<Double> offsetList = config.getDoubleList("types." + type + ".item.armorstand-offset");
        double offsetX = 0, offsetY = 0, offsetZ = 0;
        if (offsetList.size() >= 3)
        {
            offsetX = offsetList.get(0);
            offsetY = offsetList.get(1);
            offsetZ = offsetList.get(2);
        }

        // Спавн armor_stand с оффсетом
        var world = block.getWorld();
        var loc = block.getLocation().add(offsetX, offsetY, offsetZ);
        world.spawn(loc, org.bukkit.entity.ArmorStand.class, as ->
        {
            net.ayronix.luckyblocks.EventAnimationUtil.setupArmorStand(as, armorStandName, false);
            // Если задана голова — ставим кастомную (base64 или ник)
            if (head != null && !head.isEmpty())
            {
                org.bukkit.inventory.ItemStack skull = net.ayronix.luckyblocks.SkullUtil.createHead(head, null, 0);
                as.getEquipment().setHelmet(skull);
            }
        });

        // Обновляем структуру PDC: x,y,z,type,level,armorstandName
        String coord = block.getX() + "," + block.getY() + "," + block.getZ() + "," + type + "," + level + ","
                + armorStandName;
        coords.add(coord);
        pdc.set(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings(), coords);

        if (plugin.getDebug())
        {
            event.getPlayer().sendMessage("§aLucky Block установлен в чанке!");
        }
    }

}
