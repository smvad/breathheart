package ru.breathheart.client.physiology;

import ru.breathheart.client.BreathheartConfig;

/**
 * Stress 0..100: spikes on damage / hard landings / low HP, then fades.
 * Damage and landings are detected from per-tick health / fallDistance deltas,
 * so no mixins are required.
 *
 * <p>Note: vanilla resets {@code fallDistance} to 0 in the same tick the player
 * lands, so the landing spike must use the peak remembered while airborne —
 * reading {@code fallDistance} on the landing tick always yields 0.</p>
 */
public final class StressModel {
	private float stress;
	private float lastHealth = -1f;
	private float peakFall = 0f;
	private boolean wasOnGround = true;

	public void tick(PlayerState s) {
		if (lastHealth < 0f) {
			lastHealth = s.health();
		}

		// Damage spike: any health loss between ticks.
		float healthDrop = lastHealth - s.health();
		if (healthDrop >= 1.0f && s.alive()) {
			stress += healthDrop * BreathheartConfig.STRESS_PER_DAMAGE;
		}
		lastHealth = s.health();

		// Hard landing: use the peak remembered while airborne, because vanilla
		// already reset fallDistance to 0 on the landing tick.
		// Any landing above the threshold guarantees a racing heart:
		// base 30 stress (just over the trigger) plus 6 per extra block.
		if (!s.onGround()) {
			peakFall = Math.max(peakFall, s.fallDistance());
		} else {
			if (!wasOnGround && peakFall >= BreathheartConfig.STRESS_FALL_THRESHOLD) {
				stress += 30.0f + (peakFall - BreathheartConfig.STRESS_FALL_THRESHOLD) * 6.0f;
			}
			peakFall = 0f;
		}
		wasOnGround = s.onGround();

		// Low HP keeps the heart racing.
		if (s.alive() && s.health() <= 6.0f) {
			float floor = 40.0f + (6.0f - s.health()) * 10.0f;
			if (stress < floor) {
				stress = floor;
			}
		}

		// Fade.
		stress -= BreathheartConfig.STRESS_DECAY_PER_TICK;
		if (stress < 0f) {
			stress = 0f;
		}
		if (stress > 100f) {
			stress = 100f;
		}
	}

	public float value() {
		return stress;
	}

	/** Raise stress to at least the given level (used for live fall panic). */
	public void boost(float floor) {
		if (stress < floor) {
			stress = Math.min(100f, floor);
		}
	}

	public boolean racing() {
		return stress >= BreathheartConfig.STRESS_TRIGGER;
	}

	public void reset() {
		stress = 0f;
		lastHealth = -1f;
		peakFall = 0f;
		wasOnGround = true;
	}
}
