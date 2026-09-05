package ru.breathheart.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.breathheart.client.gui.BreathheartConfigScreen;

/** Adds the gear / settings button for Breathheart in the ModMenu mods list. */
@Environment(EnvType.CLIENT)
public final class BreathheartModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return BreathheartConfigScreen::new;
	}
}
