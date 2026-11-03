package org.example.hexgame.model;

import java.util.prefs.Preferences;

/** Remembers the last used menu choices between launches. */
public final class Settings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Settings.class);

    private Settings() {
    }

    public static GameConfig load() {
        GameConfig d = GameConfig.defaults();
        try {
            int size = PREFS.getInt("size", d.size());
            boolean validSize = false;
            for (int s : GameConfig.SIZES) {
                validSize |= s == size;
            }
            return new GameConfig(
                    validSize ? size : d.size(),
                    GameConfig.Mode.valueOf(PREFS.get("mode", d.mode().name())),
                    Difficulty.valueOf(PREFS.get("difficulty", d.difficulty().name())),
                    PREFS.getBoolean("swap", d.swapRule()),
                    PREFS.get("nameA", d.nameA()),
                    PREFS.get("nameB", d.nameB()),
                    Player.valueOf(PREFS.get("seatAColor", d.seatAColor().name())));
        } catch (RuntimeException e) {
            return d;
        }
    }

    public static void save(GameConfig c) {
        try {
            PREFS.putInt("size", c.size());
            PREFS.put("mode", c.mode().name());
            PREFS.put("difficulty", c.difficulty().name());
            PREFS.putBoolean("swap", c.swapRule());
            PREFS.put("nameA", c.nameA());
            PREFS.put("nameB", c.nameB());
            PREFS.put("seatAColor", c.seatAColor().name());
        } catch (RuntimeException ignored) {
            // Preferences are a convenience only.
        }
    }
}
