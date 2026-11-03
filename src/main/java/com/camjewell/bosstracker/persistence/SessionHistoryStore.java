package com.camjewell.bosstracker.persistence;

import com.google.gson.Gson;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Reads and writes {@link SessionHistoryEntry} files, one per completed session, under
 * {@code RUNELITE_DIR/boss-tracker/<accountHash>/history/}. Every method here performs blocking
 * file IO and must only be called from a background thread, never the client thread.
 */
@Slf4j
@Singleton
public class SessionHistoryStore
{
	private final Gson gson;

	@Inject
	public SessionHistoryStore(Gson gson)
	{
		this.gson = gson;
	}

	public void save(long accountHash, SessionHistoryEntry entry)
	{
		try
		{
			JsonFiles.write(entryFile(accountHash, entry), gson.toJson(entry));
		}
		catch (IOException e)
		{
			log.warn("Failed to save session history entry for {}", entry.getBossName(), e);
		}
	}

	/**
	 * @return every stored history entry for this account, newest first.
	 */
	public List<SessionHistoryEntry> loadAll(long accountHash)
	{
		File historyDir = historyDir(accountHash);
		File[] files = historyDir.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".json"));
		List<SessionHistoryEntry> entries = new ArrayList<>();

		if (files == null)
		{
			return entries;
		}

		for (File file : files)
		{
			// An unreadable file is moved aside, so it isn't retried on every History open.
			SessionHistoryEntry entry = JsonFiles.read(gson, file, SessionHistoryEntry.class);
			if (entry != null)
			{
				entries.add(entry);
			}
		}

		entries.sort(Comparator.comparingLong(SessionHistoryEntry::getEndedAtEpochMilli).reversed());
		return entries;
	}

	/**
	 * @return the most recently ended session, or null if there is none. Files are named by
	 * when the session ended, so only the newest one is read.
	 */
	public SessionHistoryEntry loadNewest(long accountHash)
	{
		File[] files = historyDir(accountHash).listFiles((FilenameFilter) (dir, name) -> name.endsWith(".json"));
		if (files == null)
		{
			return null;
		}

		File newest = null;
		long newestEndedAt = Long.MIN_VALUE;
		for (File file : files)
		{
			long endedAt = endedAt(file);
			if (endedAt > newestEndedAt)
			{
				newest = file;
				newestEndedAt = endedAt;
			}
		}
		return newest != null ? JsonFiles.read(gson, newest, SessionHistoryEntry.class) : null;
	}

	/** The end time an entry's file name starts with, or Long.MIN_VALUE for a file not named that way. */
	private static long endedAt(File file)
	{
		String name = file.getName();
		int underscore = name.indexOf('_');
		try
		{
			return Long.parseLong(underscore > 0 ? name.substring(0, underscore) : "");
		}
		catch (NumberFormatException e)
		{
			return Long.MIN_VALUE;
		}
	}

	public void delete(long accountHash, SessionHistoryEntry entry)
	{
		File file = entryFile(accountHash, entry);
		if (file.exists() && !file.delete())
		{
			log.warn("Failed to delete session history entry file {}", file.getName());
		}
	}

	private File historyDir(long accountHash)
	{
		File accountDir = new File(new File(RuneLite.RUNELITE_DIR, "boss-tracker"), String.valueOf(accountHash));
		return new File(accountDir, "history");
	}

	private File entryFile(long accountHash, SessionHistoryEntry entry)
	{
		String safeBossName = entry.getBossName().replaceAll("[^a-zA-Z0-9]+", "_");
		return new File(historyDir(accountHash), entry.getEndedAtEpochMilli() + "_" + safeBossName + ".json");
	}
}
