package com.camjewell.bosstracker.persistence;

import com.camjewell.bosstracker.boss.Boss;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
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
		BossLoot loot = read(lootFile(accountHash, boss));
		for (String legacyName : boss.getLegacyNames())
		{
			File legacy = lootFile(accountHash, legacyName);
			if (!legacy.exists())
			{
				continue;
			}
			loot.mergeFrom(read(legacy));
			save(accountHash, boss, loot);
			if (!legacy.renameTo(new File(legacy.getPath() + ".merged")))
			{
				log.warn("Failed to retire merged loot file {}", legacy);
			}
			log.info("Merged {} loot into {}", legacyName, boss.getBossName());
		}
		return loot;
	}

	private BossLoot read(File file)
	{
		BossLoot loot = JsonFiles.read(gson, file, BossLoot.class);
		return loot != null ? loot : new BossLoot();
	}

	public void save(long accountHash, Boss boss, BossLoot loot)
	{
		try
		{
			JsonFiles.write(lootFile(accountHash, boss), gson.toJson(loot));
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
		return lootFile(accountHash, boss.getBossName());
	}

	private File lootFile(long accountHash, String bossName)
	{
		File accountDir = new File(new File(RuneLite.RUNELITE_DIR, "boss-tracker"), String.valueOf(accountHash));
		File lootDir = new File(accountDir, "loot");
		return new File(lootDir, bossName + ".json");
	}
}
