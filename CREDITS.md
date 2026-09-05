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

## Breathing (procedural synthesis, no sources)

Recorded voices (sighs, panting, strangers' breathing) were all rejected one by one —
too distinctive. The final breathing is synthesized from pink noise:
neutral "air" with no mouth or voice, deterministic (fixed seeds),
no rights required.

Calm and peak are a full inhale-exhale cycle: brighter inhale (higher lowpass),
darker exhale, a pause between cycles. Gasping is exhales only (frequent, no
inhale), which is why it sounds heavier.

```powershell
# calm, 1.8 s = 0.9 s inhale + 0.9 s exhale (calm interval is 4 s — the rest is
# silence, so the tail never blocks transitions)
ffmpeg -f lavfi -i "anoisesrc=color=pink:sample_rate=44100:duration=0.9:seed=31" `
       -f lavfi -i "anoisesrc=color=pink:sample_rate=44100:duration=0.9:seed=32" `
  -filter_complex "[0:a]lowpass=f=800,highpass=f=70,volume='0.08+0.92*pow(sin(PI*t/0.9),0.8)':eval=frame,afade=t=in:st=0:d=0.1,afade=t=out:st=0.72:d=0.18[inh];[1:a]lowpass=f=350,highpass=f=60,volume='0.08+0.92*pow(sin(PI*t/0.9),0.8)':eval=frame,afade=t=in:st=0:d=0.1,afade=t=out:st=0.72:d=0.18[exh];[inh][exh]concat=n=2:v=0:a=1,volume=2dB" `
  -ac 1 -ar 44100 -c:a libvorbis -q:a 4 breath_calm.ogg

# peak, 1.6 s = 0.8 s inhale + 0.8 s exhale, no pause (seed pairs 41/42, 43/44)
# gasp, 1.4 s: a single exhale (seeds 21/22/23), louder and brighter
ffmpeg -f lavfi -i "anoisesrc=color=pink:sample_rate=44100:duration=1.4:seed=21" `
  -af "lowpass=f=1000,highpass=f=90,equalizer=f=1400:t=q:w=1:g=5,volume='0.1+0.9*pow(sin(PI*t/1.4),0.7)':eval=frame,afade=t=in:st=0:d=0.1,afade=t=out:st=1.15:d=0.25,volume=6dB" `
  -ac 1 -ar 44100 -c:a libvorbis -q:a 4 breath_gasp_a.ogg
```
