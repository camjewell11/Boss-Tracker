package com.camjewell.bosstracker.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A raid unique a teammate received and split with the group, so the player's share counts
 * toward the session's value after splits.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeammateUnique
{
	private int itemId;
	private String itemName;
	/** Players it was split between, the player included. */
	private int splitSize;
	/** Its price when it was added, so the share keeps the value it had at the time. */
	private long price;
}
