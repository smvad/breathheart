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
	public static SoundEvent BREATH_CALM;
	public static SoundEvent BREATH_PEAK;
	public static SoundEvent BREATH_GASP;
	public static SoundEvent HEART_SLOW;
	public static SoundEvent HEART_FAST;

	private ModSounds() {
	}

	public static void register() {
		BREATH_CALM = register("breath_calm");
		BREATH_PEAK = register("breath_peak");
		BREATH_GASP = register("breath_gasp");
		HEART_SLOW = register("heart_slow");
		HEART_FAST = register("heart_fast");
	}

	private static SoundEvent register(String path) {
		Identifier id = Identifier.fromNamespaceAndPath("breathheart", path);
		SoundEvent event = SoundEvent.createVariableRangeEvent(id);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
	}
}
