package org.example.hexgame.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Lightweight canvas confetti burst that removes itself when done. */
public final class Confetti extends Canvas {

    private static final class Piece {
        double x, y, vx, vy, rot, vr, w, h, phase, vp, life;
        Color color;
        boolean hex;
    }

    private final List<Piece> pieces = new ArrayList<>();
    private final Random rng = new Random();
    private AnimationTimer timer;

    private Confetti() {
        setMouseTransparent(true);
        setManaged(false);
    }

    public static void burst(Pane host, List<Color> palette) {
        Confetti c = new Confetti();
        c.widthProperty().bind(host.widthProperty());
        c.heightProperty().bind(host.heightProperty());
        host.getChildren().add(c);
        c.spawn(host.getWidth(), host.getHeight(), palette);
        c.start(host);
    }

    private void spawn(double w, double h, List<Color> palette) {
        // two cannons from the bottom corners + a soft rain from the top
        for (int i = 0; i < 260; i++) {
            Piece p = new Piece();
            int kind = i % 3;
            if (kind < 2) {
                boolean left = kind == 0;
                p.x = left ? w * 0.04 : w * 0.96;
                p.y = h + 10;
                double angle = Math.toRadians(left ? -60 - rng.nextDouble() * 25 : -120 + rng.nextDouble() * 25);
                double speed = 900 + rng.nextDouble() * 700;
                p.vx = Math.cos(angle) * speed;
                p.vy = Math.sin(angle) * speed;
            } else {
                p.x = rng.nextDouble() * w;
                p.y = -20 - rng.nextDouble() * h * 0.5;
                p.vx = (rng.nextDouble() - 0.5) * 120;
                p.vy = 80 + rng.nextDouble() * 160;
            }
            p.w = 6 + rng.nextDouble() * 8;
            p.h = 4 + rng.nextDouble() * 6;
            p.rot = rng.nextDouble() * 360;
            p.vr = (rng.nextDouble() - 0.5) * 720;
            p.phase = rng.nextDouble() * Math.PI * 2;
            p.vp = 6 + rng.nextDouble() * 8;
            p.color = palette.get(rng.nextInt(palette.size()));
            p.hex = rng.nextDouble() < 0.3;
            p.life = 4.5 + rng.nextDouble() * 1.5;
            pieces.add(p);
        }
    }

    private void start(Pane host) {
        timer = new AnimationTimer() {
            long last;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                double dt = Math.min(0.04, (now - last) / 1e9);
                last = now;
                if (!step(dt)) {
                    stop();
                    host.getChildren().remove(Confetti.this);
                }
            }
        };
        timer.start();
    }

    private boolean step(double dt) {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());
        double drag = Math.pow(0.12, dt);
        boolean alive = false;
        for (Piece p : pieces) {
            p.life -= dt;
            if (p.life <= 0 || p.y > getHeight() + 40 && p.vy > 0) {
                continue;
            }
            alive = true;
            p.vy += 1100 * dt;
            p.vx *= drag;
            // air resistance + terminal velocity so pieces flutter down instead of dropping
            p.vy = Math.min(p.vy * Math.pow(0.6, dt), 260);
            p.x += (p.vx + Math.sin(p.phase) * 40) * dt;
            p.y += p.vy * dt;
            p.rot += p.vr * dt;
            p.phase += p.vp * dt;

            g.save();
            g.setGlobalAlpha(Math.min(1, p.life / 0.8));
            g.translate(p.x, p.y);
            g.rotate(p.rot);
            g.scale(1, Math.abs(Math.cos(p.phase)) * 0.9 + 0.1);
            g.setFill(p.color);
            if (p.hex) {
                double[] pts = Fx.hexPoints(0, 0, p.w * 0.6);
                double[] xs = new double[6];
                double[] ys = new double[6];
                for (int i = 0; i < 6; i++) {
                    xs[i] = pts[2 * i];
                    ys[i] = pts[2 * i + 1];
                }
                g.fillPolygon(xs, ys, 6);
            } else {
                g.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
            }
            g.restore();
        }
        return alive;
    }
}
