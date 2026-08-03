package com.camjewell.bosstracker.ui;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.history.SessionHistoryManager;
import com.camjewell.bosstracker.lookup.BossLookupManager;
import com.camjewell.bosstracker.loot.LootTracker;
import com.camjewell.bosstracker.persistence.BossStats;
import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import com.camjewell.bosstracker.session.BossGoal;
import com.camjewell.bosstracker.session.BossSession;
import com.camjewell.bosstracker.session.CalcMode;
import com.camjewell.bosstracker.session.GoalManager;
import com.camjewell.bosstracker.util.ItemPriceCache;
import com.camjewell.bosstracker.session.SessionManager;
import com.camjewell.bosstracker.util.TimeFormat;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

/**
 * The side panel: a Session tab (title header, live session stats, pause/resume, the
 * ACTUAL/VIRTUAL calc-mode toggle, boss goals, loot grid, end-session) and a History tab
 * (collapsible, deletable log of past sessions). Boss-name lookup for bosses outside the active
 * session is added in a later phase.
 */
public class BossTrackerPanel extends PluginPanel
{
	private static final String HTML_LABEL_TEMPLATE = "<html><body style='color:%s'>%s<span style='color:white'>%s</span></body></html>";
	private static final String HTML_LABEL_STACKED_TEMPLATE =
		"<html><body style='color:%s; text-align:%s'>%s<br><span style='color:white'>%s</span></body></html>";
	private static final Color ACTIVE_COLOR = new Color(71, 226, 12);
	private static final Color PAUSED_COLOR = new Color(227, 160, 27);
	private static final Color ENDED_COLOR = new Color(187, 187, 187);

	private static final int LOOT_GRID_COLUMNS = 5;
	private static final DateTimeFormatter HISTORY_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, HH:mm")
		.withZone(ZoneId.systemDefault());
	private static final ImageIcon SEARCH_ICON = new ImageIcon(ImageUtil.loadImageResource(IconTextField.class, "search.png"));

	private final SessionManager sessionManager;
	private final GoalManager goalManager;
	private final LootTracker lootTracker;
	private final SessionHistoryManager historyManager;
	private final BossLookupManager lookupManager;
	private final BossTrackerConfig config;
	private final ItemManager itemManager;
	private final ItemPriceCache priceCache;
	private final BufferedImage slayerIcon;

	private final JLabel bossIconLabel = new JLabel();
	private final JLabel bossNameLabel = new JLabel("No Session");
	private final JLabel kphLabel = new JLabel(htmlLabel("KPH: ", "N/A"));
	private final JLabel killsLabel = new JLabel(htmlLabel("Kills: ", "N/A"));
	private final JLabel avgKillLabel = new JLabel(htmlLabel("Average Kill: ", "N/A"));
	private final JLabel fastestKillLabel = new JLabel(htmlLabel("Fastest Kill: ", "N/A"));
	private final JLabel idleTimeLabel = new JLabel(htmlLabel("Idle Time: ", "N/A"));
	private final JLabel sessionTimeLabel = new JLabel(htmlLabel("Session Time: ", "N/A"));

	private final JButton pauseResumeButton = new JButton("Pause");
	private final JButton calcModeButton = new JButton("Actual");

	private final JPanel goalsPanel = new JPanel(new BorderLayout());
	private final JLabel goalIconLabel = new JLabel();
	private final JLabel goalKphLabel = new JLabel(htmlLabel("KPH: ", "N/A"));
	private final JLabel goalTtgLabel = new JLabel(htmlLabel("TTG: ", "N/A"));
	private final JLabel goalKillsDoneLabel = new JLabel(htmlLabel("Kills Done: ", "N/A"));
	private final JLabel goalKillsLeftLabel = new JLabel(htmlLabel("Kills Left: ", "N/A"));
	private final ProgressBar goalProgressBar = new ProgressBar();
	private final JLabel goalLootLabel = new JLabel(htmlLabel("Loot Goal: ", "N/A"));
	private final SpinnerNumberModel goalStartKcModel = new SpinnerNumberModel(0, 0, 10_000_000, 1);
	private final SpinnerNumberModel goalEndKcModel = new SpinnerNumberModel(0, 0, 10_000_000, 5);
	private final SpinnerNumberModel goalLootGpModel = new SpinnerNumberModel(0L, 0L, 2_000_000_000L, 100_000L);

	private final JToggleButton lootCollapseButton = createDarkToggleButton("Loot ▾");
	private final JToggleButton showIgnoredLootButton = createDarkToggleButton("Show Ignored");
	private final JLabel lootGpPerKillLabel = new JLabel(htmlLabelStacked("GP/Kill", "N/A"));
	private final JLabel lootGpPerHourLabel = new JLabel(htmlLabelStacked("GP/Hr", "N/A"));
	private final JLabel lootTotalGpLabel = new JLabel(htmlLabelStacked("Total GP", "N/A", "right"));
	private final JPanel lootGridPanel = new JPanel(new GridLayout(0, LOOT_GRID_COLUMNS, 2, 2));
	private int lastRenderedLootVersion = -1;
	private boolean lastRenderedLootAllTime;
	private boolean lastRenderedLootShowIgnored;
	private Boss lastRenderedLootBoss;

	private final JToggleButton sessionTabButton = createDarkToggleButton("Session");
	private final JToggleButton historyTabButton = createDarkToggleButton("History");
	private final JToggleButton searchToggleButton = new JToggleButton(SEARCH_ICON);
	private final CardLayout viewCardLayout = new CardLayout();

	/**
	 * Plain CardLayout reports its preferred size as the max across every card (including
	 * hidden ones), so an expanded History entry would leave the panel stretched out with a
	 * trailing gap after switching back to the compact Session card. Overridden here to size
	 * to only the currently-visible card.
	 */
	private final JPanel viewContainer = new JPanel(viewCardLayout)
	{
		@Override
		public Dimension getPreferredSize()
		{
			for (Component c : getComponents())
			{
				if (c.isVisible())
				{
					return c.getPreferredSize();
				}
			}
			return super.getPreferredSize();
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}
	};
	private final JPanel sessionViewPanel = new JPanel();
	private final JPanel historyViewPanel = new JPanel();
	private final JPanel historyEntriesPanel = new JPanel();
	private final JButton historyCollapseAllButton = new JButton("Collapse All");
	private final JPanel searchViewPanel = new JPanel();
	private int lastRenderedHistoryVersion = -1;
	private int lastRenderedLookupVersion = -1;

