# Create: Rail Palette

Minecraft Java 1.21.1 · NeoForge · JDK 21

Install this addon on the server and every client, together with **Create 6.0.x**
and Create's normal dependencies.

## Painting

- Hold any vanilla dye and right-click a Create bogey to paint its wheels.
- Both standard small and large bogeys are supported.
- Colors remain visible when the train is assembled and survive saving.
- You can also right-click an assembled train directly at a bogey.
- Right-click a track to paint that track and its curve connections.
- Sneak-right-click a track to paint its entire connected railway.
- Branches, junctions, slopes, diagonals, and Bezier connections are followed.
- Mere physical proximity is not a connection: track endpoints must match.
- Repainting uses the newly selected dye.
- Creative mode does not consume dye.
- Survival mode consumes one dye per action, not one per track block.

Track coloring multiplies the original texture colors, retaining their shading.
Bogey coloring affects wheel partial models rather than the bogey frame.
No new blocks, recipes, items, or replacement textures are needed.

## Large railways

Whole-railway painting is a persistent server job. It is spread over multiple
ticks, loads a bounded number of chunks per tick, and resumes after a restart.
There is no arbitrary maximum railway length. Jobs execute in order, so two
players cannot unpredictably alternate colors on the same line.

The server configuration is generated in the world's serverconfig directory.

- `tracksPerTick`: default 128
- `chunkLoadsPerTick`: default 4
- `jobsPerPlayer`: default 4
- `consumeDye`: default true

## Commands

- `/railpalette status`
- `/railpalette cancel`
- `/railpalette jobs` — permission level 2

Cancelling stops further painting; it does not undo already painted tracks.

## Rendering integration

The addon wraps Create track baked models and normal curve vertex consumers.
For Flywheel curves, it applies colors to the actual curve instances.
Bogey wheel colors are applied to both buffered and instanced wheel models.

Create is a mandatory runtime dependency. The integration uses Create's stable
6.0.x class boundaries through string-targeted mixins and a narrow reflection
adapter, so its published development artifacts are not needed to compile this
addon. Renderer hooks are checked when the corresponding classes load.

## Build

Run `gradle build --no-daemon` with Gradle 8.10.2 and JDK 21.
The mod jar is written to `build/libs/`.
