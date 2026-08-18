/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.WorldCreator
 *  org.bukkit.WorldType
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.weather.WeatherChangeEvent
 *  org.bukkit.generator.BiomeProvider
 *  org.bukkit.generator.ChunkGenerator
 *  org.bukkit.plugin.Plugin
 */
package com.nexus.dimensions.world;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.NexusBiomeProvider;
import com.nexus.dimensions.generation.NexusChunkGenerator;
import com.nexus.dimensions.structure.Blueprint;
import com.nexus.dimensions.world.SeasonService;
import com.nexus.dimensions.world.StructureLootService;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

public final class DimensionManager
implements Listener {
    private final Plugin plugin;
    private final Logger logger;
    private final File worldsFile;
    private Map<String, DimensionPreset> presets;
    private Map<String, Blueprint> blueprints;
    private final StructureLootService lootService;
    private SeasonService seasonService;
    private final Map<String, String> managedWorlds = new LinkedHashMap<String, String>();

    public DimensionManager(Plugin plugin, Map<String, DimensionPreset> presets, Map<String, Blueprint> blueprints, StructureLootService lootService) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.presets = presets;
        this.blueprints = blueprints;
        this.lootService = lootService;
        this.worldsFile = new File(plugin.getDataFolder(), "worlds.yml");
        this.loadManagedWorlds();
        Bukkit.getPluginManager().registerEvents((Listener)this, plugin);
    }

    public void reloadPresets(Map<String, DimensionPreset> presets) {
        this.presets = presets;
    }

    public void reloadBlueprints(Map<String, Blueprint> blueprints) {
        this.blueprints = blueprints;
    }

    public void setSeasonService(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    public Blueprint getBlueprint(String name) {
        return name != null ? this.blueprints.get(name) : null;
    }

    public Map<String, DimensionPreset> getPresets() {
        return this.presets;
    }

    public DimensionPreset getPresetForWorld(String worldName) {
        String presetId = this.managedWorlds.get(worldName);
        if (presetId == null) {
            return null;
        }
        return this.presets.get(presetId);
    }

    private void loadManagedWorlds() {
        if (!this.worldsFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)this.worldsFile);
        for (String worldName : yaml.getKeys(false)) {
            this.managedWorlds.put(worldName, yaml.getString(worldName));
        }
    }

    private void saveManagedWorlds() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, String> e : this.managedWorlds.entrySet()) {
            yaml.set(e.getKey(), (Object)e.getValue());
        }
        try {
            yaml.save(this.worldsFile);
        }
        catch (IOException e) {
            this.logger.severe("[NexusDimensions] Could not save worlds.yml: " + e.getMessage());
        }
    }

    public void loadPersistedWorldsOnStartup() {
        for (Map.Entry<String, String> entry : this.managedWorlds.entrySet()) {
            String worldName = entry.getKey();
            String presetId = entry.getValue();
            DimensionPreset preset = this.presets.get(presetId);
            if (preset == null) {
                this.logger.warning("[NexusDimensions] worlds.yml references unknown preset '" + presetId + "' for world '" + worldName + "' \u2014 skipping, add the preset back or edit worlds.yml.");
                continue;
            }
            if (preset.isTier2() || Bukkit.getWorld((String)worldName) != null) continue;
            this.loadTier1World(worldName, preset, null);
            this.logger.info("[NexusDimensions] Re-attached generator to existing dimension '" + worldName + "'.");
        }
    }

    public void registerActiveTier2WorldsOnStartup() {
        for (World world : Bukkit.getWorlds()) {
            String presetId;
            DimensionPreset preset;
            if (!"nexus".equals(world.getKey().getNamespace()) || (preset = this.presets.get(presetId = world.getKey().getKey())) == null || !preset.isTier2()) continue;
            this.managedWorlds.put(world.getName(), presetId);
            this.applyFlavor(world, preset);
            this.logger.info("[NexusDimensions] Recognized active Tier 2 dimension '" + presetId + "' as world '" + world.getName() + "'.");
        }
    }

    public CreateResult createOrLoad(String worldName, String presetId, Long seedOverride) {
        DimensionPreset preset = this.presets.get(presetId);
        if (preset == null) {
            return CreateResult.UNKNOWN_PRESET;
        }
        if (preset.isTier2()) {
            boolean alreadyActive = Bukkit.getWorlds().stream().anyMatch(w -> "nexus".equals(w.getKey().getNamespace()) && w.getKey().getKey().equals(presetId));
            if (alreadyActive) {
                return CreateResult.TIER2_ALREADY_ACTIVE;
            }
            return CreateResult.TIER2_DATAPACK_WRITTEN_RESTART_REQUIRED;
        }
        World existing = Bukkit.getWorld((String)worldName);
        if (existing != null) {
            return this.managedWorlds.containsKey(worldName) ? CreateResult.TIER1_ALREADY_LOADED : CreateResult.NAME_COLLISION;
        }
        boolean preexisting = this.hasPreexistingChunkData(worldName);
        this.loadTier1World(worldName, preset, seedOverride);
        this.managedWorlds.put(worldName, presetId);
        this.saveManagedWorlds();
        return preexisting ? CreateResult.TIER1_CREATED_ON_PREEXISTING_FOLDER : CreateResult.TIER1_CREATED;
    }

    private boolean hasPreexistingChunkData(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        File regionFolder = new File(worldFolder, "region");
        String[] regionFiles = regionFolder.list((dir, name) -> name.endsWith(".mca"));
        return regionFiles != null && regionFiles.length > 0;
    }

    private World loadTier1World(String worldName, DimensionPreset preset, Long seedOverride) {
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(preset.environment);
        creator.type(WorldType.NORMAL);
        creator.generateStructures(preset.flavor.generateStructures);
        long seed = seedOverride != null ? seedOverride : (preset.seed != null ? preset.seed : (long)worldName.hashCode());
        creator.seed(seed);
        NexusChunkGenerator generator = new NexusChunkGenerator(preset, seed, this.blueprints, this.lootService);
        creator.generator((ChunkGenerator)generator);
        creator.biomeProvider((BiomeProvider)new NexusBiomeProvider(preset));
        World world = creator.createWorld();
        if (world != null) {
            this.applyFlavor(world, preset);
        }
        return world;
    }

    private void applyFlavor(World world, DimensionPreset preset) {
        world.getWorldBorder().setSize(5.9999968E7);
        if (preset.flavor.alwaysClearWeather) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onWeatherChange(WeatherChangeEvent event) {
        boolean alwaysClear;
        if (!event.toWeatherState()) {
            return;
        }
        String worldName = event.getWorld().getName();
        DimensionPreset preset = this.getPresetForWorld(worldName);
        if (preset == null) {
            return;
        }
        boolean bl = alwaysClear = this.seasonService != null ? this.seasonService.effectiveAlwaysClearWeather(worldName, preset) : preset.flavor.alwaysClearWeather;
        if (alwaysClear) {
            event.setCancelled(true);
        }
    }

    public static enum CreateResult {
        TIER1_CREATED,
        TIER1_CREATED_ON_PREEXISTING_FOLDER,
        TIER1_ALREADY_LOADED,
        TIER2_DATAPACK_WRITTEN_RESTART_REQUIRED,
        TIER2_ALREADY_ACTIVE,
        UNKNOWN_PRESET,
        NAME_COLLISION;

    }
}
