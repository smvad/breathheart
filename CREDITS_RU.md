# Breathheart — звуковые источники

**Read in English:** [CREDITS.md](CREDITS.md)

> **Сделано при помощи ИИ** — звуки подобраны, нарезаны и синтезированы
> AI-ассистентом (Muse Spark via OpenCode) совместно с автором (smvad).

## Сердцебиение (CC0)

Исходная запись — общественное достояние (CC0), автор Joseph SARDIN,
сайт BigSoundBank / LaSonotheque. Атрибуция не требуется, указана из уважения.

| Файл мода | Источник | Страница |
|---|---|---|
| `heart_slow.ogg`, `heart_fast.ogg` | Heart Beat, 9 c, 10 ударов (моно, 11025 Гц), нарезаны до одиночных ударов 0.75 c | https://bigsoundbank.com/heart-beat-s0218.html |

Прямая ссылка на сырой OGG: https://bigsoundbank.com/UPLOAD/ogg/0218.ogg

## Эффекты сердцебиения (вшиты в геймплей)

Производные от той же CC0-основы:

| Файл мода | Эффект | Цепочка |
|---|---|---|
| `heart_reverb.ogg` | глубокая комната (плотные ранние отражения) | `apad` до 1.4 c + `aecho=0.8:0.85:20\|40\|60\|90:0.45\|0.35\|0.25\|0.18` |
| `heart_muffled.ogg` | приглушённый/далёкий (HP ниже 5) | `lowpass=f=200` + сильный гейн |

`heart_reverb.ogg` играет рядом с живым варденом, `heart_muffled.ogg` — ниже 5 HP
(настраивается `HEART_MUFFLED_HP`). Оба также доступны через `/breathheart test`.

## Дыхание (запись от автора, одна бесшовная петля)

> Примечание: файл предоставлен автором (smvad); его исходная лицензия
> неизвестна — проверьте перед любым использованием вне мода. Сырые файлы
> лежат в `raw/` (не коммитятся).

| Файл мода | Источник | Обработка |
|---|---|---|
| `breath_loop.ogg` | `tjazheloe-muzhskoe-dyhanie.mp3` (тяжёлое мужское дыхание, 20 c) | 5.6–7.2 c (победитель прослушки N02: чистый стык, без пауз) → моно, компрессия (ровное дыхание), кроссфейд петли 0.3 c (стык проверен: транзиент на уровне фона), гейн + лимитер; ровно 1.6 c = 32 тика |

```powershell
ffmpeg -i raw/tjazheloe-muzhskoe-dyhanie.mp3 -filter_complex "[0:a]atrim=start=5.6:end=7.2,asetpts=PTS-STARTPTS,highpass=f=80,lowpass=f=2500,aformat=channel_layouts=mono,acompressor=threshold=-30dB:ratio=10:attack=5:release=90:makeup=14dB,asplit[a1][a2];[a1][a2]acrossfade=d=0.3:c1=tri:c2=tri[x];[x]atrim=start=0:end=1.6,asetpts=PTS-STARTPTS,volume=3dB,alimiter=limit=0.95:attack=5:release=50" -ar 44100 -c:a libvorbis -q:a 4 breath_loop.ogg
```
