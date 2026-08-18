/*
 * Decompiled with CFR 0.152.
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.GroundHeightSource;
import com.nexus.dimensions.generation.noise.NoiseUtil;

public final class TerrainHeightSampler
implements GroundHeightSource {
    private final DimensionPreset preset;
    private final NoiseUtil noise;

    public TerrainHeightSampler(DimensionPreset preset, NoiseUtil noise) {
        this.preset = preset;
        this.noise = noise;
    }

    @Override
    public int groundHeight(int worldX, int worldZ, int minY, int maxY) {
        return this.columnHeight(worldX, worldZ, minY, maxY);
    }

    public int columnHeight(int worldX, int worldZ, int minY, int maxY) {
        DimensionPreset.Terrain t = this.preset.terrain;
        DimensionPreset.Noise n = t.noise;
        double base = this.noise.fbm2D(worldX, worldZ, n.frequency, n.octaves, n.lacunarity, n.gain, n.ridged, n.warp);
        int columnHeight = t.baseHeight + (int)Math.round(base * (double)t.heightVariation);
        if (t.craters.enabled) {
            double crater = this.noise.worley2D(worldX, worldZ, t.craters.frequency, t.craters.jitter);
            if (crater < 0.35) {
                double bowl = 1.0 - crater / 0.35;
                columnHeight -= (int)Math.round(bowl * (double)t.craters.depth);
            } else if (crater < 0.5) {
                double rim = 1.0 - (crater - 0.35) / 0.15;
                columnHeight += (int)Math.round(rim * (double)t.craters.rimHeight);
            }
        }
        return Math.max(minY + 1, Math.min(maxY - 1, columnHeight));
    }
}
