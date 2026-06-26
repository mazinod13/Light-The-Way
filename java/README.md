# Light the Way — JavaFX port

A full Java/JavaFX port of the web game (the original vanilla-JS version lives one folder up). Same gameplay, menus, level select, settings, procedural audio, save/unlocks, and the dark main-menu torch reveal.

## Requirements

A **JDK 17+ that includes JavaFX**. The simplest is an **Azul Zulu "FX" build** (e.g. `zulu23...-ca-fx-...`), which bundles the JavaFX modules — no separate JavaFX SDK or Maven needed.

Check whether your JDK bundles JavaFX:

```
java --list-modules | findstr javafx
```

If you see `javafx.controls`, `javafx.graphics`, etc., you're set.

## Run (Windows, easiest)

```powershell
cd java
./run.ps1
```

`run.ps1` auto-detects a JavaFX-enabled JDK under `C:\Program Files\Java` (or uses `JAVA_HOME`), compiles to `out/`, and launches the game.

## Run (manual / any OS)

From the `java/` folder:

```bash
# compile
javac --add-modules javafx.controls,javafx.media -d out $(find src -name "*.java")

# run  (use ';' between classpath entries on Windows, ':' on macOS/Linux)
java  --add-modules javafx.controls,javafx.media -cp "out;resources" lighttheway.App
```

### If your JDK does NOT bundle JavaFX

Download the JavaFX SDK from https://openjfx.io and add the module path:

```bash
javac --module-path "PATH\TO\javafx-sdk\lib" --add-modules javafx.controls,javafx.media -d out (sources)
java  --module-path "PATH\TO\javafx-sdk\lib" --add-modules javafx.controls,javafx.media -cp "out;resources" lighttheway.App
```

## Controls

| Input | Action |
|-------|--------|
| Mouse move | Aim your torch / guide the wanderer |
| `T` | Toggle the torch on the main menu |
| `Esc` | Pause / resume |
| `R` | Restart the current level |
| HUD buttons | Sound / Restart / Pause |

## Project layout

```
java/
  run.ps1
  src/lighttheway/
    App.java            JavaFX app: game loop, state machine, menus, torch reveal
    Config.java         constants
    Mazes.java          curated campaign mazes
    MazeGen.java        seeded procedural generator
    Levels.java         campaign + endless + difficulty scaling
    Pathfinder.java     BFS shortest path
    Renderer.java       maze/exit drawing + coord helpers (Canvas)
    Torch.java          darkness overlay + flickering light (radial gradient)
    CharacterSprite.java sprite-sheet walk animation
    Save.java           progress + settings via Preferences API
    Sound.java          procedural audio via javax.sound (no asset files)
  resources/
    lighttheway/app.css JavaFX theme
    assets/sprites/llama.png
```

## Notes on the port

- **`localStorage` → `java.util.prefs.Preferences`** (stored per-user; "Reset" in Settings clears it).
- **Web Audio → `javax.sound.sampled`**: tones are synthesised into PCM buffers; the ambient drone streams on a background thread. No audio files.
- **CSS masks → JavaFX `Canvas` + `RadialGradient`/`LinearGradient`**: the gameplay torch and the main-menu reveal/idle-sweep are painted on canvas overlays.
- **Camera** keeps the wanderer centered; the window is resizable and the canvases track its size.
