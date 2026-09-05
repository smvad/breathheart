package ru.breathheart.client.physiology;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Reads movement / health / environment state from the client player. */
public final class PlayerStateSampler {
	private static final int WARDEN_SCAN_INTERVAL = 20;
	private static final double WARDEN_RADIUS = 24.0;

	private double lastX;
	private double lastZ;
	private boolean hasLast;
	private int wardenScanCooldown;
	private float wardenStress;

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
			return new PlayerState(false, false, false, false, false, true, false,
				0f, 20f, 20f, false, false, 300, 300, 0, 0f);
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

		updateWardenStress(client, p);

		return new PlayerState(sprinting, movingFast, jumping, swimming, swinging,
			p.onGround(), p.isFallFlying(), (float) p.fallDistance, health, maxHealth, alive,
			p.isEyeInFluid(FluidTags.WATER), p.getAirSupply(), p.getMaxAirSupply(), p.getArmorValue(), wardenStress);
	}

	/** Throttled warden proximity scan: 0 when none nearby, up to ~40 on top of it. */
	private void updateWardenStress(Minecraft client, LocalPlayer p) {
		if (wardenScanCooldown > 0) {
			wardenScanCooldown--;
			return;
		}
		wardenScanCooldown = WARDEN_SCAN_INTERVAL;
		wardenStress = 0f;
		if (client.level == null) {
			return;
		}
		AABB area = p.getBoundingBox().inflate(WARDEN_RADIUS, 12.0, WARDEN_RADIUS);
		List<Warden> wardens = client.level.getEntitiesOfClass(Warden.class, area, w -> w.isAlive());
		double nearest = WARDEN_RADIUS;
		for (Warden w : wardens) {
			nearest = Math.min(nearest, Math.sqrt(w.distanceToSqr(p)));
		}
		if (nearest < WARDEN_RADIUS) {
			float closeness = (float) (1.0 - nearest / WARDEN_RADIUS);
			wardenStress = 15f + 25f * closeness;
		}
	}

	public void reset() {
		hasLast = false;
		wardenStress = 0f;
		wardenScanCooldown = 0;
	}
}
