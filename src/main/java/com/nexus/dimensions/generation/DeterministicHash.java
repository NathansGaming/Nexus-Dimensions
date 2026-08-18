/*
 * Decompiled with CFR 0.152.
 */
package com.nexus.dimensions.generation;

public final class DeterministicHash {
    private DeterministicHash() {
    }

    public static double hash01(long seed, int chunkX, int chunkZ, int salt) {
        long h = seed;
        h = h * 6364136223846793005L + (long)chunkX * 1442695040888963407L;
        h = h * 6364136223846793005L + (long)chunkZ * 1442695040888963407L;
        h = h * 6364136223846793005L + (long)salt * 1442695040888963407L;
        h ^= h >>> 33;
        h *= -49064778989728563L;
        h ^= h >>> 33;
        return (double)(h & 0xFFFFFFFFFFFFFL) / 4.503599627370495E15;
    }
}
