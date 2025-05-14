package net.ayronix.luckyblocks.events;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.ayronix.luckyblocks.LuckyBlockPlugin;

public class PlaceBlockEvent implements ICustomEvent
{
    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        // Новый синтаксис: blocks: [...] — массив с описанием блоков, иначе
        // fallback на старый
        if (eventConfig.contains("blocks") && eventConfig.get("blocks") instanceof List<?> blockList)
        {
            for (Object obj : blockList)
            {
                if (obj instanceof Map<?, ?> map)
                {
                    placeBlockFromMap(player, location, map, plugin);
                }
            }
        } else
        {
            placeBlockFromMap(player, location, eventConfig.getValues(false), plugin);
        }
    }

    // Метод для установки одного блока по свойствам из map
    private void placeBlockFromMap(Player player, Location location, Map<?, ?> map, LuckyBlockPlugin plugin)
    {
        Object matObj = map.get("material");
        String materialName = matObj != null ? String.valueOf(matObj) : "STONE";
        int cModelData = map.containsKey("custom-model-data")
                ? Integer.parseInt(String.valueOf(map.get("custom-model-data")))
                : -1;
        Object relObj = map.get("relative-coords");
        int dx = 0, dy = 0, dz = 0;
        if (relObj instanceof Map<?, ?> relMap)
        {
            dx = relMap.containsKey("x") ? Integer.parseInt(String.valueOf(relMap.get("x"))) : 0;
            dy = relMap.containsKey("y") ? Integer.parseInt(String.valueOf(relMap.get("y"))) : 0;
            dz = relMap.containsKey("z") ? Integer.parseInt(String.valueOf(relMap.get("z"))) : 0;
        }

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null)
            material = Material.STONE;

        Location placeLoc = location.clone().add(dx, dy, dz);
        Block block = placeLoc.getBlock();

        // Проверка наличия лаки-блока по PDC чанка
        org.bukkit.Chunk chunk = block.getChunk();
        org.bukkit.persistence.PersistentDataContainer chunkPDC = chunk.getPersistentDataContainer();
        java.util.List<String> luckyCoords = chunkPDC.get(net.ayronix.luckyblocks.LuckyBlockPlugin.LUCKY_BLOCK_KEY,
                org.bukkit.persistence.PersistentDataType.LIST.strings());

        if (luckyCoords != null && luckyCoords.stream().anyMatch(entry -> matchesLocation(placeLoc, entry)))
        {
            plugin.getLogger().warning("[PlaceBlockEvent] Попытка подменить лакиблок по координате " + placeLoc
                    + " — операция запрещена!");
            return;
        }
        // Если замена разрешена, сразу удалить координату из allowReplaceSet
        // (однократное разрешение)
        if (net.ayronix.luckyblocks.LuckyBlockReplaceManager.mayReplace(placeLoc))
        {
            net.ayronix.luckyblocks.LuckyBlockReplaceManager.remove(placeLoc);
            // safety: no touch to PDC if luckyCoords already NOT contains this
            // loc
        }

        final Material fMaterial = material;
        final Block fBlock = block;
        // Установка блока на следующий тик для корректной замены лаки-блока
        new org.bukkit.scheduler.BukkitRunnable()
        {
            @Override
            public void run()
            {
                Material oldType = fBlock.getType();
                fBlock.setType(fMaterial, false);
                // Получаем актуальный PDC чанка после установки
                org.bukkit.Chunk actChunk = fBlock.getChunk();
                org.bukkit.persistence.PersistentDataContainer actPdc = actChunk.getPersistentDataContainer();
                java.util.List<String> luckyCoords = actPdc.get(
                        net.ayronix.luckyblocks.LuckyBlockPlugin.LUCKY_BLOCK_KEY,
                        org.bukkit.persistence.PersistentDataType.LIST.strings());

                String key = net.ayronix.luckyblocks.LuckyBlockReplaceManager.locToKey(fBlock.getLocation());
                StringBuilder pdcKeys = new StringBuilder();
                if (luckyCoords != null)
                {
                    for (String lck : luckyCoords)
                    {
                        pdcKeys.append(lck).append("; ");
                    }
                }
                boolean wasLuckyBlock = luckyCoords != null && luckyCoords.contains(key);

                String pdcState = wasLuckyBlock ? "В PDC присутствует лакиблок" : "В PDC НЕТ лакиблока";
                player.sendMessage("§e(Dev) Поставлен блок: " + fMaterial.name() + " на " + fBlock.getX() + ","
                        + fBlock.getY() + "," + fBlock.getZ() + ", заменено: " + oldType.name() + " | " + pdcState
                        + " | key=" + key + " | pdc-keys=[" + pdcKeys + "]");
                plugin.getLogger()
                        .info("[PlaceBlockEvent] Установлен блок: " + fMaterial.name() + " на " + fBlock.getX() + ","
                                + fBlock.getY() + "," + fBlock.getZ() + ", заменено: " + oldType.name() + " | "
                                + pdcState + " | key=" + key + " | pdc-keys=[" + pdcKeys + "]");
            }
        }.runTask(plugin);

        player.sendMessage(
                "§aПоставлен блок: " + material.name() + " по координатам (" + dx + ", " + dy + ", " + dz + ")");
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
