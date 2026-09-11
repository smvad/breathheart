# Breathheart — agent notes

Fabric client-side mod (breathing + heartbeat sounds), MC 26.2. Author/user smvad, speaks Russian — reply in Russian. Experimental project, fast iteration, user playtests in-game.

## Build

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
.\gradlew.bat build --console=plain -x test
```
- No tests exist; always pass `-x test`. `build/` is gitignored, jars ship via GitHub Releases, never committed.
- Version lives in `gradle.properties` (`version=`).
- Split source sets (`splitEnvironmentSourceSets()` in `build.gradle`): ALL code is under `src/client/java`, resources (incl. `fabric.mod.json`) under `src/client/resources`. There is no `src/main`.
- `fabric.mod.json` uses `${version}` expansion — it needs both `processResources` and `processClientResources` blocks; if version shows literally, one of them broke.

## Minecraft 26.2 API (verified, don't guess)

- No Yarn mappings for 26.x — Loom 1.17-SNAPSHOT uses MojMap. When unsure of a name, verify with `javap` against the MC jar, don't invent.
- Known-good names: `ClientCommands` (not `ClientCommandManager`), `KeyMappingHelper` in `keymapping.v1`, `GuiGraphicsExtractor.extractRenderState`, `client.gui.screen()/setScreen` + `client.setScreenAndShow`, `sendSystemMessage`, `isFallFlying()`, `isEyeInFluid(FluidTags.WATER)`.
- Compiled client classes land in `build/classes/java/client` (not `.../java/main`).

## Sound engine rules (hard-earned, don't regress)

- Body sounds are one-shots via `SimpleSoundInstance.forUI(event, pitch, volume)`. Never use tickable fade-in instances starting at volume 0 — the engine culls them silent.
- Scheduler intervals must never drop below the sample length: breath loop 1.6s = 32 ticks (`LOOP_SAMPLE_TICKS`), `heart_reverb` tail 1.4s = 30 ticks (`REVERB_SAMPLE_TICKS`). Clamp cooldowns with `Math.max(interval, sampleTicks)`.
- Breath pitch is fixed `1.0f` — pitch-shifting a recorded voice sounds unnatural (user complaint, keep it).
- After adding/removing sounds, keep in sync: `ModSounds.java` ↔ `sounds.json` ↔ `assets/.../sounds/*.ogg`. Validate with a quick script that every `sounds.json` entry resolves to a file.
- `/breathheart test` plays the whole queue; the cue count in `test_intro` (en_us + ru_ru) must match `startSelfTest()` length.

## Audio asset workflow

- Raw recordings live in `raw/` (gitignored, never commit; ~MB mp3s). Final oggs: mono, 44100 Hz, q:a 4. Processing recipes belong in `CREDITS.md` / `CREDITS_RU.md`.
- Loop-making: `[a]asplit[a1][a2];[a1][a2]acrossfade...` — feeding one stream label twice into `acrossfade` without `asplit` silently produces garbage. Verify a loop by **waveform sample-jump at the wrap point** (must be at background-transient level), not by 50ms energy windows — micro-gaps vs puffs make energy matching a lottery. Grid-search (window start × fade length) when needed.
- Audition files for the user go to `raw/` (e.g. `loop_candidates.wav`, `loop_seamless_test.wav`) with beep separators and a numbered map in chat. Don't touch the mod until the user picks a winner.

## Config & settings

- `BreathheartConfig`: plain public statics; only the `Data` subset is persisted to `config/breathheart.json`. The settings screen edits the persisted subset only.
- `load()` clamps migrate stale values — use clamp-range bumps (not version migrations) when a default changes shape (e.g. peak interval floor follows loop length).
- Gameplay gates: survival/adventure only (silent in creative/spectator); breath-hold with eyes underwater.

## Docs & git workflow

- Bilingual docs, keep in sync: `README.md` (EN default) + `README_RU.md`, `CREDITS*.md`, `CHANGELOG*.md` (per-version sections).
- Don't commit, push, or cut releases unless explicitly asked. User shortcuts: «запуш» = commit+push, «релиз» = push + `gh release create vX.Y.Z` with both jars attached (gh.exe at `C:\Program Files\GitHub CLI\gh.exe`).
