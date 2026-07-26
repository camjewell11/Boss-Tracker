package com.camjewell.bosstracker.persistence;

import com.camjewell.bosstracker.boss.Boss;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
		File file = statsFile(accountHash, boss);
		if (!file.exists())
		{
			return new BossStats();
		}

		try
		{
			String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			BossStats stats = gson.fromJson(json, BossStats.class);
			return stats != null ? stats : new BossStats();
		}
		catch (IOException | JsonSyntaxException e)
		{
			log.warn("Failed to load boss stats for {}", boss.getBossName(), e);
			return new BossStats();
		}
	}

	public void save(long accountHash, Boss boss, BossStats stats)
	{
		File file = statsFile(accountHash, boss);
		file.getParentFile().mkdirs();

		try
		{
			Files.write(file.toPath(), gson.toJson(stats).getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.warn("Failed to save boss stats for {}", boss.getBossName(), e);
		}
	}

	private File statsFile(long accountHash, Boss boss)
	{
		File accountDir = new File(new File(RuneLite.RUNELITE_DIR, "boss-tracker"), String.valueOf(accountHash));
		return new File(accountDir, boss.getBossName() + ".json");
	}
}