	/**
	 * Which History entries are expanded, keyed by {@link SessionHistoryEntry#getEndedAtEpochMilli()}.
	 * refreshHistoryPanel() rebuilds every entry's JToggleButton/detail panel from scratch each
	 * time it reloads, so without this the expand state would reset whenever the user switches
	 * away to another tab and back.
	 */
	private final Set<Long> expandedHistoryEntryIds = new HashSet<>();

	private final JTextField searchField = new JTextField();
	private final JLabel searchResultLabel = new JLabel();
	private final JPanel searchResultPanel = new JPanel();

	@Inject
	public BossTrackerPanel(SessionManager sessionManager, GoalManager goalManager, LootTracker lootTracker,
		SessionHistoryManager historyManager, BossLookupManager lookupManager, BossTrackerConfig config,
		ItemManager itemManager, ItemPriceCache priceCache, SkillIconManager skillIconManager)
	{
		this.sessionManager = sessionManager;
		this.goalManager = goalManager;
		this.lootTracker = lootTracker;
		this.historyManager = historyManager;
		this.lookupManager = lookupManager;
		this.config = config;
		this.itemManager = itemManager;
		this.priceCache = priceCache;
		this.slayerIcon = skillIconManager.getSkillImage(Skill.SLAYER, true);

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		if (getScrollPane() != null)
		{
			getScrollPane().setBorder(new EmptyBorder(0, 0, 0, 0));
			customizeScrollBar();
		}

		sessionViewPanel.setLayout(new BoxLayout(sessionViewPanel, BoxLayout.Y_AXIS));
		sessionViewPanel.setOpaque(false);
		sessionViewPanel.add(buildBossInfoPanel());
		sessionViewPanel.add(Box.createRigidArea(new Dimension(0, 14)));
		sessionViewPanel.add(buildBossGoalsPanel());
		sessionViewPanel.add(Box.createRigidArea(new Dimension(0, 14)));
		sessionViewPanel.add(buildLootPanel());
		sessionViewPanel.add(buildPauseAndResumeButtons());
		sessionViewPanel.add(buildSessionEndButton());

		historyViewPanel.setLayout(new BoxLayout(historyViewPanel, BoxLayout.Y_AXIS));
		historyViewPanel.setOpaque(false);
		historyViewPanel.add(buildHistoryHeaderRow());
		historyEntriesPanel.setLayout(new BoxLayout(historyEntriesPanel, BoxLayout.Y_AXIS));
		historyEntriesPanel.setOpaque(false);
		historyViewPanel.add(historyEntriesPanel);

		searchViewPanel.setLayout(new BoxLayout(searchViewPanel, BoxLayout.Y_AXIS));
		searchViewPanel.setOpaque(false);
		searchViewPanel.add(buildSearchBar());
		searchViewPanel.add(searchResultLabel);
		searchResultPanel.setLayout(new BoxLayout(searchResultPanel, BoxLayout.Y_AXIS));
		searchResultPanel.setOpaque(false);
		searchViewPanel.add(searchResultPanel);

		viewContainer.setOpaque(false);
		viewContainer.add(sessionViewPanel, "session");
		viewContainer.add(historyViewPanel, "history");
		viewContainer.add(searchViewPanel, "search");

		JPanel sidePanel = new JPanel();
		sidePanel.setOpaque(false);
		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
		sidePanel.add(buildTitlePanel());
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		sidePanel.add(buildViewTabButtons());
		sidePanel.add(viewContainer);

		add(sidePanel, BorderLayout.NORTH);

		refresh();
	}

	/**
	 * The default scrollbar RuneLite's {@code PluginPanel} produces renders with the plain Basic
	 * L&amp;F skin (visible arrow buttons, boxy thumb) rather than the client's own thin dark
	 * style, so it's built here manually to match the rest of the client.
	 */
	private void customizeScrollBar()
	{
		getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		JScrollBar verticalScrollBar = getScrollPane().getVerticalScrollBar();
		verticalScrollBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		verticalScrollBar.setPreferredSize(new Dimension(8, 0));
		verticalScrollBar.setUnitIncrement(16);
		verticalScrollBar.setUI(new BasicScrollBarUI()
		{
			@Override
			protected void configureScrollBarColors()
			{
				this.thumbColor = ColorScheme.DARK_GRAY_COLOR;
				this.trackColor = new Color(30, 30, 30);
			}

			@Override
			protected JButton createDecreaseButton(int orientation)
			{
				return createZeroButton();
			}

			@Override
			protected JButton createIncreaseButton(int orientation)
			{
				return createZeroButton();
			}

			private JButton createZeroButton()
			{
				JButton button = new JButton();
				button.setPreferredSize(new Dimension(0, 0));
				button.setMinimumSize(new Dimension(0, 0));
				button.setMaximumSize(new Dimension(0, 0));
				return button;
			}
		});
	}

