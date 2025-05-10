package net.ayronix.luckyblocks.events;

import net.ayronix.luckyblocks.LuckyBlockPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DropWeaponEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        // В будущем можно будет настраивать тип оружия и его характеристики из
        // eventConfig
        // String materialName = eventConfig.getString("material",
        // "DIAMOND_SWORD");
        // int amount = eventConfig.getInt("amount", 1);
        // Material weaponMaterial =
        // Material.getMaterial(materialName.toUpperCase());
        // if (weaponMaterial == null) weaponMaterial = Material.DIAMOND_SWORD;

        location.getWorld().dropItemNaturally(location, new ItemStack(Material.DIAMOND_SWORD, 1));
        player.sendMessage("§6Выпало оружие!"); // Сообщение тоже можно будет
                                                // настраивать
    }
}
