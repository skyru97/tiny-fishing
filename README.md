# Tiny Fishing

Tiny Fishing is a slim Hytale fishing mod built around a short active loop:

1. Equip the rod.
2. Cast into water.
3. Wait for the bite.
4. Reel during the bite window.
5. Receive fish, trash, or prize rewards.
6. Fill a simple fishdex as you discover catches.

Current scope:

- one rod
- one bobber
- vanilla fish pools
- simple discovery catalog UI
- no XP, levels, or progression systems

## Local Development

Prerequisites:

- local `HytaleServer.jar`
- Java 25 available to Gradle toolchains

Useful commands:

```bash
./gradlew compileJava
./gradlew test
./gradlew jar
./gradlew deployToHytaleSaveMods
```

Override local paths with:

- `-Dhytale.server.jar=/path/to/HytaleServer.jar`
- `-Dhytale.save.mods.dir=/path/to/save/mods`

Game data for the mod lives under `src/main/resources/tinyfishing/fishing/`.
