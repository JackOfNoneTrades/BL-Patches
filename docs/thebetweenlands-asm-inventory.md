# Betweenlands ASM Inventory

This is the current upstream Betweenlands bytecode patch surface that still needs to be migrated to mixins.

## Loading entrypoints

- `thebetweenlands.core.TheBetweenlandsLoadingPlugin`
  - Always registers `TheBetweenlandsClassTransformer`
  - Optionally registers `OpenGLDebug` when `-Dbl.glDebug=true`
- `thebetweenlands.core.OpenGLDebug`
  - Global OpenGL call interceptor that injects `checkThrowError()` after most LWJGL static calls

## Main transformer

All of the following live in `thebetweenlands.core.TheBetweenlandsClassTransformer`.

### `net.minecraft.server.MinecraftServer`

- Adds public field `sleepPerTick`
- Initializes it to `50L` in the constructor
- Replaces the hardcoded `50L` sleep constant in `run()` with the field

### `net.minecraft.entity.EntityLivingBase`

- Rewrites the first method call inside `setRevengeTarget(...)` to `BLForgeHooks.onLivingSetRevengeTarget`

### `net.minecraft.client.Minecraft`

- `startGame()`: injects `DebugHandlerClient.onMinecraftFinishedStarting()` before return
- `runGameLoop()`: when An Extra Touch is not present, replaces the inside-opaque-block third-person assignment with `Perspective.getInsideOpaqueBlockView()`
- `runTick()`: when An Extra Touch is not present, rewrites perspective cycling to `Perspective.cyclePerspective()`
- `func_147116_af()` / `handlePlayerAttackInput()`: replaces the whole body with `BLForgeHooksClient.handlePlayerAttackInput`
- `func_147115_a(boolean)` / `handleBlockBreakingInput(boolean)`: replaces the whole body with `BLForgeHooksClient.handleBlockBreakingInput`

### `net.minecraft.client.gui.GuiScreen`

- `handleInput()`: injects `DebugHandlerClient.INSTANCE.onKeyInput(null)` after `handleKeyboardInput()`

### `net.minecraft.server.management.ServerConfigurationManager`

- `createPlayerForUser(GameProfile)`: replaces the whole body with `BLForgeHooks.createPlayerForUser`

### `net.minecraft.client.renderer.ActiveRenderInfo`

- When An Extra Touch is not present, `updateRenderInfo(...)` is trimmed down and finishes by calling `Perspective.updateRenderInfo(...)`

### `net.minecraft.client.renderer.EntityRenderer`

- `orientCamera(float)`: when An Extra Touch is not present, replaces the whole body with `Perspective.orient`
- `renderWorld(float,long)`:
  - when An Extra Touch is not present, offsets viewer position by `ActiveRenderInfo.objectX/Y/Z`
  - injects `BLForgeHooksClient.postPreRenderEntitiesEvent()`
  - injects `BLForgeHooksClient.postPostRenderEntitiesEvent()`
  - adds field `currentFrustum` and rewrites the local frustum storage to expose it there
- `getMouseOver(float)`: injects `BLForgeHooksClient.getMouseOverHook()` early-return gate
- `renderHand(float,int)`: injects `BLForgeHooksClient.postRenderHandEvent(float,int)` before return

### `net.minecraft.client.renderer.entity.RenderManager`

- When An Extra Touch is not present, `cacheActiveRenderInfo(...)` injects `Perspective.cacheActiveRenderInfo()` before return

### `net.minecraft.entity.Entity`

- When An Extra Touch is not present, replaces `setAngles(float,float)` with `Perspective.setAngles(...)`

### `net.minecraft.entity.EntityTracker`

- `addEntityToTracker(Entity,int,int,boolean)`: swaps the constructed `EntityTrackerEntry` class to `SuperbEntityTrackerEntry`

### `net.minecraft.command.CommandWeather`

- Upstream contains a deliberately bogus corruption branch guarded by `/*!*/true/*!*/`
- Our BL workspace preprocessing already rewrites that guard to `false`, so it does not survive into generated sources

## Notes for the rewrite

- The perspective-related hooks are already partially disabled when An Extra Touch is present, so those are good early mixin rewrite candidates.
- `Minecraft`, `EntityRenderer`, and `ItemRenderer` are still the highest-value mixin targets because they currently carry the most invasive overwrite-style ASM.
- `OpenGLDebug` is independent from the gameplay hooks and can be migrated or dropped on its own timeline.
