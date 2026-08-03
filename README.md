# Boss Tracker

A RuneLite plugin that tracks kills-per-hour, session stats, boss goals, and loot across Old
School RuneScape bosses.

Spiritual successor to [BossingInfo](https://github.com/Mrnice98/BossingInfo), rebuilt from
scratch on top of RuneLite's `example-plugin` template rather than forked, with an eye toward
fixing some of the original's rough edges: JSON persistence instead of a fragile fixed-line
`.txt` format, all file IO moved off the client thread, and a data-driven boss registry instead
of dozens of scattered per-boss special cases.

## Features

- **Kill detection** for ~60 bosses/raids/minigames, whether they report their own kill count in
  chat (raids, Gauntlet, Colosseum, etc.) or need to be timed from the first hitsplat to death
  (GWD generals, wilderness bosses, etc.).
- **Session tracking**: kills this session, kills-per-hour (4 selectable calculation methods),
  average/fastest kill time, idle time, and session time — shown in an above-chatbox overlay, an
  infobox, and the side panel.
- **ACTUAL vs. VIRTUAL calc mode**: ACTUAL counts idle/travel time between kills against your
  KPH; VIRTUAL only counts time spent actually fighting, as if kills happened back-to-back.
  Toggle it from the side panel.
- **Boss goals**: set a kill-count target for the boss you're currently tracking from the side
  panel (right-click the goals section), watch progress via a progress bar, kills done/left, KPH,
  and time-to-goal — in the side panel and an optional overlay — and get a one-time chat
  notification when the goal is reached. Optionally also set a loot-value (GP) goal in the same
  dialog for a one-time chat notification when that boss's all-time loot value crosses the
  threshold.
- **Loot tracking**: a per-boss loot grid (session or all-time, toggle via config) with item
  icons, quantities, Grand Exchange values (shown per-item in the hover tooltip), GP/kill, and an
  abbreviated GP/hour (e.g. `1.23m/hr`), plus a per-item "ignore" option to hide junk drops from
  the grid.
- **Session history log**: every completed session (ended manually, via `!End`, by an inactivity
  timeout, by switching to a different boss, or by closing the client mid-session) is saved to a
  collapsible log in its own side panel tab — kills, KPH, average/fastest kill, idle time, session
  time, and a mini loot grid per entry (see Slayer task detection below). Expanded entries stay
  expanded when you switch tabs and back, and a "Collapse All" button clears them in one click.
  Each entry can be deleted (with a confirmation prompt). History is stored as one JSON file per
  session under `.runelite/boss-tracker/<accountHash>/history/`.
- **Slayer task detection**: if a kill counts toward your current Slayer task, that session is
  flagged for the rest of its duration and shown with the Slayer skill icon in the History tab.
  Works automatically if the Slayer plugin is enabled; if it isn't, sessions simply aren't flagged.
- **Boss search**: look up all-time stats and loot for any boss by name or alias (e.g. "cox",
  "vetion", "General Graardor") from the side panel's Search tab, whether or not you're currently
  tracking it — total KC, kills tracked, average KPH, fastest kill, total tracked time, GP/kill,
  total GP, and a loot grid — with a "Delete Data" option (with a confirmation prompt, disabled
  when there's nothing recorded yet) to wipe a boss's saved stats and loot.
- Automatic session pause on logout, auto-resume when combat-relevant chat activity is seen while
  paused, and an optional inactivity timeout to auto-end long-idle sessions. Closing the RuneLite
  client entirely while a session is paused (rather than explicitly ending it) still persists that
  session's stats and history instead of losing them.
- Chat commands: `!Info`, `!End`, `!Pause`, `!Resume`.

## Configuration

Settings are grouped into four sections:

- **Display Options** — overlay/infobox toggles and which stats they show.
- **General Settings** — side panel position, kill-duration chat messages, session timeout, KPH
  calculation method, and the Dagannoth Kings tracking selector (individual king vs. combined).
- **Boss Goals** — goal panel/overlay toggles, relative vs. absolute kill count display, which
  stats the goal overlay's two rows show, and whether reaching a goal sends a chat notification.
- **Loot Display** — whether the loot grid shows this session's loot or all-time loot for the
  currently tracked boss.

## How to Use

Enable the plugin and open its side panel from the RuneLite toolbar icon. The panel has three
tabs:

- **Session** — the live view. Kill a tracked boss (see the boss list in Features above) and a
  session starts automatically — no setup required. From here you can Pause/Resume/End the
  session, switch between ACTUAL and VIRTUAL KPH calc mode, and view the loot grid for the boss
  you're currently tracking.
  - **Boss goals**: right-click the goals section to set a target end kill count and/or a loot
    value (GP) goal for the boss you're currently tracking. Progress (bar, KPH, time-to-goal,
    current/target GP) updates live, and you'll get a one-time chat notification when a goal is
    reached (if enabled in config). Right-click again to reset a goal.
  - **Loot grid**: right-click an item to ignore/unignore it (useful for hiding junk drops from
    GP totals). Toggle the `lootDisplayMode` config to switch the grid between this session's
    loot and the boss's all-time loot.
- **History** — every session you've completed (ended manually, via `!End`, by timing out, by
  switching bosses, or by closing the client mid-session) is logged here. Click an entry to
  expand its stats and loot (a Slayer skill icon appears if the session was fought on a matching
  Slayer task); click the ✕ and confirm to remove one, or use "Collapse All" to close every
  expanded entry at once.
- **Search** — look up all-time stats and loot for *any* tracked boss by name or alias (e.g.
  "cox", "vetion", "kbd"), whether or not you're currently tracking it. Includes a "Delete Data"
  button below the result (disabled if there's nothing recorded yet) to wipe a boss's saved stats
  and loot, with a confirmation prompt.

Chat commands work at any time: `!Info` announces your current session, `!End` ends it, `!Pause`
and `!Resume` control it manually. All display/behavior details (which stats show, KPH
calculation method, goal notifications, etc.) are configurable — see Configuration above.

## Building

Requires a JDK compatible with RuneLite's `example-plugin` template (Java 11 target).

```bash
./gradlew shadowJar   # Build the plugin JAR
./gradlew run         # Launch a RuneLite dev client with the plugin loaded
./gradlew test        # Run tests
```

`./gradlew run` launches an unauthenticated development client — log in via a
[Jagex account](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to test in-game.

If `./gradlew run`/`test` (or IntelliJ's Gradle integration) fails to resolve
`:testRuntimeClasspath` with an error like "Could not find lwjgl-*-natives-linux-arm64.jar", it's
caused by a stale/incomplete `mavenLocal()` (`~/.m2/repository`) cache silently shadowing the
real repositories for that dependency — this build no longer declares `mavenLocal()` for exactly
that reason. If you still hit it (e.g. via a different local Gradle config), removing the
incomplete `org.lwjgl` entries from `~/.m2/repository` or clearing that repo from your Gradle
setup resolves it.

## License

BSD-2-Clause — see [LICENSE](LICENSE).
