package com.camjewell.bosstracker.chat;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.boss.TimingFamily;
import java.util.Arrays;
import java.util.List;

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
		"Colosseum duration:"
	);

	private ChatKillParser()
	{
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
		sub = sub.replace(":", "");

		String hours = "0";
		String minutes;
		String seconds;
		switch (sub.length())
		{
			case 4:
				minutes = sub.substring(0, 2);
				seconds = sub.substring(2);
				break;
			case 5:
				hours = sub.substring(0, 1);
				minutes = sub.substring(1, 2);
				seconds = sub.substring(2);
				break;
			default:
				minutes = sub.substring(0, 1);
				seconds = sub.substring(1);
				break;
		}
		return Integer.parseInt(seconds) + Integer.parseInt(minutes) * 60 + Integer.parseInt(hours) * 3600;
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
		return null;
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
