package com.camjewell.bosstracker.session;

/**
 * How session totals feed into the kills-per-hour calculation. Renamed from the original
 * plugin's unlabelled 0/1 {@code calcMode} int for clarity.
 */
public enum CalcMode
{
	/**
	 * Real elapsed wall-clock time since the session's first kill, including any time spent
	 * idle, banking, or traveling between kills.
	 */
	ACTUAL,

	/**
	 * Only the summed duration of each individual kill, excluding idle time between them —
	 * the hypothetical rate if kills happened back-to-back with no downtime.
	 */
	VIRTUAL
}
