/*
 * Decompiled with CFR 0.152.
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.DeterministicHash;
import java.util.List;

public final class TreeSpeciesPicker {
    private TreeSpeciesPicker() {
    }

    public static DimensionPreset.TreeSpecies pick(DimensionPreset.Trees cfg, long seed, int chunkX, int chunkZ) {
        List<DimensionPreset.TreeSpecies> list = cfg.species;
        if (list.isEmpty()) {
            DimensionPreset.TreeSpecies fallback = new DimensionPreset.TreeSpecies();
            fallback.name = "default";
            fallback.minHeight = cfg.minHeight;
            fallback.maxHeight = cfg.maxHeight;
            fallback.canopyRadius = cfg.canopyRadius;
            fallback.trunkBlock = cfg.trunkBlock;
            fallback.leafBlock = cfg.leafBlock;
            fallback.giantCanopyLayers = cfg.giantCanopyLayers;
            fallback.branches = cfg.branches;
            fallback.buttressRoots = cfg.buttressRoots;
            fallback.canopyAccentBlock = cfg.canopyAccentBlock;
            fallback.canopyAccentChance = cfg.canopyAccentChance;
            fallback.trunkAccentBlock = cfg.trunkAccentBlock;
            fallback.trunkAccentChance = cfg.trunkAccentChance;
            fallback.vineBlock = cfg.vineBlock;
            fallback.vineChance = cfg.vineChance;
            fallback.vineMinLength = cfg.vineMinLength;
            fallback.vineMaxLength = cfg.vineMaxLength;
            return fallback;
        }
        double totalWeight = 0.0;
        for (DimensionPreset.TreeSpecies s : list) {
            totalWeight += Math.max(0.0, s.weight);
        }
        if (totalWeight <= 0.0) {
            return list.get(0);
        }
        double roll = DeterministicHash.hash01(seed, chunkX, chunkZ, 5) * totalWeight;
        double cumulative = 0.0;
        for (DimensionPreset.TreeSpecies s : list) {
            if (!(roll < (cumulative += Math.max(0.0, s.weight)))) continue;
            return s;
        }
        return list.get(list.size() - 1);
    }
}
