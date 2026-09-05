# Breathheart — звуковые источники

**Read in English:** [CREDITS.md](CREDITS.md)

> **Сделано при помощи ИИ** — звуки подобраны, нарезаны и синтезированы
> AI-ассистентом (Muse Spark via OpenCode) совместно с автором (smvad25).

## Сердцебиение (CC0)

Исходная запись — общественное достояние (CC0), автор Joseph SARDIN,
сайт BigSoundBank / LaSonotheque. Атрибуция не требуется, указана из уважения.

| Файл мода | Источник | Страница |
|---|---|---|
| `heart_slow.ogg`, `heart_fast.ogg` | Heart Beat, 9 c, 10 ударов (моно, 11025 Гц), нарезаны до одиночных ударов 0.75 c | https://bigsoundbank.com/heart-beat-s0218.html |

Прямая ссылка на сырой OGG: https://bigsoundbank.com/UPLOAD/ogg/0218.ogg

## Дыхание (процедурный синтез, без исходников)

Записанные голоса (вздохи, пыхтение, чужое дыхание) по очереди не подошли —
слишком характерные. Итоговое дыхание синтезировано из розового шума:
нейтральный «воздух» без рта и голоса, детерминировано (фиксированные seeds),
прав не требует.

Покой и пик — полный цикл вдох-выдох: вдох ярче (lowpass выше), выдох темнее,
между циклами пауза. Отдышка — только выдохи (частые, без вдоха), поэтому
звучит тяжелее.

```powershell
# покой, 1.8 c = вдох 0.9 c + выдох 0.9 c (интервал покоя 4 c — остальное тишина,
# поэтому хвост никогда не мешает переходам)
ffmpeg -f lavfi -i "anoisesrc=color=pink:sample_rate=44100:duration=0.9:seed=31" `
       -f lavfi -i "anoisesrc=color=pink:sample_rate=44100:duration=0.9:seed=32" `
  -filter_complex "[0:a]lowpass=f=800,highpass=f=70,volume='0.08+0.92*pow(sin(PI*t/0.9),0.8)':eval=frame,afade=t=in:st=0:d=0.1,afade=t=out:st=0.72:d=0.18[inh];[1:a]lowpass=f=350,highpass=f=60,volume='0.08+0.92*pow(sin(PI*t/0.9),0.8)':eval=frame,afade=t=in:st=0:d=0.1,afade=t=out:st=0.72:d=0.18[exh];[inh][exh]concat=n=2:v=0:a=1,volume=2dB" `
  -ac 1 -ar 44100 -c:a libvorbis -q:a 4 breath_calm.ogg

# пик, 1.6 c = вдох 0.8 c + выдох 0.8 c, без паузы (пары seeds 41/42, 43/44)
# отдышка, 1.4 c: один выдох (seeds 21/22/23), громче и ярче
ffmpeg -f lavfi -i "anoisesrc=color=pink:sample_rate=44100:duration=1.4:seed=21" `
  -af "lowpass=f=1000,highpass=f=90,equalizer=f=1400:t=q:w=1:g=5,volume='0.1+0.9*pow(sin(PI*t/1.4),0.7)':eval=frame,afade=t=in:st=0:d=0.1,afade=t=out:st=1.15:d=0.25,volume=6dB" `
  -ac 1 -ar 44100 -c:a libvorbis -q:a 4 breath_gasp_a.ogg
```
