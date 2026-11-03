package org.example.hexgame.ui;

import javafx.scene.paint.Color;
import org.example.hexgame.model.Player;

/** Central colour palette. Keep in sync with styles.css. */
public final class Theme {

    public static final Color BG = Color.web("#0B1020");
    public static final Color SURFACE = Color.web("#161C3A");
    public static final Color CELL = Color.web("#18204A");
    public static final Color TEXT = Color.web("#E8ECF8");
    public static final Color MUTED = Color.web("#8A93B2");
    public static final Color VIOLET = Color.web("#7B5CFF");
    public static final Color GOLD = Color.web("#FFD166");

    public static final Color RED = Color.web("#FF4D6D");
    public static final Color RED_LIGHT = Color.web("#FF9BAE");
    public static final Color RED_DARK = Color.web("#B5173F");

    public static final Color BLUE = Color.web("#4CC9F0");
    public static final Color BLUE_LIGHT = Color.web("#B4EEFF");
    public static final Color BLUE_DARK = Color.web("#3A4FD8");

    private Theme() {
    }

    public static Color color(Player p) {
        return p == Player.RED ? RED : BLUE;
    }

    public static Color light(Player p) {
        return p == Player.RED ? RED_LIGHT : BLUE_LIGHT;
    }

    public static Color dark(Player p) {
        return p == Player.RED ? RED_DARK : BLUE_DARK;
    }

    public static String css(Color c) {
        return String.format("rgba(%d,%d,%d,%.3f)",
                Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255), c.getOpacity());
    }
}
