/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.generator.BiomeProvider
 *  org.bukkit.generator.BlockPopulator
 *  org.bukkit.generator.ChunkGenerator
 *  org.bukkit.generator.ChunkGenerator$ChunkData
 *  org.bukkit.generator.WorldInfo
 */
package com.nexus.dimensions.generation;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.generation.DecorationPopulator;
import com.nexus.dimensions.generation.Density3DSampler;
import com.nexus.dimensions.generation.GiantTreePopulator;
import com.nexus.dimensions.generation.GroundHeightSource;
import com.nexus.dimensions.generation.NexusBiomeProvider;
import com.nexus.dimensions.generation.StructurePopulator;
import com.nexus.dimensions.generation.TerrainHeightSampler;
import com.nexus.dimensions.generation.noise.NoiseUtil;
import com.nexus.dimensions.structure.Blueprint;
import com.nexus.dimensions.world.StructureLootService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

public final class NexusChunkGenerator
extends ChunkGenerator {
    private final DimensionPreset preset;
    private final NoiseUtil noise;
    private final TerrainHeightSampler heightSampler;
    private final Density3DSampler density3DSampler;
    private final GroundHeightSource groundHeightSource;
    private final Blueprint blueprint;
    private final StructureLootService lootService;
    private final BlockData surface;
    private final BlockData subsurface;
    private final BlockData deep;
    private final BlockData liquid;
    private final int liquidLevel;
    private final Map<String, BlockData> extraBlockCache = new HashMap<String, BlockData>();

    private static double featureOffset(int index) {
        return (double)(index + 1) * 73856.0;
    }

    public NexusChunkGenerator(DimensionPreset preset, long worldSeed, Map<String, Blueprint> blueprints, StructureLootService lootService) {
        this.preset = preset;
        this.blueprint = preset.structures.enabled ? blueprints.get(preset.structures.blueprint) : null;
        this.lootService = lootService;
        if (preset.structures.enabled && this.blueprint == null) {
            Bukkit.getLogger().warning("[NexusDimensions] Preset '" + preset.id + "': structures.blueprint '" + preset.structures.blueprint + "' doesn't match any loaded blueprints/*.yml file \u2014 structures disabled for this dimension.");
        }
        this.noise = new NoiseUtil(preset.seed != null ? preset.seed : worldSeed);
        if (preset.terrain.isDensity3D()) {
            this.density3DSampler = new Density3DSampler(preset, this.noise);
            this.heightSampler = null;
            this.groundHeightSource = this.density3DSampler;
        } else {
            this.heightSampler = new TerrainHeightSampler(preset, this.noise);
            this.density3DSampler = null;
            this.groundHeightSource = this.heightSampler;
        }
        this.surface = NexusChunkGenerator.materialOf(preset.palette.surfaceBlock).createBlockData();
        this.subsurface = NexusChunkGenerator.materialOf(preset.palette.subsurfaceBlock).createBlockData();
        this.deep = NexusChunkGenerator.materialOf(preset.palette.deepBlock).createBlockData();
        this.liquid = NexusChunkGenerator.materialOf(preset.palette.liquidBlock).createBlockData();
        this.liquidLevel = preset.palette.liquidLevel >= 0 ? preset.palette.liquidLevel : preset.terrain.seaLevel;
    }

    private static Material materialOf(String key) {
        Material m = Material.matchMaterial((String)key);
        return m != null ? m : Material.STONE;
    }

    public boolean shouldGenerateNoise() {
        return true;
    }

    public boolean shouldGenerateSurface() {
        return false;
    }

    public boolean shouldGenerateBedrock() {
        return true;
    }

    public boolean shouldGenerateCaves() {
        return false;
    }

    public boolean shouldGenerateDecorations() {
        return this.preset.flavor.generateDecorations;
    }

    public boolean shouldGenerateMobs() {
        return true;
    }

    public boolean shouldGenerateStructures() {
        return this.preset.flavor.generateStructures;
    }

    public boolean isParallelCapable() {
        return true;
    }

    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
        if (this.preset.terrain.isDensity3D()) {
            this.generateDensity3D(chunkX, chunkZ, chunkData);
        } else {
            this.generateHeightmap(chunkX, chunkZ, chunkData);
        }
    }

    private void generateDensity3D(int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
        int minY = chunkData.getMinHeight();
        int maxY = chunkData.getMaxHeight();
        int subsurfaceDepth = this.preset.palette.subsurfaceDepth;
        for (int x = 0; x < 16; ++x) {
            int worldX = (chunkX << 4) + x;
            for (int z = 0; z < 16; ++z) {
                int worldZ = (chunkZ << 4) + z;
                BlockData columnSurface = this.resolveVariant(worldX, worldZ, true);
                BlockData columnSubsurface = this.resolveVariant(worldX, worldZ, false);
                block8: for (int y = minY; y < maxY; ++y) {
                    Density3DSampler.BlockClass cls = this.density3DSampler.classify(worldX, y, worldZ, subsurfaceDepth);
                    switch (cls) {
                        case SURFACE: {
                            chunkData.setBlock(x, y, z, columnSurface != null ? columnSurface : this.surface);
                            continue block8;
                        }
                        case SUBSURFACE: {
                            chunkData.setBlock(x, y, z, columnSubsurface != null ? columnSubsurface : this.subsurface);
                            continue block8;
                        }
                        case DEEP: {
                            BlockData deposit = this.resolveGlowDeposit(worldX, y, worldZ);
                            chunkData.setBlock(x, y, z, deposit != null ? deposit : this.deep);
                            continue block8;
                        }
                        case AIR: {
                            if (!this.preset.terrain.density3d.liquids || y > this.liquidLevel) continue block8;
                            chunkData.setBlock(x, y, z, this.liquid);
                        }
                    }
                }
            }
        }
    }

    private void generateHeightmap(int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
        int minY = chunkData.getMinHeight();
        int maxY = chunkData.getMaxHeight();
        DimensionPreset.Terrain t = this.preset.terrain;
        boolean cellularCaves = t.caves.mode.equalsIgnoreCase("cellular");
        for (int x = 0; x < 16; ++x) {
            int worldX = (chunkX << 4) + x;
            for (int z = 0; z < 16; ++z) {
                int worldZ = (chunkZ << 4) + z;
                int columnHeight = this.heightSampler.columnHeight(worldX, worldZ, minY, maxY);
                BlockData columnSurface = this.resolveVariant(worldX, worldZ, true);
                BlockData columnSubsurface = this.resolveVariant(worldX, worldZ, false);
                for (int y = minY; y < maxY; ++y) {
                    if (y > columnHeight) {
                        if (y > this.liquidLevel) continue;
                        chunkData.setBlock(x, y, z, this.liquid);
                        continue;
                    }
                    boolean carved = false;
                    if (t.caves.enabled && y > minY + 5 && y < columnHeight - 2) {
                        if (cellularCaves) {
                            double cell = this.noise.worley3D(worldX, y, worldZ, t.caves.frequency, t.caves.cellularJitter);
                            carved = cell < t.caves.cellularThreshold;
                        } else {
                            double cave = this.noise.fbm3D(worldX, y, worldZ, t.caves.frequency, 3, 2.0, 0.5);
                            boolean bl = carved = cave > t.caves.threshold;
                        }
                    }
                    if (carved) continue;
                    if (y == columnHeight) {
                        BlockData block;
                        BlockData blockData = block = y <= this.liquidLevel ? columnSubsurface : columnSurface;
                        chunkData.setBlock(x, y, z, block != null ? block : (y <= this.liquidLevel ? this.subsurface : this.surface));
                        continue;
                    }
                    if (y > columnHeight - this.preset.palette.subsurfaceDepth) {
                        chunkData.setBlock(x, y, z, columnSubsurface != null ? columnSubsurface : this.subsurface);
                        continue;
                    }
                    BlockData deposit = this.resolveGlowDeposit(worldX, y, worldZ);
                    chunkData.setBlock(x, y, z, deposit != null ? deposit : this.deep);
                }
            }
        }
    }

    private BlockData resolveVariant(int worldX, int worldZ, boolean surfaceLayer) {
        List<DimensionPreset.PaletteVariant> variants = this.preset.palette.variants;
        for (int i = 0; i < variants.size(); ++i) {
            double offset;
            double sample;
            String blockKey;
            DimensionPreset.PaletteVariant v = variants.get(i);
            String string = blockKey = surfaceLayer ? v.surfaceBlock : v.subsurfaceBlock;
            if (blockKey == null || !((sample = this.noise.fbm2D((double)worldX + (offset = NexusChunkGenerator.featureOffset(i)), (double)worldZ + offset, v.frequency, 3, 2.0, 0.5, false, 0.0)) > v.threshold)) continue;
            return this.extraBlockCache.computeIfAbsent(blockKey, k -> NexusChunkGenerator.materialOf(k).createBlockData());
        }
        return null;
    }

    private BlockData resolveGlowDeposit(int worldX, int y, int worldZ) {
        List<DimensionPreset.GlowDeposit> deposits = this.preset.palette.glowDeposits;
        for (int i = 0; i < deposits.size(); ++i) {
            DimensionPreset.GlowDeposit dep = deposits.get(i);
            double offset = NexusChunkGenerator.featureOffset(i);
            double sample = this.noise.fbm3D((double)worldX + offset, (double)y + offset, (double)worldZ + offset, dep.frequency, 2, 2.0, 0.5);
            if (!(sample > dep.threshold)) continue;
            return this.extraBlockCache.computeIfAbsent(dep.block, k -> NexusChunkGenerator.materialOf(k).createBlockData());
        }
        return null;
    }

    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new NexusBiomeProvider(this.preset);
    }

    public List<BlockPopulator> getDefaultPopulators(World world) {
        ArrayList<BlockPopulator> populators = new ArrayList<BlockPopulator>();
        if (this.preset.trees.enabled) {
            populators.add(new GiantTreePopulator(this.preset, this.groundHeightSource, this.noise));
        }
        if (this.preset.decorations.enabled && !this.preset.decorations.species.isEmpty()) {
            populators.add(new DecorationPopulator(this.preset, this.groundHeightSource));
        }
        if (this.preset.structures.enabled && this.blueprint != null) {
            populators.add(new StructurePopulator(this.preset, this.blueprint, this.groundHeightSource, this.lootService));
        }
        return populators;
    }
}
