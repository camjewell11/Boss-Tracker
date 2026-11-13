package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.game.ItemStack;

/**
 * Pairs a boss's supplies that land on the floor with the loot event they belong to. Some bosses'
 * supplies go straight to the backpack, and the game's loot event counts only those that fit:
 * the rest drop under the player in the same game tick, uncounted. The floor spawns and the loot
 * event can arrive in either order within that tick, so whichever comes first is held for the
 * other. A supply spawning on a tick with no loot event of its boss isn't counted, so one the
 * player drops themselves isn't mistaken for a drop.
 */
final class FloorSupplies
{
	private int tick = -1;
	private Boss boss;
	private final List<ItemStack> heldSpawns = new ArrayList<>();
	private boolean lootArrived;

	/**
	 * A supply of {@code boss}'s landed on the floor.
	 *
	 * @return the stack to count now, if this tick's loot event has already arrived; else null,
	 * as it's held until the loot event comes
	 */
	ItemStack onSpawn(Boss boss, int tick, ItemStack stack)
	{
		startTick(boss, tick);
		if (lootArrived)
		{
			return stack;
		}
		heldSpawns.add(stack);
		return null;
	}

	/**
	 * {@code boss}'s loot event arrived.
	 *
	 * @return the supplies that landed on the floor earlier this tick, to count with it
	 */
	List<ItemStack> onLoot(Boss boss, int tick)
	{
		startTick(boss, tick);
		lootArrived = true;
		List<ItemStack> spawns = new ArrayList<>(heldSpawns);
		heldSpawns.clear();
		return spawns;
	}

	private void startTick(Boss boss, int tick)
	{
		if (tick != this.tick || boss != this.boss)
		{
			this.tick = tick;
			this.boss = boss;
			heldSpawns.clear();
			lootArrived = false;
		}
	}
}
