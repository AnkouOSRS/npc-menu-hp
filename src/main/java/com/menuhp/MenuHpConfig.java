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
			keyName = "displayMode",
			name = "Display mode",
			description = "Which text to include in the HP bar",
			position = 3
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.LEVEL;
	}
}
