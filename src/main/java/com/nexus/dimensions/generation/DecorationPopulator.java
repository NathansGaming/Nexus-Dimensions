/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.generator.BlockPopulator
 *  org.bukkit.generator.LimitedRegion
 *  org.bukkit.generator.WorldInfo
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.DecorationShaper;
import com.nexus.dimensions.generation.DecorationSpeciesPicker;
import com.nexus.dimensions.generation.DeterministicHash;
import com.nexus.dimensions.generation.GroundHeightSource;
import java.util.Random;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

public final class DecorationPopulator
extends BlockPopulator {
    private final DimensionPreset preset;
    private final GroundHeightSource groundHeightSource;

    public DecorationPopulator(DimensionPreset preset, GroundHeightSource groundHeightSource) {
        this.preset = preset;
        this.groundHeightSource = groundHeightSource;
    }

    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
        DimensionPreset.Decorations cfg = this.preset.decorations;
        if (!cfg.enabled || cfg.species.isEmpty()) {
            return;
        }
        int minY = worldInfo.getMinHeight();
        int maxY = worldInfo.getMaxHeight();
        for (int attempt = 0; attempt < cfg.perChunkAttempts; ++attempt) {
            int localZ;
            int worldZ;
            int localX;
            int worldX;
            int groundY;
            int salt = 900 + attempt * 10;
            if (DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, salt + 1) > cfg.chancePerAttempt || (groundY = this.groundHeightSource.groundHeight(worldX = (chunkX << 4) + (localX = (int)(DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, salt + 2) * 16.0)), worldZ = (chunkZ << 4) + (localZ = (int)(DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, salt + 3) * 16.0)), minY, maxY)) <= minY) continue;
            DimensionPreset.DecorationSpecies species = DecorationSpeciesPicker.pick(cfg.species, worldInfo.getSeed(), chunkX, chunkZ, attempt);
            DecorationShaper.place(species, worldInfo.getSeed(), chunkX, chunkZ, attempt, worldX, groundY, worldZ, minY, maxY, (x, y, z, data) -> {
                if (limitedRegion.isInRegion(x, y, z)) {
                    limitedRegion.setBlockData(x, y, z, data);
                }
            });
        }
    }
}
