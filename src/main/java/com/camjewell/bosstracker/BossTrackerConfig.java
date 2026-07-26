package com.camjewell.bosstracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("boss-tracker")
public interface BossTrackerConfig extends Config
{
	String DISPLAY_SECTION = "Display Options";
	String GENERAL_SECTION = "General Settings";
	String GOALS_SECTION = "Boss Goals";
	String LOOT_SECTION = "Loot Display";

	@ConfigSection(
		name = "Display Options",
		description = "Overlay and infobox display options",
		position = 0,
		closedByDefault = true
	)
	String displaySection = DISPLAY_SECTION;

	@ConfigSection(
		name = "General Settings",
		description = "General plugin settings",
		position = 1,
		closedByDefault = false
	)
	String generalSection = GENERAL_SECTION;

	@ConfigSection(
		name = "Boss Goals",
		description = "Boss goal panel/overlay settings",
		position = 2,
		closedByDefault = true
	)
	String goalsSection = GOALS_SECTION;

	@ConfigSection(
		name = "Loot Display",
		description = "Loot tracking display settings",
		position = 3,
		closedByDefault = true
	)
	String lootSection = LOOT_SECTION;

	// ---- Display Options ----

	@ConfigItem(
		position = 0,
		keyName = "enableOverlay",
		name = "Enable Overlay",
		description = "Shows the session overlay above the chatbox",
		section = displaySection
	)
	default boolean enableOverlay()
	{
		return false;
	}

	@ConfigItem(
		position = 1,
		keyName = "renderInfobox",
		name = "Display Infobox",
		description = "Shows a session infobox",
		section = displaySection
	)
	default boolean renderInfobox()
	{
		return true;
	}

	enum InfoBoxContent
	{
		KPH,
		KILLS_THIS_SESSION,
		SESSION_TIME,
		AVG_KILL,
		FASTEST_KILL,
		IDLE_TIME
	}

	@ConfigItem(
		position = 2,
		keyName = "infoBoxContent",
		name = "Infobox Content",
		description = "What metric the infobox shows",
		section = displaySection
	)
	default InfoBoxContent infoBoxContent()
	{
		return InfoBoxContent.KPH;
	}

	@ConfigItem(
		position = 3,
		keyName = "showAverageKillTime",
		name = "Average Kill Time",
		description = "Display average kill time on the overlay",
		section = displaySection
	)
	default boolean showAverageKillTime()
	{
		return true;
	}

	@ConfigItem(
		position = 4,
		keyName = "showFastestKill",
		name = "Fastest Kill",
		description = "Display fastest kill on the overlay",
		section = displaySection
	)
	default boolean showFastestKill()
	{
		return true;
	}

	@ConfigItem(
		position = 5,
		keyName = "showKillsThisSession",
		name = "Kills This Session",
		description = "Display kills this session on the overlay",
		section = displaySection
	)
	default boolean showKillsThisSession()
	{
		return true;
	}

	@ConfigItem(
		position = 6,
		keyName = "showSessionTime",
		name = "Session Time",
		description = "Display a running count of the session time on the overlay",
		section = displaySection
	)
	default boolean showSessionTime()
	{
		return true;
	}

	@ConfigItem(
		position = 7,
		keyName = "showIdleTime",
		name = "Idle Time",
		description = "Display idle time on the overlay. Only meaningful in ACTUAL calc mode",
		section = displaySection
	)
	default boolean showIdleTime()
	{
		return false;
	}

	// ---- General Settings ----

