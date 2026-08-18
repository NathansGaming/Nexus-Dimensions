/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.block.Biome
 *  org.bukkit.generator.BiomeProvider
 *  org.bukkit.generator.WorldInfo
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.noise.NoiseUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

public final class NexusBiomeProvider
extends BiomeProvider {
    private final DimensionPreset preset;
    private final NoiseUtil blendNoise;
    private final List<Biome> resolvedBiomes;
    private final List<Double> cumulativeWeights;

    public NexusBiomeProvider(DimensionPreset preset) {
        this.preset = preset;
        long blendSeed = preset.seed != null ? preset.seed : (long)preset.id.hashCode();
        this.blendNoise = new NoiseUtil(blendSeed ^ 0x5EEDB10DL);
        ArrayList<Biome> biomes = new ArrayList<Biome>();
        ArrayList<Double> weights = new ArrayList<Double>();
        double total = 0.0;
        for (DimensionPreset.BiomeEntry entry : preset.biomes.entries) {
            Biome biome = NexusBiomeProvider.resolve(entry.id);
            biomes.add(biome);
            weights.add(total += Math.max(1.0E-4, entry.weight));
        }
        if (biomes.isEmpty()) {
            biomes.add(Biome.PLAINS);
            weights.add(1.0);
            total = 1.0;
        }
        this.resolvedBiomes = biomes;
        this.cumulativeWeights = weights;
    }

    private static Biome resolve(String id) {
        if (id == null) {
            return Biome.PLAINS;
        }
        NamespacedKey key = NamespacedKey.fromString((String)id.toLowerCase());
        if (key == null) {
            return Biome.PLAINS;
        }
        Biome biome = (Biome)Registry.BIOME.get(key);
        return biome != null ? biome : Biome.PLAINS;
    }

    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
        if (!"blended".equalsIgnoreCase(this.preset.biomes.mode) || this.resolvedBiomes.size() == 1) {
            return this.resolvedBiomes.get(0);
        }
        double n = (this.blendNoise.fbm2D(x, z, 0.004, 3, 2.0, 0.5, false, 0.0) + 1.0) / 2.0;
        double total = this.cumulativeWeights.get(this.cumulativeWeights.size() - 1);
        double target = n * total;
        for (int i = 0; i < this.cumulativeWeights.size(); ++i) {
            if (!(target <= this.cumulativeWeights.get(i))) continue;
            return this.resolvedBiomes.get(i);
        }
        return this.resolvedBiomes.get(this.resolvedBiomes.size() - 1);
    }

    public List<Biome> getBiomes(WorldInfo worldInfo) {
        return this.resolvedBiomes;
    }
}
