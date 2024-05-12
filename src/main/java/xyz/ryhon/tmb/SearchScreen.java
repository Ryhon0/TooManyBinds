package xyz.ryhon.tmb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

public class SearchScreen extends Screen {
	TextFieldWidget searchBox;
	List<String> matchedKeys = new ArrayList<>();
	int selectedIndex = 0;

	public SearchScreen() {
		super(Text.translatable("searchScreen.title"));
	}

	@Override
	protected void init() {
		searchBox = new TextFieldWidget(client.textRenderer, width / 2, 32,
				Text.translatable("searchScreen.placeholder"));
		searchBox.setChangedListener(this::onQueryChanged);

		searchBox.setPosition(width / 2 - (searchBox.getWidth() / 2), height / 2 - (searchBox.getHeight() / 2));

		addDrawable(searchBox);
		addSelectableChild(searchBox);
		setInitialFocus(searchBox);

		onQueryChanged("");
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawTextWithShadow(client.textRenderer, (selectedIndex + 1) + "/" + matchedKeys.size(),
				searchBox.getX() + searchBox.getWidth() + 8, searchBox.getY() + (searchBox.getHeight() / 2) - 4,
				0xffffff);

		int i = 0;
		int rowSize = 8;

		int halfRows = (height - searchBox.getY() - searchBox.getHeight()) / (rowSize * 2) / 2;
		int offset = selectedIndex - halfRows;
		if (selectedIndex < halfRows)
			offset = 0;

		for (String k : matchedKeys) {
			Boolean selected = selectedIndex == i;
			int nameColor = selected ? 0x00ff00 : 0xffffff;
			int keyColor = selected ? 0x006600 : 0x666666;

			context.drawCenteredTextWithShadow(client.textRenderer, Language.getInstance().get(k, k),
					searchBox.getX() + (searchBox.getWidth() / 2),
					searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2),
					nameColor);
			context.drawCenteredTextWithShadow(client.textRenderer, k,
					searchBox.getX() + (searchBox.getWidth() / 2),
					searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2) + rowSize,
					keyColor);

			i++;
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			onAccept();
			client.setScreen(null);
			return true;
		}

		else if (keyCode == GLFW.GLFW_KEY_UP) {
			selectedIndex--;
		} else if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_DOWN) {
			selectedIndex++;
		}

		if (selectedIndex == -1)
			selectedIndex = matchedKeys.size() - 1;
		if (selectedIndex == matchedKeys.size())
			selectedIndex = 0;

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	void onQueryChanged(String query) {
		String oldKey = null;
		if (matchedKeys.size() != 0)
			oldKey = matchedKeys.get(selectedIndex);

		matchedKeys = match(query);

		if (oldKey != null) {
			int newIdx = matchedKeys.indexOf(oldKey);
			if (newIdx != -1) {
				selectedIndex = newIdx;
				return;
			}
		}

		selectedIndex = 0;
	}

	KeyBinding getSelectedBind() {
		if (matchedKeys.size() == 0)
			return null;
		return KeyBinding.KEYS_BY_ID.get(matchedKeys.get(selectedIndex));
	}

	void onAccept() {
		KeyBinding bind = getSelectedBind();
		if (bind != null)
			TMB.queuePress(bind);
	}

	List<String> match(String query) {
		ArrayList<String> list = new ArrayList<>();
		ArrayList<String> keyList = new ArrayList<>();
		keyList.addAll(KeyBinding.KEYS_BY_ID.keySet());
		Collections.sort(keyList);

		if (query.length() == 0)
			return keyList;

		for (String key : keyList) {
			if (matches(key, query) || matches(Language.getInstance().get(key, null), query))
				list.add(key);
		}

		return list;
	}

	boolean matches(String key, String query) {
		if (key == null)
			return false;

		String[] queries = query.split("\\ ");
		for (String q : queries) {
			if (!key.toLowerCase().contains(q.toLowerCase())) {
				return false;
			}
		}

		return true;
	}
}
