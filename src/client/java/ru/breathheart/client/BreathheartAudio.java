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
 * <p>Single-threshold design: below {@code THRESHOLD_PEAK} calm breathing,
 * above it peak breathing at one fixed rate, plus gasping on a Schmitt trigger.
 * Intensity scales only through volume/pitch — the rhythm never wobbles.
 * Overlap is impossible by construction: every interval is clamped to at least
 * its sample length, a busy-tail counter tracks the playing breath, and
 * transitions only ever overlap inaudible faded-tail ticks.</p>
 */
public final class BreathheartAudio {
	/** Ticks a new exertion stage must persist before it takes effect. */
	private static final int STAGE_HYSTERESIS_TICKS = 8;

	// Sample lengths in ticks — must match the ogg assets. A new breath is
	// never scheduled before the previous sample ends (minus a short faded
	// tail), so breaths can never layer no matter the jitter or transitions.
	private static final int CALM_SAMPLE_TICKS = 36;
	private static final int PEAK_SAMPLE_TICKS = 32;
	private static final int GASP_SAMPLE_TICKS = 28;
	/** Faded tail ticks allowed to overlap the next breath (inaudible). */
	private static final int TAIL_OVERLAP_TICKS = 6;
	private static final int MIN_GAP_TICKS = 4;

	private final ExertionModel exertion = new ExertionModel();
	private final StressModel stress = new StressModel();

	private int breathCooldown;
	private int heartCooldown;
	/** Remaining tail of the currently playing breath. */
	private int busyTicks;

	private boolean peakActive;
	private int peakStability;
	private boolean gaspActive;
	private int gaspStability;
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
		updatePeak();
		updateGasp();
		updateFallPanic(s);
		tickTestQueue(client);

