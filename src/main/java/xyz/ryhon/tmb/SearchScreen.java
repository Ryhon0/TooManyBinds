package xyz.ryhon.tmb;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

public class SearchScreen extends Screen {
	TextFieldWidget searchBox;
	ButtonWidget idSettingButton;
	ButtonWidget underflowSettingButton;

	List<BindingEntry> binds = new ArrayList<>();
	List<BindingEntry> matched = new ArrayList<>();
	int selectedIndex = 0;

	public SearchScreen() {
		super(Text.translatable("searchScreen.title"));
		binds = getEntries();
	}

	@Override
	protected void init() {
		{
			int buttonSize = 16;
			int y = 0;

			idSettingButton = ButtonWidget.builder(Text.literal("ID"), this::onIdSetting)
					.dimensions(0, y, buttonSize, buttonSize).build();
			addDrawableChild(idSettingButton);
			y += buttonSize;

			underflowSettingButton = ButtonWidget.builder(Text.literal("^"), this::onUnderflowSetting)
					.dimensions(0, y, buttonSize, buttonSize).build();
			addDrawableChild(underflowSettingButton);
			y += buttonSize;
		}

		searchBox = new TextFieldWidget(client.textRenderer, width / 2, 24,
				Text.empty());
		searchBox.setChangedListener(this::onQueryChanged);

		searchBox.setPosition(width / 2 - (searchBox.getWidth() / 2), height / 2 - (searchBox.getHeight() / 2));

		addDrawable(searchBox);
		addSelectableChild(searchBox);
		setInitialFocus(searchBox);

		onQueryChanged("");
	}

	int getEntryHeight() {
		return TMB.Config.showBindIDs ? 9 : 5;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawTextWithShadow(client.textRenderer, (selectedIndex + 1) + "/" + matched.size(),
				searchBox.getX() + searchBox.getWidth() + 8, searchBox.getY() + (searchBox.getHeight() / 2) - 4,
				0xffffff);

		int i = 0;
		int rowSize = getEntryHeight();

		int halfRows = (height - searchBox.getY() - searchBox.getHeight()) / (rowSize * 2) / 2;
		int offset = selectedIndex - halfRows;
		if (selectedIndex < halfRows)
			offset = 0;

		for (BindingEntry be : matched) {
			if (!TMB.Config.drawUndeflowSuggestions && i - offset < 0) {
				i++;
				continue;
			}

			Boolean selected = selectedIndex == i;
			int nameColor = selected ? 0xffff00 : 0xdddddd;
			int categoryColor = selected ? 0xdddd00 : 0xaaaaaa;
			int keyColor = selected ? 0x666600 : 0x666666;

			context.drawTextWithShadow(client.textRenderer, be.name,
					searchBox.getX(),
					searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2),
					nameColor);

			int catWidth = client.textRenderer.getWidth(be.categoryName);
			context.drawTextWithShadow(client.textRenderer, be.categoryName,
					searchBox.getX() + searchBox.getWidth() - catWidth,
					searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2),
					categoryColor);

			if (TMB.Config.showBindIDs) {
				context.drawTextWithShadow(client.textRenderer, be.id,
						searchBox.getX(),
						searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2) + rowSize,
						keyColor);

				catWidth = client.textRenderer.getWidth(be.categoryId);
				context.drawTextWithShadow(client.textRenderer, be.categoryId,
						searchBox.getX() + searchBox.getWidth() - catWidth,
						searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2) + rowSize,
						keyColor);
			}

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
		BindingEntry oldSelected = null;
		if (matched.size() != 0)
			oldSelected = matched.get(selectedIndex);

		matched = match(query);

		if (oldSelected != null) {
			int newIdx = matched.indexOf(oldSelected);
			if (newIdx != -1) {
				selectedIndex = newIdx;
				return;
			}
		}

		selectedIndex = 0;
	}

	KeyBinding getSelectedBind() {
		if (matched.size() == 0)
			return null;
		return matched.get(selectedIndex).bind;
	}

	void onAccept() {
		KeyBinding bind = getSelectedBind();
		if (bind != null)
			TMB.queuePress(bind);
	}

	void onIdSetting(ButtonWidget b) {
		TMB.Config.showBindIDs = !TMB.Config.showBindIDs;
		TMB.Config.saveConfig();
	}

	void onUnderflowSetting(ButtonWidget b) {
		TMB.Config.drawUndeflowSuggestions = !TMB.Config.drawUndeflowSuggestions;
		TMB.Config.saveConfig();
	}

	List<BindingEntry> match(String query) {
		ArrayList<BindingEntry> list = new ArrayList<>();

		for (BindingEntry be : binds) {
			if (be.matches(query))
				list.add(be);
		}

		return list;
	}

	List<BindingEntry> getEntries() {
		ArrayList<BindingEntry> list = new ArrayList<>();

		for (KeyBinding e : KeyBinding.KEYS_BY_ID.values()) {
			String name = Language.getInstance().get(e.getTranslationKey(), e.getTranslationKey());
			String categoryName = Language.getInstance().get(e.getCategory(), e.getCategory());

			BindingEntry be = new BindingEntry(e,
					e.getTranslationKey(), name,
					e.getCategory(), categoryName);

			list.add(be);
		}

		return list;
	}

	class BindingEntry {
		public BindingEntry(KeyBinding bind, String id, String name, String categoryId, String categoryName) {
			this.bind = bind;

			this.id = id;
			this.name = name;

			this.categoryId = categoryId;
			this.categoryName = categoryName;
		}

		public KeyBinding bind;

		public String id;
		public String name;

		public String categoryId;
		public String categoryName;

		public boolean matches(String query) {
			String[] split = query.toLowerCase().split("\\ ");

			return stringMatches(name, split) ||
					stringMatches(id, split) ||
					stringMatches(categoryName, split) ||
					stringMatches(categoryId, split);
		}

		boolean stringMatches(String text, String[] split) {
			for (String q : split) {
				if (!text.toLowerCase().contains(q)) {
					return false;
				}
			}
			return true;
		}
	}
}
