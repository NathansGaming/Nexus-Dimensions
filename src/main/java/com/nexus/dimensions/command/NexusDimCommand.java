/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.generator.WorldInfo
 */
package com.nexus.dimensions.command;

import com.nexus.dimensions.config.DimensionPreset;
import com.nexus.dimensions.config.PresetLoader;
import com.nexus.dimensions.datapack.DatapackGenerator;
import com.nexus.dimensions.structure.BlueprintLoader;
import com.nexus.dimensions.world.DimensionManager;
import com.nexus.dimensions.world.PortalManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;

public final class NexusDimCommand
implements CommandExecutor,
TabCompleter {
    private final PresetLoader presetLoader;
    private final DimensionManager dimensionManager;
    private final DatapackGenerator datapackGenerator;
    private final BlueprintLoader blueprintLoader;
    private final PortalManager portalManager;

    public NexusDimCommand(PresetLoader presetLoader, DimensionManager dimensionManager, DatapackGenerator datapackGenerator, BlueprintLoader blueprintLoader, PortalManager portalManager) {
        this.presetLoader = presetLoader;
        this.dimensionManager = dimensionManager;
        this.datapackGenerator = datapackGenerator;
        this.blueprintLoader = blueprintLoader;
        this.portalManager = portalManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(this.usage());
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list": {
                this.handleList(sender);
                break;
            }
            case "create": {
                this.handleCreate(sender, args);
                break;
            }
            case "tp": {
                this.handleTp(sender, args);
                break;
            }
            case "reload": {
                this.handleReload(sender);
                break;
            }
            case "portal": {
                this.handlePortal(sender, args);
                break;
            }
            default: {
                sender.sendMessage(this.usage());
            }
        }
        return true;
    }

    private void handleList(CommandSender sender) {
        Map<String, DimensionPreset> presets = this.dimensionManager.getPresets();
        if (presets.isEmpty()) {
            sender.sendMessage((Component)Component.text((String)"No presets loaded. Add a .yml file to the presets/ folder and run /nexusdim reload.", (TextColor)NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage((Component)Component.text((String)"Nexus presets:", (TextColor)NamedTextColor.GOLD));
        for (DimensionPreset preset : presets.values()) {
            String tier = preset.isTier2() ? "Tier 2 (restart)" : "Tier 1 (instant)";
            sender.sendMessage((Component)Component.text((String)(" - " + preset.id + "  [" + tier + "]  \"" + preset.displayName + "\""), (TextColor)NamedTextColor.AQUA));
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage((Component)Component.text((String)"Usage: /nexusdim create <worldName> <presetId> [seed]", (TextColor)NamedTextColor.RED));
            return;
        }
        String worldName = args[1];
        String presetId = args[2];
        Long seed = null;
        if (args.length >= 4) {
            try {
                seed = Long.parseLong(args[3]);
            }
            catch (NumberFormatException e) {
                sender.sendMessage((Component)Component.text((String)"Seed must be a whole number.", (TextColor)NamedTextColor.RED));
                return;
            }
        }
        DimensionPreset preset = this.dimensionManager.getPresets().get(presetId);
        DimensionManager.CreateResult result = this.dimensionManager.createOrLoad(worldName, presetId, seed);
        switch (result) {
            case UNKNOWN_PRESET: {
                sender.sendMessage((Component)Component.text((String)("No preset named '" + presetId + "'. Try /nexusdim list."), (TextColor)NamedTextColor.RED));
                break;
            }
            case NAME_COLLISION: {
                sender.sendMessage((Component)Component.text((String)("A world named '" + worldName + "' already exists and isn't managed by Nexus Dimensions."), (TextColor)NamedTextColor.RED));
                break;
            }
            case TIER1_ALREADY_LOADED: {
                sender.sendMessage((Component)Component.text((String)("'" + worldName + "' is already loaded."), (TextColor)NamedTextColor.YELLOW));
                break;
            }
            case TIER1_CREATED: {
                sender.sendMessage((Component)Component.text((String)("Created dimension '" + worldName + "' from preset '" + presetId + "'."), (TextColor)NamedTextColor.GREEN));
                this.teleportIfPlayer(sender, worldName);
                break;
            }
            case TIER1_CREATED_ON_PREEXISTING_FOLDER: {
                sender.sendMessage((Component)Component.text((String)("Loaded '" + worldName + "' with preset '" + presetId + "', BUT that world folder already had saved chunks on disk before this. Those already-explored chunks keep their original terrain \u2014 only newly generated chunks from here on use this preset. If you wanted a completely fresh dimension, stop the server, delete that world folder, and re-run this with the same name."), (TextColor)NamedTextColor.RED));
                this.teleportIfPlayer(sender, worldName);
                break;
            }
            case TIER2_ALREADY_ACTIVE: {
                sender.sendMessage((Component)Component.text((String)("Preset '" + presetId + "' is already an active Tier 2 dimension."), (TextColor)NamedTextColor.YELLOW));
                break;
            }
            case TIER2_DATAPACK_WRITTEN_RESTART_REQUIRED: {
                boolean written;
                boolean bl = written = preset != null && this.datapackGenerator.writeDatapack(preset);
                if (written) {
                    sender.sendMessage((Component)Component.text((String)("Preset '" + presetId + "' needs a custom world height/sky, so it's Tier 2. Datapack written \u2014 restart the server to activate it."), (TextColor)NamedTextColor.GOLD));
                    break;
                }
                sender.sendMessage((Component)Component.text((String)("Failed to write the datapack for '" + presetId + "' \u2014 check the server console."), (TextColor)NamedTextColor.RED));
            }
        }
    }

    private void teleportIfPlayer(CommandSender sender, String worldName) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            World world = Bukkit.getWorld((String)worldName);
            if (world != null) {
                player.teleport(world.getSpawnLocation());
            }
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage((Component)Component.text((String)"Only players can teleport.", (TextColor)NamedTextColor.RED));
            return;
        }
        Player player = (Player)sender;
        if (args.length < 2) {
            sender.sendMessage((Component)Component.text((String)"Usage: /nexusdim tp <worldName>", (TextColor)NamedTextColor.RED));
            return;
        }
        World world = Bukkit.getWorld((String)args[1]);
        if (world == null) {
            sender.sendMessage((Component)Component.text((String)("No loaded world named '" + args[1] + "'."), (TextColor)NamedTextColor.RED));
            return;
        }
        player.teleport(world.getSpawnLocation());
        sender.sendMessage((Component)Component.text((String)("Teleported to '" + args[1] + "'."), (TextColor)NamedTextColor.GREEN));
    }

    private void handleReload(CommandSender sender) {
        Map<String, DimensionPreset> reloaded = this.presetLoader.loadAll();
        this.dimensionManager.reloadPresets(reloaded);
        this.dimensionManager.reloadBlueprints(this.blueprintLoader.loadAll());
        sender.sendMessage((Component)Component.text((String)("Reloaded " + reloaded.size() + " preset(s) and blueprint(s) from disk."), (TextColor)NamedTextColor.GREEN));
        long tier2Count = reloaded.values().stream().filter(DimensionPreset::isTier2).count();
        if (tier2Count > 0L) {
            sender.sendMessage((Component)Component.text((String)("Refreshing datapacks for " + tier2Count + " Tier 2 preset(s)..."), (TextColor)NamedTextColor.GOLD));
            for (DimensionPreset preset : reloaded.values()) {
                if (!preset.isTier2()) continue;
                this.datapackGenerator.writeDatapack(preset);
            }
            sender.sendMessage((Component)Component.text((String)"Datapacks refreshed. New/changed Tier 2 dimensions need a server restart.", (TextColor)NamedTextColor.GOLD));
        }
    }

    private void handlePortal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage((Component)Component.text((String)"Only players can manage portals - stand in the portal you mean.", (TextColor)NamedTextColor.RED));
            return;
        }
        Player player = (Player)sender;
        if (args.length < 2) {
            sender.sendMessage(this.portalUsage());
            return;
        }
        switch (args[1].toLowerCase()) {
            case "link": {
                this.handlePortalLink(player, args);
                break;
            }
            case "unlink": {
                this.handlePortalUnlink(player);
                break;
            }
            case "list": {
                this.handlePortalList(sender);
                break;
            }
            default: {
                sender.sendMessage(this.portalUsage());
            }
        }
    }

    private void handlePortalLink(Player player, String[] args) {
        PortalManager.Portal portal;
        String[] a;
        boolean bothWays = args.length >= 4 && args[args.length - 1].equalsIgnoreCase("both");
        String[] stringArray = a = bothWays ? Arrays.copyOf(args, args.length - 1) : args;
        if (a.length < 3) {
            player.sendMessage((Component)Component.text((String)"Usage: /nexusdim portal link <destinationWorld> [destX destY destZ] [both]", (TextColor)NamedTextColor.RED));
            return;
        }
        String destWorldName = a[2];
        World destWorld = Bukkit.getWorld((String)destWorldName);
        if (destWorld == null) {
            player.sendMessage((Component)Component.text((String)("No loaded world named '" + destWorldName + "'. It must be loaded to link to it."), (TextColor)NamedTextColor.RED));
            return;
        }
        Location destLoc = null;
        if (a.length >= 6) {
            try {
                double x = Double.parseDouble(a[3]);
                double y = Double.parseDouble(a[4]);
                double z = Double.parseDouble(a[5]);
                destLoc = new Location(destWorld, x, y, z);
            }
            catch (NumberFormatException e) {
                player.sendMessage((Component)Component.text((String)"destX/destY/destZ must be numbers.", (TextColor)NamedTextColor.RED));
                return;
            }
        }
        if ((portal = this.portalManager.linkNearby(player.getLocation(), destWorldName, destLoc)) == null) {
            player.sendMessage((Component)Component.text((String)"No lit Nether portal found within 3 blocks of you. Build and light one first (obsidian frame + flint and steel), then stand in it and run this again.", (TextColor)NamedTextColor.RED));
            return;
        }
        String destDesc = destLoc != null ? String.format("%.1f, %.1f, %.1f in '%s'", destLoc.getX(), destLoc.getY(), destLoc.getZ(), destWorldName) : "'" + destWorldName + "'s spawn";
        player.sendMessage((Component)Component.text((String)("Linked this portal to " + destDesc + "."), (TextColor)NamedTextColor.GREEN));
        if (bothWays) {
            boolean linkedBack = this.attemptAutoReturnLink(player, portal, destWorld, destLoc);
            if (linkedBack) {
                player.sendMessage((Component)Component.text((String)"Also found a lit portal near the destination and linked it back to this one.", (TextColor)NamedTextColor.GREEN));
            } else {
                player.sendMessage((Component)Component.text((String)("Couldn't find a lit portal within a few blocks of the destination to auto-link back - build one there, stand in it, and run '/nexusdim portal link " + player.getWorld().getName() + "' from that side."), (TextColor)NamedTextColor.YELLOW));
            }
        }
    }

    private boolean attemptAutoReturnLink(Player player, PortalManager.Portal forward, World destWorld, Location destLoc) {
        Location searchNear = destLoc != null ? destLoc : destWorld.getSpawnLocation();
        double centerX = (double)(forward.minX() + forward.maxX()) / 2.0 + 0.5;
        double centerZ = (double)(forward.minZ() + forward.maxZ()) / 2.0 + 0.5;
        Location sourceReturn = new Location(player.getWorld(), centerX, (double)forward.minY(), centerZ);
        PortalManager.Portal back = this.portalManager.linkNearby(searchNear, player.getWorld().getName(), sourceReturn);
        return back != null;
    }

    private void handlePortalUnlink(Player player) {
        boolean removed = this.portalManager.unlinkNearby(player.getLocation());
        if (removed) {
            player.sendMessage((Component)Component.text((String)"Unlinked the portal you're standing near. The physical portal blocks are untouched.", (TextColor)NamedTextColor.GREEN));
        } else {
            player.sendMessage((Component)Component.text((String)"No registered Nexus portal found near you.", (TextColor)NamedTextColor.YELLOW));
        }
    }

    private void handlePortalList(CommandSender sender) {
        List<PortalManager.Portal> portals = this.portalManager.list();
        if (portals.isEmpty()) {
            sender.sendMessage((Component)Component.text((String)"No portals registered yet. Stand in a lit portal and run /nexusdim portal link.", (TextColor)NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage((Component)Component.text((String)"Registered portals:", (TextColor)NamedTextColor.GOLD));
        for (PortalManager.Portal p : portals) {
            sender.sendMessage((Component)Component.text((String)(" - " + p.worldName() + " [" + p.minX() + "," + p.minY() + "," + p.minZ() + "] -> " + p.destWorldName()), (TextColor)NamedTextColor.AQUA));
        }
    }

    private Component usage() {
        return Component.text((String)"Usage: /nexusdim <list|create|tp|reload|portal>", (TextColor)NamedTextColor.YELLOW);
    }

    private Component portalUsage() {
        return Component.text((String)"Usage: /nexusdim portal <link|unlink|list>", (TextColor)NamedTextColor.YELLOW);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return this.filter(List.of("list", "create", "tp", "reload", "portal"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            return this.filter(Bukkit.getWorlds().stream().map(WorldInfo::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return this.filter(new ArrayList<String>(this.dimensionManager.getPresets().keySet()), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("portal")) {
            return this.filter(List.of("link", "unlink", "list"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("portal") && args[1].equalsIgnoreCase("link")) {
            return this.filter(Bukkit.getWorlds().stream().map(WorldInfo::getName).collect(Collectors.toList()), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("portal") && args[1].equalsIgnoreCase("link")) {
            return this.filter(List.of("both"), args[3]);
        }
        if (args.length == 7 && args[0].equalsIgnoreCase("portal") && args[1].equalsIgnoreCase("link")) {
            return this.filter(List.of("both"), args[6]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
