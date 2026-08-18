/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.loot.LootTable
 *  org.bukkit.loot.LootTables
 *  org.bukkit.loot.Lootable
 *  org.bukkit.plugin.Plugin
 */
package com.nexus.dimensions.world;

import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.loot.Lootable;
import org.bukkit.plugin.Plugin;

public final class StructureLootService {
    private final Logger logger;
    private final ConcurrentLinkedQueue<PendingLoot> pending = new ConcurrentLinkedQueue();

    public StructureLootService(Plugin plugin) {
        this.logger = plugin.getLogger();
        Bukkit.getScheduler().runTaskTimer(plugin, this::drain, 20L, 10L);
    }

    public void enqueue(String worldName, int x, int y, int z, String lootTableKey) {
        if (lootTableKey == null || lootTableKey.isBlank()) {
            return;
        }
        this.pending.add(new PendingLoot(worldName, x, y, z, lootTableKey));
    }

    private void drain() {
        PendingLoot item;
        while ((item = this.pending.poll()) != null) {
            this.apply(item);
        }
    }

    private void apply(PendingLoot item) {
        World world = Bukkit.getWorld((String)item.worldName());
        if (world == null) {
            this.logger.warning("[NexusDimensions] Dropped a pending loot assignment for unloaded world '" + item.worldName() + "'.");
            return;
        }
        LootTable table = this.resolveLootTable(item.lootTableKey());
        if (table == null) {
            return;
        }
        Block block = world.getBlockAt(item.x(), item.y(), item.z());
        BlockState state = block.getState();
        if (state instanceof Lootable) {
            Lootable lootable = (Lootable)state;
            lootable.setLootTable(table);
            state.update();
        } else {
            this.logger.warning("[NexusDimensions] Expected a lootable block at " + item.x() + "," + item.y() + "," + item.z() + " in '" + item.worldName() + "' but found " + String.valueOf(block.getType()) + " \u2014 the structure populator may have placed something other than a chest for a loot: true entry.");
        }
    }

    private LootTable resolveLootTable(String key) {
        try {
            LootTables table = LootTables.valueOf((String)key.trim().toUpperCase(Locale.ROOT));
            return table.getLootTable();
        }
        catch (IllegalArgumentException e) {
            this.logger.warning("[NexusDimensions] '" + key + "' isn't a known org.bukkit.loot.LootTables name.");
            return null;
        }
    }

    private record PendingLoot(String worldName, int x, int y, int z, String lootTableKey) {
    }
}
