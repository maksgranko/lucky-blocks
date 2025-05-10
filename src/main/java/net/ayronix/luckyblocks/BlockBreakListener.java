package net.ayronix.luckyblocks;

import net.ayronix.luckyblocks.events.ICustomEvent;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection; // Порядок может измениться автоформатированием

import java.util.ArrayList; // Добавлен
import java.util.List; // Добавлен
import java.util.Random; // Добавлен
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BlockBreakListener implements Listener
{

    private LuckyBlockPlugin plugin;

    public BlockBreakListener(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
    }

    private final Random random = new Random();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        List<String> existing = pdc.get(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings());
        if (existing == null)
        {
            return;
        }

        String coord = null;
        String type = null;
        int level = 0;
        for (String entry : existing)
        {
            String[] parts = entry.split(",");
            if (parts.length >= 3 && Integer.parseInt(parts[0]) == block.getX()
                    && Integer.parseInt(parts[1]) == block.getY() && Integer.parseInt(parts[2]) == block.getZ())
            {
                coord = entry;
                type = parts[3];
                level = Integer.parseInt(parts[4]);
                break;
            }
        }

        if (coord == null)
        {
            return;
        }

        List<String> coords = new ArrayList<>(existing);
        coords.remove(coord);
        pdc.set(LuckyBlockPlugin.LUCKY_BLOCK_KEY, PersistentDataType.LIST.strings(), coords);

        event.setDropItems(false);
        event.setExpToDrop(0);
        block.setType(Material.AIR);

        Player player = event.getPlayer();
        ConfigManager configManager = plugin.getConfigManager();
        String eventTypeKey = ProbabilityCalculator.getRandomEvent(configManager, type, level);

        if (eventTypeKey != null)
        {
            ICustomEvent eventHandler = plugin.getEventRegistry().get(eventTypeKey.toUpperCase());
            if (eventHandler != null)
            {
                ConfigurationSection eventSpecificConfig = configManager.getEventConfig(type, level, eventTypeKey);
                // Проверка eventSpecificConfig.getBoolean("enabled", true) уже
                // должна быть в ProbabilityCalculator
                // или здесь, если ProbabilityCalculator возвращает ключ даже
                // для отключенных
                if (eventSpecificConfig != null && eventSpecificConfig.getBoolean("enabled", true))
                {
                    eventHandler.execute(player, block.getLocation(), eventSpecificConfig, plugin);
                } else
                {
                    // Событие отключено или не найдена конфигурация, вызываем
                    // "дефолтное" событие
                    plugin.getEventRegistry().get("DEFAULT").execute(player, block.getLocation(), null, plugin);
                }
            } else
            {
                plugin.getLogger().warning("Не найден обработчик для события: " + eventTypeKey + " для типа " + type
                        + " и уровня " + level);
                plugin.getEventRegistry().get("DEFAULT").execute(player, block.getLocation(), null, plugin);
            }
        } else
        {
            // Никакое событие не выбрано (например, все веса 0 или все
            // отключены)
            plugin.getEventRegistry().get("DEFAULT").execute(player, block.getLocation(), null, plugin);
        }
    }
}
