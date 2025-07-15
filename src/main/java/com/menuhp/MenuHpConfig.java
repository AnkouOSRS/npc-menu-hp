package com.menuhp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("menuhp")
public interface MenuHpConfig extends Config
{
	@ConfigItem(
		keyName = "hpColor",
		name = "HP color",
		description = "The HP color for the monster's menu HP bar",
		position = 1
	)
	default Color hpColor()
	{
		return Color.RED;
	}

	@ConfigItem(
			keyName = "bgColor",
			name = "BG color",
			description = "The background color for the monster's menu HP bar",
			position = 2
	)
	default Color bgColor()
	{
		return Color.GRAY;
	}

	@ConfigItem(
		keyName = "recolorWhenUnknown",
		name = "Recolor when unknown",
		description = "Recolors menu entries for monsters with unknown HP",
		position = 3
	)
	default boolean recolorWhenUnknown() {
		return false;
	}

	@ConfigItem(
		keyName = "unknownColor",
		name = "Unknown color",
		description = "Color for monster menu when HP is unknown",
		position = 4
	)
	default Color unknownColor() {
		return Color.CYAN;
	}

	@ConfigItem(
			keyName = "displayMode",
			name = "Display mode",
			description = "Which text to include in the HP bar",
			position = 5
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.LEVEL;
	}

    @ConfigItem(
            keyName = "showOnAllNpcs",
            name = "Show on all NPCs",
            description = "Include an HP bar in all NPCs menu entry",
            position = 6
    )
    default boolean showOnAllNpcs()
    {
        return true;
    }

	@ConfigItem(
		keyName = "showMonsterHpPercentage",
		name = "Show monster HP percentage",
		description = "Include the monster's HP percentage in its menu entry",
		position = 7
	)
	default boolean showMonsterHpPercentage()
	{
		return false;
	}

    @ConfigItem(
            keyName = "npcsToShow",
            name = "NPCs to show HP",
            description = "Which NPCs will include an HP bar in their menu entry, if 'Show on all NPCs' is not selected",
            position = 8
    )
    default String npcsToShow()
    {
        return "";
    }

	@ConfigItem(
			keyName = "highlightHighestHpMonster",
			name = "Highlight highest HP monster",
			description = "Highlights the monster in the menu with the highest hp and same name with a color " +
					"determined by \"Highest HP monster color\".",
			position = 9
	)
	default boolean highlightHighestHpMonster()
	{
		return false;
	}

	@ConfigItem(
			keyName = "unknownHpCountsAsHighestHp",
			name = "Unknown HP counts as highest HP",
			description = "If a monster has unknown HP, it <i>usually</i> is because it has full health and has yet " +
					"to be attacked. This option will include monsters with unknown hp as if they had max health for " +
					"the purposes of \"Highlight highest HP monster\".",
			position = 10
	)
	default boolean unknownHpCountsAsHighestHp()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highestHpMonsterColor",
			name = "Highest HP monster color",
			description = "The HP color for the menu HP bar of the monster with the highest health and the same " +
					"name. Only used if \"Highlight highest HP monster\" is enabled.",
			position = 11
	)
	default Color highestHpMonsterColor()
	{
		return Color.MAGENTA;
	}
}
