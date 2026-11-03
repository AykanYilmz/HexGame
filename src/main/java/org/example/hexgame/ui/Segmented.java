package org.example.hexgame.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Segmented control with a gradient "pill" that slides to the selected option. */
public class Segmented<T> extends StackPane {

    private final HBox row = new HBox(4);
    private final Region indicator = new Region();
    private final ToggleGroup group = new ToggleGroup();
    private final ObjectProperty<T> value = new SimpleObjectProperty<>();
    private final DoubleProperty ix = new SimpleDoubleProperty();
    private final DoubleProperty iw = new SimpleDoubleProperty();
    private Timeline anim;

    @SuppressWarnings("unchecked")
    public Segmented(List<T> items, Function<T, String> label, T initial) {
        getStyleClass().add("segmented");
        indicator.getStyleClass().add("seg-indicator");
        indicator.setManaged(false);
        getChildren().addAll(indicator, row);

        for (T item : items) {
            ToggleButton b = new ToggleButton(label.apply(item));
            b.setUserData(item);
            b.setToggleGroup(group);
            b.getStyleClass().add("seg-btn");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setFocusTraversable(false);
            HBox.setHgrow(b, Priority.ALWAYS);
            row.getChildren().add(b);
            if (Objects.equals(item, initial)) {
                b.setSelected(true);
            }
        }
        value.set(initial);

        group.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) {
                if (old != null) {
                    old.setSelected(true);
                }
                return;
            }
            value.set((T) now.getUserData());
            animateTo((ToggleButton) now);
        });
        ix.addListener(x -> placeIndicator());
        iw.addListener(x -> placeIndicator());
    }

    private ToggleButton selectedButton() {
        return (ToggleButton) group.getSelectedToggle();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        ToggleButton sel = selectedButton();
        if (sel != null && (anim == null || anim.getStatus() != Animation.Status.RUNNING)) {
            Bounds b = sel.getBoundsInParent();
            ix.set(row.getLayoutX() + b.getMinX());
            iw.set(b.getWidth());
        }
        placeIndicator();
    }

    private void placeIndicator() {
        ToggleButton sel = selectedButton();
        if (sel == null) {
            indicator.setVisible(false);
            return;
        }
        indicator.setVisible(true);
        Bounds b = sel.getBoundsInParent();
        indicator.resizeRelocate(ix.get(), row.getLayoutY() + b.getMinY(), iw.get(), b.getHeight());
    }

    private void animateTo(ToggleButton b) {
        Bounds bb = b.getBoundsInParent();
        if (anim != null) {
            anim.stop();
        }
        anim = new Timeline(new KeyFrame(Duration.millis(320),
                new KeyValue(ix, row.getLayoutX() + bb.getMinX(), Fx.EASE_OUT_BACK),
                new KeyValue(iw, bb.getWidth(), Fx.EASE_OUT_CUBIC)));
        anim.play();
    }

    public ObjectProperty<T> valueProperty() {
        return value;
    }

    public T getValue() {
        return value.get();
    }
}
