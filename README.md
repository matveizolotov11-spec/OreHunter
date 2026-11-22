# OreHunter

Early Fabric client mod prototype for Minecraft **1.21.11**.

## Controls
- **J** — enable or disable the helper.

## Prototype features
- Scans loaded client-world blocks in a 16-block cube for diamond ore, deepslate diamond ore, and ancient debris.
- Turns the player's view toward the nearest detected target.
- Walks forward toward the target and attempts to mine it when the block is in the crosshair and within reach.

## Build
GitHub Actions uses Java 21 and Gradle 9.2.1. After a successful run, download **OreHunter-1.0.0** from the run's **Artifacts** section on the Actions page.

## Not implemented yet
- Obstacle-aware pathfinding or safe route planning.
- Placing blocks to bridge or climb.
- Actual ore outline/highlight rendering.
- Dark in-game GUI and status HUD.

This is an early prototype, not a finished autonomous mining bot. Test in a single-player world first. Server rules and anti-cheat may prohibit automation.
