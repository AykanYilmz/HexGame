package io.github.aykanyilmz.hexgame.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

/** Small animation and geometry toolkit shared by all views. */
public final class Fx {

    public static final double SQRT3 = Math.sqrt(3);

    /** Overshoots a little before settling - gives "pop" to appearing elements. */
    public static final Interpolator EASE_OUT_BACK = new Interpolator() {
        @Override
        protected double curve(double t) {
            double c1 = 1.70158;
            double c3 = c1 + 1;
            return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
        }
    };

    public static final Interpolator EASE_OUT_CUBIC = new Interpolator() {
        @Override
        protected double curve(double t) {
            return 1 - Math.pow(1 - t, 3);
        }
    };

    public static final Interpolator EASE_IN_OUT = Interpolator.SPLINE(0.65, 0, 0.35, 1);

    private Fx() {
    }

    /** Pointy-top hexagon vertices, starting at the top and going clockwise. */
    public static double[] hexPoints(double cx, double cy, double radius) {
        double[] pts = new double[12];
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(60 * k - 90);
            pts[2 * k] = cx + radius * Math.cos(a);
            pts[2 * k + 1] = cy + radius * Math.sin(a);
        }
        return pts;
    }

    public static Polygon hexagon(double cx, double cy, double radius) {
        return new Polygon(hexPoints(cx, cy, radius));
    }

    public static Polygon hexagon(double radius) {
        return hexagon(0, 0, radius);
    }

    /** Gentle grow on hover, slight squash on press. */
    public static void interactive(Node node, double hoverScale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(180), node);
        st.setInterpolator(EASE_OUT_CUBIC);
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> scaleTo(st, hoverScale));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> scaleTo(st, 1.0));
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> scaleTo(st, hoverScale * 0.95));
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> scaleTo(st, node.isHover() ? hoverScale : 1.0));
    }

    private static void scaleTo(ScaleTransition st, double s) {
        st.stop();
        st.setToX(s);
        st.setToY(s);
        st.playFromStart();
    }

    /** Fade + slide up entrance used for staggered reveals. */
    public static ParallelTransition reveal(Node node, double delayMs, double dy) {
        node.setOpacity(0);
        node.setTranslateY(dy);
        FadeTransition fade = new FadeTransition(Duration.millis(450), node);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(550), node);
        slide.setToY(0);
        slide.setInterpolator(EASE_OUT_CUBIC);
        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
        return pt;
    }

    public static void popIn(Node node, double delayMs) {
        node.setScaleX(0.6);
        node.setScaleY(0.6);
        node.setOpacity(0);
        ScaleTransition sc = new ScaleTransition(Duration.millis(420), node);
        sc.setToX(1);
        sc.setToY(1);
        sc.setInterpolator(EASE_OUT_BACK);
        FadeTransition f = new FadeTransition(Duration.millis(220), node);
        f.setToValue(1);
        ParallelTransition pt = new ParallelTransition(sc, f);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    /** Horizontal shake, e.g. for an illegal click. */
    public static void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(55), node);
        tt.setFromX(0);
        tt.setByX(6);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    public static LinearGradient brandGradient() {
        return new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.RED), new Stop(0.55, Theme.VIOLET), new Stop(1, Theme.BLUE));
    }

    public static Image appIcon(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();
        double[] pts = hexPoints(size / 2.0, size / 2.0, size * 0.48);
        double[] xs = new double[6];
        double[] ys = new double[6];
        for (int i = 0; i < 6; i++) {
            xs[i] = pts[2 * i];
            ys[i] = pts[2 * i + 1];
        }
        g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.RED), new Stop(0.5, Theme.VIOLET), new Stop(1, Theme.BLUE)));
        g.fillPolygon(xs, ys, 6);
        double[] inner = hexPoints(size / 2.0, size / 2.0, size * 0.22);
        for (int i = 0; i < 6; i++) {
            xs[i] = inner[2 * i];
            ys[i] = inner[2 * i + 1];
        }
        g.setFill(Color.web("#0B1020"));
        g.fillPolygon(xs, ys, 6);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }

    public static String clock(long millis) {
        long s = millis / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
