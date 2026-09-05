package ru.breathheart.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import ru.breathheart.client.gui.BreathheartConfigScreen;

/**
 * Breathheart — client-only mod.
 * Subtle breathing + heartbeat, exertion breathing, gasping, racing heart on falls / heavy damage.
 *
 * <p>Active in survival and adventure modes only; silent in creative and spectator.</p>
 */
public final class BreathheartClient implements ClientModInitializer {
	private final BreathheartAudio audio = new BreathheartAudio();
	private KeyMapping settingsKey;

	@Override
	public void onInitializeClient() {
		BreathheartConfig.load();
		ModSounds.register();

		settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.breathheart.settings",
			InputConstants.KEY_H,
			KeyMapping.Category.MISC));		ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommands.literal("breathheart")
				.then(ClientCommands.literal("config")
					.executes(context -> {
						Minecraft client = context.getSource().getClient();
						client.execute(() -> openSettings(client));
						return 1;
					}))
				.then(ClientCommands.literal("debug")
					.executes(context -> {
						context.getSource().sendFeedback(
							net.minecraft.network.chat.Component.literal(
								"[Breathheart] " + audio.debugState()));
						return 1;
					}))
				.then(ClientCommands.literal("test")
					.executes(context -> {
						audio.startSelfTest();
						context.getSource().sendFeedback(
							net.minecraft.network.chat.Component.literal(
								"[Breathheart] test: слушайте 5 звуков (покой, пик, отдышка, сердце x2)..."));
						return 1;
					}))));
	}

	private void onEndTick(Minecraft client) {
		if (client == null || client.player == null || client.level == null) {
			audio.reset();
			return;
		}
		// Survival + adventure only: no body sounds in creative / spectator.
		if (client.player.isCreative() || client.player.isSpectator()) {
			audio.reset();
			return;
		}
		while (settingsKey.consumeClick()) {
			if (client.gui.screen() == null) {
				openSettings(client);
			}
		}
		if (client.isPaused()) {
			return;
		}
		audio.tick(client);
	}

	/** Opens the settings screen; also exposed for the ModMenu entrypoint. */
	public static void openSettings(Minecraft client) {
		client.setScreenAndShow(new BreathheartConfigScreen(client.gui.screen()));
	}
}
