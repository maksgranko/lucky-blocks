package net.ayronix.luckyblocks.events;

import net.ayronix.luckyblocks.LuckyBlockPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DropFoodEvent implements ICustomEvent
{

    @Override
    public void execute(Player player, Location location, ConfigurationSection eventConfig, LuckyBlockPlugin plugin)
    {
        // В будущем можно будет настраивать тип еды и количество из eventConfig
        // String materialName = eventConfig.getString("material", "APPLE");
        // int amount = eventConfig.getInt("amount", 2);
        // Material foodMaterial =
        // Material.getMaterial(materialName.toUpperCase());
        // if (foodMaterial == null) foodMaterial = Material.APPLE;

        location.getWorld().dropItemNaturally(location, new ItemStack(Material.APPLE, 2));
        player.sendMessage("§aВыпал дроп: Еда"); // Сообщение тоже можно будет
                                                 // настраивать
    }
}
