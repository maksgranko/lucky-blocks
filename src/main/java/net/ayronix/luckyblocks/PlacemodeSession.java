package net.ayronix.luckyblocks;

import java.util.UUID;

import org.bukkit.entity.Player;

public class PlacemodeSession
{
    private final UUID uuid;
    private final String playerName; // можно использовать для debug
    private String type;
    private int level;
    private int minLevel;
    private int maxLevel;

    // Слот для сохранения хотбара (0-8) — заполняется PlacemodeManager
    public org.bukkit.inventory.ItemStack[] savedHotbar = null;

    // Дополнительно можно хранить старый инвентарь, хотбар и т.д.

    public PlacemodeSession(Player player, String type, int level, int minLevel, int maxLevel)
    {
        this.uuid = player.getUniqueId();
        this.playerName = player.getName();
        this.type = type;
        this.level = level;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public String getType()
    {
        return type;
    }

    public int getLevel()
    {
        return level;
    }

    public int getMinLevel()
    {
        return minLevel;
    }

    public int getMaxLevel()
    {
        return maxLevel;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setLevel(int level)
    {
        this.level = level;
    }

    public void setMinLevel(int minLevel)
    {
        this.minLevel = minLevel;
    }

    public void setMaxLevel(int maxLevel)
    {
        this.maxLevel = maxLevel;
    }
}
