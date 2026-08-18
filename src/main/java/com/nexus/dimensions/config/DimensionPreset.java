/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World$Environment
 */
package com.nexus.dimensions.config;

import java.util.List;
import java.util.Map;
import org.bukkit.World;

public final class DimensionPreset {
    public String id;
    public String displayName;
    public WorldHeight worldHeight;
    public Sky sky;
    public World.Environment environment = World.Environment.NORMAL;
    public Long seed;
    public Terrain terrain = new Terrain();
    public Palette palette = new Palette();
    public Biomes biomes = new Biomes();
    public List<CustomBiome> customBiomes = List.of();
    public Trees trees = new Trees();
    public Decorations decorations = new Decorations();
    public Flavor flavor = new Flavor();
    public Particles particles = new Particles();
    public Structures structures = new Structures();
    public Creatures creatures = new Creatures();
    public Seasons seasons = new Seasons();

    public boolean isTier2() {
        return this.worldHeight != null || this.sky != null || !this.customBiomes.isEmpty();
    }

    public WorldHeight resolvedWorldHeight() {
        if (this.worldHeight != null) {
            return this.worldHeight;
        }
        if (this.sky == null) {
            return null;
        }
        WorldHeight wh = new WorldHeight();
        wh.minY = -64;
        wh.height = 384;
        switch (this.sky.effects) {
            case "minecraft:the_nether": {
                wh.hasSkylight = false;
                wh.hasCeiling = true;
                wh.ambientLight = 0.1;
                break;
            }
            case "minecraft:the_end": {
                wh.hasSkylight = false;
                wh.hasCeiling = false;
                wh.ambientLight = 0.0;
                break;
            }
            default: {
                wh.hasSkylight = true;
                wh.hasCeiling = false;
                wh.ambientLight = 0.0;
            }
        }
        wh.effects = this.sky.effects;
        wh.fixedTime = this.sky.fixedTime;
        return wh;
    }

    public static final class Terrain {
        public String mode = "heightmap";
        public int seaLevel = 63;
        public int baseHeight = 68;
        public int heightVariation = 40;
        public Noise noise = new Noise();
        public Craters craters = new Craters();
        public Caves caves = new Caves();
        public Density3D density3d = new Density3D();

        public boolean isDensity3D() {
            return "density3d".equalsIgnoreCase(this.mode);
        }
    }

    public static final class Palette {
        public String surfaceBlock = "minecraft:grass_block";
        public String subsurfaceBlock = "minecraft:dirt";
        public int subsurfaceDepth = 4;
        public String deepBlock = "minecraft:stone";
        public String liquidBlock = "minecraft:water";
        public int liquidLevel = -1;
        public List<PaletteVariant> variants = List.of();
        public List<GlowDeposit> glowDeposits = List.of();
    }

    public static final class Biomes {
        public String mode = "single";
        public List<BiomeEntry> entries = List.of(new BiomeEntry());
    }

    public static final class Trees {
        public boolean enabled = false;
        public int minHeight = 6;
        public int maxHeight = 12;
        public int canopyRadius = 4;
        public String trunkBlock = "minecraft:oak_log";
        public String leafBlock = "minecraft:oak_leaves";
        public double rarityPerChunk = 0.1;
        public int giantCanopyLayers = 6;
        public boolean branches = true;
        public boolean buttressRoots = true;
        public String canopyAccentBlock;
        public double canopyAccentChance = 0.03;
        public String trunkAccentBlock;
        public double trunkAccentChance = 0.02;
        public String vineBlock;
        public double vineChance = 0.15;
        public int vineMinLength = 2;
        public int vineMaxLength = 6;
        public List<TreeSpecies> species = List.of();
    }

    public static final class Decorations {
        public boolean enabled = false;
        public int perChunkAttempts = 3;
        public double chancePerAttempt = 0.25;
        public List<DecorationSpecies> species = List.of();
    }

    public static final class Flavor {
        public double gravity = 1.0;
        public Boolean allowJumping;
        public boolean alwaysClearWeather = false;
        public boolean generateStructures = false;
        public boolean generateDecorations = false;
        public boolean generateVanillaCaves = false;
    }

    public static final class Particles {
        public boolean enabled = false;
        public String type = "ASH";
        public String color = "0xC9A8FF";
        public String toColor;
        public float size = 1.0f;
        public int density = 25;
        public int radius = 14;
        public int heightSpread = 6;
        public int intervalTicks = 4;
        public double windStrength = 0.0;
    }

