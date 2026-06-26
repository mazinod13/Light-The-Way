package lighttheway;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

import java.util.List;

/** JavaFX entry point + game loop + state machine + menus (ports js/game.js + js/menu.js). */
public class App extends Application {

    private enum State { MENU, LEVELS, SETTINGS, PLAY, PAUSE, WIN }

    // ── canvases ──────────────────────────────────────
    private Canvas gameCanvas, uiCanvas, menuVeil;
    private GraphicsContext gameGC, uiGC, veilGC;
    private StackPane root;

    // ── screens / widgets ─────────────────────────────
    private Region menuScreen, levelsScreen, settingsScreen, winScreen, pauseScreen;
    private VBox menuActions;
    private Button promptBtn;
    private GridPane levelGrid;
    private Label levelsTotal, hudLvl, hudSteps, hudBest, muteBtnLabel;
    private Button muteBtn;
    private Label winStars, winRecord, winInfo;
    private Button winNext;
    private Label setSound, setMusic;
    private Button soundToggle, musicToggle;
    private HBox setDiff;
    private BorderPane hud;

    // ── state ─────────────────────────────────────────
    private State state = State.MENU;
    private boolean torchOn = false;

    private int lvlIdx = 0;
    private int[][] maze;
    private int rows, cols, exitR, exitC;
    private int steps = 0, optimalSteps = 1;
    private double charX, charY;
    private List<int[]> path = new java.util.ArrayList<>();
    private int[] pathTarget = null;
    private long walkTick = 0;
    private boolean isMoving = false;

    private double mouseX = 0, mouseY = 0;
    private long lastNanos = 0;

    // menu sweep
    private boolean sweepActive = false;
    private double sweepT = 0;
    private double nextSweepAt = 4.5;

    @Override
    public void start(Stage stage) {
        Sound.init(Save.soundOn(), Save.musicOn());

        gameCanvas = new Canvas();
        uiCanvas = new Canvas();
        gameGC = gameCanvas.getGraphicsContext2D();
        uiGC = uiCanvas.getGraphicsContext2D();

        root = new StackPane();
        root.setStyle("-fx-background-color: black;");
        root.getChildren().addAll(gameCanvas, uiCanvas);

        buildMenu();
        buildLevels();
        buildSettings();
        buildWin();
        buildPause();
        buildHud();
        root.getChildren().addAll(menuScreen, levelsScreen, settingsScreen, winScreen, pauseScreen, hud);

        // canvases track the window size
        gameCanvas.widthProperty().bind(root.widthProperty());
        gameCanvas.heightProperty().bind(root.heightProperty());
        uiCanvas.widthProperty().bind(root.widthProperty());
        uiCanvas.heightProperty().bind(root.heightProperty());
        menuVeil.widthProperty().bind(root.widthProperty());
        menuVeil.heightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, 1040, 680, Color.BLACK);
        try {
            scene.getStylesheets().add(getClass().getResource("/lighttheway/app.css").toExternalForm());
        } catch (Exception ignored) {}

        scene.addEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        stage.setTitle("Light the Way");
        stage.setScene(scene);
        stage.show();

        showState(State.MENU);
        setTorch(false);

