package com.camjewell.bosstracker.ui;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.session.BossSession;
import com.camjewell.bosstracker.session.CalcMode;
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
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
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

	@Inject
	public BossTrackerPanel(SessionManager sessionManager, BossTrackerConfig config, ItemManager itemManager)
	{
		this.sessionManager = sessionManager;
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
	}

	private static String htmlLabel(String key, String value)
	{
		return String.format(HTML_LABEL_TEMPLATE, ColorUtil.toHexColor(ColorScheme.LIGHT_GRAY_COLOR), key, value);
	}
}
