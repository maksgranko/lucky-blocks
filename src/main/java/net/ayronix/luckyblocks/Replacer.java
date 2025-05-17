package net.ayronix.luckyblocks;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Утилита для шаблонных замен (%player%, @p, %x%, %y%, %z%, ~, ~~ и др.)
 */
public class Replacer
{

    /**
     * Выполнить стандартные подстановки в строке-команде.
     * 
     * @param template строка с шаблонами
     * @param player игрок
     * @param loc локация (или null, тогда координаты не будут заменены)
     * @return строка с заменёнными плейсхолдерами
     */
    public static String apply(String template, Player player, Location loc)
    {
        String cmd = template;
        if (player != null)
        {
            cmd = cmd.replace("%player%", player.getName()).replace("@p", player.getName());
        }
        if (loc != null)
        {
            cmd = cmd.replace("%x%", String.valueOf(loc.getBlockX())).replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ())).replace("~", String.valueOf(loc.getBlockX()))
                    .replace("~~", String.valueOf(loc.getBlockY())).replace("~~~", String.valueOf(loc.getBlockZ()));
        }

        // Поддержка %random:int:int% и %random:float:float%
        cmd = replaceRandom(cmd);

        return cmd;
    }

    private static String replaceRandom(String input)
    {
        String result = input;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("%random:([\\.\\d]+):([\\.\\d]+)%");
        java.util.regex.Matcher matcher = pattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        java.util.Random rand = new java.util.Random();
        while (matcher.find())
        {
            String left = matcher.group(1);
            String right = matcher.group(2);
            String replacement = matcher.group(0);
            try
            {
                if (left.contains(".") || right.contains("."))
                {
                    double min = Double.parseDouble(left);
                    double max = Double.parseDouble(right);
                    if (min > max)
                    {
                        double tmp = min;
                        min = max;
                        max = tmp;
                    }
                    double rnd = min + rand.nextDouble() * (max - min);
                    replacement = String.valueOf(rnd);
                } else
                {
                    int min = Integer.parseInt(left);
                    int max = Integer.parseInt(right);
                    if (min > max)
                    {
                        int tmp = min;
                        min = max;
                        max = tmp;
                    }
                    int rnd = rand.nextInt(max - min + 1) + min;
                    replacement = String.valueOf(rnd);
                }
            } catch (Exception e)
            {
                // оставим исходную строку для этого плейсхолдера
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
