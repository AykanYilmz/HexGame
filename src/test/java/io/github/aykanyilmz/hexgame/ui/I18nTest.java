package io.github.aykanyilmz.hexgame.ui;

import io.github.aykanyilmz.hexgame.model.Difficulty;
import io.github.aykanyilmz.hexgame.model.GameConfig;
import io.github.aykanyilmz.hexgame.model.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+}");

    @AfterEach
    void resetLanguage() {
        I18n.setLanguage(Language.TR);
    }

    @Test
    void everyLanguageDefinesTheSameKeys() {
        Set<String> reference = I18n.keys(Language.EN);
        for (Language l : Language.values()) {
            Set<String> missing = new TreeSet<>(reference);
            missing.removeAll(I18n.keys(l));
            Set<String> extra = new TreeSet<>(I18n.keys(l));
            extra.removeAll(reference);
            assertTrue(missing.isEmpty(), l + " is missing " + missing);
            assertTrue(extra.isEmpty(), l + " has unknown keys " + extra);
        }
    }

    @Test
    void translationsUseTheSamePlaceholders() {
        for (String key : I18n.keys(Language.EN)) {
            Set<String> expected = placeholders(I18n.get(Language.EN, key));
            for (Language l : Language.values()) {
                assertEquals(expected, placeholders(I18n.get(l, key)), l + " / " + key);
            }
        }
    }

    @Test
    void enumsUsedInTheUiHaveTexts() {
        for (Language l : Language.values()) {
            for (Difficulty d : Difficulty.values()) {
                assertTrue(I18n.keys(l).contains("difficulty." + d.name()), l + " " + d);
            }
            for (GameConfig.Mode m : GameConfig.Mode.values()) {
                assertTrue(I18n.keys(l).contains("mode." + m.name()), l + " " + m);
            }
        }
    }

    @Test
    void placeholdersAreFilledAndApostrophesSurvive() {
        assertEquals("Ada wins!", I18n.get(Language.EN, "status.won", "Ada"));
        assertEquals("Ada kazandı!", I18n.get(Language.TR, "status.won", "Ada"));
        assertTrue(I18n.get(Language.TR, "rules.step4").startsWith("Hex'te"));
    }

    @Test
    void bindingFollowsTheLanguage() {
        I18n.setLanguage(Language.TR);
        var text = I18n.text("menu.start");
        assertEquals("Oyuna başla", text.get());
        I18n.setLanguage(Language.EN);
        assertEquals("Start game", text.get());
    }

    @Test
    void defaultPlayerNamesAreTranslatedButTypedNamesAreKept() {
        I18n.setLanguage(Language.EN);
        assertEquals("Player 1", I18n.playerName("Oyuncu 1"));
        assertEquals("Player", I18n.playerName("Player"));
        assertEquals("Ada", I18n.playerName("Ada"));
        I18n.setLanguage(Language.TR);
        assertEquals("Oyuncu 2", I18n.playerName("Player 2"));
    }

    @Test
    void unknownKeyFallsBackToTheKey() {
        assertEquals("no.such.key", I18n.get("no.such.key"));
    }

    @Test
    void systemLocalePicksTheLanguage() {
        assertEquals(Language.TR, Language.fromLocale(Locale.forLanguageTag("tr-TR")));
        assertEquals(Language.EN, Language.fromLocale(Locale.GERMANY));
    }

    private static Set<String> placeholders(String s) {
        Set<String> out = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(s);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }
}
