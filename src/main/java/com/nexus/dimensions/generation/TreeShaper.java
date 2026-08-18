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
import com.nexus.dimensions.generation.noise.NoiseUtil;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public final class TreeShaper {
    private TreeShaper() {
    }

    public static void place(DimensionPreset.TreeSpecies species, NoiseUtil noise, long seed, int worldX, int groundY, int worldZ, int worldMaxY, double heightRoll, BlockData trunk, BlockData leaves, BlockWriter writer) {
        int trunkHeight = species.minHeight + (int)Math.round(heightRoll * (double)(species.maxHeight - species.minHeight));
        if ((trunkHeight = Math.min(trunkHeight, worldMaxY - groundY - 2)) < 3) {
            return;
        }
        BlockData trunkAccent = TreeShaper.dataOf(species.trunkAccentBlock);
        BlockData canopyAccent = TreeShaper.dataOf(species.canopyAccentBlock);
        BlockData vine = TreeShaper.dataOf(species.vineBlock);
        TreeShaper.placeTrunk(species, seed, worldX, groundY, worldZ, trunkHeight, trunk, trunkAccent, writer);
        int canopyBase = groundY + trunkHeight - Math.max(2, species.giantCanopyLayers / 3);
        int canopyTop = groundY + trunkHeight + Math.max(2, species.giantCanopyLayers / 2);
        TreeShaper.placeCanopy(species, noise, seed, worldX, worldZ, canopyBase, canopyTop, groundY + trunkHeight, species.canopyRadius, leaves, canopyAccent, vine, writer);
        if (species.buttressRoots) {
            TreeShaper.placeButtressRoots(species, seed, worldX, groundY, worldZ, trunk, writer);
        }
        if (species.branches && trunkHeight >= 18) {
            TreeShaper.placeBranches(species, noise, seed, worldX, groundY, worldZ, trunkHeight, trunk, leaves, trunkAccent, canopyAccent, writer);
        }
    }

    private static void placeTrunk(DimensionPreset.TreeSpecies species, long seed, int worldX, int groundY, int worldZ, int trunkHeight, BlockData trunk, BlockData trunkAccent, BlockWriter writer) {
        for (int i = 1; i <= trunkHeight; ++i) {
            int y = groundY + i;
            BlockData block = trunk;
            if (trunkAccent != null && TreeShaper.blockRoll(seed, worldX, y, worldZ, 700) < species.trunkAccentChance) {
                block = trunkAccent;
            }
            writer.set(worldX, y, worldZ, block);
        }
    }

    private static void placeCanopy(DimensionPreset.TreeSpecies species, NoiseUtil noise, long seed, int worldX, int worldZ, int canopyBase, int canopyTop, int trunkTop, int baseRadius, BlockData leaves, BlockData canopyAccent, BlockData vine, BlockWriter writer) {
        for (int y = canopyBase; y <= canopyTop; ++y) {
            double heightFrac = (double)(y - canopyBase) / (double)Math.max(1, canopyTop - canopyBase);
            double radiusFrac = 1.0 - Math.abs(heightFrac - 0.35) / 0.75;
            int layerRadius = Math.max(1, (int)Math.round((double)baseRadius * Math.max(0.15, radiusFrac)));
            for (int dx = -layerRadius; dx <= layerRadius; ++dx) {
                for (int dz = -layerRadius; dz <= layerRadius; ++dz) {
                    double wobble;
                    double effectiveRadius;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > (effectiveRadius = (double)layerRadius * (1.0 + (wobble = noise.perlin3((double)(worldX + dx) * 0.15 + 4000.0, (double)y * 0.15, (double)(worldZ + dz) * 0.15 + 4000.0)) * 0.3)) || dx == 0 && dz == 0 && y <= trunkTop) continue;
                    int x = worldX + dx;
                    int z = worldZ + dz;
                    BlockData block = leaves;
                    if (canopyAccent != null && TreeShaper.blockRoll(seed, x, y, z, 710) < species.canopyAccentChance) {
                        block = canopyAccent;
                    }
                    writer.set(x, y, z, block);
                    if (vine == null || !(heightFrac < 0.3) || !(TreeShaper.blockRoll(seed, x, y, z, 720) < species.vineChance)) continue;
                    int length = species.vineMinLength + (int)(TreeShaper.blockRoll(seed, x, y, z, 721) * (double)Math.max(1, species.vineMaxLength - species.vineMinLength));
                    for (int v = 1; v <= length; ++v) {
                        writer.set(x, y - v, z, vine);
                    }
                }
            }
        }
    }

    private static void placeButtressRoots(DimensionPreset.TreeSpecies species, long seed, int worldX, int groundY, int worldZ, BlockData trunk, BlockWriter writer) {
        int[][] directions = new int[][]{{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
        for (int i = 0; i < directions.length; ++i) {
            double roll = TreeShaper.blockRoll(seed, worldX, groundY, worldZ, 730 + i);
            int length = 2 + (int)(roll * 3.0);
            int dx = directions[i][0];
            int dz = directions[i][1];
            for (int step = 1; step <= length; ++step) {
                writer.set(worldX + dx * step, groundY, worldZ + dz * step, trunk);
                if (step > 2) continue;
                writer.set(worldX + dx * step, groundY + 1, worldZ + dz * step, trunk);
            }
        }
    }

    private static void placeBranches(DimensionPreset.TreeSpecies species, NoiseUtil noise, long seed, int worldX, int groundY, int worldZ, int trunkHeight, BlockData trunk, BlockData leaves, BlockData trunkAccent, BlockData canopyAccent, BlockWriter writer) {
        int branchCount = 1 + (int)(TreeShaper.blockRoll(seed, worldX, groundY, worldZ, 800) * 3.0);
        for (int b = 0; b < branchCount; ++b) {
            int salt = 810 + b * 10;
            double startFrac = 0.45 + TreeShaper.blockRoll(seed, worldX, groundY, worldZ, salt + 1) * 0.35;
            double angle = TreeShaper.blockRoll(seed, worldX, groundY, worldZ, salt + 2) * 2.0 * Math.PI;
            double lengthFrac = 0.25 + TreeShaper.blockRoll(seed, worldX, groundY, worldZ, salt + 3) * 0.25;
            int startY = groundY + (int)Math.round((double)trunkHeight * startFrac);
            int length = Math.max(3, (int)Math.round((double)trunkHeight * lengthFrac));
            double dxPerStep = Math.cos(angle) * 0.7;
            double dzPerStep = Math.sin(angle) * 0.7;
            double x = worldX;
            double y = startY;
            double z = worldZ;
            for (int step = 0; step < length; ++step) {
                int bx = (int)Math.round(x += dxPerStep);
                int by = (int)Math.round(y += 0.6);
                int bz = (int)Math.round(z += dzPerStep);
                BlockData block = trunk;
                if (trunkAccent != null && TreeShaper.blockRoll(seed, bx, by, bz, salt + 4) < species.trunkAccentChance) {
                    block = trunkAccent;
                }
                writer.set(bx, by, bz, block);
            }
            int tipX = (int)Math.round(x);
            int tipY = (int)Math.round(y);
            int tipZ = (int)Math.round(z);
            int miniRadius = Math.max(2, species.canopyRadius / 4);
            for (int dx = -miniRadius; dx <= miniRadius; ++dx) {
                for (int dy = -miniRadius; dy <= miniRadius; ++dy) {
                    for (int dz = -miniRadius; dz <= miniRadius; ++dz) {
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (dist > (double)miniRadius) continue;
                        int lx = tipX + dx;
                        int ly = tipY + dy;
                        int lz = tipZ + dz;
                        BlockData block = leaves;
                        if (canopyAccent != null && TreeShaper.blockRoll(seed, lx, ly, lz, salt + 5) < species.canopyAccentChance) {
                            block = canopyAccent;
                        }
                        writer.set(lx, ly, lz, block);
                    }
                }
            }
        }
    }

    private static double blockRoll(long seed, int x, int y, int z, int salt) {
        return DeterministicHash.hash01(seed, x * 92821 + y, z, salt);
    }

    private static BlockData dataOf(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        Material m = Material.matchMaterial((String)key);
        return m != null ? m.createBlockData() : null;
    }

    public static interface BlockWriter {
        public void set(int var1, int var2, int var3, BlockData var4);
    }
}
