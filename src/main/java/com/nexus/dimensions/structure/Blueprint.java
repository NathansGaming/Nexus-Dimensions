/*
 * Decompiled with CFR 0.152.
 */
package com.nexus.dimensions.structure;

import java.util.List;

public final class Blueprint {
    public String name;
    public List<BlockEntry> blocks = List.of();

    public static final class BlockEntry {
        public int dx;
        public int dy;
        public int dz;
        public String block = "minecraft:stone";
        public boolean loot = false;
    }
}
