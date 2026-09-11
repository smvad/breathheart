# Changelog

**Читать на русском:** [CHANGELOG_RU.md](CHANGELOG_RU.md)

## 0.1.2

- Breathing now uses author-provided recordings: calm, peak (heavy male breathing) and gasping cuts.
- Warden dread heartbeat now echoes (reverb); below 5 HP the heartbeat turns muffled and distant.
- Removed the slap-echo heartbeat variant.
- Breathing keeps its beat across regimes: stop/start of sprinting rescales the rhythm instead of restarting it.
- Gasping now starts only after ~2 s of sustained peak effort, and stops quickly on recovery.
- One dense seamless breath loop (1.6 s, audition winner N02) instead of calm/peak/gasp samples: tempo slides continuously with effort, gasping morphs the same loop louder with no switches; pitch stays fixed at 1.0.

## 0.1.1

- Low-health heartbeat now pulses in waves (swell and ease) instead of a flat floor.
- Heavy armor tires you faster: exertion gains scale with armor value (~1.6x in full diamond).
- Holding breath underwater: no breathing sounds with eyes submerged; the heartbeat rises as the air meter empties.
- Warden dread: heartbeat quickens near a live warden (throttled proximity scan, no effect otherwise).
- Self-test (`/breathheart test`) now mutes ambient sounds while cues play, so they never layer.
- Cooldown safety clamps: scheduler counters can no longer stick.
- Settings screen: "Reset to defaults" button; config JSON carries a format `version` for future migrations.

## 0.1.0

- Initial experimental release: calm / peak / gasping breathing, heartbeat on falls, damage and low HP.
- Fall panic mid-air, survival/adventure-only, settings screen (H key, `/breathheart config`, ModMenu button), `/breathheart debug|test` commands.
- Procedural breathing sounds, CC0 heartbeat, EN/RU localization.
