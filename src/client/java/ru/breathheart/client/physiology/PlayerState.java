package ru.breathheart.client.physiology;

/** Minimal per-tick snapshot of what the audio model needs. */
public record PlayerState(
	boolean sprinting,
	boolean movingFast,
	boolean jumping,
	boolean swimming,
	boolean swinging,
	boolean onGround,
	boolean gliding,
	float fallDistance,
	float health,
	float maxHealth,
	boolean alive,
	boolean eyesUnderwater,
	int airSupply,
	int maxAirSupply,
	int armorValue,
	float wardenStress
) {
}
