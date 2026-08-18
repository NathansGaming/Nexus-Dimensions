/*
 * Decompiled with CFR 0.152.
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.GroundHeightSource;
import com.nexus.dimensions.generation.noise.NoiseUtil;
import java.util.List;

public final class Density3DSampler
implements GroundHeightSource {
    private static final double MARGIN = 0.15;
    private final DimensionPreset preset;
    private final NoiseUtil noise;
    private final List<DimensionPreset.Band> bands;

    public Density3DSampler(DimensionPreset preset, NoiseUtil noise) {
        this.preset = preset;
        this.noise = noise;
        if (!preset.terrain.density3d.bands.isEmpty()) {
            this.bands = preset.terrain.density3d.bands;
        } else {
            DimensionPreset.Band implicit = new DimensionPreset.Band();
            implicit.center = preset.terrain.baseHeight;
            implicit.thickness = Math.max(4, preset.terrain.heightVariation);
            this.bands = List.of(implicit);
        }
    }

    public double density(int x, int y, int z) {
        DimensionPreset.Noise n = this.preset.terrain.noise;
        double raw = this.noise.fbm3D(x, y, z, n.frequency, n.octaves, n.lacunarity, n.gain);
        double minPenalty = Double.MAX_VALUE;
        for (DimensionPreset.Band band : this.bands) {
            double dist = Math.abs(y - band.center);
            double t = dist / Math.max(1.0, (double)band.thickness);
            double penalty = t * t * this.preset.terrain.density3d.verticalFalloff * (double)band.thickness;
            if (!(penalty < minPenalty)) continue;
            minPenalty = penalty;
        }
        double value = raw - minPenalty;
        if (this.preset.terrain.density3d.shape.equalsIgnoreCase("spires")) {
            value += this.spireBonus(x, z);
        }
        return value;
    }

    private double spireBonus(int x, int z) {
        DimensionPreset.Density3D d3 = this.preset.terrain.density3d;
        double dist = this.noise.worley2D(x, z, d3.spireFrequency, d3.spireJitter);
        double core = Math.max(0.0, 1.0 - dist / Math.max(0.01, d3.spireCoreFraction));
        return d3.spireStrength * core * core;
    }

    public boolean isSolid(int x, int y, int z) {
        return this.density(x, y, z) > this.preset.terrain.density3d.threshold;
    }

    public BlockClass classify(int x, int y, int z, int subsurfaceDepth) {
        double threshold;
        double d = this.density(x, y, z);
        if (d <= (threshold = this.preset.terrain.density3d.threshold)) {
            return BlockClass.AIR;
        }
        if (Math.abs(d - threshold) > 0.15) {
            return BlockClass.DEEP;
        }
        if (!(this.isSolid(x + 1, y, z) && this.isSolid(x - 1, y, z) && this.isSolid(x, y + 1, z) && this.isSolid(x, y - 1, z) && this.isSolid(x, y, z + 1) && this.isSolid(x, y, z - 1))) {
            return BlockClass.SURFACE;
        }
        int r = Math.max(1, subsurfaceDepth);
        if (!(this.isSolid(x + r, y, z) && this.isSolid(x - r, y, z) && this.isSolid(x, y + r, z) && this.isSolid(x, y - r, z) && this.isSolid(x, y, z + r) && this.isSolid(x, y, z - r))) {
            return BlockClass.SUBSURFACE;
        }
        return BlockClass.DEEP;
    }

    @Override
    public int groundHeight(int worldX, int worldZ, int minY, int maxY) {
        for (int y = maxY - 1; y >= minY; --y) {
            if (!this.isSolid(worldX, y, worldZ)) continue;
            return y;
        }
        return minY;
    }

    public static enum BlockClass {
        AIR,
        SURFACE,
        SUBSURFACE,
        DEEP;

    }
}
