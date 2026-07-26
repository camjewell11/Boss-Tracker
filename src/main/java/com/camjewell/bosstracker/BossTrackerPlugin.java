package com.camjewell.bosstracker;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.session.BossSession;
import com.camjewell.bosstracker.session.SessionManager;
import com.camjewell.bosstracker.ui.BossTrackerPanel;
import com.camjewell.bosstracker.ui.SessionInfobox;
import com.camjewell.bosstracker.ui.SessionOverlay;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Boss Tracker",
	description = "Tracks kills per hour, session stats, boss goals, loot and historical stats across OSRS bosses",
	tags = {"pvm", "boss", "kph", "kills per hour", "bossing", "kill times", "loot", "goals"}
)
public class BossTrackerPlugin extends Plugin
{
	private static final int[] FIGHT_CAVE_REGION = {9551};
	private static final int[] INFERNO_REGION = {9043};
	private static final int STARTUP_GRACE_TICKS = 5;

	@Inject
	private Client client;

	@Inject
	private BossTrackerConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SessionOverlay overlay;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SessionManager sessionManager;

	@Inject
	private BossTrackerPanel panel;

	private ScheduledExecutorService executor;
	private NavigationButton navButton;
	private BufferedImage icon;
	private SessionInfobox infobox;
	private Boss infoboxBoss;
	private int ticksSinceStart;

	@Provides
	BossTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		executor = Executors.newSingleThreadScheduledExecutor();
		sessionManager.setAsyncExecutor(executor);

		chatCommandManager.registerCommandAsync("!Info", this::infoCommand);
		chatCommandManager.registerCommandAsync("!End", this::endCommand);
		chatCommandManager.registerCommandAsync("!Pause", this::pauseCommand);
		chatCommandManager.registerCommandAsync("!Resume", this::resumeCommand);

		icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Boss Tracker")
			.icon(icon)
			.priority(config.sidePanelPosition())
			.panel(panel)
			.build();
		if (config.showSidePanel())
		{
			clientToolbar.addNavigation(navButton);
		}

		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		chatCommandManager.unregisterCommand("!Info");
		chatCommandManager.unregisterCommand("!End");
		chatCommandManager.unregisterCommand("!Pause");
		chatCommandManager.unregisterCommand("!Resume");

		clientToolbar.removeNavigation(navButton);
		infoBoxManager.removeInfoBox(infobox);
		overlayManager.remove(overlay);

		executor.shutdownNow();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"boss-tracker".equals(event.getGroup()))
		{
			return;
		}

		switch (event.getKey())
		{
			case "showSidePanel":
				if (config.showSidePanel())
				{
					clientToolbar.addNavigation(navButton);
				}
				else
				{
					clientToolbar.removeNavigation(navButton);
				}
				break;

			case "sidePanelPosition":
				if (config.showSidePanel())
				{
					clientToolbar.removeNavigation(navButton);
					navButton = NavigationButton.builder()
						.tooltip("Boss Tracker")
						.icon(icon)
						.priority(config.sidePanelPosition())
						.panel(panel)
						.build();
					clientToolbar.addNavigation(navButton);
				}
				break;

			case "renderInfobox":
				if (!config.renderInfobox())
				{
					infoBoxManager.removeInfoBox(infobox);
					infoboxBoss = null;
				}
				else
				{
					syncUi();
				}
				break;

			case "dksSelector":
				BossSession session = sessionManager.getSession();
				if (session != null && session.getBoss().getBossName().startsWith("Dagannoth"))
				{
					sessionManager.end();
					syncUi();
				}
				break;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.LOGGING_IN)
		{
			ticksSinceStart = 0;
			sessionManager.onWorldHopOrLogin();
		}

		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			sessionManager.pause();
			syncUi();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (client.getLocalPlayer() == null || ticksSinceStart < STARTUP_GRACE_TICKS)
		{
			return;
		}

		if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE
			&& chatMessage.getType() != ChatMessageType.FRIENDSCHATNOTIFICATION
			&& chatMessage.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		int[] mapRegions = client.getTopLevelWorldView().getMapRegions();
		boolean inFightCavesOrInferno = Arrays.equals(mapRegions, FIGHT_CAVE_REGION)
			|| Arrays.equals(mapRegions, INFERNO_REGION);

		sessionManager.onChatMessage(chatMessage.getMessage(), inFightCavesOrInferno);
		syncUi();
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getHitsplat().isMine() && event.getActor() instanceof NPC)
		{
			sessionManager.onHitsplatApplied((NPC) event.getActor());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (ticksSinceStart < STARTUP_GRACE_TICKS)
		{
			ticksSinceStart++;
		}

		sessionManager.onGameTick();
		syncUi();
	}

	private void infoCommand(ChatMessage chatMessage, String message)
	{
		sessionManager.announceCurrentInfo();
	}

	private void endCommand(ChatMessage chatMessage, String message)
	{
		sessionManager.end();
		syncUi();
	}

	private void pauseCommand(ChatMessage chatMessage, String message)
	{
		sessionManager.pause();
		syncUi();
	}

	private void resumeCommand(ChatMessage chatMessage, String message)
	{
		sessionManager.resume();
		syncUi();
	}

	/**
	 * Rebuilds the infobox when the tracked boss changes and schedules a panel refresh. Cheap
	 * enough to call on every tick and every chat message.
	 */
	private void syncUi()
	{
		BossSession session = sessionManager.getSession();
		Boss currentBoss = session != null ? session.getBoss() : null;
		if (currentBoss != infoboxBoss)
		{
			infoboxBoss = currentBoss;
			infoBoxManager.removeInfoBox(infobox);
			if (config.renderInfobox() && currentBoss != null)
			{
				infobox = new SessionInfobox(itemManager.getImage(currentBoss.getIconItemId()), this, config, sessionManager);
				infoBoxManager.addInfoBox(infobox);
			}
		}

		SwingUtilities.invokeLater(panel::refresh);
	}
}
