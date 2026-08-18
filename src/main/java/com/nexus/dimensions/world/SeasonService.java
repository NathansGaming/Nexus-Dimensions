/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.plugin.Plugin
 */
package com.nexus.dimensions.world;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.world.DimensionManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public final class SeasonService {
    private static final long ADVANCE_INTERVAL_TICKS = 20L;
    private final DimensionManager dimensionManager;
    private final Map<String, Long> elapsedTicks = new ConcurrentHashMap<String, Long>();

    public SeasonService(Plugin plugin, DimensionManager dimensionManager) {
        this.dimensionManager = dimensionManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::advance, 20L, 20L);
    }

    private void advance() {
        for (World world : Bukkit.getWorlds()) {
            DimensionPreset preset = this.dimensionManager.getPresetForWorld(world.getName());
            if (preset == null || !preset.seasons.enabled) continue;
            this.elapsedTicks.merge(world.getName(), 20L, Long::sum);
        }
    }

    public DimensionPreset.SeasonStage currentStage(String worldName, DimensionPreset preset) {
        if (preset == null || !preset.seasons.enabled || preset.seasons.stages.isEmpty()) {
            return null;
        }
        List<DimensionPreset.SeasonStage> stages = preset.seasons.stages;
        long cycleLength = 0L;
        for (DimensionPreset.SeasonStage s : stages) {
            cycleLength += (long)s.durationTicks;
        }
        if (cycleLength <= 0L) {
            return null;
        }
        long elapsed = this.elapsedTicks.getOrDefault(worldName, 0L) % cycleLength;
        long cursor = 0L;
        for (DimensionPreset.SeasonStage s : stages) {
            if (elapsed >= (cursor += (long)s.durationTicks)) continue;
            return s;
        }
        return stages.get(stages.size() - 1);
    }

    public DimensionPreset.Particles effectiveParticles(String worldName, DimensionPreset preset) {
        DimensionPreset.SeasonStage stage = this.currentStage(worldName, preset);
        if (stage != null && stage.particles != null) {
            return stage.particles;
        }
        return preset.particles;
    }

    public double effectiveSpawnMultiplier(String worldName, DimensionPreset preset) {
        DimensionPreset.SeasonStage stage = this.currentStage(worldName, preset);
        if (stage != null && stage.spawnMultiplierOverride != null) {
            return stage.spawnMultiplierOverride;
        }
        return preset.creatures.spawnMultiplier;
    }

    public boolean effectiveAlwaysClearWeather(String worldName, DimensionPreset preset) {
        DimensionPreset.SeasonStage stage = this.currentStage(worldName, preset);
        if (stage != null && stage.forceClearWeather != null) {
            return stage.forceClearWeather;
        }
        return preset.flavor.alwaysClearWeather;
    }
}
