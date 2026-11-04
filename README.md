# Healing Ruby

A Fabric mod for Minecraft 1.21.1, using Java 21.

## Gameplay

Craft one Ruby with a diamond in the center and redstone directly above, below, left, and right (four redstone total).

Right-click with a Ruby in either hand to instantly restore up to 4 hearts (8 health points). Each use consumes one Ruby outside Creative mode and has a one-second cooldown. Rubies cannot be used or wasted at full health. Healing never exceeds maximum health.

Rubies appear in the Food & Drinks creative tab. The item model reuses the vanilla redstone texture. Obtaining a diamond unlocks the recipe in the recipe book.

## Build and install

Run `gradle build` with Gradle 8.8 and Java 21. No Gradle wrapper is included.

Install the resulting `build/libs/healing-ruby-1.0.0.jar` alongside Fabric API on Fabric Loader for Minecraft 1.21.1. Install the mod on both client and server for multiplayer.
