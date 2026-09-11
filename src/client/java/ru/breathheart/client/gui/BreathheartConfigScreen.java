package ru.breathheart.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import ru.breathheart.client.BreathheartConfig;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

/**
 * Vanilla settings screen (no Cloth Config dependency).
 * Open with {@code /breathheart config}. Values apply live, saved to
 * {@code config/breathheart.json} on Done / close.
 *
 * <p>All texts are translatable ({@code en_us} default, {@code ru_ru} included);
 * the game language switches the screen automatically.</p>
 */
public final class BreathheartConfigScreen extends Screen {
	private final Screen parent;

	public BreathheartConfigScreen(Screen parent) {
		super(Component.translatable("breathheart.settings.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int w = 300;
		int x = centerX - w / 2;
		int y = 50;
		int step = 24;

		addRenderableWidget(toggleButton(x, y, w,
			() -> BreathheartConfig.ENABLE_BREATHING,
			value -> BreathheartConfig.ENABLE_BREATHING = value,
			"breathheart.settings.breathing"));
		y += step;
		addRenderableWidget(toggleButton(x, y, w,
			() -> BreathheartConfig.ENABLE_HEARTBEAT,
			value -> BreathheartConfig.ENABLE_HEARTBEAT = value,
			"breathheart.settings.heartbeat"));
		y += step + 4;

		addRenderableWidget(new PercentSlider(x, y, w, "breathheart.settings.masterVolume",
			() -> BreathheartConfig.MASTER_VOLUME, value -> BreathheartConfig.MASTER_VOLUME = (float) value));
		y += step;
		addRenderableWidget(new PercentSlider(x, y, w, "breathheart.settings.breathVolume",
			() -> BreathheartConfig.BREATH_VOLUME_SCALE, value -> BreathheartConfig.BREATH_VOLUME_SCALE = (float) value));
		y += step;
		addRenderableWidget(new PercentSlider(x, y, w, "breathheart.settings.heartVolume",
			() -> BreathheartConfig.HEART_VOLUME_SCALE, value -> BreathheartConfig.HEART_VOLUME_SCALE = (float) value));
		y += step + 4;

		addRenderableWidget(new RangeSlider(x, y, w, "breathheart.settings.recovery",
			value -> String.format("%.2f", value),
			0.1, 3.0, () -> BreathheartConfig.EXERT_DECAY_PER_TICK,
			value -> BreathheartConfig.EXERT_DECAY_PER_TICK = (float) value));
		y += step;
		addRenderableWidget(new RangeSlider(x, y, w, "breathheart.settings.heartSensitivity",
			value -> String.format("%.0f", value),
			5, 80, () -> BreathheartConfig.STRESS_TRIGGER,
			value -> BreathheartConfig.STRESS_TRIGGER = (float) value));
		y += step;
		addRenderableWidget(new RangeSlider(x, y, w, "breathheart.settings.calmInterval",
			value -> String.format("%.0f", value),
			30, 200, () -> BreathheartConfig.BREATH_INTERVAL_CALM,
			value -> BreathheartConfig.BREATH_INTERVAL_CALM = (int) Math.round(value)));
		y += step;
		addRenderableWidget(new RangeSlider(x, y, w, "breathheart.settings.peakInterval",
			value -> String.format("%.0f", value),
			32, 60, () -> BreathheartConfig.PEAK_INTERVAL,
			value -> BreathheartConfig.PEAK_INTERVAL = (int) Math.round(value)));
		y += step + 4;

		addRenderableWidget(Button.builder(Component.translatable("breathheart.settings.reset"), button -> {
				BreathheartConfig.resetDefaults();
				Minecraft client = this.minecraft;
				if (client != null) {
					client.setScreenAndShow(new BreathheartConfigScreen(parent));
				}
			})
			.bounds(centerX - 100, y, 200, 20)
			.build());
		y += step + 4;

		addRenderableWidget(Button.builder(Component.translatable("breathheart.settings.done"), button -> onClose())
			.bounds(centerX - 100, y, 200, 20)
			.build());
	}

	private static Button toggleButton(int x, int y, int w, BooleanSupplier get,
			Consumer<Boolean> set, String nameKey) {
		return Button.builder(toggleLabel(nameKey, get.getAsBoolean()), button -> {
				set.accept(!get.getAsBoolean());
				button.setMessage(toggleLabel(nameKey, get.getAsBoolean()));
			})
			.bounds(x, y, w, 20)
			.build();
	}

	private static MutableComponent toggleLabel(String nameKey, boolean on) {
		return Component.translatable(nameKey)
			.append(": ")
			.append(Component.translatable(on ? "breathheart.settings.on" : "breathheart.settings.off"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title.getString(), this.width / 2, 25, 0xFFFFFF);
		graphics.centeredText(this.font,
			Component.translatable("breathheart.settings.hint").getString(),
			this.width / 2, 37, 0xA0A0A0);
	}

	@Override
	public void onClose() {
		BreathheartConfig.save();
		this.minecraft.setScreenAndShow(parent);
	}

	/** Slider showing a 0..150% value. */
	private static final class PercentSlider extends AbstractSliderButton {
		private final String nameKey;
		private final DoubleSupplier get;
		private final DoubleConsumer set;

		PercentSlider(int x, int y, int width, String nameKey, DoubleSupplier get, DoubleConsumer set) {
			super(x, y, width, 20, Component.empty(), toSlider(get.getAsDouble()));
			this.nameKey = nameKey;
			this.get = get;
			this.set = set;
			updateMessage();
		}

		static double toSlider(double volume) {
			return Math.min(1.0, Math.max(0.0, volume / 1.5));
		}

		@Override
		protected void updateMessage() {
			// May be called from the super-constructor before our fields are set.
			if (this.get == null) {
				setMessage(Component.empty());
				return;
			}
			setMessage(Component.translatable(this.nameKey == null ? "" : this.nameKey,
				Math.round(get.getAsDouble() * 100) + "%"));
		}

		@Override
		protected void applyValue() {
			set.accept(this.value * 1.5);
		}
	}

	/** Slider mapping [min..max] to the raw value. */
	private static final class RangeSlider extends AbstractSliderButton {
		private final String nameKey;
		private final Function<Double, String> format;
		private final double min;
		private final double max;
		private final DoubleSupplier get;
		private final DoubleConsumer set;

		RangeSlider(int x, int y, int width, String nameKey, Function<Double, String> format,
				double min, double max, DoubleSupplier get, DoubleConsumer set) {
			super(x, y, width, 20, Component.empty(), toSlider(get.getAsDouble(), min, max));
			this.nameKey = nameKey;
			this.format = format;
			this.min = min;
			this.max = max;
			this.get = get;
			this.set = set;
			updateMessage();
		}

		static double toSlider(double value, double min, double max) {
			return Math.min(1.0, Math.max(0.0, (value - min) / (max - min)));
		}

		@Override
		protected void updateMessage() {
			// May be called from the super-constructor before our fields are set.
			if (this.get == null || this.nameKey == null || this.format == null) {
				setMessage(Component.empty());
				return;
			}
			setMessage(Component.translatable(this.nameKey, format.apply(get.getAsDouble())));
		}

		@Override
		protected void applyValue() {
			set.accept(min + this.value * (max - min));
		}
	}
}
