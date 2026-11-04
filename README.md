# Old Content

A Fabric mod for Minecraft 1.20.1, using Java 17.

## Play
Install Fabric Loader and Fabric API, and place this mod in `mods`. Install it on both the client and server for multiplayer.

Type `/old` in chat to receive:
- 16 Forgotten Rubies: collectible recreations of the unused ruby concept.
- 1 Old Quiver: stores up to 64 ordinary arrows. Hold it and right-click to collect arrows from your inventory. Sneak-right-click to retrieve them. Withdraw arrows before using a bow; it does not automatically feed weapons.
- 16 Early Gears: placeable, decorative recreations inspired by Minecraft's early gear block. These do not transmit power or rotate.
- 64 arrows to try the quiver.

The command is available to every player without cheats or operator permission and can be repeated. Items that do not fit in your inventory drop at your feet. Quiver contents persist when saved.

These are small, modern reinterpretations of removed or unused ideas, not an exact restoration of every historical feature. Models reuse vanilla textures; no historical art is bundled. Items also appear in the Ingredients, Combat, and Functional Blocks creative tabs respectively.

## Build
Use Java 17 and Gradle 8.7:

```sh
gradle build
```

The playable JAR is `build/libs/old-content-1.0.0.jar`. No Gradle wrapper is included.
