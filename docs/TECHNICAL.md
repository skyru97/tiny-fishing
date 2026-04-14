# Technical

Key technical decisions:

- plugin implemented as a standalone `JavaPlugin` under `com.tinyfishing`
- compile/test classpath pinned to local `HytaleServer.jar`
- runtime logic stays server-authoritative
- discovered fish entries persist through a custom ECS component
- content lives in JSON under `src/main/resources/tinyfishing/fishing/`
- command entrypoint is `/tf`

The first implementation pass uses runtime-verified Hytale APIs for:

- `PlayerMouseButtonEvent`
- `CommandRegistry`
- `EventRegistry`
- `PageManager`
- `HudManager`
- `ParticleUtil`
- `SoundUtil`