	private JPanel buildViewTabButtons()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(0, 0, 4, 0));

		JPanel tabRow = new JPanel(new GridLayout(1, 2, 2, 0));
		tabRow.setOpaque(false);

		ButtonGroup group = new ButtonGroup();
		group.add(sessionTabButton);
		group.add(historyTabButton);
		sessionTabButton.setSelected(true);
		styleButton(sessionTabButton);
		styleButton(historyTabButton);

		sessionTabButton.addActionListener(e ->
		{
			searchToggleButton.setSelected(false);
			viewCardLayout.show(viewContainer, "session");
		});
		historyTabButton.addActionListener(e ->
		{
			searchToggleButton.setSelected(false);
			viewCardLayout.show(viewContainer, "history");
			historyManager.reload();
		});

		tabRow.add(sessionTabButton);
		tabRow.add(historyTabButton);

		SwingUtil.removeButtonDecorations(searchToggleButton);
		searchToggleButton.setOpaque(false);
		searchToggleButton.setContentAreaFilled(false);
		searchToggleButton.setPreferredSize(new Dimension(24, 24));
		searchToggleButton.setToolTipText("Look up all-time stats and loot for any boss");
		searchToggleButton.addActionListener(e ->
			viewCardLayout.show(viewContainer, searchToggleButton.isSelected() ? "search"
				: (sessionTabButton.isSelected() ? "session" : "history")));

		row.add(tabRow, BorderLayout.CENTER);
		row.add(searchToggleButton, BorderLayout.EAST);
		return row;
	}

	private JPanel buildSearchBar()
	{
		JPanel container = new JPanel(new BorderLayout(4, 0))
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		container.setOpaque(false);
		container.setBorder(new EmptyBorder(0, 0, 8, 0));

		searchField.setToolTipText("Boss name or alias, e.g. \"cox\", \"vetion\", \"General Graardor\"");
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setForeground(Color.WHITE);
		searchField.setCaretColor(Color.WHITE);
		searchField.setBorder(new CompoundBorder(new MatteBorder(1, 1, 1, 1, new Color(49, 49, 49)), new EmptyBorder(3, 5, 3, 5)));
		JButton searchButton = new JButton("Search");
		styleButton(searchButton);

		Runnable runSearch = this::performSearch;
		searchButton.addActionListener(e -> runSearch.run());
		searchField.addActionListener(e -> runSearch.run());

		container.add(searchField, BorderLayout.CENTER);
		container.add(searchButton, BorderLayout.EAST);
		return container;
	}

	private void performSearch()
	{
		String query = searchField.getText().trim();
		if (query.isEmpty())
		{
			return;
		}

		Boss match = Boss.byNameOrAlias(query);
		if (match == null)
		{
			String lowerQuery = query.toLowerCase();
			for (Boss candidate : Boss.values())
			{
				if (candidate.getBossName().toLowerCase().contains(lowerQuery))
				{
					match = candidate;
					break;
				}
			}
		}

		if (match == null)
		{
			lookupManager.clear();
			searchResultLabel.setText("No boss found matching \"" + query + "\".");
			refreshSearchPanel();
			return;
		}

		searchResultLabel.setText("");
		lookupManager.lookup(match);
		refreshSearchPanel();
	}

	private JPanel buildTitlePanel()
	{
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setOpaque(false);
		titlePanel.setBorder(new CompoundBorder(new EmptyBorder(5, 0, 0, 0), new MatteBorder(0, 0, 1, 0, new Color(37, 125, 141))));

		PluginErrorPanel errorPanel = new PluginErrorPanel();
		errorPanel.setBorder(new EmptyBorder(2, 0, 3, 0));
		errorPanel.setContent("Boss Tracker", "Tracks kills per hour and session stats");
		titlePanel.add(errorPanel, BorderLayout.CENTER);
		return titlePanel;
	}

	private JPanel buildBossInfoPanel()
	{
		JPanel bossInfoPanel = new JPanel(new BorderLayout());
		bossInfoPanel.setOpaque(false);
		bossInfoPanel.setBorder(new EmptyBorder(0, 0, 4, 0));

		JPanel sessionInfoSection = new JPanel(new GridLayout(7, 1, 0, 10));
		sessionInfoSection.setBorder(new EmptyBorder(10, 5, 3, 0));
		sessionInfoSection.setOpaque(false);

		bossNameLabel.setFont(FontManager.getRunescapeBoldFont());

		sessionInfoSection.add(bossNameLabel);
		sessionInfoSection.add(kphLabel);
		sessionInfoSection.add(killsLabel);
		sessionInfoSection.add(avgKillLabel);
		sessionInfoSection.add(fastestKillLabel);
		sessionInfoSection.add(idleTimeLabel);
		sessionInfoSection.add(sessionTimeLabel);

		bossInfoPanel.add(bossIconLabel, BorderLayout.EAST);
		bossInfoPanel.add(sessionInfoSection, BorderLayout.WEST);
		return bossInfoPanel;
	}

	private JPanel buildPauseAndResumeButtons()
	{
		JPanel container = new JPanel(new BorderLayout());
		container.setOpaque(false);
		container.setBorder(new EmptyBorder(4, 5, 0, 10));

		JPanel buttons = new JPanel(new GridLayout(1, 2, 5, 0));
		buttons.setOpaque(false);
		buttons.setBorder(new EmptyBorder(5, 5, 0, 0));

		calcModeButton.setToolTipText("<html>ACTUAL: real elapsed time, including any idle/travel time between "
			+ "kills.<br>VIRTUAL: only the time spent actually fighting, as if kills happened back-to-back.</html>");
		styleButton(pauseResumeButton);
		styleButton(calcModeButton);

		pauseResumeButton.addActionListener(e ->
		{
			BossSession session = sessionManager.getSession();
			if (session != null && session.isPaused())
			{
				sessionManager.resume();
			}
			else
			{
				sessionManager.pause();
			}
			refresh();
		});

		calcModeButton.addActionListener(e ->
		{
			sessionManager.toggleCalcMode();
			refresh();
		});

		buttons.add(pauseResumeButton);
		buttons.add(calcModeButton);
		container.add(buttons, BorderLayout.CENTER);
		return container;
	}

	private JPanel buildBossGoalsPanel()
	{
		goalsPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 5, 0), new MatteBorder(1, 1, 1, 1, new Color(49, 49, 49))));
		goalsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem setGoalItem = new JMenuItem("Set Boss Goal");
		setGoalItem.addActionListener(e -> openSetGoalDialog());
		JMenuItem resetGoalItem = new JMenuItem("Reset Boss Goal");
		resetGoalItem.addActionListener(e ->
		{
			goalManager.resetGoal();
			refresh();
		});
		popupMenu.add(setGoalItem);
		popupMenu.add(resetGoalItem);
		goalsPanel.setComponentPopupMenu(popupMenu);

		JPanel iconPanel = new JPanel();
		iconPanel.setOpaque(false);
		iconPanel.add(goalIconLabel);

		JPanel kphTtgPanel = new JPanel(new GridLayout(2, 1));
		kphTtgPanel.setOpaque(false);
		kphTtgPanel.setBorder(new EmptyBorder(5, 3, 0, 3));
		kphTtgPanel.add(goalKphLabel);
		kphTtgPanel.add(goalTtgLabel);

		JPanel killsPanel = new JPanel(new GridLayout(2, 1));
		killsPanel.setOpaque(false);
		killsPanel.setBorder(new EmptyBorder(5, 0, 0, 17));
		killsPanel.add(goalKillsDoneLabel);
		killsPanel.add(goalKillsLeftLabel);

		goalProgressBar.setBackground(new Color(61, 56, 49));
		goalProgressBar.setForeground(new Color(91, 154, 47));
		goalProgressBar.setMaximumValue(100);

		JPanel progressBarPanel = new JPanel(new BorderLayout());
		progressBarPanel.setOpaque(false);
		progressBarPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
		progressBarPanel.add(goalProgressBar);

		goalLootLabel.setBorder(new EmptyBorder(4, 5, 7, 5));
		goalLootLabel.setHorizontalAlignment(SwingConstants.LEFT);

		JPanel goalLootLabelWrapper = new JPanel(new BorderLayout());
		goalLootLabelWrapper.setOpaque(false);
		goalLootLabelWrapper.add(goalLootLabel, BorderLayout.WEST);

		JPanel southPanel = new JPanel();
		southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
		southPanel.setOpaque(false);
		southPanel.add(progressBarPanel);
		southPanel.add(goalLootLabelWrapper);

		goalsPanel.add(iconPanel, BorderLayout.WEST);
		goalsPanel.add(kphTtgPanel, BorderLayout.CENTER);
		goalsPanel.add(killsPanel, BorderLayout.EAST);
		goalsPanel.add(southPanel, BorderLayout.SOUTH);

		return goalsPanel;
	}

	private void openSetGoalDialog()
	{
		BossGoal goal = goalManager.getGoal();
		if (goal == null)
		{
			return;
		}

		BossSession current = sessionManager.getSession();
		int liveKc = current != null ? current.getKillCount() : goal.getStartKc();

		goalStartKcModel.setValue(liveKc);
		goalEndKcModel.setMinimum(liveKc);
		goalEndKcModel.setValue(Math.max(liveKc, goal.getEndKc()));
		goalLootGpModel.setValue(goal.getLootGoalGp());

		JSpinner startSpinner = new JSpinner(goalStartKcModel);
		JSpinner endSpinner = new JSpinner(goalEndKcModel);
		JSpinner lootGpSpinner = new JSpinner(goalLootGpModel);

		JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		inputPanel.add(new JLabel("Start KC:"));
		inputPanel.add(startSpinner);
		inputPanel.add(new JLabel("End KC:"));
		inputPanel.add(endSpinner);
		inputPanel.add(new JLabel("Loot Goal (GP, 0 = off):"));
		inputPanel.add(lootGpSpinner);

		int option = JOptionPane.showConfirmDialog(null, inputPanel, "Set Boss Goal (" + goal.getBoss().getBossName() + ")",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (option == JOptionPane.OK_OPTION)
		{
			goalManager.setGoal((int) goalStartKcModel.getValue(), (int) goalEndKcModel.getValue());
			goalManager.setLootGoal((long) goalLootGpModel.getValue());
			refresh();
		}
	}

	private void refreshGoalsPanel(BossSession display)
	{
		goalsPanel.setVisible(config.displayBossGoalsPanel());
		if (!config.displayBossGoalsPanel())
		{
			return;
		}

		BossGoal goal = goalManager.getGoal();
		boolean goalForDisplayedBoss = display != null && goal != null && goal.getBoss() == display.getBoss();
		boolean hasGoal = goalForDisplayedBoss && goal.isSet();

		refreshLootGoalLabel(goalForDisplayedBoss ? goal : null);

		if (!hasGoal)
		{
			goalIconLabel.setIcon(null);
			goalKphLabel.setText(htmlLabel("KPH: ", "N/A"));
			goalTtgLabel.setText(htmlLabel("TTG: ", "N/A"));
			goalKillsDoneLabel.setText(htmlLabel("Kills Done: ", "N/A"));
			goalKillsLeftLabel.setText(htmlLabel("Kills Left: ", "N/A"));
			goalProgressBar.setLeftLabel("");
			goalProgressBar.setRightLabel("");
			goalProgressBar.setCenterLabel("Set a goal");
			goalProgressBar.setValue(0);
			return;
		}

		itemManager.getImage(display.getBoss().getIconItemId()).addTo(goalIconLabel);

		int currentKc = display.getKillCount();
		int killsDone = goal.killsDone(currentKc);
		int totalToGet = goal.totalKillsToGet();
		int killsLeft = Math.max(0, totalToGet - killsDone);
		boolean complete = goal.isComplete(currentKc);
		double percentDone = totalToGet > 0 ? 100.0 * killsDone / totalToGet : 0;
		double ttgHours = display.getKillsPerHour() > 0 ? killsLeft / display.getKillsPerHour() : 0;

		goalKphLabel.setText(htmlLabel("KPH: ", TimeFormat.kph(display.getKillsPerHour(), config.kphMethod())));
		goalTtgLabel.setText(htmlLabel("TTG: ", complete ? "00:00:00" : TimeFormat.minutesSeconds((int) (ttgHours * 3600))));
		goalKillsDoneLabel.setText(htmlLabel("Kills Done: ", String.valueOf(complete ? totalToGet : killsDone)));
		goalKillsLeftLabel.setText(htmlLabel("Kills Left: ", String.valueOf(complete ? 0 : killsLeft)));

		if (config.displayRelativeKills())
		{
			goalProgressBar.setLeftLabel("0");
			goalProgressBar.setRightLabel(String.valueOf(totalToGet));
		}
		else
		{
			goalProgressBar.setLeftLabel(String.valueOf(goal.getStartKc()));
			goalProgressBar.setRightLabel(String.valueOf(goal.getEndKc()));
		}

		if (complete)
		{
			goalProgressBar.setCenterLabel("Completed");
			goalProgressBar.setValue(100);
		}
		else
		{
			goalProgressBar.setCenterLabel((int) percentDone + "%");
			goalProgressBar.setValue((int) percentDone);
		}
	}

	private void refreshLootGoalLabel(BossGoal goal)
	{
		if (goal == null || !goal.isLootGoalSet())
		{
			goalLootLabel.setText(htmlLabel("Loot Goal: ", "N/A"));
			return;
		}

		long currentGp = 0;
		for (Map.Entry<Integer, Integer> entry : lootTracker.getLifetimeLoot().entrySet())
		{
			currentGp += priceCache.getPrice(entry.getKey()) * entry.getValue();
		}

		goalLootLabel.setText(htmlLabel("Loot Goal: ", formatGp(currentGp) + " / " + formatGp(goal.getLootGoalGp()) + " gp"));
	}

	private JPanel buildLootPanel()
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setOpaque(false);
		wrapper.setBorder(new MatteBorder(0, 0, 1, 0, new Color(37, 125, 141)));

		JPanel headerPanel = new JPanel(new BorderLayout(0, 8));
		headerPanel.setBackground(new Color(30, 30, 30));

		JPanel toggleRow = new JPanel(new GridLayout(1, 2, 4, 0));
		toggleRow.setOpaque(false);
		styleButton(lootCollapseButton);
		styleButton(showIgnoredLootButton);
		lootCollapseButton.setFont(FontManager.getRunescapeSmallFont());
		showIgnoredLootButton.setFont(FontManager.getRunescapeSmallFont());
		showIgnoredLootButton.setToolTipText("Show loot items you've marked as ignored");
		lootCollapseButton.addActionListener(e ->
		{
			lootGridPanel.setVisible(!lootCollapseButton.isSelected());
			sessionViewPanel.revalidate();
			sessionViewPanel.repaint();
		});
		showIgnoredLootButton.addActionListener(e -> refresh());
		toggleRow.add(lootCollapseButton);
		toggleRow.add(showIgnoredLootButton);

		JPanel statsRow = new JPanel(new GridLayout(1, 3));
		statsRow.setOpaque(false);
		statsRow.setBorder(new EmptyBorder(0, 5, 3, 5));
		lootGpPerKillLabel.setFont(FontManager.getRunescapeSmallFont());
		lootGpPerHourLabel.setFont(FontManager.getRunescapeSmallFont());
		lootTotalGpLabel.setFont(FontManager.getRunescapeSmallFont());
		lootGpPerKillLabel.setHorizontalAlignment(SwingConstants.LEFT);
		lootGpPerHourLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lootTotalGpLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		statsRow.add(lootGpPerKillLabel);
		statsRow.add(lootGpPerHourLabel);
		statsRow.add(lootTotalGpLabel);

		headerPanel.add(toggleRow, BorderLayout.NORTH);
		headerPanel.add(statsRow, BorderLayout.SOUTH);

		lootGridPanel.setOpaque(false);
		lootGridPanel.setBorder(new EmptyBorder(10, 2, 8, 2));

		wrapper.add(headerPanel, BorderLayout.NORTH);
		wrapper.add(lootGridPanel, BorderLayout.CENTER);
		return wrapper;
	}

	private void refreshLootPanel(BossSession display)
	{
		if (display == null)
		{
			lootGpPerKillLabel.setText(htmlLabelStacked("GP/Kill", "N/A", "left"));
			lootGpPerHourLabel.setText(htmlLabelStacked("GP/Hr", "N/A"));
			lootTotalGpLabel.setText(htmlLabelStacked("Total GP", "N/A", "right"));
			if (lastRenderedLootBoss != null)
			{
				lootGridPanel.removeAll();
				lootGridPanel.revalidate();
				lootGridPanel.repaint();
				lastRenderedLootBoss = null;
				lastRenderedLootVersion = -1;
			}
			return;
		}

		boolean allTime = config.lootDisplayMode() == BossTrackerConfig.LootDisplayMode.ALL_TIME;
		Map<Integer, Integer> lootMap = allTime ? lootTracker.getLifetimeLoot() : lootTracker.getSessionLoot();

		double totalGp = 0;
		Map<Integer, Double> valueByItem = new LinkedHashMap<>();
		for (Map.Entry<Integer, Integer> entry : lootMap.entrySet())
		{
			double value = (double) priceCache.getPrice(entry.getKey()) * entry.getValue();
			valueByItem.put(entry.getKey(), value);
			totalGp += value;
		}

		int kills = allTime ? lootTracker.getLifetimeKillsTracked() : display.getKillsThisSession();
		double hours = allTime
			? lootTracker.getLifetimeTimeActualSeconds() / 3600.0
			: sessionManager.computeActualElapsedSeconds() / 3600.0;

		lootGpPerKillLabel.setText(htmlLabelStacked("GP/Kill", kills > 0 ? formatGp(totalGp / kills) : "N/A", "left"));
		lootGpPerHourLabel.setText(htmlLabelStacked("GP/Hr", hours > 0 ? formatGpAbbreviated(totalGp / hours) : "N/A"));
		lootTotalGpLabel.setText(htmlLabelStacked("Total GP", formatGp(totalGp), "right"));

		boolean showIgnored = showIgnoredLootButton.isSelected();
		int version = lootTracker.getVersion();
		if (version == lastRenderedLootVersion && allTime == lastRenderedLootAllTime
			&& showIgnored == lastRenderedLootShowIgnored && display.getBoss() == lastRenderedLootBoss)
		{
			return;
		}
		lastRenderedLootVersion = version;
		lastRenderedLootAllTime = allTime;
		lastRenderedLootShowIgnored = showIgnored;
		lastRenderedLootBoss = display.getBoss();

		lootGridPanel.removeAll();

		List<Integer> sortedIds = new ArrayList<>(valueByItem.keySet());
		sortedIds.sort((a, b) -> Double.compare(valueByItem.get(b), valueByItem.get(a)));

		for (int itemId : sortedIds)
		{
			boolean ignored = lootTracker.getIgnoredItemIds().contains(itemId);
			if (ignored && !showIgnored)
			{
				continue;
			}

			int quantity = lootMap.get(itemId);
			JLabel itemLabel = new JLabel();
			itemLabel.setHorizontalAlignment(SwingConstants.CENTER);
			itemLabel.setToolTipText(buildLootTooltip(itemId, quantity, valueByItem.get(itemId)));

			AsyncBufferedImage image = itemManager.getImage(itemId, quantity, quantity > 1);
			if (ignored)
			{
				Runnable applyAlpha = () -> itemLabel.setIcon(new ImageIcon(ImageUtil.alphaOffset(image, 0.3f)));
				image.onLoaded(applyAlpha);
				applyAlpha.run();
			}
			else
			{
				image.addTo(itemLabel);
			}

			JPopupMenu popupMenu = new JPopupMenu();
			JMenuItem toggleIgnore = new JMenuItem(ignored ? "Unignore Item" : "Ignore Item");
			toggleIgnore.addActionListener(e ->
			{
				lootTracker.toggleIgnored(itemId);
				refresh();
			});
			popupMenu.add(toggleIgnore);
			itemLabel.setComponentPopupMenu(popupMenu);

			JPanel slot = new JPanel(new BorderLayout());
			slot.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			slot.setPreferredSize(new Dimension(40, 40));
			slot.add(itemLabel, BorderLayout.CENTER);
			lootGridPanel.add(slot);
		}

		lootGridPanel.revalidate();
		lootGridPanel.repaint();
	}

	private String buildLootTooltip(int itemId, int quantity, double totalValue)
	{
		String name = priceCache.getName(itemId);
		return "<html>" + name + " x " + quantity + "<br>GP/Item: " + formatGp(priceCache.getPrice(itemId))
			+ "<br>Value: " + formatGp(totalValue) + "</html>";
	}

	private static String formatGp(double value)
	{
		return String.format("%,.0f", value);
	}

	private static String formatGpAbbreviated(double value)
	{
		if (Math.abs(value) >= 1_000_000)
		{
			return String.format("%,.2fm", value / 1_000_000);
		}
		if (Math.abs(value) >= 1_000)
		{
			return String.format("%,.2fk", value / 1_000);
		}
		return String.format("%,.0f", value);
	}

	private static final Color BUTTON_SELECTED_COLOR = new Color(77, 77, 77);

	private static void styleButton(AbstractButton button)
	{
		button.setBackground(ColorScheme.DARK_GRAY_COLOR);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.putClientProperty("FlatLaf.style",
			"borderColor: #282828; focusedBorderColor: #282828; hoverBackground: #333333; "
				+ "selectedBackground: #4d4d4d; selectedForeground: #ffffff; "
				+ "focusWidth: 0; innerFocusWidth: 0;");
	}

	/**
	 * FlatLaf's toggle-button selected-state color didn't come through correctly via the
	 * "FlatLaf.style" client property in testing, so these paint their own flat background
	 * directly instead of relying on FlatLaf's per-state color resolution.
	 */
	private static JToggleButton createDarkToggleButton(String text)
	{
		JToggleButton button = new JToggleButton(text)
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				g.setColor(isSelected() ? BUTTON_SELECTED_COLOR : ColorScheme.DARK_GRAY_COLOR);
				g.fillRect(0, 0, getWidth(), getHeight());
				super.paintComponent(g);
			}
		};
		button.setContentAreaFilled(false);
		return button;
	}

	/**
	 * GridLayout stretches each label to the panel's full width, so JLabel's default left-leading
	 * text alignment reliably puts stat rows flush against the left edge; a plain BoxLayout with
	 * alignmentX set on each label did not render as left-aligned in testing. The maximumSize
	 * override caps the panel's height at its own preferred height so it doesn't get stretched to
	 * fill leftover space when nested in a BoxLayout.Y_AXIS list.
	 */
	private static JPanel createStatsPanel(JLabel... labels)
	{
		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4))
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		panel.setOpaque(false);
		for (JLabel label : labels)
		{
			panel.add(label);
		}
		return panel;
	}

	private JPanel buildSessionEndButton()
	{
		JPanel container = new JPanel(new BorderLayout());
		container.setOpaque(false);
		container.setBorder(new MatteBorder(0, 0, 1, 0, new Color(37, 125, 141)));

		JPanel buttonWrapper = new JPanel(new GridLayout(1, 1, 5, 5));
		buttonWrapper.setOpaque(false);
		buttonWrapper.setBorder(new EmptyBorder(3, 10, 8, 10));

		JButton endSessionButton = new JButton("End Session");
		styleButton(endSessionButton);
		endSessionButton.addActionListener(e ->
		{
			sessionManager.end();
			refresh();
		});

		buttonWrapper.add(endSessionButton);
		container.add(buttonWrapper, BorderLayout.CENTER);
		return container;
	}

	/**
	 * Refreshes all labels, the icon, and button text from current session state. Must be
	 * called on the EDT.
	 */
	public void refresh()
	{
		BossSession session = sessionManager.getSession();
		BossSession display = session != null ? session : sessionManager.getLastCompletedSession();

		if (display == null)
		{
			bossNameLabel.setText("No Session");
			bossNameLabel.setForeground(ENDED_COLOR);
			bossIconLabel.setIcon(null);
			kphLabel.setText(htmlLabel("KPH: ", "N/A"));
			killsLabel.setText(htmlLabel("Kills: ", "N/A"));
			avgKillLabel.setText(htmlLabel("Average Kill: ", "N/A"));
			fastestKillLabel.setText(htmlLabel("Fastest Kill: ", "N/A"));
			idleTimeLabel.setText(htmlLabel("Idle Time: ", "N/A"));
			sessionTimeLabel.setText(htmlLabel("Session Time: ", "N/A"));
			pauseResumeButton.setText("Pause");
			calcModeButton.setText("Actual");
			refreshGoalsPanel(null);
			refreshLootPanel(null);
			refreshHistoryPanel();
			refreshSearchPanel();
			return;
		}

		bossNameLabel.setText(display.getBoss().getBossName());
		bossNameLabel.setForeground(session == null ? ENDED_COLOR : (session.isPaused() ? PAUSED_COLOR : ACTIVE_COLOR));
		itemManager.getImage(display.getBoss().getIconItemId()).addTo(bossIconLabel);

		kphLabel.setText(htmlLabel("KPH: ", TimeFormat.kph(display.getKillsPerHour(), config.kphMethod())));
		killsLabel.setText(htmlLabel("Kills: ", String.valueOf(display.getKillsThisSession())));
		avgKillLabel.setText(htmlLabel("Average Kill: ", TimeFormat.minutesSeconds(display.getAverageKillTimeSeconds())));
		fastestKillLabel.setText(htmlLabel("Fastest Kill: ", TimeFormat.minutesSeconds(display.getFastestKillSeconds())));
		idleTimeLabel.setText(htmlLabel("Idle Time: ", TimeFormat.minutesSeconds(display.getIdleSeconds())));
		sessionTimeLabel.setText(htmlLabel("Session Time: ",
			TimeFormat.minutesSeconds(session != null ? sessionManager.computeActualElapsedSeconds() : 0)));

		pauseResumeButton.setText(session != null && session.isPaused() ? "Resume" : "Pause");
		calcModeButton.setText(display.getCalcMode() == CalcMode.VIRTUAL ? "Virtual" : "Actual");

		refreshGoalsPanel(display);
		refreshLootPanel(display);
		refreshHistoryPanel();
		refreshSearchPanel();
	}

	private void refreshSearchPanel()
	{
		if (lookupManager.getVersion() == lastRenderedLookupVersion)
		{
			return;
		}
		lastRenderedLookupVersion = lookupManager.getVersion();

		searchResultPanel.removeAll();
		Boss boss = lookupManager.getBoss();
		if (boss != null)
		{
			searchResultPanel.add(buildLookupResultPanel(boss));
		}

		searchResultPanel.revalidate();
		searchResultPanel.repaint();
	}

	private JPanel buildLookupResultPanel(Boss boss)
	{
		BossStats stats = lookupManager.getStats();
		Map<Integer, Integer> lootMap = lookupManager.getLootItemQuantities();

		JPanel resultPanel = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		resultPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 4, 0), new MatteBorder(1, 1, 1, 1, new Color(49, 49, 49))));
		resultPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel headerRow = new JPanel(new BorderLayout());
		headerRow.setOpaque(false);
		headerRow.setBorder(new EmptyBorder(4, 6, 4, 6));

		JLabel nameLabel = new JLabel(boss.getBossName());
		nameLabel.setFont(FontManager.getRunescapeBoldFont());
		JLabel iconLabel = new JLabel();
		itemManager.getImage(boss.getIconItemId()).addTo(iconLabel);

		JButton deleteButton = new JButton("Delete Data");
		styleButton(deleteButton);
		deleteButton.setEnabled(stats.getKillsTracked() > 0 || !lootMap.isEmpty());
		deleteButton.addActionListener(e ->
		{
			int confirm = JOptionPane.showConfirmDialog(this,
				"Delete all saved stats and loot for " + boss.getBossName() + "? This cannot be undone.",
				"Delete Boss Data", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (confirm == JOptionPane.YES_OPTION)
			{
				lookupManager.deleteData(boss);
				refreshSearchPanel();
			}
		});

		headerRow.add(iconLabel, BorderLayout.WEST);
		headerRow.add(nameLabel, BorderLayout.CENTER);

		JPanel detailPanel = new JPanel();
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		detailPanel.setOpaque(false);
		detailPanel.setBorder(new EmptyBorder(0, 8, 6, 8));

		if (stats.getKillsTracked() == 0)
		{
			JLabel noDataLabel = new JLabel("No data recorded for this boss yet.");
			noDataLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			detailPanel.add(noDataLabel);
		}
		else
		{
			int fastestKill = stats.getFastestKillSeconds() == Integer.MAX_VALUE ? 0 : stats.getFastestKillSeconds();
			double avgKillSeconds = (double) stats.getTotalTimeActualSeconds() / stats.getKillsTracked();
			double kph = avgKillSeconds > 0 ? 3600.0 / avgKillSeconds : 0;

			JLabel totalKcLabel = new JLabel(htmlLabel("Total KC: ", String.valueOf(stats.getTotalKc())));
			JLabel killsTrackedLabel = new JLabel(htmlLabel("Kills Tracked: ", String.valueOf(stats.getKillsTracked())));
			JLabel avgKphLabel = new JLabel(htmlLabel("Average KPH: ", TimeFormat.kph(kph, config.kphMethod())));
			JLabel fastestKillLabel = new JLabel(htmlLabel("Fastest Kill: ", TimeFormat.minutesSeconds(fastestKill)));
			JLabel totalTrackedTimeLabel = new JLabel(htmlLabel("Total Tracked Time: ", TimeFormat.minutesSeconds((int) stats.getTotalTimeActualSeconds())));
			detailPanel.add(createStatsPanel(totalKcLabel, killsTrackedLabel, avgKphLabel, fastestKillLabel, totalTrackedTimeLabel));
		}

		if (!lootMap.isEmpty())
		{
			double totalGp = 0;
			for (Map.Entry<Integer, Integer> entry : lootMap.entrySet())
			{
				totalGp += (double) priceCache.getPrice(entry.getKey()) * entry.getValue();
			}

			int lootKills = Math.max(stats.getLootKillsTracked(), 1);
			JLabel gpPerKillLabel = new JLabel(htmlLabel("GP/Kill: ", formatGp(totalGp / lootKills)));
			JLabel totalGpLabel = new JLabel(htmlLabel("Total GP: ", formatGp(totalGp)));
			detailPanel.add(createStatsPanel(gpPerKillLabel, totalGpLabel));

			JPanel lootGrid = new JPanel(new GridLayout(0, LOOT_GRID_COLUMNS, 2, 2));
			lootGrid.setOpaque(false);
			for (Map.Entry<Integer, Integer> item : lootMap.entrySet())
			{
				JLabel itemLabel = new JLabel();
				itemLabel.setHorizontalAlignment(SwingConstants.CENTER);
				itemLabel.setToolTipText(priceCache.getName(item.getKey()) + " x" + item.getValue());
				itemManager.getImage(item.getKey(), item.getValue(), item.getValue() > 1).addTo(itemLabel);

				JPanel slot = new JPanel(new BorderLayout());
				slot.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				slot.setPreferredSize(new Dimension(32, 32));
				slot.add(itemLabel, BorderLayout.CENTER);
				lootGrid.add(slot);
			}
			detailPanel.add(lootGrid);
		}

		JPanel deleteButtonPanel = new JPanel(new GridLayout(1, 1, 5, 5));
		deleteButtonPanel.setOpaque(false);
		deleteButtonPanel.setBorder(new EmptyBorder(0, 8, 8, 8));
		deleteButtonPanel.add(deleteButton);

		resultPanel.add(headerRow, BorderLayout.NORTH);
		resultPanel.add(detailPanel, BorderLayout.CENTER);
		resultPanel.add(deleteButtonPanel, BorderLayout.SOUTH);
		return resultPanel;
	}

	private void refreshHistoryPanel()
	{
		if (historyManager.getVersion() == lastRenderedHistoryVersion)
		{
			return;
		}
		lastRenderedHistoryVersion = historyManager.getVersion();

		historyEntriesPanel.removeAll();
		List<SessionHistoryEntry> entries = historyManager.getEntries();
		if (entries.isEmpty())
		{
			JLabel emptyLabel = new JLabel("No session history yet.");
			emptyLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
			historyEntriesPanel.add(emptyLabel);
		}
		else
		{
			for (SessionHistoryEntry entry : entries)
			{
				historyEntriesPanel.add(buildHistoryEntryPanel(entry));
			}
		}

		historyEntriesPanel.revalidate();
		historyEntriesPanel.repaint();
	}

	private JPanel buildHistoryHeaderRow()
	{
		JPanel row = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(0, 0, 4, 0));

		styleButton(historyCollapseAllButton);
		historyCollapseAllButton.setFont(FontManager.getRunescapeSmallFont());
		historyCollapseAllButton.setToolTipText("Collapse all expanded sessions");
		historyCollapseAllButton.addActionListener(e ->
		{
			expandedHistoryEntryIds.clear();
			lastRenderedHistoryVersion = -1;
			refreshHistoryPanel();
		});

		row.add(historyCollapseAllButton, BorderLayout.EAST);
		return row;
	}

	private JPanel buildHistoryEntryPanel(SessionHistoryEntry entry)
	{
		JPanel entryPanel = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		entryPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 4, 0), new MatteBorder(1, 1, 1, 1, new Color(49, 49, 49))));
		entryPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel headerContainer = new JPanel(new BorderLayout());
		headerContainer.setOpaque(false);

		JPanel headerRow = new JPanel(new BorderLayout());
		headerRow.setOpaque(false);

		JToggleButton expandButton = new JToggleButton(entry.getBossName() + " - " + entry.getKillsThisSession() + " kills");
		expandButton.setHorizontalAlignment(SwingConstants.LEFT);
		expandButton.setSelected(expandedHistoryEntryIds.contains(entry.getEndedAtEpochMilli()));
		styleButton(expandButton);

		JButton deleteButton = new JButton("✕");
		deleteButton.setToolTipText("Delete this session");
		styleButton(deleteButton);
		deleteButton.setPreferredSize(new Dimension(28, deleteButton.getPreferredSize().height));
		deleteButton.addActionListener(e ->
		{
			int confirm = JOptionPane.showConfirmDialog(this,
				"Delete this session (" + entry.getBossName() + " - " + entry.getKillsThisSession() + " kills)? "
					+ "This cannot be undone.",
				"Delete Session", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (confirm == JOptionPane.YES_OPTION)
			{
				historyManager.delete(entry);
				expandedHistoryEntryIds.remove(entry.getEndedAtEpochMilli());
				refreshHistoryPanel();
			}
		});

		JPanel eastControls = new JPanel(new BorderLayout(4, 0));
		eastControls.setOpaque(false);
		if (entry.isOnSlayerTask())
		{
			JLabel slayerIconLabel = new JLabel(new ImageIcon(slayerIcon));
			slayerIconLabel.setToolTipText("This session was on a Slayer task");
			eastControls.add(slayerIconLabel, BorderLayout.WEST);
		}
		eastControls.add(deleteButton, BorderLayout.EAST);

		headerRow.add(expandButton, BorderLayout.CENTER);
		headerRow.add(eastControls, BorderLayout.EAST);

		String dateText = HISTORY_DATE_FORMAT.format(Instant.ofEpochMilli(entry.getEndedAtEpochMilli()));
		JLabel dateLabel = new JLabel(dateText);
		dateLabel.setFont(FontManager.getRunescapeSmallFont());
		dateLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		dateLabel.setBorder(new EmptyBorder(2, 8, 2, 8));

		headerContainer.add(headerRow, BorderLayout.NORTH);
		headerContainer.add(dateLabel, BorderLayout.SOUTH);

		JPanel detailPanel = new JPanel();
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		detailPanel.setOpaque(false);
		detailPanel.setBorder(new EmptyBorder(10, 8, 6, 8));

		JLabel kphStatLabel = new JLabel(htmlLabel("KPH: ", TimeFormat.kph(entry.getKillsPerHour(), config.kphMethod())));
		JLabel sessionTimeStatLabel = new JLabel(htmlLabel("Session Time: ", TimeFormat.minutesSeconds(entry.getSessionDurationSeconds())));
		JLabel avgKillStatLabel = new JLabel(htmlLabel("Average Kill: ", TimeFormat.minutesSeconds(entry.getAverageKillTimeSeconds())));
		JLabel fastestKillStatLabel = new JLabel(htmlLabel("Fastest Kill: ", TimeFormat.minutesSeconds(entry.getFastestKillSeconds())));
		JLabel idleTimeStatLabel = new JLabel(htmlLabel("Idle Time: ", TimeFormat.minutesSeconds(entry.getIdleSeconds())));
		detailPanel.add(createStatsPanel(kphStatLabel, sessionTimeStatLabel, avgKillStatLabel, fastestKillStatLabel, idleTimeStatLabel));
		detailPanel.add(Box.createRigidArea(new Dimension(0, 8)));

		if (!entry.getLootItemQuantities().isEmpty())
		{
			double totalGp = 0;
			for (Map.Entry<Integer, Integer> item : entry.getLootItemQuantities().entrySet())
			{
				totalGp += (double) priceCache.getPrice(item.getKey()) * item.getValue();
			}
			int kills = entry.getKillsThisSession();

			JPanel lootStatsRow = new JPanel(new GridLayout(1, 2));
			lootStatsRow.setOpaque(false);
			JLabel gpPerKillLabel = new JLabel(htmlLabelStacked("GP/Kill", kills > 0 ? formatGp(totalGp / kills) : "N/A", "left"));
			JLabel totalGpLabel = new JLabel(htmlLabelStacked("Total GP", formatGp(totalGp), "right"));
			gpPerKillLabel.setFont(FontManager.getRunescapeSmallFont());
			totalGpLabel.setFont(FontManager.getRunescapeSmallFont());
			gpPerKillLabel.setHorizontalAlignment(SwingConstants.LEFT);
			totalGpLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			lootStatsRow.add(gpPerKillLabel);
			lootStatsRow.add(totalGpLabel);
			detailPanel.add(lootStatsRow);
			detailPanel.add(Box.createRigidArea(new Dimension(0, 10)));

			JPanel lootGrid = new JPanel(new GridLayout(0, LOOT_GRID_COLUMNS, 2, 2));
			lootGrid.setOpaque(false);
			for (Map.Entry<Integer, Integer> item : entry.getLootItemQuantities().entrySet())
			{
				JLabel itemLabel = new JLabel();
				itemLabel.setHorizontalAlignment(SwingConstants.CENTER);
				itemLabel.setToolTipText(priceCache.getName(item.getKey()) + " x" + item.getValue());
				itemManager.getImage(item.getKey(), item.getValue(), item.getValue() > 1).addTo(itemLabel);

				JPanel slot = new JPanel(new BorderLayout());
				slot.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				slot.setBorder(new MatteBorder(1, 1, 1, 1, new Color(49, 49, 49)));
				slot.setPreferredSize(new Dimension(32, 32));
				slot.add(itemLabel, BorderLayout.CENTER);
				lootGrid.add(slot);
			}
			detailPanel.add(lootGrid);
		}

		detailPanel.setVisible(expandButton.isSelected());
		expandButton.addActionListener(e ->
		{
			boolean expanded = expandButton.isSelected();
			detailPanel.setVisible(expanded);
			if (expanded)
			{
				expandedHistoryEntryIds.add(entry.getEndedAtEpochMilli());
			}
			else
			{
				expandedHistoryEntryIds.remove(entry.getEndedAtEpochMilli());
			}
			historyViewPanel.revalidate();
			historyViewPanel.repaint();
		});

		entryPanel.add(headerContainer, BorderLayout.NORTH);
		entryPanel.add(detailPanel, BorderLayout.CENTER);
		return entryPanel;
	}

	private static String htmlLabel(String key, String value)
	{
		return String.format(HTML_LABEL_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), key, value);
	}

	private static String htmlLabelStacked(String key, String value)
	{
		return htmlLabelStacked(key, value, "center");
	}

	private static String htmlLabelStacked(String key, String value, String align)
	{
		return String.format(HTML_LABEL_STACKED_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), align, key, value);
	}
}
