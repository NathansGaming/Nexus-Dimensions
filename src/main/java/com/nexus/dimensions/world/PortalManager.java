/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.Plugin
 */
package com.nexus.dimensions.world;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class PortalManager {
    private static final int MAX_FLOOD_FILL_BLOCKS = 4096;
    private final Logger logger;
    private final File portalsFile;
    private final List<Portal> portals = new ArrayList<Portal>();

    public PortalManager(Plugin plugin) {
        this.logger = plugin.getLogger();
        this.portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        this.load();
    }

    public List<Portal> list() {
        return List.copyOf(this.portals);
    }

    public Portal findContaining(String worldName, Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        for (Portal p : this.portals) {
            if (!p.contains(worldName, x, y, z)) continue;
            return p;
        }
        return null;
    }

    public Portal linkNearby(Location near, String destWorldName, Location destLoc) {
        Block start = this.findNearbyPortalBlock(near, 3);
        if (start == null) {
            return null;
        }
        Set<Block> connected = this.floodFillPortalBlocks(start);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Block b : connected) {
            minX = Math.min(minX, b.getX());
            minY = Math.min(minY, b.getY());
            minZ = Math.min(minZ, b.getZ());
            maxX = Math.max(maxX, b.getX());
            maxY = Math.max(maxY, b.getY());
            maxZ = Math.max(maxZ, b.getZ());
        }
        Portal portal = new Portal(UUID.randomUUID(), near.getWorld().getName(), minX, minY, minZ, maxX, maxY, maxZ, destWorldName, destLoc != null ? Double.valueOf(destLoc.getX()) : null, destLoc != null ? Double.valueOf(destLoc.getY()) : null, destLoc != null ? Double.valueOf(destLoc.getZ()) : null);
        this.portals.add(portal);
        this.save();
        return portal;
    }

    public boolean unlinkNearby(Location near) {
        Portal found = this.findContaining(near.getWorld().getName(), near);
        if (found == null) {
            return false;
        }
        this.portals.remove(found);
        this.save();
        return true;
    }

    public Location resolveDestination(Portal portal) {
        World world = Bukkit.getWorld((String)portal.destWorldName());
        if (world == null) {
            return null;
        }
        if (portal.destX() != null) {
            return new Location(world, portal.destX().doubleValue(), portal.destY().doubleValue(), portal.destZ().doubleValue());
        }
        return world.getSpawnLocation();
    }

    private Block findNearbyPortalBlock(Location center, int radius) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dy = -radius; dy <= radius; ++dy) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    if (b.getType() != Material.NETHER_PORTAL) continue;
                    return b;
                }
            }
        }
        return null;
    }

    private Set<Block> floodFillPortalBlocks(Block start) {
        HashSet<Block> visited = new HashSet<Block>();
        ArrayDeque<Block> queue = new ArrayDeque<Block>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && visited.size() < 4096) {
            Block current = (Block)queue.poll();
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
                Block next = current.getRelative(face);
                if (next.getType() != Material.NETHER_PORTAL || visited.contains(next)) continue;
                visited.add(next);
                queue.add(next);
            }
        }
        return visited;
    }

    private void load() {
        if (!this.portalsFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)this.portalsFile);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection s = yaml.getConfigurationSection(key);
            if (s == null) continue;
            try {
                UUID id = UUID.fromString(key);
                Double destX = s.contains("destX") ? Double.valueOf(s.getDouble("destX")) : null;
                Double destY = s.contains("destY") ? Double.valueOf(s.getDouble("destY")) : null;
                Double destZ = s.contains("destZ") ? Double.valueOf(s.getDouble("destZ")) : null;
                this.portals.add(new Portal(id, s.getString("world"), s.getInt("minX"), s.getInt("minY"), s.getInt("minZ"), s.getInt("maxX"), s.getInt("maxY"), s.getInt("maxZ"), s.getString("destWorld"), destX, destY, destZ));
            }
            catch (Exception e) {
                this.logger.warning("[NexusDimensions] Skipped malformed portal entry '" + key + "' in portals.yml: " + e.getMessage());
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Portal p : this.portals) {
            String key = p.id().toString();
            yaml.set(key + ".world", (Object)p.worldName());
            yaml.set(key + ".minX", (Object)p.minX());
            yaml.set(key + ".minY", (Object)p.minY());
            yaml.set(key + ".minZ", (Object)p.minZ());
            yaml.set(key + ".maxX", (Object)p.maxX());
            yaml.set(key + ".maxY", (Object)p.maxY());
            yaml.set(key + ".maxZ", (Object)p.maxZ());
            yaml.set(key + ".destWorld", (Object)p.destWorldName());
            if (p.destX() == null) continue;
            yaml.set(key + ".destX", (Object)p.destX());
            yaml.set(key + ".destY", (Object)p.destY());
            yaml.set(key + ".destZ", (Object)p.destZ());
        }
        try {
            yaml.save(this.portalsFile);
        }
        catch (IOException e) {
            this.logger.severe("[NexusDimensions] Could not save portals.yml: " + e.getMessage());
        }
    }

    public record Portal(UUID id, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String destWorldName, Double destX, Double destY, Double destZ) {
        public boolean contains(String worldName, int x, int y, int z) {
            return this.worldName.equals(worldName) && x >= this.minX - 1 && x <= this.maxX + 1 && y >= this.minY - 1 && y <= this.maxY + 1 && z >= this.minZ - 1 && z <= this.maxZ + 1;
        }
    }
}
