NexusDimensions - portal fix + colored linked portals (source patch)
======================================================================

WHY THIS IS SOURCE, NOT A JAR
------------------------------
The pack_format fix earlier was a plain text config file inside the jar,
safe to hand-edit directly. This fix changes actual plugin logic, which
means it has to be compiled against Paper's API (io.papermc.paper:paper-api,
per this project's own pom.xml) to produce a valid class file. That API is
only published on repo.papermc.io, which I don't have network access to
from where I'm working. So instead of guessing at raw bytecode for
something this involved, here is the real source change, ready to compile.

WHAT'S INCLUDED
----------------
src/main/java/com/nexus/dimensions/world/PortalListener.java   (replaces the existing file)
src/main/java/com/nexus/dimensions/world/PortalEffectManager.java   (new file)

WHAT CHANGED AND WHY
----------------------
1. THE LINK BUG
   The old PortalListener called event.setTo(destination) and disabled
   Paper's search/creation radius, trusting its built-in portal-placement
   engine to drop the player at that spot. That engine still does its own
   internal portal search near getTo(), and when the destination isn't
   itself sitting on a real portal block (like a plain dimension spawn
   point), it can silently fail to move the player - no error, nothing.
   This is a known PlayerPortalEvent quirk, not specific to this plugin.
   The fix: cancel the vanilla event entirely and teleport the player
   ourselves with Player#teleportAsync(Location, TeleportCause).

2. COLORED LINKED PORTALS
   New PortalEffectManager.java. Nether portal blocks can't be recolored
   without a resource pack, so this overlays a colored dust particle
   ambient effect on top of each linked portal's blocks - color is
   deterministic from the destination world's name (a small fixed 8-color
   palette, picked by hash), so the same destination always reads the same
   color, and different destinations are visually distinct from each
   other. Unlinked portals are untouched (plain vanilla purple), which
   covers "color change when linked vs unlinked" directly. A brighter
   burst also plays the moment a portal is linked and each time a player
   travels through one.

ONE SMALL WIRING CHANGE NEEDED IN THE MAIN PLUGIN CLASS
-----------------------------------------------------------
PortalListener's constructor now takes an extra PortalEffectManager
argument. In NexusDimensionsPlugin.java, in onEnable():

    PortalManager portalManager = new PortalManager(this);
    ...
    new PortalListener(this, portalManager);          // <- old line

becomes:

    PortalManager portalManager = new PortalManager(this);
    PortalEffectManager effectManager = new PortalEffectManager(this, portalManager);
    effectManager.start();
    ...
    new PortalListener(this, portalManager, effectManager);

And in onDisable(), add:

    effectManager.stop();

(store the effectManager reference as a field alongside wherever
portalManager already lives, so onDisable() can reach it)

BUILD & INSTALL
-----------------
1. Drop these two files into the plugin's existing source tree at the
   paths shown above (overwriting the old PortalListener.java).
2. Make the one wiring edit above in NexusDimensionsPlugin.java.
3. mvn package (needs network access to repo.papermc.io for the Paper API
   dependency, per this project's pom.xml).
4. Copy the resulting target/NexusDimensions-0.1.0.jar to your server's
   plugins/ folder, replacing the old one.
5. Restart the server.

If nobody on your side maintains the plugin's actual source, whoever gave
you the jar originally is the one who needs to apply this - I can't
compile it myself without access to repo.papermc.io from this environment.
