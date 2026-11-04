package io.github.aykanyilmz.hexgame.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.Subscription;
import io.github.aykanyilmz.hexgame.ai.HexAI;
import io.github.aykanyilmz.hexgame.model.Board;
import io.github.aykanyilmz.hexgame.model.Difficulty;
import io.github.aykanyilmz.hexgame.model.GameConfig;
import io.github.aykanyilmz.hexgame.model.Player;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The match screen: board, player panels, toolbar, status pill and end-of-game flow. */
public class GameView extends StackPane {

    private static final ExecutorService AI_POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "hex-ai");
        t.setDaemon(true);
        return t;
    });
    private static final int SWAP = -1;

    private final Runnable onMenu;
    private final Runnable onFullScreen;
    private final Random rng = new Random();

    private GameConfig config;
    private Board board;
    private final int[] seatScore = new int[2];
    private final long[] timeMs = new long[2];
    private long lastTick;
    private boolean gameOver;
    private boolean aiBusy;
    private int token;

    private final BorderPane layout = new BorderPane();
    private final BoardView boardView = new BoardView();
    private final PlayerCard redCard = new PlayerCard(Player.RED);
    private final PlayerCard blueCard = new PlayerCard(Player.BLUE);
    private final Label title = new Label();
    private final Label scoreLine = new Label();
    private final Circle statusDot = new Circle(6);
    private final Label statusText = new Label();
    private final HBox statusPill = new HBox(10, statusDot, statusText);
    private final Button swapBtn = textButton("game.swap", Icons.SWAP, Color.WHITE);
    private final Button rematchBtn = textButton("game.rematch", Icons.RESTART, Color.WHITE);
    private final Button undoBtn = iconButton(Icons.UNDO, "game.undo");
    private final Button hintBtn = iconButton(Icons.BULB, "game.hint");
    private final Timeline clock;
    private final Subscription languageSubscription;
    private Modal winModal;

    public GameView(GameConfig config, Runnable onMenu, Runnable onFullScreen) {
        this.config = config;
        this.onMenu = onMenu;
        this.onFullScreen = onFullScreen;
        getStyleClass().add("game-view");

        layout.setTop(buildTopBar());
        layout.setCenter(buildCenter());
        layout.setBottom(buildBottom());
        getChildren().add(layout);

        boardView.setOnCellClick(this::humanMove);
        boardView.setTurnSupplier(() -> board.currentPlayer());

        clock = new Timeline(new KeyFrame(Duration.millis(200), e -> tickClock()));
        clock.setCycleCount(Animation.INDEFINITE);

        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isShortcutDown() && e.getCode() == KeyCode.Z) {
                undo();
                e.consume();
            } else if (e.getCode() == KeyCode.H && !e.isShortcutDown()) {
                hint();
                e.consume();
            } else if (e.getCode() == KeyCode.R && !e.isShortcutDown()) {
                newGame(false);
                e.consume();
            }
        });
        setFocusTraversable(true);
        sceneProperty().addListener((o, a, s) -> {
            if (s != null) {
                Platform.runLater(this::requestFocus);
            }
        });

        newGame(false);

        // bound labels translate themselves; composed texts are rebuilt here
        languageSubscription = I18n.onChange(() -> {
            updateTitle();
            refresh();
        });
    }

    // ------------------------------------------------------------------ layout

    private Region buildTopBar() {
        Button back = textButton("game.menu", Icons.BACK, Theme.TEXT);
        back.getStyleClass().add("btn-ghost");
        back.setOnAction(e -> leave());
        Fx.interactive(back, 1.04);

        title.getStyleClass().add("topbar-title");
        scoreLine.getStyleClass().add("muted");
        VBox center = new VBox(2, title, scoreLine);
        center.setAlignment(Pos.CENTER);

        Button restart = iconButton(Icons.RESTART, "game.restart");
        restart.setOnAction(e -> newGame(false));
        Button full = iconButton(Icons.FULLSCREEN, "game.fullscreen");
        full.setOnAction(e -> onFullScreen.run());
        undoBtn.setOnAction(e -> undo());
        hintBtn.setOnAction(e -> hint());
        HBox actions = new HBox(10, hintBtn, undoBtn, restart, full, new LanguageSwitch());
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region l = new Region();
        Region r = new Region();
        HBox.setHgrow(l, Priority.ALWAYS);
        HBox.setHgrow(r, Priority.ALWAYS);
        HBox left = new HBox(back);
        left.setAlignment(Pos.CENTER_LEFT);
        actions.setMinWidth(220);
        left.minWidthProperty().bind(actions.widthProperty()); // keeps the title centred
        HBox bar = new HBox(left, l, center, r, actions);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(18, 28, 6, 28));
        Fx.reveal(bar, 0, -16);
        return bar;
    }

    private Region buildCenter() {
        HBox.setHgrow(boardView, Priority.ALWAYS);
        boardView.setMaxWidth(Double.MAX_VALUE);
        boardView.setMaxHeight(Double.MAX_VALUE);
        VBox leftCol = new VBox(redCard);
        VBox rightCol = new VBox(blueCard);
        leftCol.setAlignment(Pos.CENTER);
        rightCol.setAlignment(Pos.CENTER);
        HBox center = new HBox(12, leftCol, boardView, rightCol);
        center.setPadding(new Insets(4, 28, 4, 28));
        center.setAlignment(Pos.CENTER);
        Fx.reveal(leftCol, 120, 24);
        Fx.reveal(rightCol, 200, 24);
        return center;
    }

    private Region buildBottom() {
        statusPill.getStyleClass().add("status-pill");
        statusPill.setAlignment(Pos.CENTER);
        statusText.getStyleClass().add("status-text");

        swapBtn.getStyleClass().add("btn-accent");
        swapBtn.setOnAction(e -> humanSwap());
        swapBtn.setTooltip(tip("game.swap.tooltip"));
        Fx.interactive(swapBtn, 1.05);
        rematchBtn.getStyleClass().add("btn-accent");
        rematchBtn.setOnAction(e -> rematch());
        Fx.interactive(rematchBtn, 1.05);
        setShown(swapBtn, false);
        setShown(rematchBtn, false);

        HBox bottom = new HBox(14, statusPill, swapBtn, rematchBtn);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(6, 28, 22, 28));
        Fx.reveal(bottom, 260, 20);
        return bottom;
    }

    private static Button iconButton(String path, String tooltipKey) {
        Button b = new Button(null, Icons.icon(path, 20, Theme.TEXT));
        b.getStyleClass().addAll("btn-ghost", "icon-btn");
        b.setTooltip(tip(tooltipKey));
        Fx.interactive(b, 1.08);
        return b;
    }

    /** A button with an icon and a caption that follows the interface language. */
    private static Button textButton(String key, String icon, javafx.scene.paint.Paint iconFill) {
        Button b = new Button(null, Icons.icon(icon, 18, iconFill));
        b.textProperty().bind(I18n.text(key));
        return b;
    }

    private static Tooltip tip(String key) {
        Tooltip t = new Tooltip();
        t.textProperty().bind(I18n.text(key));
        t.setShowDelay(Duration.millis(250));
        return t;
    }

    private static void setShown(Region node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    // ------------------------------------------------------------------ game flow

    private void newGame(boolean keepIntroQuick) {
        token++;
        aiBusy = false;
        if (winModal != null) {
            winModal.close();
            winModal = null;
        }
        board = new Board(config.size());
        gameOver = false;
        timeMs[0] = 0;
        timeMs[1] = 0;
        lastTick = System.nanoTime();
        updateTitle();
        boardView.build(board, true);
        clock.play();
        refresh();
        PauseTransition settle = new PauseTransition(Duration.millis(keepIntroQuick ? 200 : 650));
        int t = token;
        settle.setOnFinished(e -> {
            if (t == token) {
                maybeAiTurn();
            }
        });
        settle.play();
    }

    /** The player's name, with untouched default names shown in the current language. */
    private String nameOf(Player p) {
        return I18n.playerName(config.nameOf(p));
    }

    private void updateTitle() {
        String mode = config.mode() == GameConfig.Mode.PVP
                ? I18n.get("game.title.pvp")
                : I18n.get("game.title.ai", I18n.get("difficulty." + config.difficulty().name()));
        title.setText(config.size() + "×" + config.size() + "  ·  " + mode
                + (config.swapRule() ? "  ·  " + I18n.get("game.title.swap") : ""));
    }

    private void rematch() {
        config = config.withSeatAColor(config.seatAColor().opponent());
        newGame(true);
    }

    private void humanMove(int row, int col) {
        if (gameOver || aiBusy || config.isAI(board.currentPlayer()) || !board.isEmpty(row, col)) {
            return;
        }
        board.play(row, col);
        afterMove();
    }

    private void humanSwap() {
        if (gameOver || aiBusy || !swapAvailable() || config.isAI(board.currentPlayer())) {
            return;
        }
        board.swap();
        afterMove();
    }

    private boolean swapAvailable() {
        return config.swapRule() && board.canSwap();
    }

    private void afterMove() {
        tickClock();
        boardView.sync(board, true);
        Player winner = board.winner();
        if (winner != null) {
            finish(winner);
            return;
        }
        refresh();
        maybeAiTurn();
    }

    private void maybeAiTurn() {
        Player me = board.currentPlayer();
        if (gameOver || !config.isAI(me)) {
            boardView.setInteractive(!gameOver);
            refresh();
            return;
        }
        aiBusy = true;
        boardView.setInteractive(false);
        refresh();
        int myToken = ++token;
        int[] snapshot = board.toCodes();
        int n = board.size();
        boolean canSwap = swapAvailable();
        int[] first = canSwap ? new int[]{board.history().get(0).row(), board.history().get(0).col()} : null;
        long started = System.nanoTime();
        CompletableFuture.supplyAsync(() -> {
            if (canSwap && HexAI.shouldSwap(n, first[0], first[1], config.difficulty(), rng)) {
                return new int[]{SWAP, SWAP};
            }
            return HexAI.chooseMove(snapshot, n, me.code(), config.difficulty(), config.swapRule(), rng);
        }, AI_POOL).whenComplete((move, error) -> Platform.runLater(() -> {
            long elapsed = (System.nanoTime() - started) / 1_000_000;
            PauseTransition pause = new PauseTransition(Duration.millis(Math.max(0, 550 - elapsed)));
            pause.setOnFinished(e -> applyAiMove(myToken, move, error));
            pause.play();
        }));
    }

    private void applyAiMove(int myToken, int[] move, Throwable error) {
        if (myToken != token) {
            return;
        }
        aiBusy = false;
        if (error != null || move == null) {
            boardView.setInteractive(true);
            refresh();
            return;
        }
        if (move[0] == SWAP) {
            board.swap();
            toast(I18n.get("game.toast.aiSwapped"));
        } else {
            board.play(move[0], move[1]);
        }
        afterMove();
    }

    private void undo() {
        if (!canUndo()) {
            return;
        }
        token++;
        aiBusy = false;
        if (gameOver) {
            reopenFinishedGame();
        }
        board.undo();
        while (config.mode() == GameConfig.Mode.AI && board.moveCount() > 0 && config.isAI(board.currentPlayer())) {
            board.undo();
        }
        boardView.sync(board, true);
        refresh();
        maybeAiTurn();
    }

    /** Takes back a finished game's result so its last move can be undone. */
    private void reopenFinishedGame() {
        Player winner = board.winner();
        if (winner != null) {
            seatScore[config.seatOf(winner)]--;
        }
        gameOver = false;
        if (winModal != null) {
            Modal m = winModal;
            winModal = null;
            m.close();
        }
        boardView.resetCelebration();
        lastTick = System.nanoTime();
        clock.play();
    }

    private boolean canUndo() {
        if (board.moveCount() == 0) {
            return false;
        }
        if (config.mode() == GameConfig.Mode.PVP) {
            return true;
        }
        return board.history().stream().anyMatch(m -> !config.isAI(m.player()));
    }

    private void hint() {
        if (gameOver || aiBusy || config.isAI(board.currentPlayer())) {
            return;
        }
        int myToken = token;
        int[] snapshot = board.toCodes();
        int n = board.size();
        int me = board.currentPlayer().code();
        if (swapAvailable()) {
            Board.Move first = board.history().get(0);
            if (HexAI.shouldSwap(n, first.row(), first.col())) {
                toast(I18n.get("game.toast.hintSwap"));
                Fx.popIn(swapBtn, 0);
                return;
            }
        }
        boolean openingWithSwap = config.swapRule() && board.moveCount() == 0;
        hintBtn.setDisable(true);
        CompletableFuture.supplyAsync(() -> HexAI.chooseMove(snapshot, n, me,
                        Difficulty.MEDIUM, openingWithSwap, rng), AI_POOL)
                .whenComplete((move, error) -> Platform.runLater(() -> {
                    hintBtn.setDisable(false);
                    if (error == null && move != null && myToken == token && !gameOver) {
                        boardView.showHint(move[0], move[1]);
                    }
                }));
    }

    private void finish(Player winner) {
        gameOver = true;
        token++;
        clock.stop();
        boardView.setInteractive(false);
        seatScore[config.seatOf(winner)]++;
        refresh();
        (winner == Player.RED ? redCard : blueCard).bumpScore();
        List<int[]> path = board.winningPath(winner);
        boardView.celebrate(path, winner, () -> showWinner(winner, path.size()));
    }

    private void showWinner(Player winner, int pathLength) {
        if (!gameOver) {
            return;
        }
        StackPane trophy = new StackPane();
        Polygon hex = Fx.hexagon(52);
        hex.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.light(winner)), new Stop(1, Theme.dark(winner))));
        hex.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.color(winner), 40, 0.3, 0, 0));
        trophy.getChildren().addAll(hex, Icons.icon(Icons.TROPHY, 46, Color.WHITE));
        boolean humanLost = config.mode() == GameConfig.Mode.AI && config.isAI(winner);
        Label kicker = new Label();
        kicker.textProperty().bind(I18n.text(humanLost ? "win.kicker.lost" : "win.kicker.won"));
        kicker.getStyleClass().add("section-title");
        Text name = new Text(nameOf(winner));
        name.getStyleClass().add("win-name");
        name.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.light(winner)), new Stop(1, Theme.color(winner))));
        Label sub = new Label();
        sub.textProperty().bind(I18n.text(humanLost ? "win.sub.lost" : "win.sub.won"));
        sub.getStyleClass().add("win-sub");

        HBox stats = new HBox(
                statBox(String.valueOf(board.movesBy(winner)), "win.stat.moves"),
                statBox(String.valueOf(pathLength), "win.stat.chain"),
                statBox(Fx.clock(timeMs[0] + timeMs[1]), "win.stat.time"),
                statBox(seatScore[0] + " – " + seatScore[1], "win.stat.score"));
        stats.setSpacing(10);
        stats.setAlignment(Pos.CENTER);

        Button again = textButton("win.rematch", Icons.RESTART, Color.WHITE);
        again.getStyleClass().add("btn-primary");
        again.setMaxWidth(Double.MAX_VALUE);
        again.setDefaultButton(true);
        again.setOnAction(e -> rematch());
        Fx.interactive(again, 1.03);

        Button view = textButton("win.review", Icons.EYE, Theme.TEXT);
        view.getStyleClass().add("btn-ghost");
        Button menu = textButton("win.menu", Icons.HOME, Theme.TEXT);
        menu.getStyleClass().add("btn-ghost");
        menu.setOnAction(e -> leave());
        Fx.interactive(view, 1.04);
        Fx.interactive(menu, 1.04);
        Button takeBack = textButton("win.undo", Icons.UNDO, Theme.TEXT);
        takeBack.getStyleClass().add("btn-ghost");
        takeBack.setOnAction(e -> undo());
        Fx.interactive(takeBack, 1.04);
        HBox secondary = new HBox(10, view, takeBack, menu);
        for (Button b : List.of(view, takeBack, menu)) {
            HBox.setHgrow(b, Priority.ALWAYS);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPrefWidth(1); // equal share of the row
        }

        VBox body = new VBox(14, trophy, kicker, name, sub, stats, again, secondary);
        body.setAlignment(Pos.CENTER);
        body.setPrefWidth(460);
        VBox.setMargin(stats, new Insets(8, 0, 8, 0));

        winModal = new Modal(layout, false, body);
        Modal thisModal = winModal;
        view.setOnAction(e -> thisModal.close());
        winModal.setOnClosed(() -> {
            boardView.resumeLoops();
            if (winModal == thisModal) {
                winModal = null;
            }
        });
        boardView.pauseLoops();
        winModal.show(this);
        int i = 0;
        for (var node : body.getChildren()) {
            Fx.reveal(node, 120 + i++ * 70, 14);
        }
    }

    private static VBox statBox(String value, String captionKey) {
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label c = new Label();
        c.textProperty().bind(I18n.text(captionKey));
        c.getStyleClass().add("stat-label");
        VBox box = new VBox(2, v, c);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stat-chip");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void leave() {
        dispose();
        onMenu.run();
    }

    public void dispose() {
        languageSubscription.unsubscribe();
        token++;
        clock.stop();
        redCard.stopAnimations();
        blueCard.stopAnimations();
    }

    // ------------------------------------------------------------------ UI state

    private void tickClock() {
        long now = System.nanoTime();
        if (!gameOver && board != null) {
            int idx = board.currentPlayer() == Player.RED ? 0 : 1;
            // the clock starts with the first stone so the intro animation is free
            if (board.moveCount() > 0 || config.isAI(board.currentPlayer())) {
                timeMs[idx] += (now - lastTick) / 1_000_000;
            }
            redCard.setTime(timeMs[0]);
            blueCard.setTime(timeMs[1]);
        }
        lastTick = now;
    }

    private void refresh() {
        Player turn = board.currentPlayer();
        redCard.update(nameOf(Player.RED), config.isAI(Player.RED), board.movesBy(Player.RED),
                timeMs[0], seatScore[config.seatOf(Player.RED)]);
        blueCard.update(nameOf(Player.BLUE), config.isAI(Player.BLUE), board.movesBy(Player.BLUE),
                timeMs[1], seatScore[config.seatOf(Player.BLUE)]);
        redCard.setActive(!gameOver && turn == Player.RED);
        blueCard.setActive(!gameOver && turn == Player.BLUE);
        redCard.setThinking(aiBusy && turn == Player.RED);
        blueCard.setThinking(aiBusy && turn == Player.BLUE);

        String a = I18n.playerName(config.nameA());
        String b = config.mode() == GameConfig.Mode.AI ? GameConfig.AI_NAME : I18n.playerName(config.nameB());
        scoreLine.setText(a + "  " + seatScore[0] + " – " + seatScore[1] + "  " + b);

        boolean humanTurn = !gameOver && !aiBusy && !config.isAI(turn);
        undoBtn.setDisable(!canUndo());
        hintBtn.setDisable(!humanTurn);
        setShown(swapBtn, humanTurn && swapAvailable());
        setShown(rematchBtn, gameOver);

        Player shownColor = gameOver ? board.winner() : turn;
        Color color = Theme.color(shownColor);
        String text;
        if (gameOver) {
            text = I18n.get("status.won", nameOf(shownColor));
        } else if (aiBusy) {
            text = I18n.get("status.thinking", GameConfig.AI_NAME);
        } else if (swapAvailable()) {
            text = I18n.get("status.turnOrSwap", nameOf(turn));
        } else {
            text = I18n.get("status.turn", nameOf(turn));
        }
        if (!text.equals(statusText.getText())) {
            statusText.setText(text);
            ScaleTransition st = new ScaleTransition(Duration.millis(320), statusPill);
            st.setFromX(0.9);
            st.setFromY(0.9);
            st.setToX(1);
            st.setToY(1);
            st.setInterpolator(Fx.EASE_OUT_BACK);
            st.play();
        }
        statusDot.setFill(color);
        statusDot.setEffect(new DropShadow(BlurType.GAUSSIAN, color, 12, 0.5, 0, 0));
        statusPill.setStyle("-fx-border-color: " + Theme.css(color.deriveColor(0, 1, 1, 0.55)) + ";");
    }

    private void toast(String message) {
        Label t = new Label(message);
        t.getStyleClass().add("toast");
        StackPane.setAlignment(t, Pos.TOP_CENTER);
        StackPane.setMargin(t, new Insets(90, 0, 0, 0));
        t.setMouseTransparent(true);
        getChildren().add(t);
        Fx.popIn(t, 0);
        FadeTransition out = new FadeTransition(Duration.millis(400), t);
        out.setDelay(Duration.millis(1800));
        out.setToValue(0);
        out.setOnFinished(e -> getChildren().remove(t));
        out.play();
    }
}
