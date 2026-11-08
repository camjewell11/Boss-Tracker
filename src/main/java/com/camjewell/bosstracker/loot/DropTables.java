package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * What each boss can drop, bundled with the plugin in {@code drop_tables.json} (generated from
 * the OSRS Wiki), so item goals can name any drop, untradeables included, before it's been had.
 */
@Slf4j
@Singleton
public class DropTables
{
	private static final String RESOURCE = "/com/camjewell/bosstracker/drop_tables.json";

	/** The resource's shape: every item's IDs by name, and each boss's drop names. */
	private static class Data
	{
		Map<String, List<Integer>> items;
		Map<String, List<String>> bosses;
	}

	private final Gson gson;
	private Data data;

	@Inject
	public DropTables(Gson gson)
	{
		this.gson = gson;
	}

	/**
	 * Each item the boss can drop by its in-game name, in the wiki's order, with every item ID
	 * it has (pets with several forms have several). Empty for a boss without a table.
	 */
	public synchronized Map<String, List<Integer>> drops(Boss boss)
	{
		Data loaded = load();
		List<String> names = loaded.bosses.get(boss.name());
		if (names == null)
		{
			return Collections.emptyMap();
		}

		Map<String, List<Integer>> drops = new LinkedHashMap<>();
		for (String name : names)
		{
			List<Integer> ids = loaded.items.get(name);
			if (ids != null)
			{
				drops.put(name, ids);
			}
		}
		return drops;
	}

	private Data load()
	{
		if (data != null)
		{
			return data;
		}

		try (InputStream in = DropTables.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				throw new IOException("missing " + RESOURCE);
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				data = gson.fromJson(reader, Data.class);
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Couldn't read the bundled drop tables", e);
		}

		if (data == null || data.items == null || data.bosses == null)
		{
			data = new Data();
			data.items = Collections.emptyMap();
			data.bosses = Collections.emptyMap();
		}
		return data;
	}
}
