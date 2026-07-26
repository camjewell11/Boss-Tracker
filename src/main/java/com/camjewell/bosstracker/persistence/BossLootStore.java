package com.camjewell.bosstracker.persistence;

import com.camjewell.bosstracker.boss.Boss;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Reads and writes per-boss {@link BossLoot} JSON files under
 * {@code RUNELITE_DIR/boss-tracker/<accountHash>/loot/<boss>.json}. Every method here performs
 * blocking file IO and must only be called from a background thread, never the client thread.
 */
@Slf4j
@Singleton
public class BossLootStore
{
	private final Gson gson;

	@Inject
	public BossLootStore(Gson gson)
	{
		this.gson = gson;
	}

	public BossLoot load(long accountHash, Boss boss)
	{
		File file = lootFile(accountHash, boss);
		if (!file.exists())
		{
			return new BossLoot();
		}

		try
		{
			String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			BossLoot loot = gson.fromJson(json, BossLoot.class);
			return loot != null ? loot : new BossLoot();
		}
		catch (IOException | JsonSyntaxException e)
		{
			log.warn("Failed to load boss loot for {}", boss.getBossName(), e);
			return new BossLoot();
		}
	}

	public void save(long accountHash, Boss boss, BossLoot loot)
	{
		File file = lootFile(accountHash, boss);
		file.getParentFile().mkdirs();

		try
		{
			Files.write(file.toPath(), gson.toJson(loot).getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.warn("Failed to save boss loot for {}", boss.getBossName(), e);
		}
	}

	public void delete(long accountHash, Boss boss)
	{
		File file = lootFile(accountHash, boss);
		if (file.exists() && !file.delete())
		{
			log.warn("Failed to delete boss loot file for {}", boss.getBossName());
		}
	}

	private File lootFile(long accountHash, Boss boss)
	{
		File accountDir = new File(new File(RuneLite.RUNELITE_DIR, "boss-tracker"), String.valueOf(accountHash));
		File lootDir = new File(accountDir, "loot");
		return new File(lootDir, boss.getBossName() + ".json");
	}
}
