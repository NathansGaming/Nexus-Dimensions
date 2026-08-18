/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.block.data.BlockData
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.DeterministicHash;
import com.nexus.dimensions.generation.TreeShaper;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public final class DecorationShaper {
    private DecorationShaper() {
    }

    public static void place(DimensionPreset.DecorationSpecies species, long seed, int chunkX, int chunkZ, int attempt, int worldX, int groundY, int worldZ, int minY, int maxY, TreeShaper.BlockWriter writer) {
        BlockData capData;
        int salt = 900 + attempt * 10;
        double heightRoll = DeterministicHash.hash01(seed, chunkX, chunkZ, salt + 5);
        double floatRoll = DeterministicHash.hash01(seed, chunkX, chunkZ, salt + 6);
        int height = species.minHeight + (int)Math.round(heightRoll * (double)(species.maxHeight - species.minHeight));
        int floatOffset = species.minFloatHeight + (int)Math.round(floatRoll * (double)(species.maxFloatHeight - species.minFloatHeight));
        int baseY = groundY + 1 + floatOffset;
        BlockData columnData = DecorationShaper.dataOf(species.block);
        if (columnData != null) {
            for (int h = 0; h < height; ++h) {
                int y = baseY + h;
                if (y < minY || y >= maxY) continue;
                writer.set(worldX, y, worldZ, columnData);
            }
        }
        if ((capData = DecorationShaper.dataOf(species.capBlock)) == null) {
            return;
        }
        int capY = baseY + height;
        if (capY < minY || capY >= maxY) {
            return;
        }
        if (species.capRadius <= 0) {
            writer.set(worldX, capY, worldZ, capData);
            return;
        }
        int r = species.capRadius;
        for (int dx = -r; dx <= r; ++dx) {
            for (int dz = -r; dz <= r; ++dz) {
                if (dx * dx + dz * dz > r * r) continue;
                writer.set(worldX + dx, capY, worldZ + dz, capData);
            }
        }
    }

    private static BlockData dataOf(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        Material m = Material.matchMaterial((String)key);
        return m != null ? m.createBlockData() : null;
    }
}
