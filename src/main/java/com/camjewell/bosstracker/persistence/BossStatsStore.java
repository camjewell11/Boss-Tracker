package com.camjewell.bosstracker.persistence;

import com.camjewell.bosstracker.boss.Boss;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Reads and writes per-boss {@link BossStats} JSON files under
 * {@code RUNELITE_DIR/boss-tracker/<accountHash>/<boss>.json}. Every method here performs
 * blocking file IO and must only be called from a background thread, never the client thread.
 */
@Slf4j
@Singleton
public class BossStatsStore
{
	private final Gson gson;

	@Inject
	public BossStatsStore(Gson gson)
	{
		this.gson = gson;
	}

	public BossStats load(long accountHash, Boss boss)
	{
		BossStats stats = read(statsFile(accountHash, boss));
		for (String legacyName : boss.getLegacyNames())
		{
			File legacy = statsFile(accountHash, legacyName);
			if (!legacy.exists())
			{
				continue;
			}
			stats.mergeFrom(read(legacy));
			save(accountHash, boss, stats);
			if (!legacy.renameTo(new File(legacy.getPath() + ".merged")))
			{
				log.warn("Failed to retire merged stats file {}", legacy);
			}
			log.info("Merged {} stats into {}", legacyName, boss.getBossName());
		}
		return stats;
	}

	private BossStats read(File file)
	{
		BossStats stats = JsonFiles.read(gson, file, BossStats.class);
		return stats != null ? stats : new BossStats();
	}

	public void save(long accountHash, Boss boss, BossStats stats)
	{
		try
		{
			JsonFiles.write(statsFile(accountHash, boss), gson.toJson(stats));
		}
		catch (IOException e)
		{
			log.warn("Failed to save boss stats for {}", boss.getBossName(), e);
		}
	}

	public void delete(long accountHash, Boss boss)
	{
		File file = statsFile(accountHash, boss);
		if (file.exists() && !file.delete())
		{
			log.warn("Failed to delete boss stats file for {}", boss.getBossName());
		}
	}

	private File statsFile(long accountHash, Boss boss)
	{
		return statsFile(accountHash, boss.getBossName());
	}

	private File statsFile(long accountHash, String bossName)
	{
		File accountDir = new File(new File(RuneLite.RUNELITE_DIR, "boss-tracker"), String.valueOf(accountHash));
		return new File(accountDir, bossName + ".json");
	}
}
