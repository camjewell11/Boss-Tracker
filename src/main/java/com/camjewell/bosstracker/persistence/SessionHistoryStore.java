package com.camjewell.bosstracker.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
		File file = entryFile(accountHash, entry);
		file.getParentFile().mkdirs();

		try
		{
			Files.write(file.toPath(), gson.toJson(entry).getBytes(StandardCharsets.UTF_8));
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
			try
			{
				String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
				SessionHistoryEntry entry = gson.fromJson(json, SessionHistoryEntry.class);
				if (entry != null)
				{
					entries.add(entry);
				}
			}
			catch (IOException | JsonSyntaxException e)
			{
				log.warn("Failed to load session history entry from {}", file.getName(), e);
			}
		}

		entries.sort(Comparator.comparingLong(SessionHistoryEntry::getEndedAtEpochMilli).reversed());
		return entries;
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
