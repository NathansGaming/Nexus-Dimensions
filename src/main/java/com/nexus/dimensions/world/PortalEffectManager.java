package com.nexus.dimensions.world;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * NEW (2026-08): gives linked portals a colored particle overlay so players
 * can tell at a glance where a portal goes, and that it's actually linked
 * at all (an unlinked Nexus portal is left as plain vanilla purple).
 *
 * A vanilla NETHER_PORTAL block's texture can't be recolored without a
 * resource pack, so this works the same way most "colored portal" plugins
 * do: a low-density ambient dust particle drifting over the portal's
 * blocks, colored deterministically from the destination world's name so
 * the same destination always reads as the same color. On top of the
 * ambient loop, a short brighter burst plays whenever a portal is created
 * (link) and whenever a player actually travels through one.
 */
public final class PortalEffectManager {

    // A small fixed palette rather than fully random RGB per world - keeps
    // colors visually distinct from each other instead of drifting into
    // muddy/indistinguishable hues.
    private static final Color[] PALETTE = new Color[]{
            Color.fromRGB(0x3B82F6), // blue
            Color.fromRGB(0x22C55E), // green
            Color.fromRGB(0xF97316), // orange
            Color.fromRGB(0xE11D48), // rose
            Color.fromRGB(0xA855F7), // purple (distinct from vanilla portal purple - brighter/pinker)
            Color.fromRGB(0xEAB308), // yellow
            Color.fromRGB(0x06B6D4), // cyan
            Color.fromRGB(0xF43F5E), // pink
    };

    private final PortalManager portalManager;
    private final Plugin plugin;
    private BukkitRunnable ambientTask;

    public PortalEffectManager(Plugin plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
    }

    /** Call once from the plugin's onEnable(). */
    public void start() {
        this.ambientTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickAmbient();
            }
        };
        // Every 10 ticks (0.5s) is enough to read as a steady color without
        // spamming the client with particle packets for servers with many
        // portals.
        this.ambientTask.runTaskTimer(plugin, 20L, 10L);
    }

    /** Call once from the plugin's onDisable(). */
    public void stop() {
        if (ambientTask != null) {
            ambientTask.cancel();
            ambientTask = null;
        }
    }

    /** A brighter one-shot burst - call when a portal is freshly linked or a player travels through it. */
    public void pulse(PortalManager.Portal portal) {
        World world = Bukkit.getWorld(portal.worldName());
        if (world == null) {
            return;
        }
        Color color = colorFor(portal.destWorldName());
        forEachPortalBlock(world, portal, block -> {
            Location center = block.getLocation().add(0.5, 0.5, 0.5);
            world.spawnParticle(Particle.DUST, center, 6, 0.25, 0.25, 0.25, 0.0, new Particle.DustOptions(color, 1.6f));
        });
    }

    private void tickAmbient() {
        for (PortalManager.Portal portal : portalManager.list()) {
            World world = Bukkit.getWorld(portal.worldName());
            if (world == null) {
                continue;
            }
            Color color = colorFor(portal.destWorldName());
            forEachPortalBlock(world, portal, block -> {
                Location center = block.getLocation().add(0.5, 0.5, 0.5);
                world.spawnParticle(Particle.DUST, center, 1, 0.3, 0.3, 0.3, 0.0, new Particle.DustOptions(color, 1.0f));
            });
        }
    }

    private void forEachPortalBlock(World world, PortalManager.Portal portal, java.util.function.Consumer<Block> consumer) {
        for (int x = portal.minX(); x <= portal.maxX(); x++) {
            for (int y = portal.minY(); y <= portal.maxY(); y++) {
                for (int z = portal.minZ(); z <= portal.maxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == org.bukkit.Material.NETHER_PORTAL) {
                        consumer.accept(block);
                    }
                }
            }
        }
    }

    /** Same destination world always maps to the same color, so a color is meaningful across the session. */
    private Color colorFor(String destWorldName) {
        int idx = Math.floorMod(destWorldName.hashCode(), PALETTE.length);
        return PALETTE[idx];
    }
}
