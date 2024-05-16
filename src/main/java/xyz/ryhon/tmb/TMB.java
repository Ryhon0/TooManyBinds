package xyz.ryhon.tmb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TMB implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("tmb");
	static ArrayList<KeyBinding> toPress = new ArrayList<>();
	static ArrayList<KeyBinding> toRelease = new ArrayList<>();

	@Override
	public void onInitialize() {
		Config.loadConfig();

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
		
		KeyBinding reloadConfigBind;
		reloadConfigBind = new KeyBinding(
				"key.tmb.reloadConfig",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				"category.tmb");
		KeyBindingHelper.registerKeyBinding(reloadConfigBind);
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (reloadConfigBind.wasPressed()) {
				Config.loadConfig();
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			for (KeyBinding bind : toRelease) {
				bind.pressed = false;
			}
			toRelease.clear();

			for (KeyBinding bind : toPress) {
				bind.timesPressed++;
				bind.pressed = true;
				toRelease.add(bind);
			}
			toPress.clear();
		});
	}

	public static void queuePress(KeyBinding bind)
	{
		toPress.add(bind);
	}

	public static class Config
	{
		public static boolean showBindIDs = false;
		public static boolean drawUndeflowSuggestions = false;
	
		public static final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("toomanybinds");
		public static final Path configFile = configDir.resolve("config.json");

		public static void loadConfig()
		{
			try
			{
				if(!Files.exists(configFile)) return;

				JsonObject jo = (JsonObject)JsonParser.parseString(Files.readString(configFile));

				if(jo.has("showBindIDs"))
					showBindIDs = jo.get("showBindIDs").getAsBoolean();

				if(jo.has("drawUndeflowSuggestions"))
					drawUndeflowSuggestions = jo.get("drawUndeflowSuggestions").getAsBoolean();
			}catch(Exception e){
				LOGGER.error("Failed to load config", e);
			}
		}

		public static void saveConfig()
		{
			JsonObject jo = new JsonObject();
			jo.addProperty("showBindIDs", showBindIDs);
			jo.addProperty("drawUndeflowSuggestions", drawUndeflowSuggestions);

			try
			{
				Files.createDirectories(configDir);
				Files.writeString(configFile, new Gson().toJson(jo));
			}catch(Exception e)
			{
				LOGGER.error("Failed to save config", e);
			}
		}
	}
}