package com.menuhp;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;
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

	private List<String> npcNames;
	private Map<NPC, Double> npcRatios;

	@Provides
	private MenuHpConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MenuHpConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		npcNames = Text.fromCSV(config.npcsToShow());
		npcRatios = new LinkedHashMap<>();
	}

	@Override
	protected void shutDown() throws Exception
	{
	    npcRatios = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("menuhp"))
		{
			npcNames = Text.fromCSV(config.npcsToShow());
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		npcRatios.remove(event.getNpc());
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

			if ((config.showOnAllNpcs() || shouldShowNpc(npc)))
			{
                String target = event.getTarget();
                String cleanTarget = Text.removeTags(event.getTarget());
                int levelStartIndex = cleanTarget.lastIndexOf('(');
                String levelText = levelStartIndex != -1 ? cleanTarget.substring(levelStartIndex) : "";

                String baseText;
                switch (config.displayMode()) {
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

                double ratio = -1;
                if (npc.getHealthRatio() == -1)
                {
                    if (npcRatios.containsKey(npc))
                    {
                        ratio = npcRatios.get(npc);
                    }
                }
                else if (npc.getHealthRatio() > 0) {
                    ratio = ((double) npc.getHealthRatio() / (double) npc.getHealthScale());
                }
                if (ratio != -1 || config.recolorWhenUnknown())
                {
                    int splitIndex = (int) Math.round(baseText.length() * Math.abs(ratio));
                    Color[] tagColors = getColorsFromTags(target);
					boolean isHealthUnknown = ratio < 0;
                    String finalText = buildFinalTargetText(cleanTarget, tagColors, splitIndex, baseText, levelText,
						isHealthUnknown);

                    MenuEntry[] menuEntries = client.getMenuEntries();
                    final MenuEntry menuEntry = menuEntries[menuEntries.length - 1];
                    menuEntry.setTarget(finalText);
                    client.setMenuEntries(menuEntries);
                    npcRatios.put(npc, ratio);
                }
			}
		}
	}

	private String buildFinalTargetText(String target, Color[] tagColors, int splitIndex, String baseText,
										String levelText, boolean isHealthUnknown)
	{
		int monsterEndIndex = target.lastIndexOf('(');
		String monsterText = monsterEndIndex != -1 ? target.substring(0, monsterEndIndex) : target;
		String monsterTextTagged = ColorUtil.wrapWithColorTag(monsterText, tagColors[0]);
		String levelTextTagged = ColorUtil.wrapWithColorTag(levelText, tagColors[tagColors.length - 1]);

		Color hpColor = isHealthUnknown ? config.unknownColor() : config.hpColor();
		String hpText = ColorUtil.wrapWithColorTag(baseText.substring(0, splitIndex), hpColor);
		String bgText = ColorUtil.wrapWithColorTag(baseText.substring(splitIndex), config.bgColor());

		return (baseText.contains(monsterText) ? "" : monsterTextTagged) + hpText + bgText
				+ (baseText.contains(levelText) ? "" : levelTextTagged);
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

	private boolean shouldShowNpc(NPC npc)
	{
		String npcName = npc.getName();
		if (npcName != null)
		{
			return npcNames.stream().anyMatch(name -> WildcardMatcher.matches(name, npcName));
		}
		return false;
	}
}