		if (BreathheartConfig.ENABLE_BREATHING) {
			tickBreathing(client);
		} else {
			breathCooldown = 0;
		}
		if (BreathheartConfig.ENABLE_HEARTBEAT) {
			tickHeartbeat(client);
		}
	}

	/** Hysteresis + cooldown alignment on peak transitions. */
	private void updatePeak() {
		boolean raw = exertion.isPeak();
		if (raw == peakActive) {
			peakStability = 0;
			return;
		}
		peakStability++;
		if (peakStability >= STAGE_HYSTERESIS_TICKS) {
			peakActive = raw;
			peakStability = 0;
			if (peakActive) {
				// Accelerating: start the peak as soon as the current tail
				// allows — never layered, never a long stall.
				requestEarlyBreath();
			} else {
				// Recovering: restart the calm rhythm cleanly instead of
				// firing a leftover peak breath right away.
				breathCooldown = BreathheartConfig.BREATH_INTERVAL_CALM;
			}
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
	 * Gasping (одышка) as a Schmitt trigger: enters at 70, exits at 55.
	 * The 15-point gap makes flickering impossible even without the timer,
	 * and gasping shares the peak rate — only the timbre changes.
	 */
	private void updateGasp() {
		boolean raw = gaspActive
			? exertion.value() >= BreathheartConfig.THRESHOLD_GASP_EXIT
			: exertion.value() >= BreathheartConfig.THRESHOLD_GASP_ENTER;
		if (raw == gaspActive) {
			gaspStability = 0;
			return;
		}
		gaspStability++;
		if (gaspStability >= STAGE_HYSTERESIS_TICKS) {
			gaspActive = raw;
			gaspStability = 0;
			requestEarlyBreath();
		}
	}

	private void tickBreathing(Minecraft client) {
		if (busyTicks > 0) {
			busyTicks--;
		}
		if (breathCooldown > 0) {
			breathCooldown--;
			return;
		}

		SoundEvent event;
		int interval;
		int sampleTicks;
		float volume;
		float pitch;
		if (panic > 0f) {
			// Free-fall panting: accelerates the longer the fall, up to gasping.
			event = panic > 0.55f ? ModSounds.BREATH_GASP : ModSounds.BREATH_PEAK;
			interval = Math.round(BreathheartConfig.PEAK_INTERVAL
				+ (14 - BreathheartConfig.PEAK_INTERVAL) * panic);
			sampleTicks = panic > 0.55f ? GASP_SAMPLE_TICKS : PEAK_SAMPLE_TICKS;
			volume = BreathheartConfig.PEAK_VOL_MIN
				+ (BreathheartConfig.GASP_VOL_MAX - BreathheartConfig.PEAK_VOL_MIN) * panic;
			pitch = BreathheartConfig.PEAK_PITCH_MIN
				+ (BreathheartConfig.GASP_PITCH_MAX - BreathheartConfig.PEAK_PITCH_MIN) * panic;
		} else if (gaspActive) {
			// Gasping: same rate as peak, heavier timbre + louder.
			float t = (exertion.value() - BreathheartConfig.THRESHOLD_GASP_EXIT)
				/ (100f - BreathheartConfig.THRESHOLD_GASP_EXIT);
			t = Math.min(1f, Math.max(0f, t));
			event = ModSounds.BREATH_GASP;
			interval = BreathheartConfig.PEAK_INTERVAL;
			sampleTicks = GASP_SAMPLE_TICKS;
			volume = BreathheartConfig.GASP_VOL_MIN
				+ (BreathheartConfig.GASP_VOL_MAX - BreathheartConfig.GASP_VOL_MIN) * t;
			pitch = BreathheartConfig.GASP_PITCH_MIN
				+ (BreathheartConfig.GASP_PITCH_MAX - BreathheartConfig.GASP_PITCH_MIN) * t;
		} else if (peakActive) {
			// One fixed rate; intensity scales only via volume/pitch.
			float t = (exertion.value() - BreathheartConfig.THRESHOLD_PEAK)
				/ (100f - BreathheartConfig.THRESHOLD_PEAK);
			t = Math.min(1f, Math.max(0f, t));
			event = ModSounds.BREATH_PEAK;
			interval = BreathheartConfig.PEAK_INTERVAL;
			sampleTicks = PEAK_SAMPLE_TICKS;
			volume = BreathheartConfig.PEAK_VOL_MIN
				+ (BreathheartConfig.PEAK_VOL_MAX - BreathheartConfig.PEAK_VOL_MIN) * t;
			pitch = BreathheartConfig.PEAK_PITCH_MIN
				+ (BreathheartConfig.PEAK_PITCH_MAX - BreathheartConfig.PEAK_PITCH_MIN) * t;
		} else {
			event = ModSounds.BREATH_CALM;
			interval = BreathheartConfig.BREATH_INTERVAL_CALM;
			sampleTicks = CALM_SAMPLE_TICKS;
			volume = BreathheartConfig.BREATH_VOL_CALM;
			pitch = BreathheartConfig.BREATH_PITCH_CALM;
		}

		// Slight humanization so it doesn't sound metronomic — but never shorter
		// than the sample itself, or the tail would layer with the next breath.
		float jitter = 0.94f + client.player.getRandom().nextFloat() * 0.12f;
		playLocal(client, event,
			volume * BreathheartConfig.MASTER_VOLUME * BreathheartConfig.BREATH_VOLUME_SCALE,
			pitch * jitter);
		breathCooldown = Math.max(sampleTicks, Math.round(interval * jitter));
		busyTicks = sampleTicks;
	}

	/**
	 * Request an early breath (e.g. on regime change) without ever layering over
	 * the audible part of the currently playing tail — only faded tail ticks
	 * may overlap, which is inaudible.
	 */
	private void requestEarlyBreath() {
		int wait = Math.max(MIN_GAP_TICKS, busyTicks - TAIL_OVERLAP_TICKS);
		breathCooldown = Math.min(breathCooldown, wait);
	}

	private void tickHeartbeat(Minecraft client) {
		if (!stress.racing()) {
			return;
		}
		if (heartCooldown > 0) {
			heartCooldown--;
			return;
		}

		float t = (stress.value() - BreathheartConfig.STRESS_TRIGGER) / (100f - BreathheartConfig.STRESS_TRIGGER);
		t = Math.min(1f, Math.max(0f, t));

		SoundEvent event = t > 0.45f ? ModSounds.HEART_FAST : ModSounds.HEART_SLOW;
		float volume = BreathheartConfig.HEART_VOL_MIN
			+ (BreathheartConfig.HEART_VOL_MAX - BreathheartConfig.HEART_VOL_MIN) * t;
		float pitch = 0.95f + t * 0.35f;
		int interval = Math.round(BreathheartConfig.HEART_INTERVAL_SLOW
			+ (BreathheartConfig.HEART_INTERVAL_FAST - BreathheartConfig.HEART_INTERVAL_SLOW) * t);

		playLocal(client, event, volume * BreathheartConfig.MASTER_VOLUME * BreathheartConfig.HEART_VOLUME_SCALE, pitch);
		heartCooldown = Math.max(3, interval);
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
		peakActive = false;
		peakStability = 0;
		gaspActive = false;
		gaspStability = 0;
		panic = 0f;
		breathCooldown = 0;
		heartCooldown = 0;
		busyTicks = 0;
		PlayerStateSampler.INSTANCE.reset();
	}

	/** One-line state snapshot for the {@code /breathheart debug} command. */
	public String debugState() {
		return String.format(
			"exert=%.1f peak=%s gasp=%s panic=%.2f bcd=%d | stress=%.1f hcd=%d | breath=%s heart=%s",
			exertion.value(), peakActive, gaspActive, panic, breathCooldown,
			stress.value(), heartCooldown,
			BreathheartConfig.ENABLE_BREATHING, BreathheartConfig.ENABLE_HEARTBEAT);
	}

	/**
	 * Self-test: plays every registered sound once at full volume, spaced 2s apart.
	 * If these are silent, the problem is in files/engine — not in the model.
	 */
	public void startSelfTest() {
		testQueue.clear();
		testQueue.add(new TestCue(ModSounds.BREATH_CALM, "calm", 0));
		testQueue.add(new TestCue(ModSounds.BREATH_PEAK, "peak", 40));
		testQueue.add(new TestCue(ModSounds.BREATH_GASP, "gasp", 80));
		testQueue.add(new TestCue(ModSounds.HEART_SLOW, "slow-heart", 120));
		testQueue.add(new TestCue(ModSounds.HEART_FAST, "fast-heart", 160));
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
				net.minecraft.network.chat.Component.literal("[Breathheart] test: " + played));
		}
	}
}