        new AnimationTimer() {
            @Override public void handle(long now) { tick(now); }
        }.start();
    }

    // ── MAIN LOOP ─────────────────────────────────────
    private void tick(long now) {
        if (lastNanos == 0) lastNanos = now;
        double dt = (now - lastNanos) / 1e9;
        lastNanos = now;
        double nowSec = now / 1e9;
        double w = root.getWidth(), h = root.getHeight();

        if (state == State.PLAY) {
            stepCharacter();
            double[] off = Renderer.getOffset(charX, charY, w, h);
            Renderer.drawMaze(gameGC, maze, rows, cols, off[0], off[1], exitR, exitC, now / 1e6);
            CharacterSprite.draw(gameGC, charX + off[0], charY + off[1], walkTick, isMoving);
            Torch.draw(uiGC, w, h, mouseX, mouseY);
        } else if (state == State.MENU) {
            updateSweep(nowSec, dt);
            drawMenuVeil(w, h);
        }
    }

    // ── CHARACTER MOVEMENT ────────────────────────────
    private void stepCharacter() {
        if (path.isEmpty()) { isMoving = false; return; }
        isMoving = true;
        int[] next = path.get(0);
        double[] target = Renderer.cellToWorld(next[0], next[1]);
        double dx = target[0] - charX, dy = target[1] - charY;
        double dist = Math.hypot(dx, dy);
        if (dist < Config.CHAR_SPEED + 0.5) {
            charX = target[0]; charY = target[1];
            path.remove(0);
            steps++;
            Sound.step();
            refreshHud();
            int[] cc = Renderer.worldToCell(charX, charY);
            if (cc[0] == exitR && cc[1] == exitC) winLevel();
        } else {
            charX += dx / dist * Config.CHAR_SPEED;
            charY += dy / dist * Config.CHAR_SPEED;
            walkTick++;
        }
    }

    private void repath(int tr, int tc) {
        if (tc < 0 || tr < 0 || tr >= rows || tc >= cols) return;
        if (maze[tr][tc] == 1) return;
        if (pathTarget != null && pathTarget[0] == tr && pathTarget[1] == tc) return;
        pathTarget = new int[]{tr, tc};
        int[] cc = Renderer.worldToCell(charX, charY);
        path = Pathfinder.bfs(maze, rows, cols, cc[0], cc[1], tr, tc);
    }

    // ── LEVEL FLOW ────────────────────────────────────
    private void loadLevel(int idx) {
        lvlIdx = idx;
        Levels.Level lv = Levels.get(idx);
        maze = lv.grid;
        rows = maze.length;
        cols = maze[0].length;
        exitR = lv.er; exitC = lv.ec;
        double[] sw = Renderer.cellToWorld(lv.sr, lv.sc);
        charX = sw[0]; charY = sw[1];
        path.clear(); pathTarget = null;
        steps = 0;
        List<int[]> op = Pathfinder.bfs(maze, rows, cols, lv.sr, lv.sc, exitR, exitC);
        optimalSteps = Math.max(1, op.size());
        Torch.setRadius(Levels.torchRadius(idx, Save.difficulty()));
        refreshHud();
    }

    private void playLevel(int idx) {
        loadLevel(idx);
        showState(State.PLAY);
    }

    private int starsFor(int s) {
        if (s <= Math.ceil(optimalSteps * 1.15)) return 3;
        if (s <= Math.ceil(optimalSteps * 1.6))  return 2;
        return 1;
    }

    private void winLevel() {
        int stars = starsFor(steps);
        Integer prevBest = Save.best(lvlIdx);
        Save.recordWin(lvlIdx, steps, stars);
        Sound.win();
        winStars.setText(starString(stars));
        winInfo.setText("Cleared in " + steps + " steps  ·  Best " + Save.best(lvlIdx));
        winRecord.setVisible(prevBest == null || steps < prevBest);
        winNext.setVisible(lvlIdx + 1 < Levels.TOTAL);
        showState(State.WIN);
    }

    private void pause()   { if (state == State.PLAY)  showState(State.PAUSE); }
    private void resume()  { if (state == State.PAUSE) showState(State.PLAY); }
    private void restart() { Sound.click(); playLevel(lvlIdx); }

    // ── STATE MANAGEMENT ──────────────────────────────
    private void showState(State next) {
        state = next;
        menuScreen.setVisible(next == State.MENU);
        levelsScreen.setVisible(next == State.LEVELS);
        settingsScreen.setVisible(next == State.SETTINGS);
        winScreen.setVisible(next == State.WIN);
        pauseScreen.setVisible(next == State.PAUSE);
        hud.setVisible(next == State.PLAY || next == State.PAUSE);

        if (next == State.PLAY) Sound.startMusic(); else Sound.stopMusic();

        if (next == State.MENU || next == State.LEVELS || next == State.SETTINGS) {
            gameGC.clearRect(0, 0, root.getWidth(), root.getHeight());
            uiGC.clearRect(0, 0, root.getWidth(), root.getHeight());
        }
        updateCursor();
    }

    private void updateCursor() {
        boolean hide = state == State.PLAY || (state == State.MENU && torchOn);
        root.setCursor(hide ? Cursor.NONE : Cursor.DEFAULT);
    }

    private void openLevels()   { Sound.click(); refreshLevels(); showState(State.LEVELS); }
    private void openSettings() { Sound.click(); refreshSettings(); showState(State.SETTINGS); }

    // ── MAIN-MENU TORCH ───────────────────────────────
    private void setTorch(boolean on) {
        torchOn = on;
        menuActions.setVisible(on);
        promptBtn.setVisible(!on);
        updateCursor();
    }

    private void updateSweep(double nowSec, double dt) {
        if (torchOn) { sweepActive = false; return; }
        if (!sweepActive && nowSec >= nextSweepAt) { sweepActive = true; sweepT = 0; }
        if (sweepActive) {
            sweepT += dt;
            if (sweepT >= 2.4) { sweepActive = false; nextSweepAt = nowSec + 4.5; }
        }
    }

    private void drawMenuVeil(double w, double h) {
        veilGC.clearRect(0, 0, w, h);
        if (torchOn) {
            double r = 190;
            RadialGradient g = new RadialGradient(0, 0, mouseX, mouseY, r, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(0, 0, 0, 0)),
                new Stop(0.34, Color.rgb(0, 0, 0, 0)),
                new Stop(0.66, Color.rgb(0, 0, 0, 0.55)),
                new Stop(1.00, Color.rgb(0, 0, 0, 0.95)));
            veilGC.setFill(g);
            veilGC.fillRect(0, 0, w, h);
            // flame at the cursor
            RadialGradient glow = new RadialGradient(0, 0, mouseX, mouseY, 42, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(255, 200, 90, 0.30)),
                new Stop(1.0, Color.rgb(255, 150, 40, 0)));
            veilGC.setFill(glow);
            veilGC.fillRect(mouseX - 42, mouseY - 42, 84, 84);
            veilGC.setFill(Color.rgb(255, 225, 150, 0.95));
            veilGC.fillOval(mouseX - 6, mouseY - 6, 12, 12);
        } else if (sweepActive) {
            double p = (-0.25 + 1.5 * (sweepT / 2.4));   // band centre as a fraction of width
            double hw = 0.09;
            double a = Math.max(0, Math.min(1, p - hw));
            double b = Math.max(0, Math.min(1, p));
            double c = Math.max(0, Math.min(1, p + hw));
            LinearGradient band = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0, 0, 0, 0.98)),
                new Stop(a,   Color.rgb(0, 0, 0, 0.98)),
                new Stop(b,   Color.rgb(0, 0, 0, 0.0)),
                new Stop(c,   Color.rgb(0, 0, 0, 0.98)),
                new Stop(1.0, Color.rgb(0, 0, 0, 0.98)));
            veilGC.setFill(band);
            veilGC.fillRect(0, 0, w, h);
        } else {
            veilGC.setFill(Color.rgb(0, 0, 0, 0.98));
            veilGC.fillRect(0, 0, w, h);
        }
    }

    // ── INPUT ─────────────────────────────────────────
    private void onMouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (state == State.PLAY) {
            int[] cell = Renderer.screenToCell(mouseX, mouseY, charX, charY, root.getWidth(), root.getHeight());
            repath(cell[0], cell[1]);
        } else if (state == State.MENU && !torchOn) {
            nextSweepAt = (System.nanoTime() / 1e9) + 4.5;
        }
    }

    private void onKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.T) {
            if (state == State.MENU) { Sound.click(); setTorch(!torchOn); }
        } else if (e.getCode() == KeyCode.ESCAPE) {
            if (state == State.PLAY) pause();
            else if (state == State.PAUSE) resume();
        } else if (e.getCode() == KeyCode.R) {
            if (state == State.PLAY || state == State.PAUSE) restart();
        }
    }

    // ── UI BUILDERS ───────────────────────────────────
    private Button btn(String text, String cls, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add(cls);
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        return b;
    }

    private void buildMenu() {
        Label title = new Label("Light the Way"); title.getStyleClass().add("title");
        Label desc = new Label("Move your mouse — your torch leads the way.\n"
            + "The wanderer follows the light through the dark."); desc.getStyleClass().add("desc");
        Label sub = new Label("MOVE MOUSE TO GUIDE · FIND THE EXIT ◈"); sub.getStyleClass().add("sub");
        VBox content = new VBox(14, title, desc, sub);
        content.setAlignment(Pos.CENTER);
        content.setTranslateY(-50);

        menuVeil = new Canvas();
        menuVeil.setMouseTransparent(true);
        veilGC = menuVeil.getGraphicsContext2D();

        menuActions = new VBox(12,
            btn("Enter the Dark", "btn", () -> { Sound.click(); playLevel(Math.min(Save.unlocked() - 1, Levels.TOTAL - 1)); }),
            btn("Level Select", "btn", this::openLevels),
            btn("Settings", "btn", this::openSettings));
        menuActions.setAlignment(Pos.CENTER);
        menuActions.setTranslateY(85);

        promptBtn = btn("PRESS  T  FOR TORCH", "btn-ghost", () -> { Sound.click(); setTorch(!torchOn); });
        promptBtn.getStyleClass().add("prompt");
        promptBtn.setTranslateY(85);
        StackPane.setAlignment(promptBtn, Pos.CENTER);

        // veil is mouse-transparent so it never blocks the buttons above it;
        // actions / prompt overlap but only one is ever visible (hidden = not clickable)
        StackPane sp = new StackPane(content, menuVeil, menuActions, promptBtn);
        sp.getStyleClass().add("screen");
        menuScreen = sp;
    }

    private void buildLevels() {
        Label title = new Label("Choose Your Depth"); title.getStyleClass().add("title");
        levelsTotal = new Label(); levelsTotal.getStyleClass().add("sub");
        levelGrid = new GridPane();
        levelGrid.setHgap(14); levelGrid.setVgap(14);
        levelGrid.setAlignment(Pos.CENTER);
        Button back = btn("← Back", "btn-ghost", () -> { Sound.click(); showState(State.MENU); });

        VBox box = new VBox(18, title, levelsTotal, levelGrid, back);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("screen");
        levelsScreen = box;
    }

    private void refreshLevels() {
        levelGrid.getChildren().clear();
        for (int i = 0; i < Levels.TOTAL; i++) {
            final int idx = i;
            boolean unlocked = Save.isUnlocked(i);
            Label num = new Label(String.valueOf(i + 1)); num.getStyleClass().add("lc-num");
            VBox card = new VBox(4);
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().add("level-card");
            if (Levels.isEndless(i)) card.getStyleClass().add("endless");
            if (unlocked) {
                Label st = new Label(starString(Save.stars(i))); st.getStyleClass().add("lc-stars");
                Label nm = new Label(Levels.name(i)); nm.getStyleClass().add("lc-name");
                card.getChildren().addAll(num, st, nm);
                card.setOnMouseClicked(e -> { Sound.click(); playLevel(idx); });
            } else {
                Label nm = new Label("Locked"); nm.getStyleClass().add("lc-name");
                card.getChildren().addAll(num, nm);
                card.getStyleClass().add("locked");
            }
            levelGrid.add(card, i % 4, i / 4);
        }
        levelsTotal.setText("★ " + Save.totalStars() + " / " + (Levels.TOTAL * 3) + " collected");
    }

    private void buildSettings() {
        Label title = new Label("Settings"); title.getStyleClass().add("title");

        setSound = new Label();
        soundToggle = wrapToggle(setSound, () -> {
            boolean v = !Save.soundOn(); Save.setSound(v); Sound.setSound(v);
            if (v) Sound.setMusic(Save.musicOn());
            refreshSettings(); refreshHud(); Sound.click();
        });
        setMusic = new Label();
        musicToggle = wrapToggle(setMusic, () -> {
            boolean v = !Save.musicOn(); Save.setMusic(v); Sound.setMusic(v);
            if (v && state == State.PLAY) Sound.startMusic();
            refreshSettings(); Sound.click();
        });

        setDiff = new HBox();
        setDiff.getStyleClass().add("segmented");
        for (String d : new String[]{"easy", "normal", "hard"}) {
            Button seg = new Button(d.substring(0, 1).toUpperCase() + d.substring(1));
            seg.getStyleClass().add("seg");
            seg.setFocusTraversable(false);
            seg.setUserData(d);
            seg.setOnAction(e -> { Save.setDifficulty(d); refreshSettings(); Sound.click(); });
            setDiff.getChildren().add(seg);
        }

        Button reset = btn("RESET", "toggle", () -> {
            Save.reset(); Sound.init(Save.soundOn(), Save.musicOn()); refreshSettings(); refreshHud();
        });
        reset.getStyleClass().add("danger");

        VBox list = new VBox(14,
            row("Sound effects", soundToggle),
            row("Ambient music", musicToggle),
            row("Difficulty", setDiff),
            row("Progress", reset));
        list.setMaxWidth(360);

        Button back = btn("← Back", "btn-ghost", () -> { Sound.click(); showState(State.MENU); });

        VBox box = new VBox(18, title, list, back);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("screen");
        settingsScreen = box;
    }

    private Button wrapToggle(Label label, Runnable action) {
        Button b = new Button();
        b.getStyleClass().add("toggle");
        b.setGraphic(label);
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        return b;
    }

    private HBox row(String name, Region control) {
        Label l = new Label(name); l.getStyleClass().add("set-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox h = new HBox(16, l, spacer, control);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("set-row");
        return h;
    }

    private void refreshSettings() {
        setSound.setText(Save.soundOn() ? "ON" : "OFF");
        setMusic.setText(Save.musicOn() ? "ON" : "OFF");
        soundToggle.getStyleClass().remove("on");
        if (Save.soundOn()) soundToggle.getStyleClass().add("on");
        musicToggle.getStyleClass().remove("on");
        if (Save.musicOn()) musicToggle.getStyleClass().add("on");
        for (var n : setDiff.getChildren()) {
            n.getStyleClass().remove("active");
            if (Save.difficulty().equals(n.getUserData())) n.getStyleClass().add("active");
        }
    }

    private void buildWin() {
        Label title = new Label("Escaped"); title.getStyleClass().addAll("title", "cyan");
        winStars = new Label(); winStars.getStyleClass().add("stars");
        winRecord = new Label("★ NEW BEST ★"); winRecord.getStyleClass().add("record");
        winInfo = new Label(); winInfo.getStyleClass().add("desc");
        winNext = btn("Go Deeper →", "btn", () -> { Sound.click(); playLevel(Math.min(lvlIdx + 1, Levels.TOTAL - 1)); });
        VBox actions = new VBox(12, winNext,
            btn("Replay", "btn", () -> { Sound.click(); playLevel(lvlIdx); }),
            btn("Level Select", "btn-ghost", this::openLevels));
        actions.setAlignment(Pos.CENTER);

        VBox box = new VBox(14, title, winStars, winRecord, winInfo, actions);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("screen");
        winScreen = box;
    }

    private void buildPause() {
        Label title = new Label("Paused"); title.getStyleClass().add("title");
        VBox actions = new VBox(12,
            btn("Resume", "btn", () -> { Sound.click(); resume(); }),
            btn("Restart Level", "btn", this::restart),
            btn("Level Select", "btn-ghost", this::openLevels),
            btn("Main Menu", "btn-ghost", () -> { Sound.click(); showState(State.MENU); }));
        actions.setAlignment(Pos.CENTER);
        VBox box = new VBox(18, title, actions);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("screen");
        pauseScreen = box;
    }

    private void buildHud() {
        Label titleL = new Label("Light the Way"); titleL.getStyleClass().add("hud-title");
        hudLvl = new Label("1"); hudSteps = new Label("0"); hudBest = new Label("—");
        Label stats = new Label();
        hudLvl.getStyleClass().add("hud-num"); hudSteps.getStyleClass().add("hud-num"); hudBest.getStyleClass().add("hud-num");
        HBox statsBox = new HBox(0,
            lbl("Lvl "), hudLvl, lbl("  ·  Steps "), hudSteps, lbl("  ·  Best "), hudBest);
        statsBox.setAlignment(Pos.CENTER_RIGHT);
        statsBox.getStyleClass().add("hud-stats");

        muteBtn = btn("SOUND", "icon-btn", this::toggleMute);
        HBox controls = new HBox(8,
            muteBtn,
            btn("RESTART", "icon-btn", this::restart),
            btn("PAUSE", "icon-btn", () -> { Sound.click(); pause(); }));
        controls.setAlignment(Pos.CENTER_RIGHT);

        HBox right = new HBox(18, statsBox, controls);
        right.setAlignment(Pos.CENTER_RIGHT);

        hud = new BorderPane();
        hud.getStyleClass().add("hud");
        hud.setLeft(titleL);
        hud.setRight(right);
        hud.setPadding(new Insets(14, 22, 14, 22));
        BorderPane.setAlignment(titleL, Pos.CENTER_LEFT);
        StackPane.setAlignment(hud, Pos.TOP_CENTER);
        hud.setMaxHeight(Region.USE_PREF_SIZE);
        hud.prefWidthProperty().bind(rootWidth());
    }

    private javafx.beans.binding.DoubleBinding rootWidth() {
        return root.widthProperty().add(0);
    }

    private Label lbl(String s) { Label l = new Label(s); l.getStyleClass().add("hud-stats"); return l; }

    private void refreshHud() {
        hudLvl.setText(String.valueOf(lvlIdx + 1));
        hudSteps.setText(String.valueOf(steps));
        Integer b = Save.best(lvlIdx);
        hudBest.setText(b == null ? "—" : String.valueOf(b));
        muteBtn.setText(Save.soundOn() ? "SOUND" : "MUTED");
        if (Save.soundOn()) muteBtn.getStyleClass().remove("off");
        else if (!muteBtn.getStyleClass().contains("off")) muteBtn.getStyleClass().add("off");
    }

    private void toggleMute() {
        boolean on = !Save.soundOn();
        Save.setSound(on); Sound.setSound(on);
        if (!on) Sound.setMusic(false);
        else { Sound.setMusic(Save.musicOn()); if (Save.musicOn() && state == State.PLAY) Sound.startMusic(); }
        refreshHud();
    }

    private static String starString(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) sb.append(i < n ? '★' : '☆');
        return sb.toString();
    }

    public static void main(String[] args) { launch(args); }
}
