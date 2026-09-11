# Breathheart

**Читать на русском:** [README_RU.md](README_RU.md)

> **Made with AI** — the code, sounds and docs of this mod were created
> by an AI assistant (Muse Spark via OpenCode) together with the author (smvad).
>
> **Experiment, not a final product.** This project was created for experimental
> purposes: mechanics, sounds and balance may change without backward compatibility.

A client-side Fabric mod for Minecraft 26.2: a breathing and heartbeat system.

- **Background:** barely audible breathing and heartbeat at rest.
- **Exertion:** running, sprinting, jumping, swimming, hitting/mining → breathing gets faster and louder; prolonged effort leads to gasping that fades smoothly.
- **Critical moments:** falling from height, sudden heavy damage, low health → racing heartbeat that fades out. The heart also reacts to thin air underwater and a nearby warden, and pulses in waves at low HP.
- **Weight and water:** heavy armor tires you faster; no breathing sounds with eyes underwater (breath-holding).

Everything is detected from movement/health on the client (no mixins needed); sound is
non-positional one-shot samples with dynamic volume, pitch and interval. Breathing
is a single seamless loop: the tempo slides continuously with effort down to gapless
tiling, so the beat never breaks; gasping morphs the same loop louder and higher
instead of switching samples.

The mod is active in survival and adventure only; silent in creative and spectator.

Tuning: walking and occasional jumps stay near resting tempo (decay runs every
tick). Effort drives the loop continuously: ~1 sec of sprinting brings it to the
full 1.6 sec gapless tiling, intensity scaling through rate and volume (pitch
stays 1.0 — a shifted voice sounds unnatural).
Gasping latches after ~2 sec of sustained peak effort (Schmitt 70/55) and morphs
the same loop louder over ~1.5 sec — no switches, no broken beat.
Falling: heart and breathing accelerate mid-air, the stronger the longer the fall
(threshold ~2.5 blocks, small hops stay silent, elytra gliding doesn't count);
landing from 4+ blocks adds a stress spike on top.

Breathing sound is one seamless loop from an author-provided recording (see
`CREDITS.md`). The heart is a CC0 recording, cut down to
single 0.75 s thumps so beats don't layer at fast rates; near a warden the
heartbeat echoes, below 5 HP it turns muffled and distant.

## Settings

Three ways to open (values apply instantly, stored in
`config/breathheart.json`):

- **H** key in game (Controls → Miscellaneous, rebindable);
- `/breathheart config` command;
- `/breathheart debug` command — a status line in chat
  (exertion, gasp morph, cooldowns, stress): for diagnosing anything silent;
- `/breathheart test` command — plays all 5 sounds in sequence
  at full volume (breath, heart x2, reverb, muffled): if you hear them,
  the engine and files are fine and it's about triggers/volumes.

If breathing disappeared after playing with settings — delete
`config/breathheart.json` (resets to defaults).
- settings button (gear) next to the mod in the ModMenu list — requires
  ModMenu 20.0.1+ installed (into the game, not the mod: the mod works without it).

The menu has: breathing/heartbeat toggles, volumes, recovery speed,
heart sensitivity, breathing pauses, reset to defaults. No extra config libraries needed.

## Building

JDK 25+ required:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
./gradlew build
```

Or download the ready jar from [Releases](https://github.com/smvad/breathheart/releases).
To run you need Fabric Loader 0.19.3+ and Fabric API 0.158.0+26.2.

## Checking sounds without the game

```powershell
ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1 src/client/resources/assets/breathheart/sounds/heart_slow.ogg
```

## Structure

- `src/client/java/ru/breathheart/client/` — `BreathheartClient`, `ModSounds`, `BreathheartConfig`, `BreathheartAudio`
- `.../physiology/` — `PlayerStateSampler`, `ExertionModel`, `StressModel`
- `src/client/resources/assets/breathheart/` — `sounds.json`, `sounds/*.ogg`
- `CREDITS.md` — CC0 sound sources
