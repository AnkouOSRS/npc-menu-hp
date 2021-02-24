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

    @ConfigItem(
            keyName = "showOnAllNpcs",
            name = "Show on all NPCs",
            description = "Include an HP bar in all NPCs menu entry",
            position = 4
    )
    default boolean showOnAllNpcs()
    {
        return true;
    }

    @ConfigItem(
            keyName = "npcsToShow",
            name = "NPCs to show HP",
            description = "Which NPCs will include an HP bar in their menu entry, if 'Show on all NPCs' is not selected",
            position = 5
    )
    default String npcsToShow()
    {
        return "";
    }
}
