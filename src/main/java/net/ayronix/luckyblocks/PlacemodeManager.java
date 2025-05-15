package net.ayronix.luckyblocks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class PlacemodeManager
{
    private static PlacemodeManager instance;

    // Активные placemode-сессии игроков
    private final Map<UUID, PlacemodeSession> sessions = new ConcurrentHashMap<>();
    // Для каждого мира — кэш координат лакиблоков (позиции, тип, уровень)
    private final Map<String, List<BlockData>> worldBlocks = new ConcurrentHashMap<>();

    private PlacemodeManager()
    {
    }

    public static PlacemodeManager getInstance()
    {
        if (instance == null)
        {
            instance = new PlacemodeManager();
        }
        return instance;
    }

    // --- Placemode для игрока ---
    public void enableFor(Player player)
    {
        if (sessions.containsKey(player.getUniqueId()))
            return;
        // Сохраняем хотбар (только 0-8 слоты), остальные инвентарь не трогаем
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        org.bukkit.inventory.ItemStack[] savedHotbar = new org.bukkit.inventory.ItemStack[9];
        for (int i = 0; i < 9; i++)
        {
            savedHotbar[i] = inv.getItem(i);
        }

        // Выбрать дефолтные значения (можно расширить)
        PlacemodeSession session = new PlacemodeSession(player, "classic", 1, 1, 7);
        // Сохраняем хотбар в сессию
        session.savedHotbar = savedHotbar;
        sessions.put(player.getUniqueId(), session);

        // Заменяем хотбар на placemode-инструменты
        inv.clear(0);
        inv.setItem(0, new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLAZE_ROD, 1)); // инструмент
        inv.setItem(1, new org.bukkit.inventory.ItemStack(org.bukkit.Material.SOUL_TORCH, 1)); // уровень
                                                                                               // -
        inv.setItem(2, new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE_TORCH, 1)); // уровень
                                                                                                   // +
        inv.setItem(3, new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLUE_WOOL, 1)); // type
                                                                                              // смена
                                                                                              // (пример)

        // Делаем игрока бессмертным, переводим в SURVIVAL и включаем полёт
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);

        // Устанавливаем уровень игрока = level лакиблока
        player.setLevel(session.getLevel());

        // Остальные слоты по желанию, можно задать предметы для других типов

        player.sendMessage(ChatColor.GREEN
                + "[LuckyBlock] Placemode запущен. Используйте хотбар для выбора типа и уровня лакиблока!");
    }

    public void disableFor(Player player)
    {
        PlacemodeSession session = sessions.remove(player.getUniqueId());
        if (session != null && session.savedHotbar != null)
        {
            // Возвращаем хотбар (0-8)
            org.bukkit.inventory.PlayerInventory inv = player.getInventory();
            for (int i = 0; i < 9; i++)
            {
                inv.setItem(i, session.savedHotbar[i]);
            }

            // Отключить бессмертие и возможность полёта (оставляем SURVIVAL)
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setInvulnerable(false);

            player.sendMessage(ChatColor.YELLOW + "[LuckyBlock] Placemode завершён. Хотбар восстановлен.");
        } else if (session != null)
        {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setInvulnerable(false);
            player.sendMessage(ChatColor.YELLOW + "[LuckyBlock] Placemode завершён.");
        }
    }

    public boolean isActive(Player player)
    {
        return sessions.containsKey(player.getUniqueId());
    }

    public PlacemodeSession getSession(Player player)
    {
        return sessions.get(player.getUniqueId());
    }

    // --- Работа с блоками на карте ---
    public List<BlockData> getBlocksForWorld(World world)
    {
        return worldBlocks.computeIfAbsent(world.getName(), k -> new ArrayList<>());
    }

    public void addBlock(World world, BlockData data)
    {
        getBlocksForWorld(world).add(data);
        // Можно сразу сохранять, или по команде/manual
    }

    public void clearBlocks(World world)
    {
        getBlocksForWorld(world).clear();
    }

    // --- Сохранение/загрузка ---
    public File getWorldBlocksFile(World world)
    {
        File worldDir = world.getWorldFolder();
        return new File(worldDir, "luckyblocks_mapped.yml");
    }

    public void loadBlocks(World world)
    {
        File file = getWorldBlocksFile(world);
        List<BlockData> out = new ArrayList<>();
        if (!file.exists())
        {
            worldBlocks.put(world.getName(), out);
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> list = cfg.getMapList("placed");
        for (Map<?, ?> entry : list)
        {
            try
            {
                int x = (int) entry.get("x");
                int y = (int) entry.get("y");
                int z = (int) entry.get("z");
                String type = (String) entry.get("type");
                int level = (int) entry.get("level");
                out.add(new BlockData(x, y, z, type, level));
            } catch (Exception e)
            {
                Bukkit.getLogger().log(Level.WARNING,
                        "[LuckyBlock] Ошибка загрузки точки лакиблока: " + e.getMessage());
            }
        }
        worldBlocks.put(world.getName(), out);
    }

    public void saveBlocks(World world)
    {
        File file = getWorldBlocksFile(world);
        YamlConfiguration cfg = new YamlConfiguration();
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (BlockData b : getBlocksForWorld(world))
        {
            Map<String, Object> m = new HashMap<>();
            m.put("x", b.x);
            m.put("y", b.y);
            m.put("z", b.z);
            m.put("type", b.type);
            m.put("level", b.level);
            serialized.add(m);
        }
        cfg.set("placed", serialized);
        // Опционально: сохранять min/max, если включено на карте
        try
        {
            cfg.save(file);
        } catch (IOException e)
        {
            Bukkit.getLogger().log(Level.SEVERE,
                    "[LuckyBlock] Не удалось сохранить luckyblocks_mapped.yml: " + e.getMessage());
        }
    }

    // --- Инициализация блоков на карте ---
    public void initBlocks(World world)
    {
        // Поставить лакиблоки из списка на карту
        ConfigManager configManager = null;
        // Попытка получить ConfigManager (ищем через активный плагин)
        org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LuckyBlocks");
        if (plugin instanceof LuckyBlockPlugin lbp)
        {
            configManager = lbp.getConfigManager();
        }
        for (BlockData data : getBlocksForWorld(world))
        {
            Location loc = new Location(world, data.x, data.y, data.z);
            // Получаем материал по типу из конфига
            org.bukkit.Material mat = org.bukkit.Material.GOLD_BLOCK;
            if (configManager != null)
            {
                org.bukkit.Material m = configManager.getLuckyBlockMaterial(data.type);
                if (m != null)
                    mat = m;
            }
            // Ставим блок на карте
            world.getBlockAt(loc).setType(mat);
        }
    }
}
