package com.camjewell.bosstracker.ui;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.session.BossGoal;
import com.camjewell.bosstracker.session.BossSession;
import com.camjewell.bosstracker.session.CalcMode;
import com.camjewell.bosstracker.session.GoalManager;
import com.camjewell.bosstracker.session.SessionManager;
import com.camjewell.bosstracker.util.TimeFormat;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.ProgressBar;
import net.runelite.client.util.ColorUtil;

/**
 * The Phase 1 slice of the side panel: title header, live session stats, pause/resume, the
 * ACTUAL/VIRTUAL calc-mode toggle, and end-session. Boss goals, loot, and historical lookup are
 * added in later phases.
 */
public class BossTrackerPanel extends PluginPanel
{
	private static final String HTML_LABEL_TEMPLATE = "<html><body style='color:%s'>%s<span style='color:white'>%s</span></body></html>";
	private static final Color ACTIVE_COLOR = new Color(71, 226, 12);
	private static final Color PAUSED_COLOR = new Color(227, 160, 27);
	private static final Color ENDED_COLOR = new Color(187, 187, 187);

	private final SessionManager sessionManager;
	private final GoalManager goalManager;
	private final BossTrackerConfig config;
	private final ItemManager itemManager;

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
	private final SpinnerNumberModel goalStartKcModel = new SpinnerNumberModel(0, 0, 10_000_000, 1);
	private final SpinnerNumberModel goalEndKcModel = new SpinnerNumberModel(0, 0, 10_000_000, 5);

	@Inject
	public BossTrackerPanel(SessionManager sessionManager, GoalManager goalManager, BossTrackerConfig config,
		ItemManager itemManager)
	{
		this.sessionManager = sessionManager;
		this.goalManager = goalManager;
		this.config = config;
		this.itemManager = itemManager;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel sidePanel = new JPanel();
		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
		sidePanel.add(buildTitlePanel());
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		sidePanel.add(buildBossInfoPanel());
		sidePanel.add(buildPauseAndResumeButtons());
		sidePanel.add(buildBossGoalsPanel());
		sidePanel.add(buildSessionEndButton());

		add(sidePanel, BorderLayout.NORTH);

		refresh();
	}

	private JPanel buildTitlePanel()
	{
		JPanel titlePanel = new JPanel(new BorderLayout());
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
		container.setBorder(new EmptyBorder(4, 5, 0, 10));

		JPanel buttons = new JPanel(new GridLayout(1, 2, 5, 0));
		buttons.setBorder(new EmptyBorder(5, 5, 0, 0));

		calcModeButton.setToolTipText("<html>ACTUAL: real elapsed time, including any idle/travel time between "
			+ "kills.<br>VIRTUAL: only the time spent actually fighting, as if kills happened back-to-back.</html>");

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
		container.add(buttons, BorderLayout.WEST);
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
		progressBarPanel.setBorder(new EmptyBorder(5, 5, 7, 5));
		progressBarPanel.add(goalProgressBar);

		goalsPanel.add(iconPanel, BorderLayout.WEST);
		goalsPanel.add(kphTtgPanel, BorderLayout.CENTER);
		goalsPanel.add(killsPanel, BorderLayout.EAST);
		goalsPanel.add(progressBarPanel, BorderLayout.SOUTH);

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

		JSpinner startSpinner = new JSpinner(goalStartKcModel);
		JSpinner endSpinner = new JSpinner(goalEndKcModel);

		JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		inputPanel.add(new JLabel("Start KC:"));
		inputPanel.add(startSpinner);
		inputPanel.add(new JLabel("End KC:"));
		inputPanel.add(endSpinner);

		int option = JOptionPane.showConfirmDialog(null, inputPanel, "Set Boss KC Goal (" + goal.getBoss().getBossName() + ")",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (option == JOptionPane.OK_OPTION)
		{
			goalManager.setGoal((int) goalStartKcModel.getValue(), (int) goalEndKcModel.getValue());
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
		boolean hasGoal = display != null && goal != null && goal.getBoss() == display.getBoss() && goal.isSet();

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

	private JPanel buildSessionEndButton()
	{
		JPanel container = new JPanel(new BorderLayout());
		container.setBorder(new MatteBorder(0, 0, 1, 0, new Color(37, 125, 141)));

		JPanel buttonWrapper = new JPanel(new GridLayout(1, 1, 5, 5));
		buttonWrapper.setBorder(new EmptyBorder(3, 10, 8, 0));

		JButton endSessionButton = new JButton("End Session");
		endSessionButton.addActionListener(e ->
		{
			sessionManager.end();
			refresh();
		});

		buttonWrapper.add(endSessionButton);
		container.add(buttonWrapper, BorderLayout.WEST);
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
	}

	private static String htmlLabel(String key, String value)
	{
		return String.format(HTML_LABEL_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), key, value);
	}
}
