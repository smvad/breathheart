package ru.breathheart.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import ru.breathheart.client.physiology.ExertionModel;
import ru.breathheart.client.physiology.PlayerState;
import ru.breathheart.client.physiology.PlayerStateSampler;
import ru.breathheart.client.physiology.StressModel;

/**
 * Schedules one-shot breath / heartbeat sounds with dynamic volume, pitch and interval.
 * One-shots are more robust than looping TickableSoundInstances across sound-engine
 * reloads (F3+T), respawns and dimension changes — and unlike fade-in instances
 * (which the engine culls for starting at volume 0) they always play.
 *
 * <p>Breathing is ONE seamless loop at a continuous tempo: the interval slides
 * from the calm setting down to the sample length (gapless tiling) as exertion
 * or free-fall panic rises, with volume scaling along (pitch stays 1.0 — a
 * recorded voice shifted in pitch sounds unnatural). There are no regime
 * switches, so the beat can never break. Gasping is not a separate
 * sample — the same loop morphs louder through a smoothed 0..1
 * factor, so the transition never clicks.</p>
 */
public final class BreathheartAudio {
	/** Ticks a new exertion stage must persist before it takes effect. */
	private static final int STAGE_HYSTERESIS_TICKS = 8;

	// The single breath loop is 1.6 s = 32 ticks. The interval never drops
	// below it, so at full drive the loop tiles gaplessly and can never layer.
	private static final int LOOP_SAMPLE_TICKS = 32;
	/** heart_slow / heart_fast / heart_muffled are 0.75 s thumps. */
	private static final int HEART_SAMPLE_TICKS = 15;
	/** heart_reverb carries a 1.4 s tail — the cooldown must cover it. */
	private static final int REVERB_SAMPLE_TICKS = 30;
	/** Gasp morph rate: full ramp in ~30 ticks up, ~8 ticks down. */
	private static final float GASP_ATTACK_PER_TICK = 1f / 30f;
	private static final float GASP_RELEASE_PER_TICK = 1f / 8f;

	private final ExertionModel exertion = new ExertionModel();
	private final StressModel stress = new StressModel();

	private int breathCooldown;
	private int heartCooldown;

	private boolean gaspLatched;
	private int gaspStability;
	/** Smoothed 0..1 gasp morph — the only thing that changes the timbre. */
	private float gaspLevel;
	/** 0..1 free-fall panic, live from fallDistance (0 on ground / gliding). */
	private float panic;

	/** Self-test queue: (event, ticks until play). Filled by /breathheart test. */
	private final java.util.ArrayDeque<TestCue> testQueue = new java.util.ArrayDeque<>();

	private record TestCue(SoundEvent event, String name, int delay) {
		TestCue tick() {
			return new TestCue(event, name, delay - 1);
		}
	}

	public void tick(Minecraft client) {
		if (client.player == null) {
			return;
		}
		PlayerState s = PlayerStateSampler.INSTANCE.sample(client);
		if (!s.alive()) {
			reset();
			return;
		}

		exertion.tick(s);
		stress.tick(s);
		updateFallPanic(s);
		updateGaspLatch();
		// Smooth morph toward the target: latched exertion gasping, or live
		// fall panic. No switches — the timbre glides.
		float gaspTarget = gaspLatched ? 1f : panic;
		if (gaspLevel < gaspTarget) {
			gaspLevel = Math.min(gaspTarget, gaspLevel + GASP_ATTACK_PER_TICK);
		} else {
			gaspLevel = Math.max(gaspTarget, gaspLevel - GASP_RELEASE_PER_TICK);
		}

		// Safety clamps: cooldowns only ever count down, never stick.
		if (breathCooldown < 0) {
			breathCooldown = 0;
		}
		if (heartCooldown < 0) {
			heartCooldown = 0;
		}

		// Self-test owns the speakers: ambient stays quiet so test cues
		// never layer with scheduled breaths or heartbeats.
		if (isTestRunning()) {
			tickTestQueue(client);
			return;
		}
		tickTestQueue(client);

		// Breath-hold: no breathing sounds with eyes underwater (the stress
		// model still raises the heartbeat as air runs out).
		if (BreathheartConfig.ENABLE_BREATHING && !s.eyesUnderwater()) {
			tickBreathing(client);
		} else {
			breathCooldown = 0;
		}
		if (BreathheartConfig.ENABLE_HEARTBEAT) {
			tickHeartbeat(client, s);
		}
	}

