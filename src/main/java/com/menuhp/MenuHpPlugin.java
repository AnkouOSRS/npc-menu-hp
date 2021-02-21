package com.menuhp;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.awt.*;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.api.MenuAction.MENU_ACTION_DEPRIORITIZE_OFFSET;

@Slf4j
@PluginDescriptor(
	name = "Monster Menu HP",
	description = "Show a monster's HP in its menu entry",
	tags = {"monsters", "npcs", "hitpoints", "hp", "menu"}
)
public class MenuHpPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private MenuHpConfig config;

	private static final Set<MenuAction> NPC_MENU_ACTIONS = ImmutableSet.of(MenuAction.NPC_FIRST_OPTION, MenuAction.NPC_SECOND_OPTION,
			MenuAction.NPC_THIRD_OPTION, MenuAction.NPC_FOURTH_OPTION, MenuAction.NPC_FIFTH_OPTION, MenuAction.SPELL_CAST_ON_NPC,
			MenuAction.ITEM_USE_ON_NPC);

	private static final Pattern COLOR_TAG_PATTERN = Pattern.compile("<col=([a-zA-Z0-9]+)>");

	@Provides
	private MenuHpConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MenuHpConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{

	}

	@Override
	protected void shutDown() throws Exception
	{

	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		int type = event.getType();

		if (type >= MENU_ACTION_DEPRIORITIZE_OFFSET)
		{
			type -= MENU_ACTION_DEPRIORITIZE_OFFSET;
		}

		final MenuAction menuAction = MenuAction.of(type);

		if (NPC_MENU_ACTIONS.contains(menuAction))
		{
			NPC npc = client.getCachedNPCs()[event.getIdentifier()];
			String finalText = null;

			if (!npc.isDead() && npc.getHealthRatio() > 0)
			{
				String target = event.getTarget();

				String cleanTarget = Text.removeTags(event.getTarget());
				int levelStartIndex = cleanTarget.lastIndexOf('(');
				String levelText = levelStartIndex != -1 ? cleanTarget.substring(levelStartIndex) : "";

				String baseText;
				DisplayMode displayMode = config.displayMode();
				switch (displayMode) {
					case LEVEL:
						baseText = levelText;
						break;
					case NAME:
						int endIndex = cleanTarget.lastIndexOf('(');
						baseText = endIndex != -1 ? cleanTarget.substring(0, endIndex) : cleanTarget;
						break;
					default:
						baseText = cleanTarget;
						break;
				}

				double ratio = ((double)npc.getHealthRatio() / (double)npc.getHealthScale());
				int splitIndex = (int)Math.round(baseText.length() * ratio);

				if (splitIndex < baseText.length())
				{
					Color[] tagColors = getColorsFromTags(target);
					int monsterEndIndex = cleanTarget.lastIndexOf('(');
					String monsterText = monsterEndIndex != -1 ? cleanTarget.substring(0, monsterEndIndex) : cleanTarget;
					String monsterTextTagged = ColorUtil.wrapWithColorTag(monsterText, tagColors[0]);
					String levelTextTagged = ColorUtil.wrapWithColorTag(levelText, tagColors[tagColors.length - 1]);
					String hpText = ColorUtil.wrapWithColorTag(baseText.substring(0, splitIndex + 1), config.hpColor());
					String bgText = ColorUtil.wrapWithColorTag(baseText.substring(splitIndex + 1), config.bgColor());
					finalText = (baseText.contains(monsterText) ? "" : monsterTextTagged) + hpText + bgText
							+ (baseText.contains(levelText) ? "" : levelTextTagged);
				}
			}

			if (finalText != null)
			{
				MenuEntry[] menuEntries = client.getMenuEntries();
				final MenuEntry menuEntry = menuEntries[menuEntries.length - 1];
				menuEntry.setTarget(finalText);
				client.setMenuEntries(menuEntries);
			}
		}
	}

	private Color[] getColorsFromTags(String text)
	{
		Color[] result = new Color[]{};
		Matcher matcher = COLOR_TAG_PATTERN.matcher(text);
		while (matcher.find())
		{
			result = ArrayUtils.add(result, Color.decode('#' + matcher.group(1)));
		}
		return result;
	}
}
