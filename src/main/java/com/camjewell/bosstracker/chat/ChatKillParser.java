package com.camjewell.bosstracker.chat;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.boss.TimingFamily;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.util.Text;

/**
 * Recognizes kill-count, kill-duration, and early-start-trigger chat messages and resolves them
 * to a {@link Boss}. Stateless by design: any game-state lookups (e.g. current map region) are
 * the caller's responsibility, passed in as plain parameters.
 */
public final class ChatKillParser
{
	/**
	 * Generic chat-message prefixes that report a SELF_REPORTED boss's completion duration.
	 * Any boss using this shared set is expected to also be gated on {@link Boss#getKcChatIdentifiers()}
	 * having already matched the corresponding kill-count message.
	 */
	private static final List<String> DURATION_MESSAGE_PREFIXES = Arrays.asList(
		"Fight duration:",
		"Congratulations - your raid is complete!",
		"Corrupted challenge duration:",
		"Challenge duration:",
		"Theatre of Blood total completion time: ",
		"Tombs of Amascut: Expert Mode total completion time:",
		"Tombs of Amascut total completion time:",
		"The Fractured Archive: Total completion time:",
		"Colosseum duration:"
	);

	/**
	 * A raid death, with the run's running total for the whole team:
	 * "<col=ff0000>Axecutioner</col> has died. Total deaths: <col=ff0000>1</col>." or, for the player,
	 * "You have died. Total deaths: <col=ff0000>3</col>.". Seen in-game in the Fractured Archive.
	 */
	private static final Pattern RAID_DEATH = Pattern.compile(
		"(?:has died|^You have died)\\. Total deaths: (?:<col=[0-9a-f]+>)?([\\d,]+)");

	private ChatKillParser()
	{
	}

	/**
	 * @return true for the game's message to the player that they received a pet: following them,
	 * sneaking into their backpack, or the "would have been followed" duplicate. Other players'
	 * broadcasts use their name ("has a funny feeling like she's being followed") and don't match.
	 * The game colours the message, so tags are stripped first.
	 */
	public static boolean isOwnPetMessage(String message)
	{
		String plain = Text.removeTags(message);
		return plain.startsWith("You have a funny feeling like you")
			|| plain.startsWith("You feel something weird sneaking into your backpack");
	}

	/**
	 * @return the Boss whose kill-count message appears in this chat line, honoring the
	 * Dagannoth Kings selector so only the currently-selected king (or the combined "Kings"
	 * choice) is considered a match, or null if no boss's identifier appears.
	 */
	public static Boss findKcBoss(String message, BossTrackerConfig.DksSelector dksSelector)
	{
		Boss activeDks = dksBossForSelector(dksSelector);
		for (Boss boss : Boss.values())
		{
			if (boss.getFamily() == TimingFamily.DAGANNOTH_KINGS && boss != activeDks)
			{
				continue;
			}
			for (String identifier : boss.getKcChatIdentifiers())
			{
				if (message.contains(identifier))
				{
					return boss;
				}
			}
		}
		return null;
	}

	private static Boss dksBossForSelector(BossTrackerConfig.DksSelector selector)
	{
		switch (selector)
		{
			case REX:
				return Boss.DAGANNOTH_REX;
			case PRIME:
				return Boss.DAGANNOTH_PRIME;
			case SUPREME:
				return Boss.DAGANNOTH_SUPREME;
			case KINGS:
			default:
				return Boss.DAGANNOTH_KINGS;
		}
	}

	/**
	 * @return the kill count parsed out of a recognized kill-count chat message.
	 */
	public static int parseKillCount(String message)
	{
		String digitsOnly = message.replace("<col=ff0000>", "").replaceAll("[^0-9]", "");
		return Integer.parseInt(digitsOnly);
	}

	/**
	 * @return true if this chat line reports a SELF_REPORTED boss's completion duration.
	 */
	public static boolean isDurationMessage(String message, boolean inFightCavesOrInferno)
	{
		return narrowToDurationSegment(message, inFightCavesOrInferno) != null;
	}

