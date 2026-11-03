package org.example.hexgame.ui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Animated dialog that blurs whatever is behind it. */
public class Modal extends StackPane {

    private final Region backdrop = new Region();
    private final VBox card;
    private final Node blurTarget;
    private Runnable onClosed;
    private boolean closing;

    public Modal(Node blurTarget, boolean dismissOnBackdrop, Node... content) {
        this.blurTarget = blurTarget;
        getStyleClass().add("modal-root");
        backdrop.getStyleClass().add("modal-backdrop");
        card = new VBox(18, content);
        card.getStyleClass().addAll("card", "modal-card");
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        getChildren().addAll(backdrop, card);
        if (dismissOnBackdrop) {
            backdrop.setOnMouseClicked(e -> close());
        }
    }

    public VBox card() {
        return card;
    }

    public void setOnClosed(Runnable r) {
        this.onClosed = r;
    }

    public void show(StackPane host) {
        host.getChildren().add(this);
        backdrop.setOpacity(0);
        card.setOpacity(0);
        card.setScaleX(0.86);
        card.setScaleY(0.86);
        FadeTransition bf = new FadeTransition(Duration.millis(260), backdrop);
        bf.setToValue(1);
        FadeTransition cf = new FadeTransition(Duration.millis(260), card);
        cf.setToValue(1);
        ScaleTransition cs = new ScaleTransition(Duration.millis(460), card);
        cs.setToX(1);
        cs.setToY(1);
        cs.setInterpolator(Fx.EASE_OUT_BACK);
        new ParallelTransition(bf, cf, cs, blur(0, 14)).play();
    }

    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        FadeTransition bf = new FadeTransition(Duration.millis(200), backdrop);
        bf.setToValue(0);
        FadeTransition cf = new FadeTransition(Duration.millis(200), card);
        cf.setToValue(0);
        ScaleTransition cs = new ScaleTransition(Duration.millis(200), card);
        cs.setToX(0.92);
        cs.setToY(0.92);
        ParallelTransition pt = new ParallelTransition(bf, cf, cs, blur(14, 0));
        pt.setOnFinished(e -> {
            if (blurTarget != null) {
                blurTarget.setEffect(null);
            }
            if (getParent() instanceof StackPane parent) {
                parent.getChildren().remove(this);
            }
            if (onClosed != null) {
                onClosed.run();
            }
        });
        pt.play();
    }

    private Timeline blur(double from, double to) {
        if (blurTarget == null) {
            return new Timeline();
        }
        GaussianBlur gb = blurTarget.getEffect() instanceof GaussianBlur existing ? existing : new GaussianBlur(from);
        gb.setRadius(from);
        blurTarget.setEffect(gb);
        return new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(gb.radiusProperty(), to, Fx.EASE_OUT_CUBIC)));
    }
}
