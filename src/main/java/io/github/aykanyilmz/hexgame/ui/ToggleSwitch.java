package io.github.aykanyilmz.hexgame.ui;

import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/** iOS-style animated on/off switch. */
public class ToggleSwitch extends StackPane {

    private static final Color OFF = Color.web("#2A335C");

    private final BooleanProperty selected = new SimpleBooleanProperty();
    private final Rectangle track = new Rectangle(48, 28);
    private final Circle thumb = new Circle(11, Color.WHITE);

    public ToggleSwitch(boolean initial) {
        track.setArcWidth(28);
        track.setArcHeight(28);
        track.setFill(initial ? Theme.VIOLET : OFF);
        thumb.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.35)));
        thumb.setTranslateX(initial ? 10 : -10);
        selected.set(initial);
        getChildren().addAll(track, thumb);
        setMaxSize(48, 28);
        setCursor(Cursor.HAND);
        setOnMouseClicked(e -> selected.set(!selected.get()));
        selected.addListener((o, a, on) -> animate(on));
    }

    private void animate(boolean on) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), thumb);
        tt.setToX(on ? 10 : -10);
        tt.setInterpolator(Fx.EASE_OUT_BACK);
        FillTransition ft = new FillTransition(Duration.millis(220), track);
        ft.setToValue(on ? Theme.VIOLET : OFF);
        new ParallelTransition(tt, ft).play();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }
}
