package io.github.aykanyilmz.hexgame.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against a throwaway preferences node so the player's real settings are never touched. */
class SettingsTest {

    private Preferences prefs;

    @BeforeEach
    void createNode() {
        prefs = Preferences.userRoot().node("hexgame-test-" + UUID.randomUUID());
    }

    @AfterEach
    void removeNode() throws BackingStoreException {
        prefs.removeNode();
    }

    @Test
    void emptyStoreGivesDefaults() {
        assertEquals(GameConfig.defaults(), Settings.load(prefs));
    }

    @Test
    void savedConfigComesBackUnchanged() {
        GameConfig c = new GameConfig(13, GameConfig.Mode.AI, Difficulty.HARD, true, "Ada", "Can", Player.BLUE);
        Settings.save(prefs, c);
        assertEquals(c, Settings.load(prefs));
    }

    @Test
    void unknownBoardSizeFallsBackToDefaultSize() {
        Settings.save(prefs, GameConfig.defaults());
        prefs.putInt("size", 8);
        assertEquals(GameConfig.defaults().size(), Settings.load(prefs).size());
    }

    @Test
    void corruptValueResetsToDefaults() {
        Settings.save(prefs, new GameConfig(5, GameConfig.Mode.AI, Difficulty.EASY, true, "Ada", "Can", Player.RED));
        prefs.put("difficulty", "IMPOSSIBLE");
        assertEquals(GameConfig.defaults(), Settings.load(prefs));
    }

    @Test
    void languageIsRemembered() {
        Settings.saveLanguage(prefs, Language.EN);
        assertEquals(Language.EN, Settings.loadLanguage(prefs));
        Settings.saveLanguage(prefs, Language.TR);
        assertEquals(Language.TR, Settings.loadLanguage(prefs));
    }

    @Test
    void unknownLanguageFallsBackToASupportedOne() {
        prefs.put("language", "KLINGON");
        assertNotNull(Settings.loadLanguage(prefs));
    }
}
