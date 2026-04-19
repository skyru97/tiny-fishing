# Tiny Fishing

Tiny Fishing is a focused Hytale fishing mod built around a short, cozy, and readable loop.

Cast into water, wait for the bite, reel at the right time, and slowly fill out a codex of fish across different biomes.

![Tiny Fishing hero loop](docs/media/hero-loop.gif)

## Quick Start

1. Craft a `Fishing Rod` in pocket crafting (`Tab` menu).
2. Equip the rod and cast into water.
3. Wait for the bite splash and sound.
4. Reel during the bite window.
5. To open the Codex, hold the rod, aim away from water, and right-click.
6. Press `Esc` to close the Codex.

## Download & Installation

CurseForge is the recommended way to download and install Tiny Fishing.

CurseForge page: link to be added.

You can also download the release jar from GitHub Releases.

Place `tiny-fishing-1.0.jar` in your world's `mods` folder and then start or reload the world.

Example paths:

- macOS: `~/Library/Application Support/Hytale/UserData/Saves/<YourWorldName>/mods`
- Windows: `%AppData%/Hytale/UserData/Saves/<YourWorldName>/mods`
- Linux: `~/.local/share/Hytale/UserData/Saves/<YourWorldName>/mods`

If you build from source, run:

```bash
./gradlew jar
```

The jar will be created at:

`build/libs/tiny-fishing-1.0.jar`

## Fishing Codex

The Codex is built around the vanilla fish currently available in Hytale and is intended to expand as vanilla adds more fish over time.

- fish are split across different zones and biomes
- every fish has a fixed slot
- undiscovered fish stay hidden as gaps
- border color reflects the best quality you have ever caught for that fish

![Fishing Codex overview](docs/media/codex-overview.png)

![Fishing Codex preview](docs/media/codex-preview.gif)

## Features

- simple cast, wait, and reel fishing flow
- no fishing minigame
- new fish and new best quality discovery alerts
- biome-based fish progression through the Codex
- region-based fish and trash tables
- global prize pool with gems, ores, and eternal crop seeds

## Loot & Crafting

Loot balance:

- fish: `70%`
- trash: `20%`
- prize: `10%`

The `Fishing Rod` is crafted in pocket crafting with:

- `Stick x2`
- `Fibre x2`
- `Wild Berries x1`

![Fishing Rod crafting](docs/media/crafting-fishing-rod.png)

## Commands

- `/tf codex`
- `/tf reset`

## Links

- `CHANGELOG.md`
- `docs/release/RELEASE_NOTES_1.0.md`
- `LICENSE`
- `NOTICE`
- `docs/dev/DESIGN.md`
- `docs/dev/CONTENT_GUIDE.md`
- `docs/dev/TECHNICAL.md`
