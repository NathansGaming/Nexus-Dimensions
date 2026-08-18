/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.util.Vector
 */
package com.nexus.dimensions.world;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.world.DimensionManager;
import com.nexus.dimensions.world.SeasonService;
import java.util.Locale;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class AmbientParticleService {
    private static final double MAX_WIND_PER_TICK = 0.3;
    private final DimensionManager dimensionManager;
    private final SeasonService seasonService;
    private final Random random = new Random();
    private long tick = 0L;

    public AmbientParticleService(Plugin plugin, DimensionManager dimensionManager) {
        this(plugin, dimensionManager, null);
    }

    public AmbientParticleService(Plugin plugin, DimensionManager dimensionManager, SeasonService seasonService) {
        this.dimensionManager = dimensionManager;
        this.seasonService = seasonService;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
    }

    private void tick() {
        ++this.tick;
        double windAngle = (double)(this.tick % 12000L) / 12000.0 * (Math.PI * 2);
        for (Player player : Bukkit.getOnlinePlayers()) {
            DimensionPreset.Particles cfg;
            String worldName = player.getWorld().getName();
            DimensionPreset preset = this.dimensionManager.getPresetForWorld(worldName);
            if (preset == null) continue;
            DimensionPreset.Particles particles = cfg = this.seasonService != null ? this.seasonService.effectiveParticles(worldName, preset) : preset.particles;
            if (!cfg.enabled || this.tick % (long)cfg.intervalTicks != 0L) continue;
            this.spawnBatch(player, cfg);
            if (!(cfg.windStrength > 0.0)) continue;
            this.nudge(player, cfg.windStrength, windAngle);
        }
    }

    private void spawnBatch(Player player, DimensionPreset.Particles cfg) {
        World world = player.getWorld();
        Particle particle = AmbientParticleService.resolveParticle(cfg.type);
        Particle.DustOptions data = "DUST".equals(cfg.type) ? AmbientParticleService.dustOptions(cfg) : null;
        Location eye = player.getEyeLocation();
        for (int i = 0; i < cfg.density; ++i) {
            double angle = this.random.nextDouble() * 2.0 * Math.PI;
            double dist = this.random.nextDouble() * (double)cfg.radius;
            double dx = Math.cos(angle) * dist;
            double dz = Math.sin(angle) * dist;
            double dy = (this.random.nextDouble() - 0.5) * (double)cfg.heightSpread;
            Location loc = eye.clone().add(dx, dy, dz);
            if (data != null) {
                world.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0, (Object)data);
                continue;
            }
            world.spawnParticle(particle, loc, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void nudge(Player player, double strength, double sharedAngle) {
        double magnitude = Math.min(strength, 0.3);
        Vector wind = new Vector(Math.cos(sharedAngle) * magnitude, 0.0, Math.sin(sharedAngle) * magnitude);
        Vector result = player.getVelocity().add(wind);
        if (result.length() > 1.2) {
            result = result.normalize().multiply(1.2);
        }
        player.setVelocity(result);
    }

    private static Particle resolveParticle(String typeName) {
        try {
            return Particle.valueOf((String)typeName);
        }
        catch (IllegalArgumentException e) {
            return Particle.ASH;
        }
    }

    private static Particle.DustOptions dustOptions(DimensionPreset.Particles cfg) {
        return new Particle.DustOptions(AmbientParticleService.parseColor(cfg.color), cfg.size);
    }

    private static Color parseColor(String hex) {
        if (hex == null) {
            return Color.WHITE;
        }
        String cleaned = hex.trim().toLowerCase(Locale.ROOT);
        try {
            if (cleaned.startsWith("0x")) {
                cleaned = cleaned.substring(2);
            } else if (cleaned.startsWith("#")) {
                cleaned = cleaned.substring(1);
            }
            int rgb = Integer.parseInt(cleaned, 16);
            return Color.fromRGB((int)rgb);
        }
        catch (NumberFormatException e) {
            return Color.WHITE;
        }
    }
}
