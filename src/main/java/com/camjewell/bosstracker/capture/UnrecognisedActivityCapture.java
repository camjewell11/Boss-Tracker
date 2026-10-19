package com.camjewell.bosstracker.capture;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.loot.LootBossMatcher;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.Text;

/**
 * Records encounters the plugin doesn't recognise yet, such as a newly released boss or raid, so
 * they can be added from real game messages and their first runs backfilled into History.
 *
 * <p>Anything shaped like a kill count ("Your ... count is: N") that no known boss claims opens a
 * capture window. Game messages for a short while after it (where a duration or completion time
 * would be), loot no boss claims, and interfaces opened while the window lasts are all recorded,
 * one JSON object per line, to {@code RUNELITE_DIR/boss-tracker/<accountHash>/unrecognised.jsonl}
 * and the debug log. Nothing is tracked from them; this only keeps the evidence.
 */
@Slf4j
@Singleton
public class UnrecognisedActivityCapture
{
	private static final Pattern KILL_COUNT = Pattern.compile("^Your .+ count is: ?[\\d,]+");

	/** How long after an unrecognised kill count its follow-up messages are recorded. */
	private static final long CHAT_WINDOW_MILLIS = 30_000;

	/** Raid chests are often looted a while after the completion message, so loot waits longer. */
	private static final long LOOT_WINDOW_MILLIS = 15 * 60_000;

	@Inject
	private Client client;

	@Inject
	private Gson gson;

	private Executor asyncExecutor;
	private long lastUnrecognisedKcAt;

	public void setAsyncExecutor(Executor executor)
	{
		this.asyncExecutor = executor;
	}

	/** Called from the client thread for every game message the plugin sees. */
	public void onChatMessage(String rawMessage)
	{
		String message = Text.removeTags(rawMessage);
		long now = System.currentTimeMillis();

		if (isUnrecognisedKillCount(rawMessage))
		{
			lastUnrecognisedKcAt = now;
			log.info("Unrecognised kill count, captured for a future boss: \"{}\"", message);
			Map<String, Object> record = record("killCount", now);
			record.put("message", rawMessage);
			record.put("regionId", currentRegionId());
			record.put("mapRegions", client.getTopLevelWorldView().getMapRegions());
			write(record);
			return;
		}

		if (now - lastUnrecognisedKcAt <= CHAT_WINDOW_MILLIS)
		{
			log.debug("Captured follow-up message: \"{}\"", message);
			Map<String, Object> record = record("chat", now);
			record.put("message", rawMessage);
			write(record);
		}
	}

	/**
	 * Called for every NPC or event loot RuneLite reports. Loot any known boss (or God Wars
	 * minion) claims is skipped; other loot is recorded while a capture window is open.
	 */
	public void onLootReceived(String name, String type, Collection<ItemStack> items)
	{
		long now = System.currentTimeMillis();
		if (now - lastUnrecognisedKcAt > LOOT_WINDOW_MILLIS || isKnownLoot(name))
		{
			return;
		}

		Map<String, Integer> quantities = new LinkedHashMap<>();
		for (ItemStack stack : items)
		{
			quantities.merge(String.valueOf(stack.getId()), stack.getQuantity(), Integer::sum);
		}
		log.info("Unrecognised loot, captured for a future boss: name='{}' type={} items={}", name, type, quantities);
		Map<String, Object> record = record("loot", now);
		record.put("name", name);
		record.put("type", type);
		record.put("items", quantities);
		record.put("regionId", currentRegionId());
		write(record);
	}

	/**
	 * Called for every interface that opens. While a capture window is open, records its group
	 * ID: a new raid's reward chest may not be reported as loot until RuneLite supports it, and
	 * this identifies its interface.
	 */
	public void onWidgetLoaded(int groupId)
	{
		long now = System.currentTimeMillis();
		if (now - lastUnrecognisedKcAt > LOOT_WINDOW_MILLIS)
		{
			return;
		}
		log.debug("Captured interface opened: group {}", groupId);
		Map<String, Object> record = record("interface", now);
		record.put("groupId", groupId);
		write(record);
	}

	/** A kill-count-shaped message no known boss claims, e.g. a boss added in a game update. */
	static boolean isUnrecognisedKillCount(String rawMessage)
	{
		return KILL_COUNT.matcher(Text.removeTags(rawMessage)).find() && !isKnownKillCount(rawMessage);
	}

	/** Every boss, ignoring the Dagannoth Kings selector: an unselected king is still known. */
	private static boolean isKnownKillCount(String message)
	{
		for (Boss boss : Boss.values())
		{
			for (String identifier : boss.getKcChatIdentifiers())
			{
				if (message.contains(identifier))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isKnownLoot(String name)
	{
		for (Boss boss : Boss.values())
		{
			if (LootBossMatcher.matchesLootEvent(boss, name) || LootBossMatcher.matchesMinionLootEvent(boss, name))
			{
				return true;
			}
		}
		return false;
	}

	private int currentRegionId()
	{
		Player player = client.getLocalPlayer();
		LocalPoint location = player != null ? player.getLocalLocation() : null;
		return location != null ? WorldPoint.fromLocalInstance(client, location).getRegionID() : -1;
	}

	private static Map<String, Object> record(String kind, long now)
	{
		Map<String, Object> record = new LinkedHashMap<>();
		record.put("kind", kind);
		record.put("at", now);
		return record;
	}

	private void write(Map<String, Object> record)
	{
		if (asyncExecutor == null)
		{
			return;
		}
		long accountHash = client.getAccountHash();
		String line = gson.toJson(record) + System.lineSeparator();
		asyncExecutor.execute(() ->
		{
			File accountDir = new File(new File(RuneLite.RUNELITE_DIR, "boss-tracker"), String.valueOf(accountHash));
			File file = new File(accountDir, "unrecognised.jsonl");
			file.getParentFile().mkdirs();
			try
			{
				Files.write(file.toPath(), line.getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			}
			catch (IOException e)
			{
				log.warn("Failed to write unrecognised activity record", e);
			}
		});
	}
}
