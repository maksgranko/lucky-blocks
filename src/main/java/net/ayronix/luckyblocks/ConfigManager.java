package net.ayronix.luckyblocks;

import java.util.Collections;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ConfigManager
{

    private final FileConfiguration config;
    private final LuckyBlockPlugin plugin;
    private final Map<String, Object> varsMap;

    public ConfigManager(LuckyBlockPlugin plugin)
    {
        this.plugin = plugin;
        this.config = plugin.getConfigFile();
        ConfigurationSection varsSection = config.getConfigurationSection("vars");
        if (varsSection != null)
        {
            varsMap = new HashMap<>();
            for (String key : varsSection.getKeys(false))
            {
                varsMap.put(key, varsSection.get(key));
            }
        } else
        {
            varsMap = java.util.Collections.emptyMap();
        }
        // После загрузки vars прогоняем всю конфигурацию кроме vars через
        // resolveVarsRecursive
        ConfigurationSection root = config;
        for (String key : root.getKeys(false))
        {
            if (!key.equals("vars"))
            {
                processSectionRecursive(root, key);
            }
        }
    }

    // Рекурсивно обновляет значения секции или ключа с учетом подстановки
    // переменных
    private void processSectionRecursive(ConfigurationSection section, String key)
    {
        Object raw = section.get(key);
        if (raw instanceof ConfigurationSection cs)
        {
            for (String sub : cs.getKeys(false))
            {
                processSectionRecursive(cs, sub);
            }
        } else
        {
            Object replaced = resolveVarsRecursive(raw);
            section.set(key, replaced);
        }
    }

    @SuppressWarnings("unchecked")
    private Object resolveVarsRecursive(Object value)
    {
        if (value == null)
            return null;
        // Полностью переменная
        if (value instanceof String s)
        {
            Pattern pattern = Pattern.compile("^\\$(\\w+)\\$$");
            Matcher matcher = pattern.matcher(s);
            if (matcher.matches())
            {
                String var = matcher.group(1);
                Object v = varsMap.get(var);
                // рекурсивно разворачиваем до "настоящего" значения
                return resolveVarsRecursive(v);
            }
            // Вхождения переменных внутри строки
            Pattern anyPattern = Pattern.compile("\\$(\\w+)\\$");
            Matcher anyMatcher = anyPattern.matcher(s);
            StringBuilder sb = new StringBuilder();
            int last = 0;
            while (anyMatcher.find())
            {
                String var = anyMatcher.group(1);
                Object v = varsMap.get(var);
                String str = v == null ? anyMatcher.group(0) : String.valueOf(resolveVarsRecursive(v));
                sb.append(s, last, anyMatcher.start());
                sb.append(str);
                last = anyMatcher.end();
            }
            sb.append(s.substring(last));
            return sb.toString();
        } else if (value instanceof List<?> list)
        {
            List<Object> out = new ArrayList<>(list.size());
            for (Object elem : list)
                out.add(resolveVarsRecursive(elem));
            return out;
        } else if (value instanceof Map<?, ?> map)
        {
            Map<Object, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet())
                out.put(entry.getKey(), resolveVarsRecursive(entry.getValue()));
            return out;
        }
        return value;
    }

    // Универсальные методы получения значений с обработкой переменных
    public String getStringRV(String path, String def)
    {
        Object raw = config.get(path, def);
        Object res = resolveVarsRecursive(raw);
        return res == null ? def : res.toString();
    }

    public int getIntRV(String path, int def)
    {
        Object raw = config.get(path, def);
        Object res = resolveVarsRecursive(raw);
        if (res instanceof Number n)
            return n.intValue();
        if (res instanceof String s)
        {
            try
            {
                return Integer.parseInt(s);
            } catch (Exception ignore)
            {
            }
        }
        return def;
    }

    public double getDoubleRV(String path, double def)
    {
        Object raw = config.get(path, def);
        Object res = resolveVarsRecursive(raw);
        if (res instanceof Number n)
            return n.doubleValue();
        if (res instanceof String s)
        {
            try
            {
                return Double.parseDouble(s);
            } catch (Exception ignore)
            {
            }
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getListRV(String path)
    {
        Object raw = config.get(path);
        Object res = resolveVarsRecursive(raw);
        if (res instanceof List<?> l)
            return new ArrayList<>(l);
        else if (res != null)
            return List.of(res);
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMapRV(String path)
    {
        Object raw = config.get(path);
        Object res = resolveVarsRecursive(raw);
        if (res instanceof Map<?, ?> m)
        {
            Map<String, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet())
                out.put(String.valueOf(e.getKey()), e.getValue());
            return out;
        }
        return Map.of();
    }

    public Set<String> getAvailableTypes()
    {
        ConfigurationSection typesSection = config.getConfigurationSection("types");
        if (typesSection == null)
        {
            plugin.getLogger().warning("Секция 'types' не найдена в luckyblocks.yml!");
            return Collections.emptySet();
        }
        return typesSection.getKeys(false);
    }

    @SuppressWarnings("LoggerStringConcat")
    public int getMinLevel(String type)
    {
        ConfigurationSection levelsSection = config.getConfigurationSection("types." + type + ".levels");
        if (levelsSection == null)
        {
            plugin.getLogger().warning("Секция 'types." + type + ".levels' не найдена!");
            return 1;
        }
        return levelsSection.getKeys(false).stream().filter(key -> key.matches("\\d+")).mapToInt(Integer::parseInt)
                .min().orElse(1);
    }

    @SuppressWarnings("LoggerStringConcat")
    public int getMaxLevel(String type)
    {
        ConfigurationSection levelsSection = config.getConfigurationSection("types." + type + ".levels");
        if (levelsSection == null)
        {
            plugin.getLogger().warning("Секция 'types." + type + ".levels' не найдена!");
            return 1;
        }
        return levelsSection.getKeys(false).stream().filter(key -> key.matches("\\d+")).mapToInt(Integer::parseInt)
                .max().orElse(1);
    }

    public boolean isValidLevel(String type, int level)
    {
        ConfigurationSection levelConfig = config.getConfigurationSection("types." + type + ".levels." + level);
        return levelConfig != null;
    }

    @SuppressWarnings("LoggerStringConcat")
    public Material getLuckyBlockMaterial(String type)
    {
        String materialName = config.getString("types." + type + ".item.material", "SPONGE");
        try
        {
            if (materialName != null)
                return Material.valueOf(materialName.toUpperCase());
            else
                return Material.SPONGE;
        } catch (IllegalArgumentException e)
        {
            plugin.getLogger()
                    .warning("Неверный материал '" + materialName + "' для типа '" + type + "'. Используется SPONGE.");
            return Material.SPONGE;
        }
    }

    public int getLuckyBlockCustomModelData(String type)
    {
        return config.getInt("types." + type + ".item.inventory-custom-model-data", 0);
    }

    /**
     * inventory-block из config. Возвращает Material или null, если не задан.
     */
    public Material getInventoryBlock(String type)
    {
        String block = config.getString("types." + type + ".item.inventory-block", null);
        if (block == null)
            return null;
        try
        {
            return Material.valueOf(block.toUpperCase());
        } catch (IllegalArgumentException e)
        {
            plugin.getLogger().warning("inventory-block '" + block + "' не распознан как материал.");
            return null;
        }
    }

    public String getLuckyBlockHead(String type)
    {
        return config.getString("types." + type + ".item.inventory-head", null);
    }

    public String getDisplayName(String type, int level)
    {
        return config.getString("types." + type + ".levels." + level + ".display-name", type + " Level " + level);
    }

    public ConfigurationSection getEventsSection(String type, int level)
    {
        return config.getConfigurationSection("types." + type + ".levels." + level + ".events");
    }

    public ConfigurationSection getEventConfig(String type, int level, String eventKey)
    {
        String[] split = eventKey.split("#");
        String key = split[0];
        int index = (split.length > 1) ? Integer.parseInt(split[1]) : -1;
        String path = "types." + type + ".levels." + level + ".events." + key;
        Object eventSectionObj = config.get(path);
        if (index >= 0 && eventSectionObj instanceof java.util.List<?> list)
        {
            Object entryObj = (index < list.size()) ? list.get(index) : null;
            if (entryObj instanceof org.bukkit.configuration.ConfigurationSection cs)
            {
                return cs;
            } else if (entryObj instanceof java.util.Map<?, ?> map)
            {
                // workaround для случаев, когда yaml парсит не как
                // ConfigurationSection
                // а как Map (сплошь и рядом при list-структуре)
                // создаем временную секцию, запихиваем туда map
                org.bukkit.configuration.MemoryConfiguration tempSection = new org.bukkit.configuration.MemoryConfiguration();
                map.forEach((k, v) -> tempSection.set(String.valueOf(k), v));
                return tempSection;
            }
            // если нет такого элемента, вернем null
            return null;
        } else
        {
            return config.getConfigurationSection(path);
        }
    }

    public FileConfiguration getRawConfig()
    {
        return config;
    }

    public LuckyBlockPlugin getPlugin()
    {
        return plugin;
    }

    /**
     * Получить все секции событий данного уровня для типа, с учётом
     * require-наследования и множителя весов (weightMultiplier). Для require с
     * синтаксисом 'name:multiplier' множитель применяется для всех событий из
     * require-наследия. Для "обычных" секций всегда multiplier=1.0.
     */
    public java.util.List<ConfigurationSection> getEffectiveEventSections(String type, int level,
            java.util.Set<String> visited, double weightMultiplier)
    {
        if (visited.contains(type))
        {
            plugin.getLogger()
                    .severe("Обнаружена циклическая зависимость require для типа: " + type + ". Конфиг сломан!");
            return Collections.emptyList();
        }
        visited.add(type);

        if (plugin.getDebug())
            plugin.getLogger().info(
                    "getEffectiveEventSections для типа: " + type + ", уровень: " + level + ", посещено: " + visited);

        java.util.List<ConfigurationSection> result = new java.util.ArrayList<>();
        ConfigurationSection events = getEventsSection(type, level);
        if (events != null)
        {
            for (String key : events.getKeys(false))
            {
                Object value = events.get(key);

                // Если значение - список (несколько событий такого типа)
                if (value instanceof java.util.List<?> cfgList)
                {
                    int idx = 0;
                    for (Object obj : cfgList)
                    {
                        if (obj instanceof java.util.Map<?, ?> map)
                        {
                            org.bukkit.configuration.MemoryConfiguration tempSection = new org.bukkit.configuration.MemoryConfiguration();
                            map.forEach((k, v) -> tempSection.set(String.valueOf(k), v));
                            tempSection.set("__event_key__", key);
                            tempSection.set("__event_index__", idx);
                            if (weightMultiplier != 1.0)
                            {
                                int baseWeight = tempSection.getInt("weight", 1);
                                tempSection.set("weight", Math.max(1, (int) Math.round(baseWeight * weightMultiplier)));
                            }
                            result.add(tempSection);
                        }
                        idx++;
                    }
                }
                // Если значение - ConfigurationSection (одиночное событие)
                else if (value instanceof ConfigurationSection ev)
                {
                    // копируем если нужно множитель, иначе добавляем напрямую
                    if (weightMultiplier != 1.0)
                    {
                        org.bukkit.configuration.MemoryConfiguration copy = new org.bukkit.configuration.MemoryConfiguration();
                        for (String k : ev.getKeys(false))
                            copy.set(k, ev.get(k));
                        copy.set("__event_key__", key);
                        copy.set("__event_index__", 0);
                        int baseWeight = ev.getInt("weight", 1);
                        copy.set("weight", Math.max(1, (int) Math.round(baseWeight * weightMultiplier)));
                        result.add(copy);
                    } else
                    {
                        ev.set("__event_key__", key);
                        ev.set("__event_index__", 0);
                        result.add(ev);
                    }
                }
                // Если значение - карта, но не секция (бывает при yaml)
                else if (value instanceof java.util.Map<?, ?> map)
                {
                    org.bukkit.configuration.MemoryConfiguration tempSection = new org.bukkit.configuration.MemoryConfiguration();
                    map.forEach((k, v) -> tempSection.set(String.valueOf(k), v));
                    tempSection.set("__event_key__", key);
                    tempSection.set("__event_index__", 0);
                    if (weightMultiplier != 1.0)
                    {
                        int baseWeight = tempSection.getInt("weight", 1);
                        tempSection.set("weight", Math.max(1, (int) Math.round(baseWeight * weightMultiplier)));
                    }
                    result.add(tempSection);
                }
            }
        }
        // Поддержка одиночных require и списковых
        java.util.List<String> requires;
        Object requireObj = config.get("types." + type + ".require");
        if (requireObj instanceof java.util.List<?> reqList)
        {
            requires = new java.util.ArrayList<>();
            for (Object o : reqList)
            {
                if (o != null)
                    requires.add(String.valueOf(o));
            }
        } else if (requireObj instanceof String str)
        {
            requires = java.util.Collections.singletonList(str);
        } else
        {
            requires = java.util.Collections.emptyList();
        }
        for (String parent : requires)
        {
            String parentType = parent;
            double multiplier = 1.0;
            int idx = parent.indexOf(':');
            if (idx > 0 && idx + 1 < parent.length())
            {
                parentType = parent.substring(0, idx).trim();
                try
                {
                    multiplier = Double.parseDouble(parent.substring(idx + 1));
                } catch (NumberFormatException e)
                {
                    multiplier = 1.0;
                }
            }
            result.addAll(getEffectiveEventSections(parentType, level, visited, weightMultiplier * multiplier));
        }
        visited.remove(type);
        if (plugin.getDebug())
            plugin.getLogger().info("getEffectiveEventSections для типа: " + type + ", уровень: " + level
                    + ", итоговый список событий: " + result.size());
        return result;
    }

    /**
     * Получить список всех доступных уровней для типа с учётом
     * require-наследования. Возвращает уникальный список всех номеров уровней.
     */
    public java.util.Set<Integer> getEffectiveLevels(String type, java.util.Set<String> visited)
    {
        if (visited.contains(type))
        {
            plugin.getLogger()
                    .severe("Обнаружена циклическая зависимость require для типа: " + type + ". Конфиг сломан!");
            return Collections.emptySet();
        }
        visited.add(type);

        java.util.Set<Integer> levels = new java.util.HashSet<>();
        ConfigurationSection levelsSection = config.getConfigurationSection("types." + type + ".levels");
        if (levelsSection != null)
        {
            for (String key : levelsSection.getKeys(false))
            {
                if (key.matches("\\d+"))
                {
                    levels.add(Integer.parseInt(key));
                }
            }
        }
        java.util.List<String> requires = config.getStringList("types." + type + ".require");
        for (String parent : requires)
        {
            levels.addAll(getEffectiveLevels(parent, visited));
        }
        visited.remove(type);
        return levels;
    }
}
