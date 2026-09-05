package ru.breathheart.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tunables, persisted as JSON in {@code config/breathheart.json}.
 * Ranges assume 20 ticks/second. The settings screen edits these live;
 * values are applied on the next tick, no restart needed.
 */
public final class BreathheartConfig {
	private BreathheartConfig() {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "breathheart.json";

	// Master switches / volumes
	public static boolean ENABLE_BREATHING = true;
	public static boolean ENABLE_HEARTBEAT = true;
	public static float MASTER_VOLUME = 1.0f;
	public static float BREATH_VOLUME_SCALE = 1.0f;
	public static float HEART_VOLUME_SCALE = 1.0f;

	// ---- Exertion (0..100) ----
	// Tuned so walking (+0.25) and single jumps (+2.0) stay below THRESHOLD_PEAK (15)
	// against the always-on decay, while sustained sprint (+1.6) ramps up fast.
	public static float EXERT_SPRINT_PER_TICK = 1.6f;
	public static float EXERT_RUN_PER_TICK = 0.25f;
	public static float EXERT_JUMP_BURST = 2.0f;
	public static float EXERT_SWIM_PER_TICK = 1.0f;
	public static float EXERT_SWING_BURST = 2.5f;
	public static float EXERT_DECAY_PER_TICK = 0.8f;

	public static float THRESHOLD_PEAK = 15.0f;
	/** Gasping uses a Schmitt trigger: enters at 70, exits at 55 — no flicker possible. */
	public static float THRESHOLD_GASP_ENTER = 70.0f;
	public static float THRESHOLD_GASP_EXIT = 55.0f;

	// ---- Breath scheduling (ticks between breaths) ----
	// Single peak threshold design: below it calm breathing, above it peak
	// breathing at ONE fixed rate. Intensity inside peak scales only through
	// volume/pitch, so the rhythm can never wobble between two tempos.
	// Every sample is shorter than its regime interval, so breaths never stack.
	public static int BREATH_INTERVAL_CALM = 80;
	public static int PEAK_INTERVAL = 32;

	public static float BREATH_VOL_CALM = 0.2f;
	public static float PEAK_VOL_MIN = 0.35f;
	public static float PEAK_VOL_MAX = 0.6f;
	public static float GASP_VOL_MIN = 0.55f;
	public static float GASP_VOL_MAX = 0.8f;

	public static float BREATH_PITCH_CALM = 0.95f;
	public static float PEAK_PITCH_MIN = 1.05f;
	public static float PEAK_PITCH_MAX = 1.18f;
	public static float GASP_PITCH_MIN = 1.1f;
	public static float GASP_PITCH_MAX = 1.25f;

	// ---- Stress (0..100) ----
	public static float STRESS_DECAY_PER_TICK = 0.9f;
	public static float STRESS_PER_FALL_BLOCK = 8.0f;
	public static float STRESS_FALL_THRESHOLD = 4.0f;
	public static float STRESS_PER_DAMAGE = 9.0f;
	public static float STRESS_TRIGGER = 20.0f;

	// ---- Heart scheduling ----
	public static int HEART_INTERVAL_SLOW = 20;
	public static int HEART_INTERVAL_FAST = 10;
	public static float HEART_VOL_MIN = 0.18f;
	public static float HEART_VOL_MAX = 0.7f;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	/** Snapshot of the mutable fields for Gson. */
	private static final class Data {
		boolean enableBreathing = true;
		boolean enableHeartbeat = true;
		float masterVolume = 1.0f;
		float breathVolumeScale = 1.0f;
		float heartVolumeScale = 1.0f;
		float exertDecayPerTick = 0.8f;
		float stressTrigger = 20.0f;
		int breathIntervalCalm = 80;
		int peakInterval = 30;
	}

	public static void load() {
		Path path = file();
		if (!Files.isRegularFile(path)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			Data data = GSON.fromJson(reader, Data.class);
			if (data == null) {
				return;
			}
			ENABLE_BREATHING = data.enableBreathing;
			ENABLE_HEARTBEAT = data.enableHeartbeat;
			MASTER_VOLUME = clamp(data.masterVolume, 0f, 1.5f);
			BREATH_VOLUME_SCALE = clamp(data.breathVolumeScale, 0f, 1.5f);
			HEART_VOLUME_SCALE = clamp(data.heartVolumeScale, 0f, 1.5f);
			EXERT_DECAY_PER_TICK = clamp(data.exertDecayPerTick, 0.1f, 3.0f);
			STRESS_TRIGGER = clamp(data.stressTrigger, 5f, 80f);
			BREATH_INTERVAL_CALM = (int) clamp(data.breathIntervalCalm, 30, 200);
			PEAK_INTERVAL = (int) clamp(data.peakInterval, 15, 60);
		} catch (IOException | RuntimeException e) {
			// Corrupt config: keep defaults, overwrite on next save.
		}
	}

	public static void save() {
		Data data = new Data();
		data.enableBreathing = ENABLE_BREATHING;
		data.enableHeartbeat = ENABLE_HEARTBEAT;
		data.masterVolume = MASTER_VOLUME;
		data.breathVolumeScale = BREATH_VOLUME_SCALE;
		data.heartVolumeScale = HEART_VOLUME_SCALE;
		data.exertDecayPerTick = EXERT_DECAY_PER_TICK;
		data.stressTrigger = STRESS_TRIGGER;
		data.breathIntervalCalm = BREATH_INTERVAL_CALM;
		data.peakInterval = PEAK_INTERVAL;
		try {
			Files.createDirectories(file().getParent());
			try (Writer writer = Files.newBufferedWriter(file())) {
				GSON.toJson(data, writer);
			}
		} catch (IOException ignored) {
		}
	}

	private static float clamp(float value, float min, float max) {
		return Math.min(max, Math.max(min, value));
	}
}
