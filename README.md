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
  notification when the goal is reached.
- **Loot tracking**: a per-boss loot grid (session or all-time, toggle via config) with item
  icons, quantities, Grand Exchange values, GP/kill, and GP/hour, plus a per-item "ignore" option
  to hide junk drops from the grid.
- **Session history log**: every completed session (ended manually, via `!End`, by an inactivity
  timeout, or by switching to a different boss) is saved to a collapsible, deletable log in its
  own side panel tab — kills, KPH, average/fastest kill, idle time, session time, and a mini loot
  grid per entry. History is stored as one JSON file per session under
  `.runelite/boss-tracker/<accountHash>/history/`.
- Automatic session pause on logout, auto-resume when combat-relevant chat activity is seen while
  paused, and an optional inactivity timeout to auto-end long-idle sessions.
- Chat commands: `!Info`, `!End`, `!Pause`, `!Resume`.

Searching/browsing stats for a boss you aren't currently tracking is planned but not yet
implemented.

## Configuration

Settings are grouped into four sections:

- **Display Options** — overlay/infobox toggles and which stats they show.
- **General Settings** — side panel position, kill-duration chat messages, session timeout, KPH
  calculation method, and the Dagannoth Kings tracking selector (individual king vs. combined).
- **Boss Goals** — goal panel/overlay toggles, relative vs. absolute kill count display, which
  stats the goal overlay's two rows show, and whether reaching a goal sends a chat notification.
- **Loot Display** — whether the loot grid shows this session's loot or all-time loot for the
  currently tracked boss.

## Building

Requires a JDK compatible with RuneLite's `example-plugin` template (Java 11 target).

```bash
./gradlew shadowJar   # Build the plugin JAR
./gradlew run         # Launch a RuneLite dev client with the plugin loaded
./gradlew test        # Run tests
```

`./gradlew run` launches an unauthenticated development client — log in via a
[Jagex account](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to test in-game.

## License

BSD-2-Clause — see [LICENSE](LICENSE).
