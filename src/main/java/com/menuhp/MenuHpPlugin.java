package com.menuhp;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
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
			MenuAction.NPC_THIRD_OPTION, MenuAction.NPC_FOURTH_OPTION, MenuAction.NPC_FIFTH_OPTION, MenuAction.WIDGET_TARGET_ON_NPC,
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
	public void onMenuOpened(MenuOpened event)
	{
		if (!config.highlightHighestHpMonster()) return;

		final WorldView wv = client.getTopLevelWorldView();

		final Map<String, MaxHpCandidate> maxHpCandidateMap = new LinkedHashMap<>();
		for (MenuEntry entry : event.getMenuEntries())
		{
			if (!validType(entry.getType().getId()))
			{
				continue;
			}

			final NPC npc = wv.npcs().byIndex(entry.getIdentifier());
			double currentMonsterHpRatio = calculateNpcHealthRatio(npc);
			if (currentMonsterHpRatio == -1d && config.unknownHpCountsAsHighestHp())
			{
				currentMonsterHpRatio = 1d;
			}

			if (currentMonsterHpRatio < 0d || (!config.showOnAllNpcs() && !shouldShowNpc(npc)))
			{
				continue;
			}

			final String target = Text.removeTags(entry.getTarget());
			final String targetName = target.substring(0, target.lastIndexOf('('));

			final MaxHpCandidate maxHpCandidate =
					maxHpCandidateMap.computeIfAbsent(targetName, (unused) -> new MaxHpCandidate());
			if (npc.equals(maxHpCandidate.npc))
			{
				maxHpCandidate.menuEntries.add(entry);
			}
			else if (currentMonsterHpRatio >= maxHpCandidate.getHpRatio())
			{
				maxHpCandidate.setNpc(npc);
				maxHpCandidate.setHpRatio(currentMonsterHpRatio);
				maxHpCandidate.menuEntries.clear();
				maxHpCandidate.menuEntries.add(entry);
			}
		}

		for (MaxHpCandidate maxHpCandidate : maxHpCandidateMap.values())
		{
			for (MenuEntry menuEntry : maxHpCandidate.getMenuEntries())
			{
				final String target = menuEntry.getTarget();
				final double hpRatio = maxHpCandidate.getHpRatio();
				final boolean isUnknownHp = calculateNpcHealthRatio(maxHpCandidate.npc) < 0d;
				final Color maxHpColor = config.highestHpMonsterColor();
				colorMenuEntry(menuEntry, target, maxHpColor, hpRatio, isUnknownHp);
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
	if (validType(event.getType()))
		{
			final WorldView wv = client.getTopLevelWorldView();
			final NPC npc = wv.npcs().byIndex(event.getIdentifier());
			if ((config.showOnAllNpcs() || shouldShowNpc(npc)))
			{
				final double hpRatio = calculateNpcHealthRatio(npc);
				final boolean isUnknownHp = hpRatio < 0;
				final Color hpColor = isUnknownHp ? config.unknownColor() : config.hpColor();
				colorMenuEntry(event.getMenuEntry(), event.getTarget(), hpColor, hpRatio, isUnknownHp);
			}
		}
	}

	private double calculateNpcHealthRatio(NPC npc) {
		double ratio = -1d;

		if (npc.getHealthRatio() == -1)
		{
			ratio = npcRatios.getOrDefault(npc, -1d);
		}
		else if (npc.getHealthRatio() >= 0)
		{
			ratio = ((double) npc.getHealthRatio() / (double) npc.getHealthScale());
		}
		else if (npc.isDead())
		{
			ratio = 0;
		}

		npcRatios.put(npc, ratio);
		return ratio;
	}

	private void colorMenuEntry(MenuEntry menuEntry, String target, Color hpColor, double hpRatio, boolean isUnknownHp)
	{
		String cleanTarget = Text.removeTags(target);
		int levelStartIndex = cleanTarget.lastIndexOf('(');
		int levelEndIndex = cleanTarget.lastIndexOf(')');
		String levelText = levelStartIndex != -1 && levelEndIndex != -1
				? cleanTarget.substring(levelStartIndex, levelEndIndex + 1) : "";

		String baseText;
		switch (config.displayMode()) {
			case LEVEL:
				baseText = levelText;
				break;
			case NAME:
				baseText = levelStartIndex != -1 ? cleanTarget.substring(0, levelStartIndex) : cleanTarget;
				break;
			default:
				baseText = levelEndIndex != -1 ? cleanTarget.substring(0, levelEndIndex + 1) : cleanTarget;
				break;
		}

		if (hpRatio != -1d || config.recolorWhenUnknown())
		{
			int splitIndex = (int) Math.round(baseText.length() * Math.abs(hpRatio));
			Color[] tagColors = getColorsFromTags(target);
			String finalText = buildFinalTargetText(cleanTarget, tagColors, hpColor, splitIndex, baseText, levelText,
					hpRatio, isUnknownHp);

			menuEntry.setTarget(finalText);
		}
	}

    private boolean validType(int typeId) {
        if (typeId >= MENU_ACTION_DEPRIORITIZE_OFFSET) typeId -= MENU_ACTION_DEPRIORITIZE_OFFSET;
        final MenuAction menuAction = MenuAction.of(typeId);
        return NPC_MENU_ACTIONS.contains(menuAction);
    }

	// `isUnknownHp` is not derived from `ratio` because the "treat unknown hp as max hp" logic
	// will force the ratio to 1.0 when it is truly unknown for the purpose of coloring the text.
	private String buildFinalTargetText(String target, Color[] tagColors, Color hpColor, int splitIndex,
										String baseText, String levelText, double ratio, boolean isUnknownHp)
	{
		int monsterEndIndex = target.lastIndexOf('(');
		String monsterText = monsterEndIndex != -1 ? target.substring(0, monsterEndIndex) : target;
		String monsterTextTagged = ColorUtil.wrapWithColorTag(monsterText, tagColors[0]);
		String levelTextTagged = ColorUtil.wrapWithColorTag(levelText, tagColors[tagColors.length - 1]);

		String hpText = ColorUtil.wrapWithColorTag(baseText.substring(0, splitIndex), hpColor);
		String bgText = ColorUtil.wrapWithColorTag(baseText.substring(splitIndex), config.bgColor());

		String hpPercentage = isUnknownHp ? "" : String.format(" - %.1f%%", ratio * 100);

		return (baseText.contains(monsterText) ? "" : monsterTextTagged) + hpText + bgText
				+ (baseText.contains(levelText) ? "" : levelTextTagged) + (config.showMonsterHpPercentage() ? hpPercentage : "");
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

	@Getter
	@Setter
	@NoArgsConstructor
	@ToString
	private static class MaxHpCandidate {
		private double hpRatio = -1d;
		private NPC npc = null;
		private final List<MenuEntry> menuEntries = new ArrayList<>();
	}
}
