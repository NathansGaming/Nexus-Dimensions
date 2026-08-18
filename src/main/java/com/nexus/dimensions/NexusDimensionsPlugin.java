/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.nexus.dimensions;

import com.nexus.dimensions.command.NexusDimCommand;
import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.config.PresetLoader;
import com.nexus.dimensions.datapack.DatapackGenerator;
import com.nexus.dimensions.generation.GiantTreePopulator;
import com.nexus.dimensions.structure.Blueprint;
import com.nexus.dimensions.structure.BlueprintLoader;
import com.nexus.dimensions.world.AmbientParticleService;
import com.nexus.dimensions.world.DimensionManager;
import com.nexus.dimensions.world.GravityService;
import com.nexus.dimensions.world.MobCustomizationListener;
import com.nexus.dimensions.world.NexusDecorationListener;
import com.nexus.dimensions.world.NexusFloraListener;
import com.nexus.dimensions.world.NexusStructureListener;
import com.nexus.dimensions.world.PortalEffectManager;
import com.nexus.dimensions.world.PortalListener;
import com.nexus.dimensions.world.PortalManager;
import com.nexus.dimensions.world.SeasonService;
import com.nexus.dimensions.world.StructureLootService;
import java.io.File;
import java.util.Map;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class NexusDimensionsPlugin
extends JavaPlugin {
    private static final String[] DEFAULT_PRESETS = new String[]{"moon.yml", "ice_moon.yml", "ocean_planet.yml", "sky_forest.yml", "iron_giant_world.yml", "floating_isles.yml", "crystal_spires.yml"};
    private static final String[] DEFAULT_BLUEPRINTS = new String[]{"ruin_small.yml"};
    private PresetLoader presetLoader;
    private BlueprintLoader blueprintLoader;
    private DimensionManager dimensionManager;
    private DatapackGenerator datapackGenerator;
    private PortalEffectManager portalEffectManager;

    public void onEnable() {
        this.saveDefaultConfig();
        this.copyBundledPresetsIfMissing();
        this.copyBundledBlueprintsIfMissing();
        this.presetLoader = new PresetLoader((Plugin)this);
        Map<String, DimensionPreset> presets = this.presetLoader.loadAll();
        this.blueprintLoader = new BlueprintLoader((Plugin)this);
        Map<String, Blueprint> blueprints = this.blueprintLoader.loadAll();
        StructureLootService lootService = new StructureLootService((Plugin)this);
        int packFormat = this.getConfig().getInt("datapackPackFormat", 61);
        this.datapackGenerator = new DatapackGenerator(this.getLogger(), packFormat);
        this.dimensionManager = new DimensionManager((Plugin)this, presets, blueprints, lootService);
        for (DimensionPreset preset : presets.values()) {
            if (!preset.isTier2()) continue;
            this.datapackGenerator.writeDatapack(preset);
        }
        this.dimensionManager.loadPersistedWorldsOnStartup();
        this.dimensionManager.registerActiveTier2WorldsOnStartup();
        PortalManager portalManager = new PortalManager((Plugin)this);
        this.portalEffectManager = new PortalEffectManager((Plugin)this, portalManager);
        this.portalEffectManager.start();
        NexusDimCommand command = new NexusDimCommand(this.presetLoader, this.dimensionManager, this.datapackGenerator, this.blueprintLoader, portalManager);
        PluginCommand pluginCommand = this.getCommand("nexusdim");
        if (pluginCommand != null) {
            pluginCommand.setExecutor((CommandExecutor)command);
            pluginCommand.setTabCompleter((TabCompleter)command);
        } else {
            this.getLogger().severe("[NexusDimensions] 'nexusdim' command missing from plugin.yml \u2014 check the jar wasn't repackaged incorrectly.");
        }
        SeasonService seasonService = new SeasonService((Plugin)this, this.dimensionManager);
        this.dimensionManager.setSeasonService(seasonService);
        new GravityService((Plugin)this, this.dimensionManager);
        new AmbientParticleService((Plugin)this, this.dimensionManager, seasonService);
        new NexusFloraListener((Plugin)this, this.dimensionManager);
        new NexusDecorationListener((Plugin)this, this.dimensionManager);
        new NexusStructureListener((Plugin)this, this.dimensionManager, lootService);
        new PortalListener((Plugin)this, portalManager, this.portalEffectManager);
        new MobCustomizationListener((Plugin)this, this.dimensionManager, seasonService);
        this.getLogger().info("[NexusDimensions] Enabled with " + presets.size() + " preset(s) and " + blueprints.size() + " blueprint(s). Note: " + GiantTreePopulator.class.getSimpleName() + " and structure placement only attach to Tier 1 dimensions you create with /nexusdim create.");
    }

    private void copyBundledPresetsIfMissing() {
        File presetsDir = new File(this.getDataFolder(), "presets");
        if (!presetsDir.exists()) {
            presetsDir.mkdirs();
        }
        for (String fileName : DEFAULT_PRESETS) {
            File target = new File(presetsDir, fileName);
            if (target.exists()) continue;
            this.saveResource("presets/" + fileName, false);
        }
    }

    private void copyBundledBlueprintsIfMissing() {
        File blueprintsDir = new File(this.getDataFolder(), "blueprints");
        if (!blueprintsDir.exists()) {
            blueprintsDir.mkdirs();
        }
        for (String fileName : DEFAULT_BLUEPRINTS) {
            File target = new File(blueprintsDir, fileName);
            if (target.exists()) continue;
            this.saveResource("blueprints/" + fileName, false);
        }
    }

    public void onDisable() {
        if (this.portalEffectManager != null) {
            this.portalEffectManager.stop();
        }
        this.getLogger().info("[NexusDimensions] Disabled.");
    }
}
