package com.camjewell.bosstracker.ui;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.session.BossSession;
import com.camjewell.bosstracker.session.SessionManager;
import com.camjewell.bosstracker.util.TimeFormat;
import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * Recreated each time the session starts or switches to a new boss, mirroring the original
 * plugin's per-boss infobox lifecycle.
 */
public class SessionInfobox extends InfoBox
{
	private final BossTrackerConfig config;
	private final SessionManager sessionManager;

	public SessionInfobox(BufferedImage image, Plugin plugin, BossTrackerConfig config, SessionManager sessionManager)
	{
		super(image, plugin);
		this.config = config;
		this.sessionManager = sessionManager;
	}

	@Override
	public String getText()
	{
		BossSession session = sessionManager.getSession();
		if (session == null)
		{
			return "";
		}

		switch (config.infoBoxContent())
		{
			case SESSION_TIME:
				return TimeFormat.minutesSeconds(sessionManager.computeActualElapsedSeconds());
			case KILLS_THIS_SESSION:
				return String.valueOf(session.getKillsThisSession());
			case AVG_KILL:
				return TimeFormat.minutesSeconds(session.getAverageKillTimeSeconds());
			case FASTEST_KILL:
				return TimeFormat.minutesSeconds(session.getFastestKillSeconds());
			case IDLE_TIME:
				return TimeFormat.minutesSeconds(session.getIdleSeconds());
			default:
				return TimeFormat.kph(session.getKillsPerHour(), config.kphMethod());
		}
	}

	@Override
	public Color getTextColor()
	{
		return Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		BossSession session = sessionManager.getSession();
		if (session == null)
		{
			return "";
		}

		StringBuilder tooltip = new StringBuilder();
		tooltip.append(session.getBoss().getBossName());
		tooltip.append("</br>KPH: ").append(TimeFormat.kph(session.getKillsPerHour(), config.kphMethod()));

		if (config.showKillsThisSession())
		{
			tooltip.append("</br>Kills: ").append(session.getKillsThisSession());
		}
		if (config.showAverageKillTime())
		{
			tooltip.append("</br>Avg Kill: ").append(TimeFormat.minutesSeconds(session.getAverageKillTimeSeconds()));
		}
		if (config.showFastestKill())
		{
			tooltip.append("</br>Fastest Kill: ").append(TimeFormat.minutesSeconds(session.getFastestKillSeconds()));
		}
		if (config.showIdleTime())
		{
			tooltip.append("</br>Idle Time: ").append(TimeFormat.minutesSeconds(session.getIdleSeconds()));
		}
		if (config.showSessionTime())
		{
			tooltip.append("</br>Session Time: ").append(TimeFormat.minutesSeconds(sessionManager.computeActualElapsedSeconds()));
		}

		return tooltip.toString();
	}
}
