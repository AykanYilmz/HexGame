package org.example.hexgame.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import org.example.hexgame.model.Board;
import org.example.hexgame.model.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Renders a {@link Board} as a scalable rhombus of hexagons with animated stones.
 * The view never mutates the board; call {@link #sync(Board, boolean)} after every change.
 */
public class BoardView extends Region {

    private static final double R = 30;
    private static final double PAD = 24;

    private final Group content = new Group();
    private final Group edgeLayer = new Group();
    private final Group cellLayer = new Group();
    private final Group labelLayer = new Group();
    private final Group stoneLayer = new Group();
    private final Group fxLayer = new Group();
    private final Scale scale = new Scale(1, 1, 0, 0);

    private final Polygon ghost = Fx.hexagon(R * 0.78);
    private final Circle lastMarker = new Circle(R * 0.15, Color.rgb(255, 255, 255, 0.9));
    private final List<Animation> loops = new ArrayList<>();

    private int n;
    private Polygon[] cells = new Polygon[0];
    private Node[] stones = new Node[0];
    private Player[] shown = new Player[0];
    private double minX, minY, naturalW, naturalH;

    private boolean interactive = true;
    private BiConsumer<Integer, Integer> onCellClick;
    private Supplier<Player> turnSupplier = () -> Player.RED;
    private Node hint;
    private Animation hintAnim;
    private int hoverIndex = -1;

    public BoardView() {
        getStyleClass().add("board");
        content.getTransforms().add(scale);
        content.getChildren().addAll(edgeLayer, cellLayer, labelLayer, stoneLayer, fxLayer);
        stoneLayer.setMouseTransparent(true);
        fxLayer.setMouseTransparent(true);
        labelLayer.setMouseTransparent(true);
        edgeLayer.setMouseTransparent(true);
        ghost.setMouseTransparent(true);
        ghost.setVisible(false);
        ghost.getStrokeDashArray().setAll(6.0, 5.0);
        ghost.setStrokeWidth(2);
        lastMarker.setMouseTransparent(true);
        lastMarker.setVisible(false);
        lastMarker.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(255, 255, 255, 0.8), 8, 0.3, 0, 0));
        getChildren().add(content);
        setMinSize(200, 200);
    }

    // ------------------------------------------------------------------ public API

    public void setOnCellClick(BiConsumer<Integer, Integer> handler) {
        this.onCellClick = handler;
    }

    public void setTurnSupplier(Supplier<Player> supplier) {
        this.turnSupplier = supplier;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        getStyleClass().remove("locked");
        if (!interactive) {
            getStyleClass().add("locked");
            ghost.setVisible(false);
        } else if (hoverIndex >= 0) {
            showGhost(hoverIndex);
        }
    }

    /** Rebuilds the grid for {@code board} and plays the intro wave. */
    public void build(Board board, boolean intro) {
        loops.forEach(Animation::stop);
        loops.clear();
        clearHint();
        n = board.size();
        cells = new Polygon[n * n];
        stones = new Node[n * n];
        shown = new Player[n * n];
        edgeLayer.getChildren().clear();
        cellLayer.getChildren().clear();
        labelLayer.getChildren().clear();
        stoneLayer.getChildren().clear();
        fxLayer.getChildren().clear();
        hoverIndex = -1;

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int idx = r * n + c;
                Polygon cell = Fx.hexagon(cx(r, c), cy(r), R - 1.6);
                cell.getStyleClass().add("hex-cell");
                final int fr = r;
                final int fc = c;
                cell.setOnMouseEntered(e -> {
                    hoverIndex = idx;
                    showGhost(idx);
                });
                cell.setOnMouseExited(e -> {
                    if (hoverIndex == idx) {
                        hoverIndex = -1;
                        ghost.setVisible(false);
                    }
                });
                cell.setOnMouseClicked(e -> {
                    if (!interactive || onCellClick == null) {
                        return;
                    }
                    if (shown[idx] != null) {
                        Fx.shake(stones[idx]);
                        return;
                    }
                    onCellClick.accept(fr, fc);
                });
                cells[idx] = cell;
                cellLayer.getChildren().add(cell);
            }
        }
        buildEdges();
        buildLabels();
        stoneLayer.getChildren().addAll(ghost, lastMarker);
        computeNaturalBounds();
        sync(board, false);
        if (intro) {
            playIntro();
        }
        requestLayout();
    }

    /** Brings the displayed stones in line with the board, animating any differences. */
    public void sync(Board board, boolean animate) {
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int idx = r * n + c;
                Player want = board.get(r, c);
                if (shown[idx] != want) {
                    if (stones[idx] != null) {
                        removeStone(idx, animate);
                    }
                    if (want != null) {
                        addStone(idx, want, animate);
                    }
                    shown[idx] = want;
                }
            }
        }
        board.lastMove().ifPresentOrElse(m -> moveMarker(m.row() * n + m.col(), animate), () -> lastMarker.setVisible(false));
        if (hoverIndex >= 0) {
            showGhost(hoverIndex);
        }
        clearHint();
    }

    /** Spotlights the winning chain, then runs {@code done}. */
    public void celebrate(List<int[]> path, Player winner, Runnable done) {
        ghost.setVisible(false);
        lastMarker.setVisible(false);
        Set<Integer> onPath = new HashSet<>();
        for (int[] p : path) {
            onPath.add(p[0] * n + p[1]);
        }
        for (int i = 0; i < stones.length; i++) {
            if (stones[i] != null && !onPath.contains(i)) {
                FadeTransition ft = new FadeTransition(Duration.millis(500), stones[i]);
                ft.setToValue(0.28);
                ft.play();
            }
        }
        for (Polygon cell : cells) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), cell);
            ft.setToValue(0.55);
            ft.play();
        }

        double step = Math.max(28, Math.min(70, 900.0 / path.size()));
        for (int k = 0; k < path.size(); k++) {
            int idx = path.get(k)[0] * n + path.get(k)[1];
            Node stone = stones[idx];
            if (stone == null) {
                continue;
            }
            stone.toFront();
            ScaleTransition up = new ScaleTransition(Duration.millis(220), stone);
            up.setToX(1.28);
            up.setToY(1.28);
            up.setInterpolator(Fx.EASE_OUT_CUBIC);
            ScaleTransition down = new ScaleTransition(Duration.millis(260), stone);
            down.setToX(1.0);
            down.setToY(1.0);
            SequentialTransition seq = new SequentialTransition(new PauseTransition(Duration.millis(k * step)), up, down);
            int fk = k;
            seq.setOnFinished(e -> {
                ScaleTransition breathe = new ScaleTransition(Duration.millis(900), stone);
                breathe.setFromX(1.0);
                breathe.setFromY(1.0);
                breathe.setToX(1.08);
                breathe.setToY(1.08);
                breathe.setAutoReverse(true);
                breathe.setCycleCount(Animation.INDEFINITE);
                breathe.setDelay(Duration.millis(fk * 40));
                breathe.play();
                loops.add(breathe);
            });
            seq.play();
            ripple(idx, Theme.light(winner), k * step);
        }

        Polyline line = new Polyline();
        for (int[] p : path) {
            line.getPoints().addAll(cx(p[0], p[1]), cy(p[0]));
        }
        line.setStroke(Color.rgb(255, 255, 255, 0.9));
        line.setStrokeWidth(R * 0.16);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.setStrokeLineJoin(StrokeLineJoin.ROUND);
        line.getStrokeDashArray().setAll(R * 0.45, R * 0.35);
        line.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.light(winner), 18, 0.5, 0, 0));
        line.setOpacity(0);
        fxLayer.getChildren().add(line);
        FadeTransition lineIn = new FadeTransition(Duration.millis(500), line);
        lineIn.setToValue(1);
        lineIn.setDelay(Duration.millis(path.size() * step));
        lineIn.play();
        Timeline flow = new Timeline(new KeyFrame(Duration.millis(900),
                new KeyValue(line.strokeDashOffsetProperty(), -R * 0.8, Interpolator.LINEAR)));
        flow.setCycleCount(Animation.INDEFINITE);
        flow.play();
        loops.add(flow);

        PauseTransition wait = new PauseTransition(Duration.millis(path.size() * step + 650));
        wait.setOnFinished(e -> done.run());
        wait.play();
    }

    /** Pulsing gold ring around a suggested cell. */
    public void showHint(int row, int col) {
        clearHint();
        Polygon ring = Fx.hexagon(cx(row, col), cy(row), R * 0.98);
        ring.setFill(Theme.GOLD.deriveColor(0, 1, 1, 0.18));
        ring.setStroke(Theme.GOLD);
        ring.setStrokeWidth(3);
        ring.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.GOLD, 16, 0.4, 0, 0));
        ring.setMouseTransparent(true);
        fxLayer.getChildren().add(ring);
        ScaleTransition pulse = new ScaleTransition(Duration.millis(520), ring);
        pulse.setFromX(0.82);
        pulse.setFromY(0.82);
        pulse.setToX(1.12);
        pulse.setToY(1.12);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(8);
        pulse.setOnFinished(e -> clearHint());
        pulse.play();
        hint = ring;
        hintAnim = pulse;
    }

    public void clearHint() {
        if (hintAnim != null) {
            hintAnim.stop();
            hintAnim = null;
        }
        if (hint != null) {
            fxLayer.getChildren().remove(hint);
            hint = null;
        }
    }

    // ------------------------------------------------------------------ geometry

    private double cx(int r, int c) {
        return R * Fx.SQRT3 * (c + r / 2.0);
    }

    private double cy(int r) {
        return 1.5 * R * r;
    }

    private void computeNaturalBounds() {
        var b = content.getLayoutBounds();
        minX = b.getMinX();
        minY = b.getMinY();
        naturalW = b.getWidth();
        naturalH = b.getHeight();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (naturalW <= 0 || naturalH <= 0) {
            return;
        }
        double s = Math.min((w - 2 * PAD) / naturalW, (h - 2 * PAD) / naturalH);
        s = Math.max(0.1, Math.min(s, 2.4));
        scale.setX(s);
        scale.setY(s);
        content.setLayoutX((w - naturalW * s) / 2 - minX * s);
        content.setLayoutY((h - naturalH * s) / 2 - minY * s);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 900;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 640;
    }

    // ------------------------------------------------------------------ construction helpers

    /** Coloured outer rims: red on the left/right sides, blue on top/bottom. */
    private void buildEdges() {
        Group red = new Group();
        Group blue = new Group();
        for (int i = 0; i < n; i++) {
            addEdge(blue, 0, i, 5, 0);
            addEdge(blue, 0, i, 0, 1);
            addEdge(blue, n - 1, i, 2, 3);
            addEdge(blue, n - 1, i, 3, 4);
            addEdge(red, i, 0, 4, 5);
            if (i < n - 1) {
                addEdge(red, i, 0, 3, 4);
            }
            addEdge(red, i, n - 1, 1, 2);
            if (i > 0) {
                addEdge(red, i, n - 1, 0, 1);
            }
        }
        red.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.RED, 22, 0.25, 0, 0));
        blue.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.BLUE, 22, 0.25, 0, 0));
        styleEdges(red, Theme.RED);
        styleEdges(blue, Theme.BLUE);
        edgeLayer.getChildren().addAll(red, blue);
    }

    private void addEdge(Group g, int r, int c, int v1, int v2) {
        double[] pts = Fx.hexPoints(cx(r, c), cy(r), R);
        g.getChildren().add(new Line(pts[2 * v1], pts[2 * v1 + 1], pts[2 * v2], pts[2 * v2 + 1]));
    }

    private void styleEdges(Group g, Color color) {
        for (Node node : g.getChildren()) {
            Line l = (Line) node;
            l.setStroke(color);
            l.setStrokeWidth(R * 0.34);
            l.setStrokeLineCap(StrokeLineCap.ROUND);
        }
    }

    private void buildLabels() {
        Font font = Font.font("Segoe UI", FontWeight.BOLD, R * 0.42);
        for (int c = 0; c < n; c++) {
            Text t = label(String.valueOf((char) ('A' + c)), font);
            t.setX(cx(0, c) - 20);
            t.setY(cy(0) - R * 1.75);
            labelLayer.getChildren().add(t);
        }
        for (int r = 0; r < n; r++) {
            Text t = label(String.valueOf(r + 1), font);
            t.setX(cx(r, 0) - R * 1.85 - 20);
            t.setY(cy(r));
            labelLayer.getChildren().add(t);
        }
    }

    private Text label(String s, Font font) {
        Text t = new Text(s);
        t.setFont(font);
        t.getStyleClass().add("coord");
        t.setTextOrigin(VPos.CENTER);
        t.setWrappingWidth(40);
        t.setTextAlignment(TextAlignment.CENTER);
        return t;
    }

    private void playIntro() {
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                Polygon cell = cells[r * n + c];
                cell.setScaleX(0);
                cell.setScaleY(0);
                cell.setOpacity(0);
                double delay = 80 + (r + c) * (520.0 / (2 * n));
                ScaleTransition st = new ScaleTransition(Duration.millis(420), cell);
                st.setToX(1);
                st.setToY(1);
                st.setInterpolator(Fx.EASE_OUT_BACK);
                FadeTransition ft = new FadeTransition(Duration.millis(260), cell);
                ft.setToValue(1);
                ParallelTransition pt = new ParallelTransition(st, ft);
                pt.setDelay(Duration.millis(delay));
                pt.play();
            }
        }
        for (Node edge : edgeLayer.getChildren()) {
            edge.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(700), edge);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(450));
            ft.play();
        }
        labelLayer.setOpacity(0);
        FadeTransition lf = new FadeTransition(Duration.millis(600), labelLayer);
        lf.setToValue(1);
        lf.setDelay(Duration.millis(700));
        lf.play();
    }

    // ------------------------------------------------------------------ stones

    private Node makeStone(Player p) {
        Polygon body = Fx.hexagon(R * 0.80);
        body.setFill(new RadialGradient(0, 0, 0.35, 0.28, 0.85, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.light(p)), new Stop(0.45, Theme.color(p)), new Stop(1, Theme.dark(p))));
        body.setStroke(Theme.light(p).deriveColor(0, 1, 1, 0.7));
        body.setStrokeWidth(1.4);
        Polygon shine = Fx.hexagon(R * 0.34);
        shine.setFill(Color.rgb(255, 255, 255, 0.16));
        shine.setTranslateY(-R * 0.22);
        shine.setScaleY(0.55);
        Group stone = new Group(body, shine);
        stone.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.color(p).deriveColor(0, 1, 1, 0.65), R * 0.6, 0.15, 0, 2));
        return stone;
    }

    private void addStone(int idx, Player p, boolean animate) {
        int r = idx / n;
        int c = idx % n;
        Node stone = makeStone(p);
        stone.setLayoutX(cx(r, c));
        stone.setLayoutY(cy(r));
        stones[idx] = stone;
        stoneLayer.getChildren().add(stoneLayer.getChildren().indexOf(ghost), stone);
        if (!animate) {
            return;
        }
        stone.setScaleX(0.25);
        stone.setScaleY(0.25);
        stone.setOpacity(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(420), stone);
        st.setToX(1);
        st.setToY(1);
        st.setInterpolator(Fx.EASE_OUT_BACK);
        FadeTransition ft = new FadeTransition(Duration.millis(140), stone);
        ft.setToValue(1);
        new ParallelTransition(st, ft).play();
        ripple(idx, Theme.color(p), 0);
        nudgeNeighbours(r, c);
        Glow glow = new Glow(0.8);
        Node body = ((Group) stone).getChildren().get(0);
        body.setEffect(glow);
        new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(glow.levelProperty(), 0))).play();
    }

    private void removeStone(int idx, boolean animate) {
        Node stone = stones[idx];
        stones[idx] = null;
        if (!animate) {
            stoneLayer.getChildren().remove(stone);
            return;
        }
        ScaleTransition st = new ScaleTransition(Duration.millis(200), stone);
        st.setToX(0.2);
        st.setToY(0.2);
        FadeTransition ft = new FadeTransition(Duration.millis(200), stone);
        ft.setToValue(0);
        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> stoneLayer.getChildren().remove(stone));
        pt.play();
    }

    private void ripple(int idx, Color color, double delayMs) {
        int r = idx / n;
        int c = idx % n;
        Polygon ring = Fx.hexagon(cx(r, c), cy(r), R * 0.8);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(color);
        ring.setStrokeWidth(3);
        ring.setOpacity(0);
        fxLayer.getChildren().add(ring);
        ScaleTransition st = new ScaleTransition(Duration.millis(650), ring);
        st.setFromX(0.9);
        st.setFromY(0.9);
        st.setToX(2.0);
        st.setToY(2.0);
        st.setInterpolator(Fx.EASE_OUT_CUBIC);
        FadeTransition ft = new FadeTransition(Duration.millis(650), ring);
        ft.setFromValue(0.9);
        ft.setToValue(0);
        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setDelay(Duration.millis(delayMs));
        pt.setOnFinished(e -> fxLayer.getChildren().remove(ring));
        pt.play();
    }

    private void nudgeNeighbours(int r, int c) {
        for (int[] d : Board.DIRECTIONS) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (nr < 0 || nr >= n || nc < 0 || nc >= n) {
                continue;
            }
            Polygon cell = cells[nr * n + nc];
            ScaleTransition st = new ScaleTransition(Duration.millis(110), cell);
            st.setFromX(1);
            st.setFromY(1);
            st.setToX(0.9);
            st.setToY(0.9);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.setDelay(Duration.millis(60));
            st.play();
        }
    }

    private void moveMarker(int idx, boolean animate) {
        int r = idx / n;
        int c = idx % n;
        lastMarker.setCenterX(cx(r, c));
        lastMarker.setCenterY(cy(r));
        lastMarker.setVisible(true);
        lastMarker.toFront();
        if (animate) {
            Fx.popIn(lastMarker, 250);
        } else {
            lastMarker.setOpacity(1);
            lastMarker.setScaleX(1);
            lastMarker.setScaleY(1);
        }
    }

    private void showGhost(int idx) {
        if (!interactive || shown.length <= idx || shown[idx] != null) {
            ghost.setVisible(false);
            return;
        }
        Player p = turnSupplier.get();
        int r = idx / n;
        int c = idx % n;
        ghost.setLayoutX(cx(r, c));
        ghost.setLayoutY(cy(r));
        ghost.setFill(Theme.color(p).deriveColor(0, 1, 1, 0.28));
        ghost.setStroke(Theme.light(p).deriveColor(0, 1, 1, 0.8));
        ghost.setVisible(true);
        ghost.toFront();
    }
}
