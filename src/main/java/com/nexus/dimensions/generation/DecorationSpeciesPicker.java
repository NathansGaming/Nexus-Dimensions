/*
 * Decompiled with CFR 0.152.
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.DeterministicHash;
import java.util.List;

public final class DecorationSpeciesPicker {
    private DecorationSpeciesPicker() {
    }

    public static DimensionPreset.DecorationSpecies pick(List<DimensionPreset.DecorationSpecies> species, long seed, int chunkX, int chunkZ, int attempt) {
        double totalWeight = 0.0;
        for (DimensionPreset.DecorationSpecies s : species) {
            totalWeight += Math.max(0.0, s.weight);
        }
        if (totalWeight <= 0.0) {
            return species.get(0);
        }
        int salt = 900 + attempt * 10 + 4;
        double roll = DeterministicHash.hash01(seed, chunkX, chunkZ, salt) * totalWeight;
        double cumulative = 0.0;
        for (DimensionPreset.DecorationSpecies s : species) {
            if (!(roll < (cumulative += Math.max(0.0, s.weight)))) continue;
            return s;
        }
        return species.get(species.size() - 1);
    }
}
