package ru.breathheart.client.physiology;

import ru.breathheart.client.BreathheartConfig;

/** Exertion 0..100: grows with activity, always decays.
 *
 * <p>Decay applies every tick, so only sustained effort (sprint, swim, repeated
 * jumps/swings) can climb out of calm — plain walking and occasional jumps
 * settle back to zero on their own.</p>
 */
public final class ExertionModel {
	private float exertion;

	public void tick(PlayerState s) {
		if (s.sprinting()) {
			exertion += BreathheartConfig.EXERT_SPRINT_PER_TICK;
		} else if (s.movingFast()) {
			exertion += BreathheartConfig.EXERT_RUN_PER_TICK;
		}
		if (s.swimming()) {
			exertion += BreathheartConfig.EXERT_SWIM_PER_TICK;
		}
		if (s.jumping()) {
			exertion += BreathheartConfig.EXERT_JUMP_BURST;
		}
		if (s.swinging()) {
			exertion += BreathheartConfig.EXERT_SWING_BURST;
		}

		exertion -= BreathheartConfig.EXERT_DECAY_PER_TICK;

		if (exertion < 0f) {
			exertion = 0f;
		}
		if (exertion > 100f) {
			exertion = 100f;
		}
	}

	public float value() {
		return exertion;
	}

	/** Single peak threshold: above it the breathing runs at peak rate. */
	public boolean isPeak() {
		return exertion >= BreathheartConfig.THRESHOLD_PEAK;
	}

	public void reset() {
		exertion = 0f;
	}
}
