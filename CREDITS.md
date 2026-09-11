# Breathheart — sound sources

**Читать на русском:** [CREDITS_RU.md](CREDITS_RU.md)

> **Made with AI** — the sounds were picked, cut and synthesized
> by an AI assistant (Muse Spark via OpenCode) together with the author (smvad).

## Heartbeat (CC0)

The source recording is public domain (CC0) by Joseph SARDIN,
BigSoundBank / LaSonotheque. Attribution is not required, given out of respect.

| Mod file | Source | Page |
|---|---|---|
| `heart_slow.ogg`, `heart_fast.ogg` | Heart Beat, 9 s, 10 beats (mono, 11025 Hz), cut to single 0.75 s thumps | https://bigsoundbank.com/heart-beat-s0218.html |

Raw OGG direct link: https://bigsoundbank.com/UPLOAD/ogg/0218.ogg

## Heartbeat effects (wired into gameplay)

Derived from the same CC0 base above:

| Mod file | Effect | Chain |
|---|---|---|
| `heart_reverb.ogg` | deep room reverb (dense early reflections) | `apad` to 1.4 s + `aecho=0.8:0.85:20\|40\|60\|90:0.45\|0.35\|0.25\|0.18` |
| `heart_muffled.ogg` | muffled/distant (HP below 5) | `lowpass=f=200` + heavy gain |

`heart_reverb.ogg` plays near a live warden, `heart_muffled.ogg` below 5 HP
(tunable `HEART_MUFFLED_HP`). Both are also playable via `/breathheart test`.

## Breathing (user-provided recording, one seamless loop)

> Note: this file was provided by the author (smvad); its original license
> is unknown — check before any use outside this mod. Raw files live in `raw/`
> (not committed).

| Mod file | Source | Processing |
|---|---|---|
| `breath_loop.ogg` | `tjazheloe-muzhskoe-dyhanie.mp3` (heavy male breathing, 20 s) | 5.6–7.2 s (audition winner N02: clean joint, no pauses) → mono, compression (even breathing), loop crossfade 0.3 s (joint verified: transient at background level), gain + limiter; exactly 1.6 s = 32 ticks |

Example:
```powershell
ffmpeg -i raw/tjazheloe-muzhskoe-dyhanie.mp3 -filter_complex "[0:a]atrim=start=5.6:end=7.2,asetpts=PTS-STARTPTS,highpass=f=80,lowpass=f=2500,aformat=channel_layouts=mono,acompressor=threshold=-30dB:ratio=10:attack=5:release=90:makeup=14dB,asplit[a1][a2];[a1][a2]acrossfade=d=0.3:c1=tri:c2=tri[x];[x]atrim=start=0:end=1.6,asetpts=PTS-STARTPTS,volume=3dB,alimiter=limit=0.95:attack=5:release=50" -ar 44100 -c:a libvorbis -q:a 4 breath_loop.ogg
```
