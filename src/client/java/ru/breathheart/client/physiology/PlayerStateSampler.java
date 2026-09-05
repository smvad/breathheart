package ru.breathheart.client.physiology;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Reads movement / health state from the client player. */
public final class PlayerStateSampler {
	private double lastX;
	private double lastZ;
	private boolean hasLast;

	private PlayerStateSampler() {
	}

	public static final PlayerStateSampler INSTANCE = new PlayerStateSampler();

	public PlayerState sample(Minecraft client) {
		LocalPlayer p = client.player;
		double dx = 0.0;
		double dz = 0.0;
		if (hasLast && p != null) {
			dx = p.getX() - lastX;
			dz = p.getZ() - lastZ;
		}
		if (p != null) {
			lastX = p.getX();
			lastZ = p.getZ();
			hasLast = true;
		}
		if (p == null) {
			return new PlayerState(false, false, false, false, false, true, false, 0f, 20f, 20f, false);
		}

		double horizontalSpeed = Math.sqrt(dx * dx + dz * dz); // blocks per client tick
		boolean movingFast = horizontalSpeed > 0.12;
		boolean sprinting = p.isSprinting() && movingFast;
		boolean jumping = !p.onGround() && p.getDeltaMovement().y > 0.15;
		boolean swimming = p.isSwimming() || (p.isInWater() && movingFast);
		boolean swinging = p.swinging;

		float health = p.getHealth();
		float maxHealth = p.getMaxHealth();
		boolean alive = p.isAlive() && health > 0.0f;

		return new PlayerState(sprinting, movingFast, jumping, swimming, swinging, p.onGround(), p.isFallFlying(), (float) p.fallDistance, health, maxHealth, alive);
	}

	public void reset() {
		hasLast = false;
	}
}
