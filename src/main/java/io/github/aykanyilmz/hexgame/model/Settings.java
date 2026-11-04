package io.github.aykanyilmz.hexgame.model;

import java.util.Locale;
import java.util.prefs.Preferences;

/** Remembers the last used menu choices and the interface language between launches. */
public final class Settings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Settings.class);

    private Settings() {
    }

    public static GameConfig load() {
        return load(PREFS);
    }

    public static void save(GameConfig c) {
        save(PREFS, c);
    }

    public static Language loadLanguage() {
        return loadLanguage(PREFS);
    }

    public static void saveLanguage(Language language) {
        saveLanguage(PREFS, language);
    }

    // Package-private overloads let tests use a throwaway preferences node.

    static GameConfig load(Preferences prefs) {
        GameConfig d = GameConfig.defaults();
        try {
            int size = prefs.getInt("size", d.size());
            boolean validSize = false;
            for (int s : GameConfig.SIZES) {
                validSize |= s == size;
            }
            return new GameConfig(
                    validSize ? size : d.size(),
                    GameConfig.Mode.valueOf(prefs.get("mode", d.mode().name())),
                    Difficulty.valueOf(prefs.get("difficulty", d.difficulty().name())),
                    prefs.getBoolean("swap", d.swapRule()),
                    prefs.get("nameA", d.nameA()),
                    prefs.get("nameB", d.nameB()),
                    Player.valueOf(prefs.get("seatAColor", d.seatAColor().name())));
        } catch (RuntimeException e) {
            return d;
        }
    }

    static void save(Preferences prefs, GameConfig c) {
        try {
            prefs.putInt("size", c.size());
            prefs.put("mode", c.mode().name());
            prefs.put("difficulty", c.difficulty().name());
            prefs.putBoolean("swap", c.swapRule());
            prefs.put("nameA", c.nameA());
            prefs.put("nameB", c.nameB());
            prefs.put("seatAColor", c.seatAColor().name());
        } catch (RuntimeException ignored) {
            // Preferences are a convenience only.
        }
    }

    /** The saved language, or the system's language on first launch. */
    static Language loadLanguage(Preferences prefs) {
        Language fallback = Language.fromLocale(Locale.getDefault());
        try {
            return Language.valueOf(prefs.get("language", fallback.name()));
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    static void saveLanguage(Preferences prefs, Language language) {
        try {
            prefs.put("language", language.name());
        } catch (RuntimeException ignored) {
            // Preferences are a convenience only.
        }
    }
}
