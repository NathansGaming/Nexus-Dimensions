NexusDimensions - full project (decompiled + portal fixes applied)
======================================================================

WHAT THIS IS
-------------
Every class decompiled back to Java source from your jar, plus the real
pom.xml (recovered from Maven metadata embedded in the jar). The two
portal fixes and the pack_format config fix are already applied and
wired in. This version has also had a round of real compile errors fixed
(see below) after the first attempt failed in your Codespace.

HOW TO USE
-----------
Replace everything in your Nexus-Dimensions repo folder with what's in
this zip, so you end up with:

    Nexus-Dimensions/
      pom.xml
      src/main/java/com/nexus/...
      src/main/resources/plugin.yml, config.yml, presets/, blueprints/

Then:

    mvn clean package

The jar lands in target/NexusDimensions-0.1.0.jar. Copy it to your
server's plugins/ folder, replacing the old one, and restart.

WHAT WAS FIXED THIS ROUND (the 47 compile errors)
-----------------------------------------------------
All 47 errors traced back to one root cause: CFR (the decompiler)
doesn't have access to the Paper API, so anywhere the original code
called a Bukkit config method like getMapList(...) or
getConfigurationSection(...), it couldn't always recover the exact
return type and fell back to plain Object, or occasionally the wrong
type entirely (e.g. writing "Iterator" where it meant "Map" for a couple
of loop variables). That's invisible in the decompiled source until you
actually try to compile it against the real API - which I couldn't do
in my sandbox, only your Codespace could, hence needing your error log.

Fixed in PresetLoader.java:
  - Every `List raw... = section.getMapList(...)` now correctly declared
    as `List<Map<?, ?>>` (7 places)
  - Two for-each loop variables that were wrongly typed `Iterator`
    instead of `Map<?, ?>`
  - One for-each loop variable wrongly typed `Object` instead of
    `Map<?, ?>` (the trees.species loop)
  - `caveSec`, which the decompiler collapsed to `Object`, retyped back
    to `ConfigurationSection`
  - `equipSec` was being reused by the decompiler for two unrelated
    purposes (a ConfigurationSection in the mob-equipment block, and a
    generic Object holder in the seasons block) - split the
    mob-equipment one out into its own properly-typed `equipmentSec`
    variable so both usages compile correctly

Fixed in BlueprintLoader.java:
  - Same pattern: `List raw = yaml.getMapList("blocks")` retyped to
    `List<Map<?, ?>>`, and its loop variable to `Map<?, ?>`

I scanned the rest of the project for this same pattern (any other
getMapList/getValues/getConfigurationSection calls) and didn't find any
other occurrences, so this should be the full set - but since I still
can't compile this myself (repo.papermc.io isn't reachable from my
sandbox), if anything else comes up, paste me the new error log and
I'll fix it the same way.

WHAT'S ACTUALLY NEW/CHANGED VS. THE ORIGINAL JAR
----------------------------------------------------
- config.yml: datapackPackFormat 61 -> 107
- world/PortalListener.java: cancels the vanilla portal event and
  teleports the player manually instead of relying on setTo()
- world/PortalEffectManager.java: new file, colored ambient particles on
  linked portals (color keyed to destination world)
- NexusDimensionsPlugin.java: wiring for PortalEffectManager
- config/PresetLoader.java, structure/BlueprintLoader.java: decompiler
  type-inference fixes described above (behavior unchanged, just makes
  it actually compile)

Everything else is an unmodified decompile of your existing jar.
