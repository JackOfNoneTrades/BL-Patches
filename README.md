# BL Patches

Patch layer for The Betweenlands 1.7.10.

This repo clones The Betweenlands from `Angry-Pixel/The-Betweenlands` branch `1.7.10`, gives you an editable `betweenlands-src/` workspace, and builds a mod jar that patches the official universal jar at runtime with `bspatch`.

## Workflow

1. Run `./gradlew runClient` or `./gradlew runServer`
2. Edit files in `betweenlands-src/`
3. Run `./gradlew generateBetweenlandsPatches`
4. Commit `patches/workspace.patch`

Do not commit `betweenlands-src/`.

## Useful tasks

- `./gradlew bootstrapBetweenlandsWorkspace`
- `./gradlew refreshBetweenlandsWorkspace`
- `./gradlew generateBetweenlandsPatches`
- `./gradlew generateBetweenlandsRuntimeArtifacts`
- `./gradlew runClient`
- `./gradlew runServer`
- `./gradlew runObfClient`
- `./gradlew runObfServer`

## Building

`./gradlew build`

## Notes

- `betweenlands-src/` and `.betweenlands/upstream/` are generated and gitignored.
- The generated workspace jar keeps The Betweenlands coremod manifest so dev runs match the official mod layout.
- Betweenlands source placeholders such as `/*!*/true/*!*/` are normalized during workspace compilation to match the original build.
