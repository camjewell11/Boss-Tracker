package com.camjewell.bosstracker.persistence;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JsonFilesTest
{
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final Gson gson = new Gson();

	@Test
	public void writeReplacesTheFileAndLeavesNoTemp() throws IOException
	{
		File file = new File(folder.getRoot(), "loot/Zulrah.json");
		JsonFiles.write(file, gson.toJson(stats(10)));
		JsonFiles.write(file, gson.toJson(stats(11)));

		assertEquals(11, JsonFiles.read(gson, file, BossStats.class).getKillsTracked());
		assertFalse(new File(file.getPath() + JsonFiles.TEMP_SUFFIX).exists());
	}

	@Test
	public void missingFileIsNull()
	{
		assertNull(JsonFiles.read(gson, new File(folder.getRoot(), "Vorkath.json"), BossStats.class));
	}

	@Test
	public void halfWrittenFileIsMovedAsideNotLost() throws IOException
	{
		File file = new File(folder.getRoot(), "Yama.json");
		String truncated = gson.toJson(stats(500)).substring(0, 8);
		Files.write(file.toPath(), truncated.getBytes(StandardCharsets.UTF_8));

		assertNull(JsonFiles.read(gson, file, BossStats.class));
		assertFalse(file.exists());
		File aside = new File(file.getPath() + JsonFiles.CORRUPT_SUFFIX);
		assertEquals(truncated, new String(Files.readAllBytes(aside.toPath()), StandardCharsets.UTF_8));
	}

	@Test
	public void emptyFileIsMovedAside() throws IOException
	{
		File file = new File(folder.getRoot(), "Cerberus.json");
		assertTrue(file.createNewFile());

		assertNull(JsonFiles.read(gson, file, BossStats.class));
		assertTrue(new File(file.getPath() + JsonFiles.CORRUPT_SUFFIX).exists());
	}

	@Test
	public void anEarlierCorruptCopyIsKept() throws IOException
	{
		File file = new File(folder.getRoot(), "Sarachnis.json");
		File first = new File(file.getPath() + JsonFiles.CORRUPT_SUFFIX);
		Files.write(first.toPath(), "first".getBytes(StandardCharsets.UTF_8));
		Files.write(file.toPath(), "second".getBytes(StandardCharsets.UTF_8));

		assertNull(JsonFiles.read(gson, file, BossStats.class));
		assertEquals("first", new String(Files.readAllBytes(first.toPath()), StandardCharsets.UTF_8));
		assertEquals(2, folder.getRoot().listFiles((dir, name) -> name.endsWith(JsonFiles.CORRUPT_SUFFIX)).length);
	}

	private static BossStats stats(int kills)
	{
		BossStats stats = new BossStats();
		stats.setKillsTracked(kills);
		return stats;
	}
}
