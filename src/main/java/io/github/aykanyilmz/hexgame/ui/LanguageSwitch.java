package io.github.aykanyilmz.hexgame.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import io.github.aykanyilmz.hexgame.model.Language;

import java.util.Arrays;

/** Compact "🌐 TR | EN" picker that switches the interface language on the fly. */
public class LanguageSwitch extends HBox {

    public LanguageSwitch() {
        super(8);
        getStyleClass().add("lang-switch");
        setAlignment(Pos.CENTER);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        Segmented<Language> picker = new Segmented<>(Arrays.asList(Language.values()),
                l -> l.name(), I18n.language());
        picker.valueProperty().addListener((o, a, b) -> I18n.setLanguage(b));
        getChildren().addAll(Icons.icon(Icons.GLOBE, 18, Theme.MUTED), picker);

        Tooltip tip = new Tooltip();
        tip.textProperty().bind(I18n.text("language.tooltip"));
        tip.setShowDelay(Duration.millis(250));
        Tooltip.install(this, tip);
    }
}