	/** True while queued self-test cues are still playing. */
	public boolean isTestRunning() {
		return !testQueue.isEmpty();
	}

	/**
	 * Gasp latch as a Schmitt trigger: latches at 70, releases at 55.
	 * Latching additionally requires sustained effort (GASP_ENTER_DELAY_TICKS) —
	 * a brief spike never triggers it. Releasing stays fast. The latch only
	 * sets the morph target; {@code gaspLevel} glides toward it every tick.
	 */
	private void updateGaspLatch() {
		boolean raw = gaspLatched
			? exertion.value() >= BreathheartConfig.THRESHOLD_GASP_EXIT
			: exertion.value() >= BreathheartConfig.THRESHOLD_GASP_ENTER;
		if (raw == gaspLatched) {
			gaspStability = 0;
			return;
		}
		gaspStability++;
		int need = gaspLatched ? STAGE_HYSTERESIS_TICKS : BreathheartConfig.GASP_ENTER_DELAY_TICKS;
		if (gaspStability >= need) {
			gaspLatched = raw;
			gaspStability = 0;
		}
	}

	/**
	 * Live fall reaction: while airborne (and not gliding) both heart and
	 * breathing intensify with fallDistance — the longer the fall, the faster.
	 * Small hops (under ~2.5 blocks) stay silent.
	 */
	private void updateFallPanic(PlayerState s) {
		if (!s.onGround() && !s.gliding() && s.fallDistance() >= 2.5f) {
			stress.boost(Math.min(90f, 14f + (s.fallDistance() - 3.5f) * 9f));
			panic = Math.min(1f, Math.max(0f, (s.fallDistance() - 2.5f) / 11f));
		} else {
			panic = 0f;
		}
	}

	/**
	 * Continuous-tempo breathing over one seamless loop — no regimes, no switches.
	 */
	private void tickBreathing(Minecraft client) {
		if (breathCooldown > 0) {
			breathCooldown--;
			return;
		}

		// Continuous drive: exertion and free-fall panic share one 0..1 axis.
		float drive = Math.min(1f, Math.max(0f, Math.max(exertion.value() / 100f, panic)));
		// Tempo: calm interval at rest, gapless loop tiling at full drive.
		float interval = BreathheartConfig.BREATH_INTERVAL_CALM
			+ (BreathheartConfig.PEAK_INTERVAL - BreathheartConfig.BREATH_INTERVAL_CALM) * drive;
		float volume = BreathheartConfig.BREATH_VOL_CALM
			+ (BreathheartConfig.PEAK_VOL_MAX - BreathheartConfig.BREATH_VOL_CALM) * drive;
		// Gasping morphs the same loop louder — gradually. Pitch stays 1.0:
		// shifting a recorded voice sounds unnatural.
		volume += (BreathheartConfig.GASP_VOL_MAX - volume) * gaspLevel;

		// Slight timing humanization so it doesn't sound metronomic — but never
		// shorter than the sample itself, or the loop would layer with itself.
		float jitter = 0.94f + client.player.getRandom().nextFloat() * 0.12f;
		playLocal(client, ModSounds.BREATH_LOOP,
			volume * BreathheartConfig.MASTER_VOLUME * BreathheartConfig.BREATH_VOLUME_SCALE,
			1.0f);
		breathCooldown = Math.max(LOOP_SAMPLE_TICKS, Math.round(interval * jitter));
	}

