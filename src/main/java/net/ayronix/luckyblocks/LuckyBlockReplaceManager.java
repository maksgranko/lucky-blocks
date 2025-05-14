package net.ayronix.luckyblocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;

public class LuckyBlockReplaceManager
{
    // Хранит координаты разрешённых к замене лаки-блоков (world:x:y:z)
    private static final Set<String> allowReplaceSet = Collections.synchronizedSet(new HashSet<>());

    public static String locToKey(Location loc)
    {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public static void allow(Location loc)
    {
        allowReplaceSet.add(locToKey(loc));
    }

    public static void remove(Location loc)
    {
        allowReplaceSet.remove(locToKey(loc));
    }

    public static boolean mayReplace(Location loc)
    {
        return allowReplaceSet.contains(locToKey(loc));
    }

    // Можно вызывать раз в 1-2 тика для очистки (если потребуется)
    public static void clear()
    {
        allowReplaceSet.clear();
    }
}
