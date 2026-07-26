package com.camjewell.bosstracker.ui;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.session.BossSession;
import com.camjewell.bosstracker.session.SessionManager;
import com.camjewell.bosstracker.util.TimeFormat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class SessionOverlay extends Overlay
{
	private static final Color PAUSED_COLOR = new Color(227, 160, 27);

	private final BossTrackerConfig config;
	private final SessionManager sessionManager;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	private SessionOverlay(BossTrackerConfig config, SessionManager sessionManager)
	{
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		this.config = config;
		this.sessionManager = sessionManager;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		BossSession session = sessionManager.getSession();
		if (!config.enableOverlay() || session == null || session.getKillsThisSession() < 1)
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(150, 0));

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(session.getBoss().getBossName())
			.color(session.isPaused() ? PAUSED_COLOR : Color.GREEN)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("KPH:")
			.right(TimeFormat.kph(session.getKillsPerHour(), config.kphMethod()))
			.build());

		if (config.showKillsThisSession())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Kills:")
				.right(Integer.toString(session.getKillsThisSession()))
				.build());
		}

		if (config.showAverageKillTime())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Average Kill:")
				.right(TimeFormat.minutesSeconds(session.getAverageKillTimeSeconds()))
				.build());
		}

		if (config.showFastestKill())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Fastest Kill:")
				.right(TimeFormat.minutesSeconds(session.getFastestKillSeconds()))
				.build());
		}

		if (config.showIdleTime())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Idle Time:")
				.right(TimeFormat.minutesSeconds(session.getIdleSeconds()))
				.build());
		}

		if (config.showSessionTime())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Session Time:")
				.right(TimeFormat.minutesSeconds(sessionManager.computeActualElapsedSeconds()))
				.build());
		}

		return panelComponent.render(graphics);
	}
}
