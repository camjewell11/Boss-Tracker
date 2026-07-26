package com.camjewell.bosstracker.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * All-time persisted loot for a single boss: item ID to total quantity received, plus the set of
 * item IDs the user has chosen to hide from the loot grid. One JSON file per boss per account,
 * separate from {@link BossStats} since loot data grows unbounded while stats stay fixed-size.
 */
@Data
@NoArgsConstructor
public class BossLoot
{
	private Map<Integer, Integer> itemQuantities = new HashMap<>();
	private Set<Integer> ignoredItemIds = new HashSet<>();
}
