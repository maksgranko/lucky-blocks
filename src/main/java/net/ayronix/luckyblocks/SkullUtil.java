package net.ayronix.luckyblocks;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class SkullUtil
{
    /**
     * Создать кастомную голову по base64 или имени игрока.
     */
    public static ItemStack createHead(String head, String displayName, int customModelData)
    {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (head != null && !head.isEmpty() && meta != null)
        {
            String trimmed = head.trim();
            boolean isBase64 = isBase64Like(trimmed);
            boolean isUrl = trimmed.startsWith("http");

            PlayerProfile profile;

            if (isBase64)
            {
                profile = Bukkit.createProfile(UUID.randomUUID());
                profile.setProperty(new ProfileProperty("textures", trimmed));
                meta.setPlayerProfile(profile);
            } else if (isUrl)
            {
                // поддержка ссылок (если потребуется): URL -> base64 внести
                // отдельно
                profile = Bukkit.createProfile(UUID.randomUUID());
                profile.setProperty(new ProfileProperty("textures", trimmed));
                meta.setPlayerProfile(profile);
            } else
            {
                profile = Bukkit.createProfile(trimmed);
                meta.setPlayerProfile(profile);
            }
        }

        if (displayName != null && meta != null)
        {
            meta.setDisplayName(displayName);
        }
        if (customModelData > 0 && meta != null)
        {
            meta.setCustomModelData(customModelData);
        }
        if (meta != null)
        {
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private static boolean isBase64Like(String value)
    {
        // base64 текстуры головы — обычно 80+ символов, без пробелов и не похож
        // на UUID/url
        return value.length() >= 60 && !value.contains(" ") && !value.contains("http")
                && !value.matches("[a-fA-F0-9\\-]{32,36}");
    }
}