    public static final class Structures {
        public boolean enabled = false;
        public String blueprint;
        public double rarityPerChunk = 0.005;
        public String lootTable;
        public boolean randomRotation = false;
        public boolean randomMirror = false;
    }

    public static final class Creatures {
        public boolean enabled = false;
        public double spawnMultiplier = 1.0;
        public Map<String, MobProfile> mobs = Map.of();
    }

    public static final class Seasons {
        public boolean enabled = false;
        public List<SeasonStage> stages = List.of();
    }

    public static final class WorldHeight {
        public int minY = -64;
        public int height = 384;
        public boolean hasCeiling = false;
        public boolean hasSkylight = true;
        public double ambientLight = 0.0;
        public String effects = "minecraft:overworld";
        public Long fixedTime;
        public boolean ultrawarm = false;
        public boolean natural = true;
        public boolean piglinSafe = false;
        public boolean bedWorks = true;
        public boolean respawnAnchorWorks = false;
        public boolean hasRaids = true;
    }

    public static final class Sky {
        public String effects = "minecraft:the_end";
        public Long fixedTime;
    }

    public static final class SeasonStage {
        public String name = "default";
        public int durationTicks = 24000;
        public Particles particles;
        public Double spawnMultiplierOverride;
        public Boolean forceClearWeather;
    }

    public static final class MobProfile {
        public String displayName;
        public boolean alwaysShowName = false;
        public double healthMultiplier = 1.0;
        public double speedMultiplier = 1.0;
        public double damageMultiplier = 1.0;
        public double scale = 1.0;
        public boolean glowing = false;
        public Map<String, String> equipment = Map.of();
    }

    public static final class DecorationSpecies {
        public String name = "default";
        public double weight = 1.0;
        public String block = "minecraft:stone";
        public int minHeight = 1;
        public int maxHeight = 1;
        public String capBlock;
        public int capRadius = 0;
        public int minFloatHeight = 0;
        public int maxFloatHeight = 0;
    }

    public static final class TreeSpecies {
        public String name = "default";
        public double weight = 1.0;
        public int minHeight = 6;
        public int maxHeight = 12;
        public int canopyRadius = 4;
        public String trunkBlock = "minecraft:oak_log";
        public String leafBlock = "minecraft:oak_leaves";
        public int giantCanopyLayers = 6;
        public boolean branches = true;
        public boolean buttressRoots = true;
        public String canopyAccentBlock;
        public double canopyAccentChance = 0.03;
        public String trunkAccentBlock;
        public double trunkAccentChance = 0.02;
        public String vineBlock;
        public double vineChance = 0.15;
        public int vineMinLength = 2;
        public int vineMaxLength = 6;
    }

    public static final class CustomBiome {
        public String id;
        public String category = "none";
        public double temperature = 0.8;
        public double downfall = 0.4;
        public String skyColor = "0x78A7FF";
        public String fogColor = "0xC0D8FF";
        public String waterColor = "0x3F76E4";
        public String waterFogColor = "0x050533";
    }

    public static final class BiomeEntry {
        public String id = "minecraft:plains";
        public double weight = 1.0;
    }

    public static final class GlowDeposit {
        public String block = "minecraft:glowstone";
        public double frequency = 0.05;
        public double threshold = 0.42;
    }

    public static final class PaletteVariant {
        public String name = "variant";
        public String surfaceBlock;
        public String subsurfaceBlock;
        public double frequency = 0.015;
        public double threshold = 0.15;
    }

    public static final class Caves {
        public boolean enabled = false;
        public double frequency = 0.02;
        public double threshold = 0.6;
        public String mode = "noise";
        public double cellularThreshold = 0.32;
        public double cellularJitter = 0.9;
    }

    public static final class Craters {
        public boolean enabled = false;
        public double frequency = 0.015;
        public int depth = 18;
        public int rimHeight = 5;
        public double jitter = 0.8;
    }

    public static final class Noise {
        public double frequency = 0.01;
        public int octaves = 4;
        public double lacunarity = 2.0;
        public double gain = 0.5;
        public boolean ridged = false;
        public double warp = 0.0;
    }

    public static final class Band {
        public int center = 100;
        public int thickness = 20;
    }

    public static final class Density3D {
        public double threshold = 0.0;
        public double verticalFalloff = 0.02;
        public List<Band> bands = List.of();
        public String shape = "bands";
        public double spireFrequency = 0.02;
        public double spireJitter = 0.9;
        public double spireCoreFraction = 0.18;
        public double spireStrength = 2.5;
        public boolean liquids = false;
    }
}
