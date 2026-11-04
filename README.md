# Debug Stash

A simple Fabric mod for Minecraft Java 1.20.1, using Java 17.

## Usage

Type `/Debug` (capital D), or `/debug`. Requires permission level 2: enable cheats in singleplayer or obtain operator permissions on a server. The command must be run as a player.

Each use gives you:

- Giant, Illusioner, Zombie Horse, and Killer Bunny spawn eggs.
- Debug stick; impulse, chain, and repeating command blocks; command-block minecart.
- Barriers, light blocks, structure voids, structure block, and jigsaw block.
- A default pig spawner, bedrock, end portal frames, budding amethyst, and reinforced deepslate.

Items that do not fit in your inventory drop at your feet. Repeating the command gives another set.

The Giant and Illusioner eggs use vanilla spawn eggs with entity NBT. The Killer Bunny is the unused hostile rabbit variant, not a separate entity type. These eggs use vanilla textures and have descriptive custom names. All other items use their existing vanilla names and translations; this mod registers no new items or blocks.

Creative mode is recommended. Vanilla restrictions are not bypassed: some tools and blocks require Creative/operator privileges, and command blocks require `enable-command-block=true` on dedicated servers. Peaceful difficulty can remove hostile mobs. The Giant retains vanilla behavior, including its lack of normal AI.

“Illegal” means normally unobtainable in Survival, not invalid or hacked item IDs. Removed mobs no longer present in Minecraft 1.20.1 are not included. Portal interiors and end gateways have no inventory item forms, so they are not given.

## Installation

Install Fabric Loader and Fabric API for Minecraft 1.20.1, then place the built JAR into `mods`. On multiplayer, install this mod on the server; clients do not need it because all granted items are vanilla items.

## Build

Use Java 17 and Gradle 8.6:

```sh
gradle build
```

The distributable JAR is `build/libs/debug-stash-1.0.0.jar`. There is no Gradle wrapper.
