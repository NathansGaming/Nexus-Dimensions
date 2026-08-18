/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.CreatureSpawnEvent
 *  org.bukkit.event.entity.CreatureSpawnEvent$SpawnReason
 *  org.bukkit.inventory.EntityEquipment
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package com.nexus.dimensions.world;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.world.DimensionManager;
import com.nexus.dimensions.world.SeasonService;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class MobCustomizationListener
implements Listener {
    private final DimensionManager dimensionManager;
    private final SeasonService seasonService;

    public MobCustomizationListener(Plugin plugin, DimensionManager dimensionManager) {
        this(plugin, dimensionManager, null);
    }

    public MobCustomizationListener(Plugin plugin, DimensionManager dimensionManager, SeasonService seasonService) {
        this.dimensionManager = dimensionManager;
        this.seasonService = seasonService;
        plugin.getServer().getPluginManager().registerEvents((Listener)this, plugin);
    }

    @EventHandler(ignoreCancelled=true)
    public void onSpawn(CreatureSpawnEvent event) {
        double spawnMultiplier;
        String worldName = event.getEntity().getWorld().getName();
        DimensionPreset preset = this.dimensionManager.getPresetForWorld(worldName);
        if (preset == null || !preset.creatures.enabled) {
            return;
        }
        double d = spawnMultiplier = this.seasonService != null ? this.seasonService.effectiveSpawnMultiplier(worldName, preset) : preset.creatures.spawnMultiplier;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL && spawnMultiplier < 1.0 && ThreadLocalRandom.current().nextDouble() >= spawnMultiplier) {
            event.setCancelled(true);
            return;
        }
        DimensionPreset.MobProfile profile = preset.creatures.mobs.get(event.getEntityType().name());
        if (profile == null) {
            return;
        }
        this.applyProfile((Entity)event.getEntity(), profile);
    }

    private void applyProfile(Entity entity, DimensionPreset.MobProfile profile) {
        EntityEquipment equipment;
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity)entity;
        this.applyAttribute(living, Attribute.MAX_HEALTH, profile.healthMultiplier);
        AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            living.setHealth(maxHealth.getValue());
        }
        this.applyAttribute(living, Attribute.MOVEMENT_SPEED, profile.speedMultiplier);
        this.applyAttribute(living, Attribute.ATTACK_DAMAGE, profile.damageMultiplier);
        if (profile.scale != 1.0) {
            this.applyAttribute(living, Attribute.SCALE, profile.scale);
        }
        if (profile.displayName != null && !profile.displayName.isBlank()) {
            living.setCustomName(profile.displayName);
            living.setCustomNameVisible(profile.alwaysShowName);
        }
        living.setGlowing(profile.glowing);
        if (!profile.equipment.isEmpty() && (equipment = living.getEquipment()) != null) {
            for (Map.Entry<String, String> entry : profile.equipment.entrySet()) {
                ItemStack item = new ItemStack(MobCustomizationListener.materialOf(entry.getValue()));
                switch (entry.getKey()) {
                    case "hand": {
                        equipment.setItemInMainHand(item);
                        equipment.setItemInMainHandDropChance(0.0f);
                        break;
                    }
                    case "offhand": {
                        equipment.setItemInOffHand(item);
                        equipment.setItemInOffHandDropChance(0.0f);
                        break;
                    }
                    case "head": {
                        equipment.setHelmet(item);
                        equipment.setHelmetDropChance(0.0f);
                        break;
                    }
                    case "chest": {
                        equipment.setChestplate(item);
                        equipment.setChestplateDropChance(0.0f);
                        break;
                    }
                    case "legs": {
                        equipment.setLeggings(item);
                        equipment.setLeggingsDropChance(0.0f);
                        break;
                    }
                    case "feet": {
                        equipment.setBoots(item);
                        equipment.setBootsDropChance(0.0f);
                        break;
                    }
                }
            }
        }
    }

    private void applyAttribute(LivingEntity living, Attribute attribute, double multiplier) {
        if (multiplier == 1.0) {
            return;
        }
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.setBaseValue(instance.getBaseValue() * multiplier);
    }

    private static Material materialOf(String key) {
        Material m = Material.matchMaterial((String)key);
        return m != null ? m : Material.AIR;
    }
}
