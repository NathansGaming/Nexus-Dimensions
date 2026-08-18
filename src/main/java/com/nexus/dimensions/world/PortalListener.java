package com.nexus.dimensions.world;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.plugin.Plugin;

/**
 * FIX (2026-08): the previous version called event.setTo(destination) and
 * disabled the vanilla search/creation radius, trusting Paper's built-in
 * portal-placement engine to drop the player at that exact location. In
 * practice that engine still runs its own internal portal search/placement
 * near getTo(), and when the destination isn't itself sitting on an actual
 * portal block (e.g. a dimension's plain spawn point) it can silently fail
 * to move the player at all - no error, no teleport. This is a known
 * Paper/Spigot PlayerPortalEvent quirk, not something specific to this
 * plugin's destinations.
 *
 * The reliable fix is to cancel the vanilla portal event entirely and
 * perform the teleport ourselves.
 */
public final class PortalListener implements Listener {

    private final PortalManager portalManager;
    private final PortalEffectManager effectManager;

    public PortalListener(Plugin plugin, PortalManager portalManager, PortalEffectManager effectManager) {
        this.portalManager = portalManager;
        this.effectManager = effectManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        PortalManager.Portal portal = portalManager.findContaining(event.getFrom().getWorld().getName(), event.getFrom());
        if (portal == null) {
            return;
        }

        Location destination = portalManager.resolveDestination(portal);
        if (destination == null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("That portal's destination world isn't loaded right now.", NamedTextColor.RED));
            return;
        }

        // Cancel the vanilla portal handling completely - we're doing the
        // teleport ourselves, so none of Paper's search/creation/travel
        // logic should touch this event at all.
        event.setCancelled(true);

        // teleportAsync avoids the main-thread chunk-load stall you'd get
        // from a plain teleport() into an unloaded destination chunk.
        event.getPlayer().teleportAsync(destination, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);

        if (effectManager != null) {
            effectManager.pulse(portal);
        }
    }
}
