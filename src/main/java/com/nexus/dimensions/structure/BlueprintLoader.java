/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.Plugin
 */
package com.nexus.dimensions.structure;

import com.nexus.dimensions.structure.Blueprint;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class BlueprintLoader {
    private final Plugin plugin;
    private final Logger logger;

    public BlueprintLoader(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public Map<String, Blueprint> loadAll() {
        File[] files;
        LinkedHashMap<String, Blueprint> blueprints = new LinkedHashMap<String, Blueprint>();
        File dir = new File(this.plugin.getDataFolder(), "blueprints");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if ((files = dir.listFiles((d, name) -> name.endsWith(".yml") || name.endsWith(".yaml"))) == null) {
            return blueprints;
        }
        for (File file : files) {
            try {
                Blueprint blueprint = this.parse(file);
                if (blueprint == null) continue;
                blueprints.put(blueprint.name, blueprint);
                this.logger.info("[NexusDimensions] Loaded blueprint '" + blueprint.name + "' (" + blueprint.blocks.size() + " blocks)");
            }
            catch (Exception e) {
                this.logger.severe("[NexusDimensions] Failed to parse blueprint " + file.getName() + ": " + e.getMessage());
            }
        }
        return blueprints;
    }

    private Blueprint parse(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)file);
        Blueprint blueprint = new Blueprint();
        blueprint.name = yaml.getString("name");
        if (blueprint.name == null || blueprint.name.isBlank()) {
            String fileName = file.getName();
            blueprint.name = fileName.substring(0, fileName.lastIndexOf(46));
        }
        List raw = yaml.getMapList("blocks");
        ArrayList<Blueprint.BlockEntry> entries = new ArrayList<Blueprint.BlockEntry>();
        for (Map m : raw) {
            Object lootObj;
            Blueprint.BlockEntry entry = new Blueprint.BlockEntry();
            entry.dx = BlueprintLoader.intOf(m.get("dx"), 0);
            entry.dy = BlueprintLoader.intOf(m.get("dy"), 0);
            entry.dz = BlueprintLoader.intOf(m.get("dz"), 0);
            Object blockObj = m.get("block");
            if (blockObj != null) {
                entry.block = blockObj.toString();
            }
            if ((lootObj = m.get("loot")) instanceof Boolean) {
                Boolean b = (Boolean)lootObj;
                entry.loot = b;
            }
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            this.logger.warning("[NexusDimensions] Blueprint '" + blueprint.name + "' has no blocks \u2014 check the 'blocks' list in " + file.getName() + ".");
        }
        blueprint.blocks = entries;
        return blueprint;
    }

    private static int intOf(Object o, int def) {
        int n;
        if (o instanceof Number) {
            Number n2 = (Number)o;
            n = n2.intValue();
        } else {
            n = def;
        }
        return n;
    }
}