	/**
	 * Phase-continuous regime change: rescale the remaining cooldown to the
	 * new tempo instead of restarting it, so the beat never breaks on
	 * transitions. Still never overlaps the audible part of the currently
	 * playing tail — only faded tail ticks may overlap, which is inaudible.
	 */
	private void tickHeartbeat(Minecraft client, PlayerState s) {
		if (!stress.racing()) {
			return;
		}
		if (heartCooldown > 0) {
			heartCooldown--;
			return;
		}

		float t = (stress.value() - BreathheartConfig.STRESS_TRIGGER) / (100f - BreathheartConfig.STRESS_TRIGGER);
		t = Math.min(1f, Math.max(0f, t));

		// Timbre overrides: near death the heart sounds distant and muffled;
		// near a warden it echoes like the deep dark itself.
		SoundEvent event;
		int sampleTicks;
		if (s.alive() && s.health() < BreathheartConfig.HEART_MUFFLED_HP) {
			event = ModSounds.HEART_MUFFLED;
			sampleTicks = HEART_SAMPLE_TICKS;
		} else if (s.wardenStress() > 0f) {
			event = ModSounds.HEART_REVERB;
			sampleTicks = REVERB_SAMPLE_TICKS;
		} else {
			event = t > 0.45f ? ModSounds.HEART_FAST : ModSounds.HEART_SLOW;
			sampleTicks = HEART_SAMPLE_TICKS;
		}
		float volume = BreathheartConfig.HEART_VOL_MIN
			+ (BreathheartConfig.HEART_VOL_MAX - BreathheartConfig.HEART_VOL_MIN) * t;
		float pitch = 0.95f + t * 0.35f;
		int interval = Math.round(BreathheartConfig.HEART_INTERVAL_SLOW
			+ (BreathheartConfig.HEART_INTERVAL_FAST - BreathheartConfig.HEART_INTERVAL_SLOW) * t);

		playLocal(client, event, volume * BreathheartConfig.MASTER_VOLUME * BreathheartConfig.HEART_VOLUME_SCALE, pitch);
		// The reverb tail (30 ticks) is longer than the fast interval —
		// never schedule the next beat before the sample ends.
		heartCooldown = Math.max(3, Math.max(interval, sampleTicks));
	}

	private static void playLocal(Minecraft client, SoundEvent event, float volume, float pitch) {
		if (event == null || volume <= 0.001f) {
			return;
		}
		// forUI = non-positional, plays at full volume regardless of camera — ideal for body sounds.
		client.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
	}

	public void reset() {
		exertion.reset();
		stress.reset();
		gaspLatched = false;
		gaspStability = 0;
		gaspLevel = 0f;
		panic = 0f;
		breathCooldown = 0;
		heartCooldown = 0;
		PlayerStateSampler.INSTANCE.reset();
	}

	/** One-line state snapshot for the {@code /breathheart debug} command. */
	public String debugState() {
		return String.format(
			"exert=%.1f gasp=%.2f panic=%.2f bcd=%d | stress=%.1f hcd=%d | breath=%s heart=%s",
			exertion.value(), gaspLevel, panic, breathCooldown,
			stress.value(), heartCooldown,
			BreathheartConfig.ENABLE_BREATHING, BreathheartConfig.ENABLE_HEARTBEAT);
	}

	/**
	 * Self-test: plays every registered sound once at full volume, spaced 2s apart.
	 * If these are silent, the problem is in files/engine — not in the model.
	 */
	public void startSelfTest() {
		testQueue.clear();
		testQueue.add(new TestCue(ModSounds.BREATH_LOOP, "breath", 0));
		testQueue.add(new TestCue(ModSounds.HEART_SLOW, "slow-heart", 40));
		testQueue.add(new TestCue(ModSounds.HEART_FAST, "fast-heart", 80));
		testQueue.add(new TestCue(ModSounds.HEART_REVERB, "reverb-heart", 120));
		testQueue.add(new TestCue(ModSounds.HEART_MUFFLED, "muffled-heart", 160));
	}

	public String pollTestCue(Minecraft client) {
		TestCue head = testQueue.peek();
		if (head == null) {
			return null;
		}
		testQueue.poll();
		if (head.delay() <= 0) {
			playLocal(client, head.event(), 1.0f, 1.0f);
			return head.name();
		}
		testQueue.addFirst(head.tick());
		return null;
	}

	private void tickTestQueue(Minecraft client) {
		// Play at most one cue per tick; pollTestCue re-queues until delay expires.
		String played = pollTestCue(client);
		if (played != null && client.player != null) {
			client.player.sendSystemMessage(
				net.minecraft.network.chat.Component.translatable(
					"breathheart.chat.test_cue", played));
		}
	}
}
