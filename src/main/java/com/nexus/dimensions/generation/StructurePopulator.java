/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.generator.BlockPopulator
 *  org.bukkit.generator.LimitedRegion
 *  org.bukkit.generator.WorldInfo
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.DeterministicHash;
import com.nexus.dimensions.generation.GroundHeightSource;
import com.nexus.dimensions.structure.Blueprint;
import com.nexus.dimensions.structure.BlueprintTransform;
import com.nexus.dimensions.world.StructureLootService;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

public final class StructurePopulator
extends BlockPopulator {
    private final DimensionPreset preset;
    private final Blueprint blueprint;
    private final GroundHeightSource groundHeightSource;
    private final StructureLootService lootService;

    public StructurePopulator(DimensionPreset preset, Blueprint blueprint, GroundHeightSource groundHeightSource, StructureLootService lootService) {
        this.preset = preset;
        this.blueprint = blueprint;
        this.groundHeightSource = groundHeightSource;
        this.lootService = lootService;
    }

    private static Material materialOf(String key) {
        Material m = Material.matchMaterial((String)key);
        return m != null ? m : Material.STONE;
    }

    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
        int maxY;
        int minY;
        int localZ;
        int worldZ;
        DimensionPreset.Structures cfg = this.preset.structures;
        if (!cfg.enabled || this.blueprint == null || this.blueprint.blocks.isEmpty()) {
            return;
        }
        if (DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, 20) > cfg.rarityPerChunk) {
            return;
        }
        int localX = (int)(DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, 21) * 16.0);
        int worldX = (chunkX << 4) + localX;
        int groundY = this.groundHeightSource.groundHeight(worldX, worldZ = (chunkZ << 4) + (localZ = (int)(DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, 22) * 16.0)), minY = worldInfo.getMinHeight(), maxY = worldInfo.getMaxHeight());
        if (groundY <= minY) {
            return;
        }
        int rotationStep = cfg.randomRotation ? (int)(DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, 23) * 4.0) : 0;
        boolean mirror = cfg.randomMirror && DeterministicHash.hash01(worldInfo.getSeed(), chunkX, chunkZ, 24) < 0.5;
        for (Blueprint.BlockEntry entry : this.blueprint.blocks) {
            int[] transformed = BlueprintTransform.apply(entry.dx, entry.dz, rotationStep, mirror);
            int x = worldX + transformed[0];
            int y = groundY + 1 + entry.dy;
            int z = worldZ + transformed[1];
            if (y < minY || y >= maxY || !limitedRegion.isInRegion(x, y, z)) continue;
            if (entry.loot) {
                limitedRegion.setBlockData(x, y, z, Material.CHEST.createBlockData());
                this.lootService.enqueue(worldInfo.getName(), x, y, z, cfg.lootTable);
                continue;
            }
            limitedRegion.setBlockData(x, y, z, StructurePopulator.materialOf(entry.block).createBlockData());
        }
    }
}
