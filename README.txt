NexusDimensions - full project (decompiled + portal fixes applied)
======================================================================

WHAT THIS IS
-------------
Your repo didn't have the plugin's actual source in it, so `mvn` had
nothing to build. This is the whole project reconstructed from your
NexusDimensions-0_1_0.jar - every class decompiled back to Java source,
plus the real pom.xml (recovered from the Maven metadata embedded inside
the jar itself, at META-INF/maven/...). The two portal fixes from before
are already applied and wired in, and the pack_format config fix from
earlier is already in here too.

HOW TO USE
-----------
Delete whatever is currently in your Nexus-Dimensions repo folder (or
just work in a fresh clone) and copy everything from this zip in, so you
end up with:

    Nexus-Dimensions/
      pom.xml
      src/main/java/com/nexus/...
      src/main/resources/plugin.yml, config.yml, presets/, blueprints/

Then commit and push if you want it saved, and run:

    mvn clean package

The jar will land in target/NexusDimensions-0.1.0.jar. Copy that to your
server's plugins/ folder, replacing the old one, and restart.

IMPORTANT - PLEASE READ
-------------------------
This is decompiled code, not the original hand-written source. CFR (the
decompiler) did a clean pass with no errors on all 33 classes, and
everything reads as ordinary, valid Java - but decompiled output can
occasionally need a small tweak to compile (a generic type CFR couldn't
fully infer, a synthetic bridge method, that kind of thing), especially
in the terrain-generation package, which is the most complex part of the
plugin. I have NOT been able to actually run `mvn package` on this myself
- repo.papermc.io (where the Paper API dependency lives) isn't reachable
from where I'm working, so this hasn't been build-tested.

If `mvn clean package` throws a compile error, paste it back to me
(or to whoever's helping you) - decompiler-artifact errors are usually a
one-line fix once you can see the actual error message.

WHAT'S ACTUALLY NEW/CHANGED VS. THE ORIGINAL JAR
----------------------------------------------------
- config.yml: datapackPackFormat 61 -> 107 (the original restart-doesn't-
  fix-it bug)
- world/PortalListener.java: cancels the vanilla portal event and
  teleports the player manually, instead of relying on setTo(), which is
  why linking said "Linked!" but didn't actually move you
- world/PortalEffectManager.java: new file, colored ambient particles on
  linked portals (color keyed to destination world), silent on unlinked
  ones
- NexusDimensionsPlugin.java: three small edits to construct, start, and
  stop PortalEffectManager, and pass it into PortalListener

Everything else is an unmodified decompile of your existing jar.
