/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.player.PlayerJumpEvent
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package com.nexus.dimensions.world;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.world.DimensionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class GravityService
implements Listener {
    private static final long REFRESH_INTERVAL_TICKS = 100L;
    private static final int EFFECT_DURATION_TICKS = Integer.MAX_VALUE;
    private static final double GROUNDED_THRESHOLD = 2.5;
    private final Plugin plugin;
    private final DimensionManager dimensionManager;

    public GravityService(Plugin plugin, DimensionManager dimensionManager) {
        this.plugin = plugin;
        this.dimensionManager = dimensionManager;
        Bukkit.getPluginManager().registerEvents((Listener)this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 100L);
    }

    private void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.apply(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.apply(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        this.apply(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(this.plugin, () -> this.apply(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled=true)
    public void onJump(PlayerJumpEvent event) {
        DimensionPreset preset = this.dimensionManager.getPresetForWorld(event.getPlayer().getWorld().getName());
        if (preset != null && !GravityService.allowsJumping(preset)) {
            event.setCancelled(true);
        }
    }

    private void apply(Player player) {
        DimensionPreset preset = this.dimensionManager.getPresetForWorld(player.getWorld().getName());
        double gravity = preset != null ? preset.flavor.gravity : 1.0;
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        if (preset == null || gravity == 1.0) {
            return;
        }
        int jumpAmplifier = GravityService.jumpAmplifier(gravity);
        if (jumpAmplifier != 0 && (preset.flavor.allowJumping == null || preset.flavor.allowJumping.booleanValue())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, jumpAmplifier, true, false, false));
        }
        if (gravity < 1.0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false, false));
        }
    }

    public static boolean allowsJumping(DimensionPreset preset) {
        if (preset.flavor.allowJumping != null) {
            return preset.flavor.allowJumping;
        }
        return preset.flavor.gravity < 2.5;
    }

    static int jumpAmplifier(double gravity) {
        double raw = 1.0 / gravity - 1.0;
        int amplifier = (int)Math.round(raw * 2.0);
        return Math.max(-8, Math.min(8, amplifier));
    }
}
