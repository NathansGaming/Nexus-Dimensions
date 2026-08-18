/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.World$Environment
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.EntityType
 *  org.bukkit.plugin.Plugin
 */
package com.nexus.dimensions.config;

import com.nexus.dimensions.config.DimensionPreset;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

public final class PresetLoader {
    private final Plugin plugin;
    private final Logger logger;

    public PresetLoader(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public Map<String, DimensionPreset> loadAll() {
        File[] files;
        LinkedHashMap<String, DimensionPreset> presets = new LinkedHashMap<String, DimensionPreset>();
        File presetsDir = new File(this.plugin.getDataFolder(), "presets");
        if (!presetsDir.exists()) {
            presetsDir.mkdirs();
        }
        if ((files = presetsDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"))) == null || files.length == 0) {
            this.logger.warning("[NexusDimensions] No presets found in " + presetsDir.getPath() + " \u2014 drop a .yml file in there, or run /nexusdim reload after adding one.");
            return presets;
        }
        for (File file : files) {
            try {
                DimensionPreset preset = this.parse(file);
                if (preset == null) continue;
                presets.put(preset.id, preset);
                this.logger.info("[NexusDimensions] Loaded preset '" + preset.id + "' (" + (preset.isTier2() ? "Tier 2 - datapack" : "Tier 1 - instant") + ")");
            }
            catch (Exception e) {
                this.logger.severe("[NexusDimensions] Failed to parse preset " + file.getName() + ": " + e.getMessage());
            }
        }
        return presets;
    }

    private DimensionPreset parse(File file) {
        ConfigurationSection particlesSec;
        ConfigurationSection flavorSec;
        ConfigurationSection seasonsSec;
        Object equipSec;
        String normalized;
        ConfigurationSection creaturesSec;
        ConfigurationSection structuresSec;
        ConfigurationSection decorationsSec;
        ConfigurationSection treesSec;
        List rawCustomBiomes;
        ConfigurationSection biomesSec;
        ConfigurationSection paletteSec;
        Number number;
        ConfigurationSection terrainSec;
        ConfigurationSection s2;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)file);
        DimensionPreset preset = new DimensionPreset();
        preset.id = yaml.getString("id");
        if (preset.id == null || preset.id.isBlank()) {
            String name = file.getName();
            preset.id = name.substring(0, name.lastIndexOf(46));
            this.logger.warning("[NexusDimensions] " + file.getName() + " has no 'id' field, using filename: " + preset.id);
        }
        preset.displayName = yaml.getString("displayName", preset.id);
        String envStr = yaml.getString("environment", "NORMAL");
        try {
            preset.environment = World.Environment.valueOf((String)envStr.toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': unknown environment '" + envStr + "', defaulting to NORMAL");
            preset.environment = World.Environment.NORMAL;
        }
        if (yaml.contains("seed")) {
            preset.seed = yaml.getLong("seed");
        }
        if (yaml.isConfigurationSection("worldHeight")) {
            s2 = yaml.getConfigurationSection("worldHeight");
            DimensionPreset.WorldHeight wh = new DimensionPreset.WorldHeight();
            wh.minY = s2.getInt("minY", wh.minY);
            wh.height = s2.getInt("height", wh.height);
            wh.hasCeiling = s2.getBoolean("hasCeiling", wh.hasCeiling);
            wh.hasSkylight = s2.getBoolean("hasSkylight", wh.hasSkylight);
            wh.ambientLight = s2.getDouble("ambientLight", wh.ambientLight);
            wh.effects = s2.getString("effects", wh.effects);
            if (s2.contains("fixedTime")) {
                wh.fixedTime = s2.getLong("fixedTime");
            }
            wh.ultrawarm = s2.getBoolean("ultrawarm", wh.ultrawarm);
            wh.natural = s2.getBoolean("natural", wh.natural);
            wh.piglinSafe = s2.getBoolean("piglinSafe", wh.piglinSafe);
            wh.bedWorks = s2.getBoolean("bedWorks", wh.bedWorks);
            wh.respawnAnchorWorks = s2.getBoolean("respawnAnchorWorks", wh.respawnAnchorWorks);
            wh.hasRaids = s2.getBoolean("hasRaids", wh.hasRaids);
            if (wh.height % 16 != 0 || wh.minY % 16 != 0) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': worldHeight.minY and height should be multiples of 16 (vanilla dimension-type rule).");
            }
            if (wh.minY + wh.height > 2032) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': minY + height exceeds vanilla's 2032 registry cap; the datapack may fail to load.");
            }
            preset.worldHeight = wh;
        }
        if (yaml.isConfigurationSection("sky")) {
            s2 = yaml.getConfigurationSection("sky");
            DimensionPreset.Sky sky = new DimensionPreset.Sky();
            sky.effects = s2.getString("effects", sky.effects);
            if (!(sky.effects.equals("minecraft:overworld") || sky.effects.equals("minecraft:the_nether") || sky.effects.equals("minecraft:the_end"))) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': sky.effects '" + sky.effects + "' isn't one of vanilla's three valid values (minecraft:overworld / minecraft:the_nether / minecraft:the_end) \u2014 the client will likely reject or ignore it.");
            }
            if (s2.contains("fixedTime")) {
                sky.fixedTime = s2.getLong("fixedTime");
            }
            preset.sky = sky;
        }
        if ((terrainSec = yaml.getConfigurationSection("terrain")) != null) {
            Object caveSec;
            ConfigurationSection craterSec;
            ConfigurationSection noiseSec;
            DimensionPreset.Terrain t = preset.terrain;
            t.mode = terrainSec.getString("mode", t.mode);
            if (!t.mode.equalsIgnoreCase("heightmap") && !t.mode.equalsIgnoreCase("density3d")) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': terrain.mode '" + t.mode + "' isn't 'heightmap' or 'density3d', defaulting to heightmap.");
                t.mode = "heightmap";
            }
            t.seaLevel = terrainSec.getInt("seaLevel", t.seaLevel);
            t.baseHeight = terrainSec.getInt("baseHeight", t.baseHeight);
            t.heightVariation = terrainSec.getInt("heightVariation", t.heightVariation);
            ConfigurationSection density3dSec = terrainSec.getConfigurationSection("density3d");
            if (density3dSec != null) {
                DimensionPreset.Density3D d = t.density3d;
                d.threshold = density3dSec.getDouble("threshold", d.threshold);
                d.verticalFalloff = density3dSec.getDouble("verticalFalloff", d.verticalFalloff);
                d.shape = density3dSec.getString("shape", d.shape);
                if (!d.shape.equalsIgnoreCase("bands") && !d.shape.equalsIgnoreCase("spires")) {
                    this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': terrain.density3d.shape '" + d.shape + "' isn't 'bands' or 'spires', defaulting to bands.");
                    d.shape = "bands";
                }
                d.spireFrequency = density3dSec.getDouble("spireFrequency", d.spireFrequency);
                d.spireJitter = density3dSec.getDouble("spireJitter", d.spireJitter);
                d.spireCoreFraction = density3dSec.getDouble("spireCoreFraction", d.spireCoreFraction);
                d.spireStrength = density3dSec.getDouble("spireStrength", d.spireStrength);
                d.liquids = density3dSec.getBoolean("liquids", d.liquids);
                List rawBands = density3dSec.getMapList("bands");
                if (!rawBands.isEmpty()) {
                    ArrayList<DimensionPreset.Band> bands = new ArrayList<DimensionPreset.Band>();
                    for (Map raw : rawBands) {
                        DimensionPreset.Band band = new DimensionPreset.Band();
                        Object centerObj = raw.get("center");
                        Object thicknessObj = raw.get("thickness");
                        if (centerObj instanceof Number) {
                            number = (Number)centerObj;
                            band.center = number.intValue();
                        }
                        if (thicknessObj instanceof Number) {
                            number = (Number)thicknessObj;
                            band.thickness = number.intValue();
                        }
                        bands.add(band);
                    }
                    d.bands = bands;
                }
            }
            if (t.isDensity3D() && t.craters.enabled) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': terrain.craters is a heightmap-only concept and is ignored in density3d mode (overhangs/holes come from the 3D noise itself - tune terrain.noise/density3d instead).");
            }
            if ((noiseSec = terrainSec.getConfigurationSection("noise")) != null) {
                DimensionPreset.Noise n = t.noise;
                n.frequency = noiseSec.getDouble("frequency", n.frequency);
                n.octaves = noiseSec.getInt("octaves", n.octaves);
                n.lacunarity = noiseSec.getDouble("lacunarity", n.lacunarity);
                n.gain = noiseSec.getDouble("gain", n.gain);
                n.ridged = noiseSec.getBoolean("ridged", n.ridged);
                n.warp = noiseSec.getDouble("warp", n.warp);
            }
            if ((craterSec = terrainSec.getConfigurationSection("craters")) != null) {
                DimensionPreset.Craters c = t.craters;
                c.enabled = craterSec.getBoolean("enabled", c.enabled);
                c.frequency = craterSec.getDouble("frequency", c.frequency);
                c.depth = craterSec.getInt("depth", c.depth);
                c.rimHeight = craterSec.getInt("rimHeight", c.rimHeight);
                c.jitter = craterSec.getDouble("jitter", c.jitter);
            }
            if ((caveSec = terrainSec.getConfigurationSection("caves")) != null) {
                DimensionPreset.Caves c = t.caves;
                c.enabled = caveSec.getBoolean("enabled", c.enabled);
                c.frequency = caveSec.getDouble("frequency", c.frequency);
                c.threshold = caveSec.getDouble("threshold", c.threshold);
                c.mode = caveSec.getString("mode", c.mode);
                if (!c.mode.equalsIgnoreCase("noise") && !c.mode.equalsIgnoreCase("cellular")) {
                    this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': caves.mode '" + c.mode + "' isn't 'noise' or 'cellular', defaulting to noise.");
                    c.mode = "noise";
                }
                c.cellularThreshold = caveSec.getDouble("cellularThreshold", c.cellularThreshold);
                c.cellularJitter = caveSec.getDouble("cellularJitter", c.cellularJitter);
            }
            if (t.isDensity3D() && t.caves.enabled) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': terrain.caves is a heightmap-only concept and is ignored in density3d mode (caves/overhangs come from the 3D noise itself there).");
            }
        }
        if ((paletteSec = yaml.getConfigurationSection("palette")) != null) {
            List rawDeposits;
            DimensionPreset.Palette p = preset.palette;
            p.surfaceBlock = paletteSec.getString("surfaceBlock", p.surfaceBlock);
            p.subsurfaceBlock = paletteSec.getString("subsurfaceBlock", p.subsurfaceBlock);
            p.subsurfaceDepth = paletteSec.getInt("subsurfaceDepth", p.subsurfaceDepth);
            p.deepBlock = paletteSec.getString("deepBlock", p.deepBlock);
            p.liquidBlock = paletteSec.getString("liquidBlock", p.liquidBlock);
            p.liquidLevel = paletteSec.getInt("liquidLevel", p.liquidLevel);
            List rawVariants = paletteSec.getMapList("variants");
            if (!rawVariants.isEmpty()) {
                ArrayList<DimensionPreset.PaletteVariant> variants = new ArrayList<DimensionPreset.PaletteVariant>();
                for (Iterator raw : rawVariants) {
                    DimensionPreset.PaletteVariant v = new DimensionPreset.PaletteVariant();
                    v.name = PresetLoader.str(raw.get("name"), v.name);
                    Object surfaceObj = raw.get("surfaceBlock");
                    v.surfaceBlock = surfaceObj != null ? surfaceObj.toString() : null;
                    Object subsurfaceObj = raw.get("subsurfaceBlock");
                    v.subsurfaceBlock = subsurfaceObj != null ? subsurfaceObj.toString() : null;
                    v.frequency = PresetLoader.num(raw.get("frequency"), v.frequency);
                    v.threshold = PresetLoader.num(raw.get("threshold"), v.threshold);
                    if (v.surfaceBlock == null && v.subsurfaceBlock == null) {
                        this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': palette.variants entry '" + v.name + "' sets neither surfaceBlock nor subsurfaceBlock - it can never do anything, skipped.");
                        continue;
                    }
                    variants.add(v);
                }
                p.variants = variants;
            }
            if (!(rawDeposits = paletteSec.getMapList("glowDeposits")).isEmpty()) {
                ArrayList<DimensionPreset.GlowDeposit> deposits = new ArrayList<DimensionPreset.GlowDeposit>();
                for (Map raw : rawDeposits) {
                    DimensionPreset.GlowDeposit dep = new DimensionPreset.GlowDeposit();
                    dep.block = PresetLoader.str(raw.get("block"), dep.block);
                    dep.frequency = PresetLoader.num(raw.get("frequency"), dep.frequency);
                    dep.threshold = PresetLoader.num(raw.get("threshold"), dep.threshold);
                    deposits.add(dep);
                }
                p.glowDeposits = deposits;
            }
        }
        if ((biomesSec = yaml.getConfigurationSection("biomes")) != null) {
            DimensionPreset.Biomes b = preset.biomes;
            b.mode = biomesSec.getString("mode", b.mode);
            List rawEntries = biomesSec.getMapList("entries");
            if (!rawEntries.isEmpty()) {
                ArrayList entries = new ArrayList();
                for (Map raw : rawEntries) {
                    Object weightObj;
                    DimensionPreset.BiomeEntry entry = new DimensionPreset.BiomeEntry();
                    Object idObj = raw.get("id");
                    if (idObj != null) {
                        entry.id = idObj.toString();
                    }
                    if ((weightObj = raw.get("weight")) instanceof Number) {
                        number = (Number)weightObj;
                        entry.weight = number.doubleValue();
                    }
                    entries.add(entry);
                }
                b.entries = entries;
            }
        }
        if (!(rawCustomBiomes = yaml.getMapList("customBiomes")).isEmpty()) {
            ArrayList<DimensionPreset.CustomBiome> customBiomes = new ArrayList<DimensionPreset.CustomBiome>();
            for (Iterator raw : rawCustomBiomes) {
                DimensionPreset.CustomBiome cb = new DimensionPreset.CustomBiome();
                cb.id = PresetLoader.str(raw.get("id"), null);
                cb.category = PresetLoader.str(raw.get("category"), cb.category);
                cb.temperature = PresetLoader.num(raw.get("temperature"), cb.temperature);
                cb.downfall = PresetLoader.num(raw.get("downfall"), cb.downfall);
                cb.skyColor = PresetLoader.str(raw.get("skyColor"), cb.skyColor);
                cb.fogColor = PresetLoader.str(raw.get("fogColor"), cb.fogColor);
                cb.waterColor = PresetLoader.str(raw.get("waterColor"), cb.waterColor);
                cb.waterFogColor = PresetLoader.str(raw.get("waterFogColor"), cb.waterFogColor);
                if (cb.id != null) {
                    customBiomes.add(cb);
                    continue;
                }
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': customBiomes entry missing 'id', skipped.");
            }
            preset.customBiomes = customBiomes;
        }
        if ((treesSec = yaml.getConfigurationSection("trees")) != null) {
            DimensionPreset.Trees tr = preset.trees;
            tr.enabled = treesSec.getBoolean("enabled", tr.enabled);
            tr.minHeight = treesSec.getInt("minHeight", tr.minHeight);
            tr.maxHeight = treesSec.getInt("maxHeight", tr.maxHeight);
            tr.canopyRadius = treesSec.getInt("canopyRadius", tr.canopyRadius);
            tr.trunkBlock = treesSec.getString("trunkBlock", tr.trunkBlock);
            tr.leafBlock = treesSec.getString("leafBlock", tr.leafBlock);
            tr.rarityPerChunk = treesSec.getDouble("rarityPerChunk", tr.rarityPerChunk);
            tr.giantCanopyLayers = treesSec.getInt("giantCanopyLayers", tr.giantCanopyLayers);
            tr.branches = treesSec.getBoolean("branches", tr.branches);
            tr.buttressRoots = treesSec.getBoolean("buttressRoots", tr.buttressRoots);
            tr.canopyAccentBlock = treesSec.getString("canopyAccentBlock", tr.canopyAccentBlock);
            tr.canopyAccentChance = treesSec.getDouble("canopyAccentChance", tr.canopyAccentChance);
            tr.trunkAccentBlock = treesSec.getString("trunkAccentBlock", tr.trunkAccentBlock);
            tr.trunkAccentChance = treesSec.getDouble("trunkAccentChance", tr.trunkAccentChance);
            tr.vineBlock = treesSec.getString("vineBlock", tr.vineBlock);
            tr.vineChance = treesSec.getDouble("vineChance", tr.vineChance);
            tr.vineMinLength = treesSec.getInt("vineMinLength", tr.vineMinLength);
            tr.vineMaxLength = treesSec.getInt("vineMaxLength", tr.vineMaxLength);
            List rawSpecies = treesSec.getMapList("species");
            if (!rawSpecies.isEmpty()) {
                ArrayList<DimensionPreset.TreeSpecies> species = new ArrayList<DimensionPreset.TreeSpecies>();
                for (Object raw : rawSpecies) {
                    Object canopyAccentObj;
                    Boolean b;
                    DimensionPreset.TreeSpecies s3 = new DimensionPreset.TreeSpecies();
                    s3.name = PresetLoader.str(raw.get("name"), s3.name);
                    s3.weight = Math.max(0.0, PresetLoader.num(raw.get("weight"), s3.weight));
                    s3.minHeight = (int)PresetLoader.num(raw.get("minHeight"), s3.minHeight);
                    s3.maxHeight = (int)PresetLoader.num(raw.get("maxHeight"), s3.maxHeight);
                    s3.canopyRadius = (int)PresetLoader.num(raw.get("canopyRadius"), s3.canopyRadius);
                    s3.trunkBlock = PresetLoader.str(raw.get("trunkBlock"), s3.trunkBlock);
                    s3.leafBlock = PresetLoader.str(raw.get("leafBlock"), s3.leafBlock);
                    s3.giantCanopyLayers = (int)PresetLoader.num(raw.get("giantCanopyLayers"), s3.giantCanopyLayers);
                    Object v = raw.get("branches");
                    if (v instanceof Boolean) {
                        b = (Boolean)v;
                        s3.branches = b;
                    }
                    if ((v = raw.get("buttressRoots")) instanceof Boolean) {
                        b = (Boolean)v;
                        s3.buttressRoots = b;
                    }
                    s3.canopyAccentBlock = (canopyAccentObj = raw.get("canopyAccentBlock")) != null ? canopyAccentObj.toString() : null;
                    s3.canopyAccentChance = PresetLoader.num(raw.get("canopyAccentChance"), s3.canopyAccentChance);
                    Object trunkAccentObj = raw.get("trunkAccentBlock");
                    s3.trunkAccentBlock = trunkAccentObj != null ? trunkAccentObj.toString() : null;
                    s3.trunkAccentChance = PresetLoader.num(raw.get("trunkAccentChance"), s3.trunkAccentChance);
                    Object vineObj = raw.get("vineBlock");
                    s3.vineBlock = vineObj != null ? vineObj.toString() : null;
                    s3.vineChance = PresetLoader.num(raw.get("vineChance"), s3.vineChance);
                    s3.vineMinLength = (int)PresetLoader.num(raw.get("vineMinLength"), s3.vineMinLength);
                    s3.vineMaxLength = (int)PresetLoader.num(raw.get("vineMaxLength"), s3.vineMaxLength);
                    species.add(s3);
                }
                double totalWeight = species.stream().mapToDouble(s -> s.weight).sum();
                if (totalWeight <= 0.0) {
                    this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': trees.species entries all have weight <= 0 \u2014 ignoring the list, falling back to the single-species trees.* fields.");
                } else {
                    long distinctNames = species.stream().map(s -> s.name).distinct().count();
                    if (distinctNames != (long)species.size()) {
                        this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': trees.species has duplicate 'name' values \u2014 each species is cached by name for block-data reuse, so duplicates will silently share the first entry's trunk/leaf blocks. Give every species a unique name.");
                    }
                    tr.species = species;
                }
            }
        }
        if ((decorationsSec = yaml.getConfigurationSection("decorations")) != null) {
            DimensionPreset.Decorations dc = preset.decorations;
            dc.enabled = decorationsSec.getBoolean("enabled", dc.enabled);
            dc.perChunkAttempts = Math.max(0, decorationsSec.getInt("perChunkAttempts", dc.perChunkAttempts));
            dc.chancePerAttempt = decorationsSec.getDouble("chancePerAttempt", dc.chancePerAttempt);
            List rawDecoSpecies = decorationsSec.getMapList("species");
            if (!rawDecoSpecies.isEmpty()) {
                ArrayList<DimensionPreset.DecorationSpecies> list = new ArrayList<DimensionPreset.DecorationSpecies>();
                for (Map raw : rawDecoSpecies) {
                    DimensionPreset.DecorationSpecies s4 = new DimensionPreset.DecorationSpecies();
                    s4.name = PresetLoader.str(raw.get("name"), s4.name);
                    s4.weight = Math.max(0.0, PresetLoader.num(raw.get("weight"), s4.weight));
                    s4.block = PresetLoader.str(raw.get("block"), s4.block);
                    s4.minHeight = Math.max(1, (int)PresetLoader.num(raw.get("minHeight"), s4.minHeight));
                    s4.maxHeight = Math.max(s4.minHeight, (int)PresetLoader.num(raw.get("maxHeight"), s4.maxHeight));
                    Object capObj = raw.get("capBlock");
                    s4.capBlock = capObj != null ? capObj.toString() : null;
                    s4.capRadius = Math.max(0, (int)PresetLoader.num(raw.get("capRadius"), s4.capRadius));
                    s4.minFloatHeight = Math.max(0, (int)PresetLoader.num(raw.get("minFloatHeight"), s4.minFloatHeight));
                    s4.maxFloatHeight = Math.max(s4.minFloatHeight, (int)PresetLoader.num(raw.get("maxFloatHeight"), s4.maxFloatHeight));
                    list.add(s4);
                }
                double totalWeight = list.stream().mapToDouble(s -> s.weight).sum();
                if (totalWeight <= 0.0) {
                    this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': decorations.species entries all have weight <= 0 \u2014 ignoring the list.");
                } else {
                    dc.species = list;
                }
            }
            if (dc.enabled && dc.species.isEmpty()) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': decorations.enabled is true but no valid species configured \u2014 disabling.");
                dc.enabled = false;
            }
        }
        if ((structuresSec = yaml.getConfigurationSection("structures")) != null) {
            DimensionPreset.Structures s5 = preset.structures;
            s5.enabled = structuresSec.getBoolean("enabled", s5.enabled);
            s5.blueprint = structuresSec.getString("blueprint", s5.blueprint);
            s5.rarityPerChunk = structuresSec.getDouble("rarityPerChunk", s5.rarityPerChunk);
            s5.lootTable = structuresSec.getString("lootTable", s5.lootTable);
            s5.randomRotation = structuresSec.getBoolean("randomRotation", s5.randomRotation);
            s5.randomMirror = structuresSec.getBoolean("randomMirror", s5.randomMirror);
            if (s5.enabled && (s5.blueprint == null || s5.blueprint.isBlank())) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': structures.enabled is true but no blueprint is set \u2014 disabling.");
                s5.enabled = false;
            }
        }
        if ((creaturesSec = yaml.getConfigurationSection("creatures")) != null) {
            DimensionPreset.Creatures cr = preset.creatures;
            cr.enabled = creaturesSec.getBoolean("enabled", cr.enabled);
            cr.spawnMultiplier = creaturesSec.getDouble("spawnMultiplier", cr.spawnMultiplier);
            if (cr.spawnMultiplier > 1.0) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': creatures.spawnMultiplier (" + cr.spawnMultiplier + ") is above 1.0, which isn't supported (no vanilla hook to spawn *more* than vanilla already attempts) \u2014 clamping to 1.0.");
                cr.spawnMultiplier = 1.0;
            } else if (cr.spawnMultiplier < 0.0) {
                cr.spawnMultiplier = 0.0;
            }
            ConfigurationSection mobsSec = creaturesSec.getConfigurationSection("mobs");
            if (mobsSec != null) {
                LinkedHashMap<String, DimensionPreset.MobProfile> mobs = new LinkedHashMap<String, DimensionPreset.MobProfile>();
                for (Object typeKey : mobsSec.getKeys(false)) {
                    normalized = ((String)typeKey).toUpperCase(Locale.ROOT);
                    try {
                        EntityType.valueOf((String)normalized);
                    }
                    catch (IllegalArgumentException ex) {
                        this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': creatures.mobs entry '" + (String)typeKey + "' isn't a known org.bukkit.entity.EntityType name \u2014 skipped.");
                        continue;
                    }
                    ConfigurationSection mobSec = mobsSec.getConfigurationSection((String)typeKey);
                    DimensionPreset.MobProfile profile = new DimensionPreset.MobProfile();
                    if (mobSec != null) {
                        profile.displayName = mobSec.getString("displayName", profile.displayName);
                        profile.alwaysShowName = mobSec.getBoolean("alwaysShowName", profile.alwaysShowName);
                        profile.healthMultiplier = Math.max(0.01, mobSec.getDouble("healthMultiplier", profile.healthMultiplier));
                        profile.speedMultiplier = Math.max(0.01, mobSec.getDouble("speedMultiplier", profile.speedMultiplier));
                        profile.damageMultiplier = Math.max(0.0, mobSec.getDouble("damageMultiplier", profile.damageMultiplier));
                        profile.scale = Math.max(0.05, mobSec.getDouble("scale", profile.scale));
                        profile.glowing = mobSec.getBoolean("glowing", profile.glowing);
                        equipSec = mobSec.getConfigurationSection("equipment");
                        if (equipSec != null) {
                            LinkedHashMap<String, String> equipment = new LinkedHashMap<String, String>();
                            for (String slot : equipSec.getKeys(false)) {
                                equipment.put(slot.toLowerCase(Locale.ROOT), equipSec.getString(slot));
                            }
                            profile.equipment = equipment;
                        }
                    }
                    mobs.put(normalized, profile);
                }
                cr.mobs = mobs;
            }
            if (cr.enabled && cr.mobs.isEmpty() && cr.spawnMultiplier >= 1.0) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': creatures.enabled is true but no valid mobs/spawnMultiplier < 1.0 configured \u2014 nothing to do.");
            }
        }
        if ((seasonsSec = yaml.getConfigurationSection("seasons")) != null) {
            DimensionPreset.Seasons se = preset.seasons;
            se.enabled = seasonsSec.getBoolean("enabled", se.enabled);
            List rawStages = seasonsSec.getMapList("stages");
            ArrayList<DimensionPreset.SeasonStage> stages = new ArrayList<DimensionPreset.SeasonStage>();
            for (Map raw : rawStages) {
                Object particlesObj;
                DimensionPreset.SeasonStage stage = new DimensionPreset.SeasonStage();
                stage.name = PresetLoader.str(raw.get("name"), stage.name);
                stage.durationTicks = Math.max(20, (int)PresetLoader.num(raw.get("durationTicks"), stage.durationTicks));
                equipSec = raw.get("spawnMultiplierOverride");
                if (equipSec instanceof Number) {
                    Number number2 = (Number)equipSec;
                    stage.spawnMultiplierOverride = Math.max(0.0, number2.doubleValue());
                }
                if ((equipSec = raw.get("forceClearWeather")) instanceof Boolean) {
                    Boolean b;
                    stage.forceClearWeather = b = (Boolean)equipSec;
                }
                if ((particlesObj = raw.get("particles")) instanceof Map) {
                    Map particlesMap = (Map)particlesObj;
                    stage.particles = this.parseParticlesMap(particlesMap, preset.id, stage.name);
                }
                stages.add(stage);
            }
            if (se.enabled && stages.isEmpty()) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': seasons.enabled is true but no stages are defined \u2014 disabling.");
                se.enabled = false;
            }
            se.stages = stages;
        }
        if ((flavorSec = yaml.getConfigurationSection("flavor")) != null) {
            DimensionPreset.Flavor f = preset.flavor;
            f.gravity = flavorSec.getDouble("gravity", f.gravity);
            if (flavorSec.contains("allowJumping")) {
                f.allowJumping = flavorSec.getBoolean("allowJumping");
            }
            f.alwaysClearWeather = flavorSec.getBoolean("alwaysClearWeather", f.alwaysClearWeather);
            f.generateStructures = flavorSec.getBoolean("generateStructures", f.generateStructures);
            f.generateDecorations = flavorSec.getBoolean("generateDecorations", f.generateDecorations);
            f.generateVanillaCaves = flavorSec.getBoolean("generateVanillaCaves", f.generateVanillaCaves);
            if (f.gravity <= 0.0) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': flavor.gravity must be > 0, resetting to 1.0.");
                f.gravity = 1.0;
            }
        }
        if ((particlesSec = yaml.getConfigurationSection("particles")) != null) {
            DimensionPreset.Particles p = preset.particles;
            p.enabled = particlesSec.getBoolean("enabled", p.enabled);
            String rawType = particlesSec.getString("type", p.type);
            normalized = (rawType.contains(":") ? rawType.substring(rawType.indexOf(58) + 1) : rawType).toUpperCase(Locale.ROOT);
            try {
                Particle.valueOf((String)normalized);
                p.type = normalized;
            }
            catch (IllegalArgumentException ex) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': particles.type '" + rawType + "' isn't a valid org.bukkit.Particle name, defaulting to ASH.");
                p.type = "ASH";
            }
            p.color = particlesSec.getString("color", p.color);
            p.toColor = particlesSec.getString("toColor", p.toColor);
            p.size = (float)particlesSec.getDouble("size", (double)p.size);
            p.density = particlesSec.getInt("density", p.density);
            p.radius = particlesSec.getInt("radius", p.radius);
            p.heightSpread = particlesSec.getInt("heightSpread", p.heightSpread);
            p.intervalTicks = Math.max(1, particlesSec.getInt("intervalTicks", p.intervalTicks));
            p.windStrength = particlesSec.getDouble("windStrength", p.windStrength);
        }
        if (preset.trees.enabled) {
            int tallestConfigured;
            DimensionPreset.WorldHeight resolved = preset.resolvedWorldHeight();
            int availableHeight = resolved != null ? resolved.height : 384;
            int n = tallestConfigured = preset.trees.species.isEmpty() ? preset.trees.maxHeight : preset.trees.species.stream().mapToInt(s -> s.maxHeight).max().orElse(preset.trees.maxHeight);
            if (tallestConfigured > availableHeight - 16) {
                this.logger.warning("[NexusDimensions] Preset '" + preset.id + "': the tallest configured tree height (" + tallestConfigured + ") does not comfortably fit the resolved world height (" + availableHeight + "). Add/raise a worldHeight block (Tier 2) or lower it.");
            }
        }
        return preset;
    }

    private DimensionPreset.Particles parseParticlesMap(Map<?, ?> raw, String presetId, String stageName) {
        boolean bl;
        DimensionPreset.Particles p = new DimensionPreset.Particles();
        Object obj = raw.get("enabled");
        if (obj instanceof Boolean) {
            Boolean b = (Boolean)obj;
            bl = b;
        } else {
            bl = true;
        }
        p.enabled = bl;
        String rawType = PresetLoader.str(raw.get("type"), p.type);
        String normalized = (rawType.contains(":") ? rawType.substring(rawType.indexOf(58) + 1) : rawType).toUpperCase(Locale.ROOT);
        try {
            Particle.valueOf((String)normalized);
            p.type = normalized;
        }
        catch (IllegalArgumentException ex) {
            this.logger.warning("[NexusDimensions] Preset '" + presetId + "': seasons stage '" + stageName + "' particles.type '" + rawType + "' isn't a valid org.bukkit.Particle name, defaulting to ASH.");
            p.type = "ASH";
        }
        p.color = PresetLoader.str(raw.get("color"), p.color);
        Object toColorObj = raw.get("toColor");
        p.toColor = toColorObj != null ? toColorObj.toString() : null;
        p.size = (float)PresetLoader.num(raw.get("size"), p.size);
        p.density = (int)PresetLoader.num(raw.get("density"), p.density);
        p.radius = (int)PresetLoader.num(raw.get("radius"), p.radius);
        p.heightSpread = (int)PresetLoader.num(raw.get("heightSpread"), p.heightSpread);
        p.intervalTicks = Math.max(1, (int)PresetLoader.num(raw.get("intervalTicks"), p.intervalTicks));
        p.windStrength = PresetLoader.num(raw.get("windStrength"), p.windStrength);
        return p;
    }

    private static String str(Object o, String def) {
        return o != null ? o.toString() : def;
    }

    private static double num(Object o, double def) {
        double d;
        if (o instanceof Number) {
            Number number = (Number)o;
            d = number.doubleValue();
        } else {
            d = def;
        }
        return d;
    }
}
