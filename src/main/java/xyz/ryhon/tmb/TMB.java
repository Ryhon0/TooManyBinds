package xyz.ryhon.tmb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMB implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("tmb");
	static ArrayList<KeyBinding> toPress = new ArrayList<>();
	static ArrayList<KeyBinding> toRelease = new ArrayList<>();

	@Override
	public void onInitialize() {
		KeyBinding searchScreenBind;
		searchScreenBind = new KeyBinding(
				"key.tmb.search",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_ENTER,
				"category.tmb");
		KeyBindingHelper.registerKeyBinding(searchScreenBind);
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (searchScreenBind.wasPressed()) {
				SearchScreen s = new SearchScreen();
				client.setScreen(s);
			}
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			for (KeyBinding bind : toRelease) {
				bind.pressed = false;
			}
			toRelease.clear();

			for (KeyBinding bind : toPress) {
				bind.timesPressed++;
				bind.pressed = true;
			}
			toPress.clear();
		});
	}

	public static void queuePress(KeyBinding bind)
	{
		toPress.add(bind);
	}
}