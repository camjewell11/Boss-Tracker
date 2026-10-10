package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.BossStats;
import com.camjewell.bosstracker.persistence.BossStatsStore;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

/**
 * Learns where each boss is fought, as the set of map regions its kills have happened in, so a
 * trip can end when the player leaves. Learned rather than hand-entered: there is no reliable
 * per-boss table to maintain, and a kill proves the player was standing in the boss's area. Region
 * IDs come from instance-translated coordinates, so instanced fights resolve to stable regions too.
 */
@Slf4j
@Singleton
public class BossAreaTracker
{
	@Inject
	private BossStatsStore statsStore;

	@Inject
	private Client client;

	private Executor asyncExecutor;

	/** Boss whose regions are loaded; regions for any other boss are not held. */
	private volatile Boss boss;

	/** Written by the async load and read on the client thread, hence concurrent. */
	private final Set<Integer> regions = ConcurrentHashMap.newKeySet();

	public void setAsyncExecutor(Executor executor)
	{
		this.asyncExecutor = executor;
	}

	/**
	 * Switches to {@code target}'s learned regions, loading them asynchronously. Kills recorded
	 * before the load lands are kept: they are merged, not overwritten.
	 */
	public void ensureLoaded(Boss target)
	{
		if (target == null || target == boss)
		{
			return;
		}
		boss = target;
		regions.clear();

		if (asyncExecutor == null)
		{
			return;
		}
		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() ->
		{
			BossStats stats = statsStore.load(accountHash, target);
			if (boss == target)
			{
				regions.addAll(stats.getRegionIds());
			}
		});
	}

	/**
	 * Records that {@code target} was killed in {@code regionId}, saving it if it is new.
	 */
	public void learn(Boss target, int regionId)
	{
		ensureLoaded(target);
		if (!regions.add(regionId) || asyncExecutor == null)
		{
			return;
		}

		log.debug("Learned region {} for {}", regionId, target);
		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() ->
		{
			BossStats stats = statsStore.load(accountHash, target);
			if (stats.getRegionIds().add(regionId))
			{
				statsStore.save(accountHash, target, stats);
			}
		});
	}

	/**
	 * @return true if nothing is known about {@code target}'s area yet, or {@code regionId} is
	 * within {@link #NEARBY_REGIONS} of a learned region. Unknown counts as inside so a boss with no learned area never
	 * ends a trip.
	 */
	public boolean isInsideOrUnknown(Boss target, int regionId)
	{
		if (target != boss || regions.isEmpty())
		{
			return true;
		}
		for (int learned : regions)
		{
			if (isNearby(learned, regionId))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * How many map squares (regions) from a learned one still count as the boss's area. Fight
	 * areas don't respect region boundaries: Cerberus's lairs are 4883, 5140 and 5395 with the
	 * entrance hall 5139 between them, and the west and east lairs are two squares apart. At two,
	 * a kill in any one lair covers the whole complex, while a teleport out still lands far
	 * enough away to end the trip.
	 */
	static final int NEARBY_REGIONS = 2;

	static boolean isNearby(int a, int b)
	{
		int dx = (a >> 8) - (b >> 8);
		int dy = (a & 0xFF) - (b & 0xFF);
		return Math.abs(dx) <= NEARBY_REGIONS && Math.abs(dy) <= NEARBY_REGIONS;
	}
}
