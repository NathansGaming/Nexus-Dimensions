/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 */
package com.nexus.dimensions.datapack;

import com.nexus.dimensions.config.DimensionPreset;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class DatapackGenerator {
    private final int packFormat;
    private final Logger logger;

    public DatapackGenerator(Logger logger, int packFormat) {
        this.logger = logger;
        this.packFormat = packFormat;
    }

    public File resolveDatapacksDir() {
        if (Bukkit.getWorlds().isEmpty()) {
            return null;
        }
        File primaryWorldFolder = ((World)Bukkit.getWorlds().get(0)).getWorldFolder();
        return new File(primaryWorldFolder, "datapacks");
    }

    public boolean writeDatapack(DimensionPreset preset) {
        File datapacksDir = this.resolveDatapacksDir();
        if (datapacksDir == null) {
            this.logger.warning("[NexusDimensions] Cannot resolve the datapacks folder yet (no world loaded).");
            return false;
        }
        String packName = "nexus_" + preset.id;
        File packDir = new File(datapacksDir, packName);
        File dataDir = new File(packDir, "data/nexus");
        try {
            Files.createDirectories(dataDir.toPath(), new FileAttribute[0]);
            this.write(new File(packDir, "pack.mcmeta"), this.packMcMeta(preset));
            File dimensionTypeDir = new File(dataDir, "dimension_type");
            Files.createDirectories(dimensionTypeDir.toPath(), new FileAttribute[0]);
            this.write(new File(dimensionTypeDir, preset.id + ".json"), this.dimensionType(preset));
            File dimensionDir = new File(dataDir, "dimension");
            Files.createDirectories(dimensionDir.toPath(), new FileAttribute[0]);
            this.write(new File(dimensionDir, preset.id + ".json"), this.dimensionEntry(preset));
            if (!preset.customBiomes.isEmpty()) {
                File biomeDir = new File(dataDir, "worldgen/biome");
                Files.createDirectories(biomeDir.toPath(), new FileAttribute[0]);
                for (DimensionPreset.CustomBiome cb : preset.customBiomes) {
                    String path = cb.id.contains(":") ? cb.id.substring(cb.id.indexOf(58) + 1) : cb.id;
                    this.write(new File(biomeDir, path + ".json"), this.biome(cb));
                }
            }
            this.logger.info("[NexusDimensions] Wrote/refreshed datapack for '" + preset.id + "' \u2014 a server restart is required for it to take effect.");
            return true;
        }
        catch (IOException e) {
            this.logger.severe("[NexusDimensions] Failed writing datapack for '" + preset.id + "': " + e.getMessage());
            return false;
        }
    }

    private void write(File file, String content) throws IOException {
        Files.writeString(file.toPath(), (CharSequence)content, StandardCharsets.UTF_8, new OpenOption[0]);
    }

    private String packMcMeta(DimensionPreset preset) {
        return "{\n  \"pack\": {\n    \"pack_format\": %d,\n    \"description\": \"Nexus Dimensions - generated dimension type for %s\"\n  }\n}\n".formatted(this.packFormat, preset.id);
    }

    private String dimensionType(DimensionPreset preset) {
        DimensionPreset.WorldHeight wh = preset.resolvedWorldHeight();
        String fixedTime = wh.fixedTime != null ? "\n  \"fixed_time\": " + wh.fixedTime + "," : "";
        String infiniburn = wh.ultrawarm ? "#minecraft:infiniburn_nether" : "#minecraft:infiniburn_overworld";
        return "{\n  \"ultrawarm\": %b,\n  \"natural\": %b,\n  \"coordinate_scale\": 1.0,\n  \"has_skylight\": %b,\n  \"has_ceiling\": %b,\n  \"ambient_light\": %s,%s\n  \"monster_spawn_light_level\": 7,\n  \"monster_spawn_block_light_limit\": 0,\n  \"piglin_safe\": %b,\n  \"bed_works\": %b,\n  \"respawn_anchor_works\": %b,\n  \"has_raids\": %b,\n  \"logical_height\": %d,\n  \"min_y\": %d,\n  \"height\": %d,\n  \"infiniburn\": \"%s\",\n  \"effects\": \"%s\"\n}\n".formatted(wh.ultrawarm, wh.natural, wh.hasSkylight, wh.hasCeiling, wh.ambientLight, fixedTime, wh.piglinSafe, wh.bedWorks, wh.respawnAnchorWorks, wh.hasRaids, wh.height, wh.minY, wh.height, infiniburn, wh.effects);
    }

    private String dimensionEntry(DimensionPreset preset) {
        DimensionPreset.WorldHeight wh = preset.resolvedWorldHeight();
        String baseBiome = preset.biomes.entries.isEmpty() ? "minecraft:the_void" : preset.biomes.entries.get((int)0).id;
        String surface = preset.palette.surfaceBlock;
        String subsurface = preset.palette.subsurfaceBlock;
        String deep = preset.palette.deepBlock;
        return "{\n  \"type\": \"nexus:%s\",\n  \"generator\": {\n    \"type\": \"minecraft:flat\",\n    \"settings\": {\n      \"layers\": [\n        { \"block\": \"%s\", \"height\": %d },\n        { \"block\": \"%s\", \"height\": 1 },\n        { \"block\": \"%s\", \"height\": 1 }\n      ],\n      \"biome\": \"%s\"\n    }\n  }\n}\n".formatted(preset.id, deep, Math.max(1, preset.terrain.baseHeight - wh.minY - 2), subsurface, surface, baseBiome);
    }

    private String biome(DimensionPreset.CustomBiome cb) {
        double skyColor = DatapackGenerator.parseColor(cb.skyColor);
        double fogColor = DatapackGenerator.parseColor(cb.fogColor);
        double waterColor = DatapackGenerator.parseColor(cb.waterColor);
        double waterFogColor = DatapackGenerator.parseColor(cb.waterFogColor);
        return "{\n  \"temperature\": %s,\n  \"downfall\": %s,\n  \"has_precipitation\": true,\n  \"effects\": {\n    \"sky_color\": %d,\n    \"fog_color\": %d,\n    \"water_color\": %d,\n    \"water_fog_color\": %d,\n    \"mood_sound\": {\n      \"sound\": \"minecraft:ambient.cave\",\n      \"tick_delay\": 6000,\n      \"block_search_extent\": 8,\n      \"offset\": 2.0\n    }\n  },\n  \"carvers\": [],\n  \"features\": [[], [], [], [], [], [], [], [], [], [], []],\n  \"spawners\": { \"monster\": [], \"creature\": [], \"ambient\": [], \"water_ambient\": [], \"water_creature\": [], \"underground_water_creature\": [], \"misc\": [] },\n  \"spawn_costs\": {}\n}\n".formatted(cb.temperature, cb.downfall, (long)skyColor, (long)fogColor, (long)waterColor, (long)waterFogColor);
    }

    private static double parseColor(String hex) {
        if (hex == null) {
            return 0.0;
        }
        String cleaned = hex.trim().toLowerCase(Locale.ROOT);
        try {
            if (cleaned.startsWith("0x")) {
                return Long.parseLong(cleaned.substring(2), 16);
            }
            if (cleaned.startsWith("#")) {
                return Long.parseLong(cleaned.substring(1), 16);
            }
            return Long.parseLong(cleaned);
        }
        catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
