/*
 * Decompiled with CFR 0.152.
 */
package com.nexus.dimensions.structure;

public final class BlueprintTransform {
    private BlueprintTransform() {
    }

    public static int[] apply(int dx, int dz, int rotationStep, boolean mirror) {
        int x = mirror ? -dx : dx;
        int z = dz;
        int steps = (rotationStep % 4 + 4) % 4;
        for (int i = 0; i < steps; ++i) {
            int newX = -z;
            int newZ = x;
            x = newX;
            z = newZ;
        }
        return new int[]{x, z};
    }
}
