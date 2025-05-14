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
                    placeBlockFromMap(player, location, map);
                }
            }
        } else
        {
            placeBlockFromMap(player, location, eventConfig.getValues(false));
        }
    }

    // Метод для установки одного блока по свойствам из map
    private void placeBlockFromMap(Player player, Location location, Map<?, ?> map)
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
        block.setType(material);

        // Пока custom-model-data для блока не используется напрямую
        if (cModelData > 0)
        {
            // см. пояснение выше
        }

        player.sendMessage(
                "§aПоставлен блок: " + material.name() + " по координатам (" + dx + ", " + dy + ", " + dz + ")");
    }
}