	@ConfigItem(
		position = 0,
		keyName = "showSidePanel",
		name = "Side Panel",
		description = "Enables the side panel",
		section = generalSection
	)
	default boolean showSidePanel()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "sidePanelPosition",
		name = "Side Panel Position",
		description = "Panel icon position; lower number = higher position",
		section = generalSection
	)
	default int sidePanelPosition()
	{
		return 6;
	}

	@ConfigItem(
		position = 2,
		keyName = "displayKillTimes",
		name = "Kill Duration",
		description = "Adds a chat message with the kill's duration after each kill",
		section = generalSection
	)
	default boolean displayKillTimes()
	{
		return true;
	}

	@ConfigItem(
		position = 3,
		keyName = "outputOnChange",
		name = "Output Info",
		description = "Outputs session info in chat when the session ends or switches bosses",
		section = generalSection
	)
	default boolean outputOnChange()
	{
		return false;
	}

	@ConfigItem(
		position = 4,
		keyName = "printKphInChat",
		name = "KPH In Chat Box",
		description = "Outputs current KPH to the chatbox after every kill",
		section = generalSection
	)
	default boolean printKphInChat()
	{
		return false;
	}

	@ConfigItem(
		position = 5,
		keyName = "sessionTimeoutMinutes",
		name = "Session Timeout",
		description = "Minutes of inactivity before the session automatically ends (0 = never)",
		section = generalSection
	)
	default int sessionTimeoutMinutes()
	{
		return 0;
	}

	enum KphMethod
	{
		PRECISE,
		ROUNDED,
		ROUND_UP,
		TRADITIONAL
	}

	@ConfigItem(
		position = 6,
		keyName = "kphMethod",
		name = "KPH Calc",
		description = "The method used to calculate displayed kills-per-hour",
		section = generalSection
	)
	default KphMethod kphMethod()
	{
		return KphMethod.PRECISE;
	}

	enum DksSelector
	{
		REX,
		PRIME,
		SUPREME,
		KINGS
	}

	@ConfigItem(
		position = 7,
		keyName = "dksSelector",
		name = "Dagannoth Selector",
		description = "Which Dagannoth King (or all three combined) the plugin tracks",
		section = generalSection
	)
	default DksSelector dksSelector()
	{
		return DksSelector.KINGS;
	}

	// ---- Boss Goals ----

	@ConfigItem(
		position = 0,
		keyName = "displayBossGoalsPanel",
		name = "Goals Panel",
		description = "Shows the boss goal section in the side panel",
		section = goalsSection
	)
	default boolean displayBossGoalsPanel()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "displayBossGoalsOverlay",
		name = "Goals Overlay",
		description = "Shows a boss goal progress overlay",
		section = goalsSection
	)
	default boolean displayBossGoalsOverlay()
	{
		return true;
	}

	@ConfigItem(
		position = 2,
		keyName = "displayRelativeKills",
		name = "Relative Kills",
		description = "Displays goal progress relative to the start KC (0 to kills needed) instead of the raw KC range",
		section = goalsSection
	)
	default boolean displayRelativeKills()
	{
		return true;
	}

	@ConfigItem(
		position = 3,
		keyName = "notifyOnGoalComplete",
		name = "Notify On Completion",
		description = "Sends a chat message when a boss goal's kill count is reached",
		section = goalsSection
	)
	default boolean notifyOnGoalComplete()
	{
		return true;
	}

	enum GoalOverlayRow
	{
		KILLS_DONE,
		KILLS_LEFT,
		KPH,
		TTG
	}

	@ConfigItem(
		position = 4,
		keyName = "topGoalOverlay",
		name = "Overlay Top Row",
		description = "What the top row of the goal overlay shows",
		section = goalsSection
	)
	default GoalOverlayRow topGoalOverlay()
	{
		return GoalOverlayRow.KPH;
	}

	@ConfigItem(
		position = 5,
		keyName = "bottomGoalOverlay",
		name = "Overlay Bottom Row",
		description = "What the bottom row of the goal overlay shows",
		section = goalsSection
	)
	default GoalOverlayRow bottomGoalOverlay()
	{
		return GoalOverlayRow.TTG;
	}

	// ---- Loot Display ----

	enum LootDisplayMode
	{
		SESSION,
		ALL_TIME
	}

	@ConfigItem(
		position = 0,
		keyName = "lootDisplayMode",
		name = "Loot Display",
		description = "Whether the loot grid shows this session's loot or all-time loot for the tracked boss",
		section = lootSection
	)
	default LootDisplayMode lootDisplayMode()
	{
		return LootDisplayMode.SESSION;
	}
}