	/**
	 * @return the duration in seconds parsed out of a recognized duration chat message, or null
	 * if this message isn't a recognized duration message.
	 */
	public static Integer parseDurationSeconds(String message, boolean inFightCavesOrInferno)
	{
		String segment = narrowToDurationSegment(message, inFightCavesOrInferno);
		if (segment == null)
		{
			return null;
		}

		String trimmed = segment.replaceFirst("<", "");
		int startOfTime = trimmed.indexOf(">");
		int endOfTime = trimmed.indexOf("<");
		if (startOfTime < 0 || endOfTime < 0 || endOfTime <= startOfTime)
		{
			return null;
		}
		String sub = trimmed.substring(startOfTime + 1, endOfTime);

		if (sub.contains("."))
		{
			sub = sub.substring(0, sub.indexOf("."));
		}

		// "s", "m:ss" or "h:mm:ss"; each part is worth 60 of the next.
		int total = 0;
		for (String part : sub.split(":"))
		{
			if (!part.matches("\\d+"))
			{
				return null;
			}
			total = total * 60 + Integer.parseInt(part);
		}
		return total;
	}

	/**
	 * Narrows a chat message down to the substring containing the "<col=...>mm:ss</col>"-style
	 * duration value, skipping past any preceding colored segments (e.g. raid completion
	 * messages that print personal points before the duration).
	 */
	private static String narrowToDurationSegment(String message, boolean inFightCavesOrInferno)
	{
		if (message.contains("Fight duration:"))
		{
			return message.substring(message.indexOf("Fight duration:"));
		}
		if (inFightCavesOrInferno && message.contains("Duration:"))
		{
			return message;
		}
		if (message.contains("Congratulations - your raid is complete!"))
		{
			int idx = message.indexOf("Duration:</col>");
			return idx >= 0 ? message.substring(idx + "Duration:</col>".length()) : message;
		}
		if (message.contains("Corrupted challenge duration:"))
		{
			return message;
		}
		if (message.contains("Challenge duration:"))
		{
			return message;
		}
		if (message.contains("Theatre of Blood total completion time: "))
		{
			int idx = message.indexOf("time: ");
			return idx >= 0 ? message.substring(idx + "time: ".length()) : message;
		}
		if (message.contains("Tombs of Amascut total completion time:")
			|| message.contains("Tombs of Amascut: Expert Mode total completion time:"))
		{
			return message;
		}
		if (message.contains("Colosseum duration:"))
		{
			return message;
		}
		if (message.contains("The Fractured Archive: Total completion time:"))
		{
			return message;
		}
		return null;
	}

	/** @return true for the message each Fractured Archive run starts with. */
	public static boolean isFracturedArchiveStart(String message)
	{
		return message.startsWith("You enter the Archive");
	}

	/** @return true when the whole team has died, ending a Fractured Archive run without a clear. */
	public static boolean isFracturedArchiveWipe(String message)
	{
		return message.startsWith("You failed to survive the Fractured Archive");
	}

	/**
	 * @return the team's total deaths so far this run from a raid death message, or null if the
	 * message isn't one
	 */
	public static Integer parseRaidTotalDeaths(String message)
	{
		Matcher m = RAID_DEATH.matcher(message);
		return m.find() ? Integer.parseInt(m.group(1).replace(",", "")) : null;
	}

	/** @return true when a raid death message is the player's own. */
	public static boolean isOwnRaidDeath(String message)
	{
		return message.startsWith("You have died. Total deaths:");
	}

	/**
	 * @return true if any recognized kill-count or duration message appears in this chat line —
	 * used to auto-resume a paused session on relevant chat activity.
	 */
	public static boolean isRecognizedBossMessage(String message, BossTrackerConfig.DksSelector dksSelector, boolean inFightCavesOrInferno)
	{
		return findKcBoss(message, dksSelector) != null || isDurationMessage(message, inFightCavesOrInferno);
	}

	/**
	 * @return the Boss whose earlyStartTrigger chat substring appears in this message, or null.
	 */
	public static Boss findEarlyStartBoss(String message)
	{
		for (Boss boss : Boss.values())
		{
			if (boss.getEarlyStartTrigger() != null && message.contains(boss.getEarlyStartTrigger()))
			{
				return boss;
			}
		}
		return null;
	}
}
