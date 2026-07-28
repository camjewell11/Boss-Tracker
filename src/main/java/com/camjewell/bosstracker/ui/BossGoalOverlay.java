package com.camjewell.bosstracker.ui;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.BossTrackerConfig.GoalOverlayRow;
import com.camjewell.bosstracker.loot.LootTracker;
import com.camjewell.bosstracker.session.BossGoal;
import com.camjewell.bosstracker.session.BossSession;
import com.camjewell.bosstracker.session.GoalManager;
import com.camjewell.bosstracker.session.SessionManager;
import com.camjewell.bosstracker.util.ItemPriceCache;
import com.camjewell.bosstracker.util.TimeFormat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.SplitComponent;

/**
 * Shows kills-done/kills-left/KPH/TTG progress toward the active {@link BossGoal} for the
 * currently tracked boss. Mirrors the original plugin's goal overlay, driven by
 * {@link GoalManager} instead of synchronous per-frame file reads.
 */
public class BossGoalOverlay extends Overlay
{
	private static final Color PAUSED_COLOR = new Color(173, 128, 29);
	private static final Color PROGRESS_COLOR = new Color(91, 154, 47);

	private final BossTrackerConfig config;
	private final SessionManager sessionManager;
	private final GoalManager goalManager;
	private final ItemManager itemManager;
	private final LootTracker lootTracker;
	private final ItemPriceCache priceCache;
	private final PanelComponent panelComponent = new PanelComponent();
	private final ProgressBarComponent progressBarComponent = new ProgressBarComponent();

	@Inject
	private BossGoalOverlay(BossTrackerConfig config, SessionManager sessionManager, GoalManager goalManager,
		ItemManager itemManager, LootTracker lootTracker, ItemPriceCache priceCache)
	{
		setPosition(OverlayPosition.TOP_LEFT);
		this.config = config;
		this.sessionManager = sessionManager;
		this.goalManager = goalManager;
		this.itemManager = itemManager;
		this.lootTracker = lootTracker;
		this.priceCache = priceCache;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		BossSession session = sessionManager.getSession();
		BossGoal goal = goalManager.getGoal();

		boolean kcGoalSet = goal != null && goal.isSet();
		boolean lootGoalSet = goal != null && goal.isLootGoalSet();

		if (!config.displayBossGoalsOverlay() || session == null || goal == null
			|| goal.getBoss() != session.getBoss() || (!kcGoalSet && !lootGoalSet))
		{
			return null;
		}

		int currentKc = session.getKillCount();
		int killsDone = goal.killsDone(currentKc);
		int totalToGet = goal.totalKillsToGet();
		int killsLeft = Math.max(0, totalToGet - killsDone);
		long currentGp = computeCurrentLifetimeGp();
		long lootGoalGp = goal.getLootGoalGp();

		boolean complete = kcGoalSet ? goal.isComplete(currentKc) : goal.isLootGoalComplete(currentGp);
		double percentDone = kcGoalSet
			? (totalToGet > 0 ? 100.0 * killsDone / totalToGet : 0)
			: (lootGoalGp > 0 ? 100.0 * currentGp / lootGoalGp : 0);
		double ttgHours = (kcGoalSet && session.getKillsPerHour() > 0) ? killsLeft / session.getKillsPerHour() : 0;

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(150, 0));

		LineComponent topLine = rowFor(config.topGoalOverlay(), killsDone, killsLeft, kcGoalSet, ttgHours, session, complete);
		LineComponent bottomLine = rowFor(config.bottomGoalOverlay(), killsDone, killsLeft, kcGoalSet, ttgHours, session, complete);
		SplitComponent linesSplit = SplitComponent.builder()
			.first(topLine)
			.second(bottomLine)
			.orientation(ComponentOrientation.VERTICAL)
			.build();

		ImageComponent imageComponent = new ImageComponent(itemManager.getImage(session.getBoss().getIconItemId()));
		SplitComponent iconAndLines = SplitComponent.builder()
			.first(imageComponent)
			.second(linesSplit)
			.orientation(ComponentOrientation.HORIZONTAL)
			.gap(new Point(4, 0))
			.build();

		progressBarComponent.setBackgroundColor(new Color(61, 56, 49));
		if (session.isPaused())
		{
			progressBarComponent.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.TEXT_ONLY);
			progressBarComponent.setCenterLabel("Paused");
			progressBarComponent.setForegroundColor(PAUSED_COLOR);
			progressBarComponent.setValue(percentDone);
		}
		else if (complete)
		{
			progressBarComponent.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.TEXT_ONLY);
			progressBarComponent.setCenterLabel("Completed");
			progressBarComponent.setForegroundColor(PROGRESS_COLOR);
			progressBarComponent.setValue(100);
		}
		else
		{
			progressBarComponent.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.PERCENTAGE);
			progressBarComponent.setForegroundColor(PROGRESS_COLOR);
			progressBarComponent.setValue(percentDone);
		}

		if (kcGoalSet)
		{
			if (config.displayRelativeKills())
			{
				progressBarComponent.setLeftLabel("0");
				progressBarComponent.setRightLabel(String.valueOf(totalToGet));
			}
			else
			{
				progressBarComponent.setLeftLabel(String.valueOf(goal.getStartKc()));
				progressBarComponent.setRightLabel(String.valueOf(goal.getEndKc()));
			}
		}
		else
		{
			progressBarComponent.setLeftLabel(formatGp(currentGp));
			progressBarComponent.setRightLabel(formatGp(lootGoalGp));
		}

		panelComponent.getChildren().add(iconAndLines);
		panelComponent.getChildren().add(progressBarComponent);

		return panelComponent.render(graphics);
	}

	private long computeCurrentLifetimeGp()
	{
		long total = 0;
		for (Map.Entry<Integer, Integer> entry : lootTracker.getLifetimeLoot().entrySet())
		{
			total += priceCache.getPrice(entry.getKey()) * entry.getValue();
		}
		return total;
	}

	private static String formatGp(long value)
	{
		return String.format("%,d", value);
	}

	private LineComponent rowFor(GoalOverlayRow row, int killsDone, int killsLeft, boolean kcGoalSet, double ttgHours,
		BossSession session, boolean complete)
	{
		switch (row)
		{
			case KILLS_DONE:
				return LineComponent.builder()
					.left("Kills Done:")
					.right(String.valueOf(killsDone))
					.build();
			case KILLS_LEFT:
				return LineComponent.builder()
					.left("Kills Left:")
					.right(kcGoalSet ? String.valueOf(complete ? 0 : killsLeft) : "N/A")
					.build();
			case TTG:
				return LineComponent.builder()
					.left("TTG:")
					.right(!kcGoalSet ? "N/A" : complete ? "00:00:00" : TimeFormat.minutesSeconds((int) (ttgHours * 3600)))
					.build();
			case KPH:
			default:
				return LineComponent.builder()
					.left("KPH:")
					.right(TimeFormat.kph(session.getKillsPerHour(), config.kphMethod()))
					.build();
		}
	}
}
