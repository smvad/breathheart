package ru.breathheart.client;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Registers all custom SoundEvents. Audio files live in
 * {@code assets/breathheart/sounds.json} + {@code sounds/*.ogg}.
 *
 * <p>One event maps to several ogg variants in sounds.json, so Minecraft
 * picks a random variant per play — no code needed for variety.</p>
 */
public final class ModSounds {
	public static SoundEvent BREATH_LOOP;
	public static SoundEvent HEART_SLOW;
	public static SoundEvent HEART_FAST;
	/** Warden dread: heartbeat with room reverb. */
	public static SoundEvent HEART_REVERB;
	/** Near death (HP below threshold): muffled, distant heartbeat. */
	public static SoundEvent HEART_MUFFLED;

	private ModSounds() {
	}

	public static void register() {
		BREATH_LOOP = register("breath_loop");
		HEART_SLOW = register("heart_slow");
		HEART_FAST = register("heart_fast");
		HEART_REVERB = register("heart_reverb");
		HEART_MUFFLED = register("heart_muffled");
	}

	private static SoundEvent register(String path) {
		Identifier id = Identifier.fromNamespaceAndPath("breathheart", path);
		SoundEvent event = SoundEvent.createVariableRangeEvent(id);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
	}
}
