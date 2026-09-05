# Breathheart

**Читать на русском:** [README_RU.md](README_RU.md)

> **Made with AI** — the code, sounds and docs of this mod were created
> by an AI assistant (Muse Spark via OpenCode) together with the author (smvad25).
>
> **Experiment, not a final product.** This project was created for experimental
> purposes: mechanics, sounds and balance may change without backward compatibility.

A client-side Fabric mod for Minecraft 26.2: a breathing and heartbeat system.

- **Background:** barely audible breathing and heartbeat at rest.
- **Exertion:** running, sprinting, jumping, swimming, hitting/mining → breathing gets faster, louder, higher-pitched; prolonged effort leads to gasping that fades smoothly.
- **Critical moments:** falling from height, sudden heavy damage, low health → racing heartbeat that fades out.

Everything is detected from movement/health on the client (no mixins needed); sound is
non-positional one-shot samples with dynamic volume, pitch and interval. Every sample
is shorter than its stage interval, so breaths never overlap and nothing is cut off
mid-play; stage switches are additionally smoothed by an 8-tick hysteresis.

The mod is active in survival and adventure only; silent in creative and spectator.

Tuning: walking and occasional jumps stay in the calm stage (decay runs every
tick). A single peak threshold (exertion 15, ~1 sec of sprinting): above it
breathing runs at a fixed 1.6 sec rhythm, intensity scaling only through volume
and pitch — the rhythm never wobbles. Gasping is a Schmitt trigger (enter at 70,
exit at 55, plus hysteresis): same rhythm, but heavy exhale timbre and higher volume.
Falling: heart and breathing accelerate mid-air, the stronger the longer the fall
(threshold ~2.5 blocks, small hops stay silent, elytra gliding doesn't count);
landing from 4+ blocks adds a stress spike on top.

Breathing sound is procedural (pink noise + slow volume swell):
neutral air with no voice or mouth. The heart is a CC0 recording, cut down to
single 0.75 s thumps so beats don't layer at fast rates.
Details in `CREDITS.md`.

## Settings

Three ways to open (values apply instantly, stored in
`config/breathheart.json`):

- **H** key in game (Controls → Miscellaneous, rebindable);
- `/breathheart config` command;
- `/breathheart debug` command — a status line in chat
  (exertion, peak, cooldowns, stress): for diagnosing anything silent;
- `/breathheart test` command — plays all 5 sounds in sequence
  at full volume (calm, peak, gasp, heart x2): if you hear them,
  the engine and files are fine and it's about triggers/volumes.

If breathing disappeared after playing with settings — delete
`config/breathheart.json` (resets to defaults).
- settings button (gear) next to the mod in the ModMenu list — requires
  ModMenu 20.0.1+ installed (into the game, not the mod: the mod works without it).

The menu has: breathing/heartbeat toggles, volumes, recovery speed,
heart sensitivity, breathing pauses. No extra config libraries needed.

## Building

JDK 25+ required:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
./gradlew build
```

Ready jar: `build/libs/breathheart-0.1.0.jar` (take the short-named file).
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
