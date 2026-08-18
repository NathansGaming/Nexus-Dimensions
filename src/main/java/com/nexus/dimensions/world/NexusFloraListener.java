/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.HeightMap
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.world.ChunkLoadEvent
 *  org.bukkit.plugin.Plugin
 */
package com.nexus.dimensions.world;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.DeterministicHash;
import com.nexus.dimensions.generation.TreeShaper;
import com.nexus.dimensions.generation.TreeSpeciesPicker;
import com.nexus.dimensions.generation.noise.NoiseUtil;
import com.nexus.dimensions.world.DimensionManager;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

public final class NexusFloraListener
implements Listener {
    private final DimensionManager dimensionManager;
    private final Map<String, BlockData> trunkCache = new HashMap<String, BlockData>();
    private final Map<String, BlockData> leafCache = new HashMap<String, BlockData>();
    private final Map<String, NoiseUtil> noiseCache = new HashMap<String, NoiseUtil>();

    public NexusFloraListener(Plugin plugin, DimensionManager dimensionManager) {
        this.dimensionManager = dimensionManager;
        plugin.getServer().getPluginManager().registerEvents((Listener)this, plugin);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            return;
        }
        World world = event.getWorld();
        DimensionPreset preset = this.dimensionManager.getPresetForWorld(world.getName());
        if (preset == null || !preset.isTier2() || !preset.trees.enabled) {
            return;
        }
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        if (DeterministicHash.hash01(world.getSeed(), chunkX, chunkZ, 1) > preset.trees.rarityPerChunk) {
            return;
        }
        int localX = (int)(DeterministicHash.hash01(world.getSeed(), chunkX, chunkZ, 2) * 16.0);
        int localZ = (int)(DeterministicHash.hash01(world.getSeed(), chunkX, chunkZ, 3) * 16.0);
        int worldX = (chunkX << 4) + localX;
        int worldZ = (chunkZ << 4) + localZ;
        int groundY = world.getHighestBlockYAt(worldX, worldZ, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        double heightRoll = DeterministicHash.hash01(world.getSeed(), chunkX, chunkZ, 4);
        DimensionPreset.TreeSpecies species = TreeSpeciesPicker.pick(preset.trees, world.getSeed(), chunkX, chunkZ);
        String cacheKey = preset.id + ":" + species.name;
        BlockData trunk = this.trunkCache.computeIfAbsent(cacheKey, k -> NexusFloraListener.materialOf(species.trunkBlock).createBlockData());
        BlockData leaves = this.leafCache.computeIfAbsent(cacheKey, k -> NexusFloraListener.materialOf(species.leafBlock).createBlockData());
        NoiseUtil noise = this.noiseCache.computeIfAbsent(preset.id, id -> new NoiseUtil(preset.seed != null ? preset.seed.longValue() : world.getSeed()));
        int maxY = world.getMaxHeight();
        TreeShaper.place(species, noise, world.getSeed(), worldX, groundY, worldZ, maxY, heightRoll, trunk, leaves, (x, y, z, data) -> {
            if (y >= world.getMinHeight() && y < world.getMaxHeight()) {
                world.getBlockAt(x, y, z).setBlockData(data, false);
            }
        });
    }

    private static Material materialOf(String key) {
        Material m = Material.matchMaterial((String)key);
        return m != null ? m : Material.OAK_LOG;
    }
}
