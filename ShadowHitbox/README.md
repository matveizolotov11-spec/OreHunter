# ShadowHitbox

Client-side Fabric mod for Minecraft Java 1.21.11.

## Features
- Right Shift opens the dark GUI.
- Combat tab: hitbox expansion toggle and independent width/height sliders.
- Visuals tab: hitbox ESP, self-ESP, RGB color and opacity controls.
- Settings are saved to `config/shadowhitbox.json`.
- Actual client geometry expansion is intentionally restricted to singleplayer. On multiplayer servers, the mod only draws local ESP and leaves server hit registration/collision untouched.

## Build
Requires Java 21.

```bash
gradle build
```

The distributable jar is in `build/libs/`.
