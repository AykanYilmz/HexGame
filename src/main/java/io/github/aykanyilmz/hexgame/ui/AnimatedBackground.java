package io.github.aykanyilmz.hexgame.ui;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Living backdrop: static colour glows plus a few slowly rising hexagon outlines.
 * <p>
 * Performance notes: the big glows only move on resize (moving them every frame forces a
 * full-screen repaint), the outlines have no fill and no effects, and the whole animation is
 * paused while a game is on screen.
 */
public class AnimatedBackground extends Pane {

    private static final class Floater {
        Polygon node;
        double x, y, vx, vy, rot, vr;
        double radius;
    }

    private final Random rng = new Random();
    private final List<Floater> floaters = new ArrayList<>();
    private final Group floaterLayer = new Group();
    private final Circle glowRed = glow(Theme.RED, 0.30);
    private final Circle glowBlue = glow(Theme.BLUE, 0.26);
    private final Circle glowViolet = glow(Theme.VIOLET, 0.16);
    private final AnimationTimer timer;
    private boolean seeded;
    private long last;

    public AnimatedBackground() {
        setMouseTransparent(true);
        getChildren().addAll(glowViolet, glowRed, glowBlue, floaterLayer);
        for (int i = 0; i < 18; i++) {
            Floater f = new Floater();
            f.radius = 12 + Math.pow(rng.nextDouble(), 2) * 64;
            f.node = Fx.hexagon(f.radius);
            Color base = switch (i % 3) {
                case 0 -> Theme.RED;
                case 1 -> Theme.BLUE;
                default -> Color.WHITE;
            };
            f.node.setFill(null);
            f.node.setStroke(base.deriveColor(0, 1, 1, 0.10 + rng.nextDouble() * 0.14));
            f.node.setStrokeWidth(1.2);
            f.vy = -(6 + rng.nextDouble() * 16) * (40 / (f.radius + 20));
            f.vx = (rng.nextDouble() - 0.5) * 6;
            f.vr = (rng.nextDouble() - 0.5) * 12;
            f.rot = rng.nextDouble() * 60;
            floaters.add(f);
            floaterLayer.getChildren().add(f.node);
        }

        widthProperty().addListener(o -> placeGlows());
        heightProperty().addListener(o -> placeGlows());

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                double dt = Math.min(0.05, (now - last) / 1e9);
                last = now;
                tick(dt);
            }
        };
        timer.start();
    }

    private static Circle glow(Color c, double alpha) {
        Circle circle = new Circle(400);
        circle.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, c.deriveColor(0, 1, 1, alpha)), new Stop(1, c.deriveColor(0, 1, 1, 0))));
        return circle;
    }

    private void placeGlows() {
        double w = getWidth();
        double h = getHeight();
        double big = Math.max(w, h);
        glowRed.setRadius(big * 0.42);
        glowRed.setCenterX(w * 0.12);
        glowRed.setCenterY(h * 0.18);
        glowBlue.setRadius(big * 0.45);
        glowBlue.setCenterX(w * 0.88);
        glowBlue.setCenterY(h * 0.85);
        glowViolet.setRadius(big * 0.35);
        glowViolet.setCenterX(w * 0.5);
        glowViolet.setCenterY(h * 0.5);
    }

    private void tick(double dt) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (!seeded) {
            for (Floater f : floaters) {
                f.x = rng.nextDouble() * w;
                f.y = rng.nextDouble() * h;
            }
            seeded = true;
        }
        for (Floater f : floaters) {
            f.x += f.vx * dt;
            f.y += f.vy * dt;
            f.rot += f.vr * dt;
            if (f.y < -f.radius * 2) {
                f.y = h + f.radius * 2;
                f.x = rng.nextDouble() * w;
            }
            if (f.x < -f.radius * 2) {
                f.x = w + f.radius;
            } else if (f.x > w + f.radius * 2) {
                f.x = -f.radius;
            }
            f.node.setLayoutX(f.x);
            f.node.setLayoutY(f.y);
            f.node.setRotate(f.rot);
        }
    }

    /** During a game the decoration fades down and stops moving, so the board gets the whole GPU. */
    public void setCalm(boolean calm) {
        FadeTransition ft = new FadeTransition(Duration.millis(600), floaterLayer);
        ft.setToValue(calm ? 0.35 : 1.0);
        if (calm) {
            ft.setOnFinished(e -> timer.stop());
        } else {
            last = 0;
            timer.start();
        }
        ft.play();
    }
}
