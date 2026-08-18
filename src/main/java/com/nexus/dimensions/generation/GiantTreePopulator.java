/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.generator.BlockPopulator
 *  org.bukkit.generator.LimitedRegion
 *  org.bukkit.generator.WorldInfo
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.DeterministicHash;
import com.nexus.dimensions.generation.GroundHeightSource;
import com.nexus.dimensions.generation.TreeShaper;
import com.nexus.dimensions.generation.TreeSpeciesPicker;
import com.nexus.dimensions.generation.noise.NoiseUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

public final class GiantTreePopulator
extends BlockPopulator {
    private final DimensionPreset preset;
    private final GroundHeightSource groundHeightSource;
    private final NoiseUtil noise;
    private final Map<String, BlockData> trunkCache = new HashMap<String, BlockData>();
    private final Map<String, BlockData> leafCache = new HashMap<String, BlockData>();

    public GiantTreePopulator(DimensionPreset preset, GroundHeightSource groundHeightSource, NoiseUtil noise) {
        this.preset = preset;
        this.groundHeightSource = groundHeightSource;
        this.noise = noise;
    }

    private static Material materialOf(String key) {
        Material m = Material.matchMaterial((String)key);
        return m != null ? m : Material.OAK_LOG;
    }

    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
        DimensionPreset.Trees cfg = this.preset.trees;
        if (!cfg.enabled) {
            return;
        }
        if (DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, 1) > cfg.rarityPerChunk) {
            return;
        }
        int localX = (int)(DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, 2) * 16.0);
        int localZ = (int)(DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, 3) * 16.0);
        int worldX = (chunkX << 4) + localX;
        int worldZ = (chunkZ << 4) + localZ;
        int minY = worldInfo.getMinHeight();
        int maxY = worldInfo.getMaxHeight();
        int groundY = this.groundHeightSource.groundHeight(worldX, worldZ, minY, maxY);
        double heightRoll = DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, 4);
        DimensionPreset.TreeSpecies species = TreeSpeciesPicker.pick(cfg, worldInfo.getSeed(), chunkX, chunkZ);
        BlockData trunk = this.trunkCache.computeIfAbsent(species.name, n -> GiantTreePopulator.materialOf(species.trunkBlock).createBlockData());
        BlockData leaves = this.leafCache.computeIfAbsent(species.name, n -> GiantTreePopulator.materialOf(species.leafBlock).createBlockData());
        TreeShaper.place(species, this.noise, worldInfo.getSeed(), worldX, groundY, worldZ, maxY, heightRoll, trunk, leaves, (x, y, z, data) -> {
            if (limitedRegion.isInRegion(x, y, z)) {
                limitedRegion.setBlockData(x, y, z, data);
            }
        });
    }
}
