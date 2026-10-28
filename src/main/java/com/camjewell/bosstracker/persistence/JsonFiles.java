package com.camjewell.bosstracker.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import lombok.extern.slf4j.Slf4j;

/**
 * Crash-safe JSON files. Lifetime stats and loot are rewritten after every kill for as long as
 * the plugin is installed, so a crash or power cut mid-write must never leave a half-written
 * file, and a file that can't be read must never be overwritten with an empty one.
 */
@Slf4j
final class JsonFiles
{
	static final String TEMP_SUFFIX = ".tmp";
	static final String CORRUPT_SUFFIX = ".corrupt";

	private JsonFiles()
	{
	}

	/**
	 * Writes to a temporary file beside {@code file}, flushes it to disk, then moves it into
	 * place in one step, so a crash leaves either the old file or the new one.
	 */
	static void write(File file, String json) throws IOException
	{
		file.getParentFile().mkdirs();
		Path target = file.toPath();
		Path temp = target.resolveSibling(target.getFileName() + TEMP_SUFFIX);

		try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))
		{
			ByteBuffer bytes = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
			while (bytes.hasRemaining())
			{
				channel.write(bytes);
			}
			channel.force(true);
		}

		try
		{
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException e)
		{
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * @return the file's contents, or null if it doesn't exist. A file that exists but can't be
	 * read, is empty, or isn't valid JSON is moved aside by {@link #quarantine} first, so the
	 * caller's next save starts a new file rather than destroying the old one; null is returned
	 * then too.
	 */
	static <T> T read(Gson gson, File file, Class<T> type)
	{
		if (!file.exists())
		{
			return null;
		}

		try
		{
			String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			T value = gson.fromJson(json, type);
			if (value != null)
			{
				return value;
			}
			log.warn("{} is empty", file);
		}
		catch (IOException | JsonParseException e)
		{
			log.warn("Failed to read {}", file, e);
		}
		quarantine(file);
		return null;
	}

	/**
	 * Renames an unreadable file to {@code <name>.corrupt}, or {@code <name>.<millis>.corrupt}
	 * when that's taken, keeping its bytes for recovery by hand.
	 */
	static void quarantine(File file)
	{
		File aside = new File(file.getPath() + CORRUPT_SUFFIX);
		if (aside.exists())
		{
			aside = new File(file.getPath() + "." + System.currentTimeMillis() + CORRUPT_SUFFIX);
		}
		try
		{
			Files.move(file.toPath(), aside.toPath());
			log.warn("Moved unreadable {} aside to {}", file.getName(), aside.getName());
		}
		catch (IOException e)
		{
			log.warn("Failed to move unreadable {} aside", file, e);
		}
	}
}
