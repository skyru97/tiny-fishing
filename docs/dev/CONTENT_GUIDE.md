# Content Guide

Fishing content is authored through JSON files in `tinyfishing/fishing/`:

- `rods.json`: rod tuning and item ids
- `fish.json`: fish ids, reward item ids, and fishdex links
- `codex.json`: fishdex labels, icons, and hints
- `fishing-regions.json`: region pools for fish, trash, and prize rewards

Guidelines:

- use exact vanilla environment ids
- keep fish ids aligned with codex entry ids
- keep region pools small and readable
- prefer vanilla items and straightforward reward tables over layered progression data
