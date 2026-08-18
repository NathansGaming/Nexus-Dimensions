/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.HeightMap
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.world.ChunkLoadEvent
 *  org.bukkit.plugin.Plugin
 */
package com.nexus.dimensions.world;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.DeterministicHash;
import com.nexus.dimensions.structure.Blueprint;
import com.nexus.dimensions.structure.BlueprintTransform;
import com.nexus.dimensions.world.DimensionManager;
import com.nexus.dimensions.world.StructureLootService;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

public final class NexusStructureListener
implements Listener {
    private final DimensionManager dimensionManager;
    private final StructureLootService lootService;

    public NexusStructureListener(Plugin plugin, DimensionManager dimensionManager, StructureLootService lootService) {
        this.dimensionManager = dimensionManager;
        this.lootService = lootService;
        plugin.getServer().getPluginManager().registerEvents((Listener)this, plugin);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            return;
        }
        World world = event.getWorld();
        DimensionPreset preset = this.dimensionManager.getPresetForWorld(world.getName());
        if (preset == null || !preset.isTier2() || !preset.structures.enabled) {
            return;
        }
        Blueprint blueprint = this.dimensionManager.getBlueprint(preset.structures.blueprint);
        if (blueprint == null || blueprint.blocks.isEmpty()) {
            return;
        }
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        if (DeterministicHash.hash01(world.getSeed(), chunkX, chunkZ, 20) > preset.structures.rarityPerChunk) {
            return;
        }
        int localX = (int)(DeterministicHash.hash01(world.getSeed(), chunkX, chunkZ, 21) * 16.0);
        int localZ = (int)(DeterministicHash.hash01(world.getSeed(), chunkX, chunkZ, 22) * 16.0);
        int worldX = (chunkX << 4) + localX;
        int worldZ = (chunkZ << 4) + localZ;
        int groundY = world.getHighestBlockYAt(worldX, worldZ, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        int rotationStep = preset.structures.randomRotation ? (int)(DeterministicHash.hash01(world.getSeed(), chunkX, chunkZ, 23) * 4.0) : 0;
        boolean mirror = preset.structures.randomMirror && DeterministicHash.hash01(world.getSeed(), chunkX, chunkZ, 24) < 0.5;
        for (Blueprint.BlockEntry entry : blueprint.blocks) {
            int[] transformed = BlueprintTransform.apply(entry.dx, entry.dz, rotationStep, mirror);
            int x = worldX + transformed[0];
            int y = groundY + 1 + entry.dy;
            int z = worldZ + transformed[1];
            if (y < minY || y >= maxY) continue;
            if (entry.loot) {
                world.getBlockAt(x, y, z).setType(Material.CHEST, false);
                this.lootService.enqueue(world.getName(), x, y, z, preset.structures.lootTable);
                continue;
            }
            world.getBlockAt(x, y, z).setBlockData(NexusStructureListener.materialOf(entry.block).createBlockData(), false);
        }
    }

    private static Material materialOf(String key) {
        Material m = Material.matchMaterial((String)key);
        return m != null ? m : Material.STONE;
    }
}
