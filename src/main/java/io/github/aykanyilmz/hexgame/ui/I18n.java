package io.github.aykanyilmz.hexgame.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.util.Subscription;
import io.github.aykanyilmz.hexgame.model.Language;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Interface texts in the current language. Texts are kept in {@code messages_<code>.properties}
 * next to this class. Placeholders are written {@code {0}}, {@code {1}}, ... (plain replacement,
 * so apostrophes need no escaping as they would with {@code MessageFormat}).
 * <p>
 * Labels bound with {@link #text} follow language changes on their own; screens subscribe to
 * {@link #languageProperty()} for texts they compose themselves.
 */
public final class I18n {

    private static final ObjectProperty<Language> LANGUAGE = new SimpleObjectProperty<>(Language.TR);
    private static final Map<Language, Properties> BUNDLES = new EnumMap<>(Language.class);
    private static final String[] DEFAULT_NAMES = {"name.player1", "name.player2", "name.player"};

    private I18n() {
    }

    public static ObjectProperty<Language> languageProperty() {
        return LANGUAGE;
    }

    public static Language language() {
        return LANGUAGE.get();
    }

    public static void setLanguage(Language language) {
        LANGUAGE.set(language);
    }

    /**
     * Runs {@code action} after every language change. Screens must cancel the returned
     * subscription when they are disposed, since the language property outlives them.
     */
    public static Subscription onChange(Runnable action) {
        ChangeListener<Language> listener = (o, a, b) -> action.run();
        LANGUAGE.addListener(listener);
        return () -> LANGUAGE.removeListener(listener);
    }

    /** The text for {@code key} in the current language. */
    public static String get(String key, Object... args) {
        return get(language(), key, args);
    }

    /** The text for {@code key} in {@code language}; falls back to English, then to the key itself. */
    public static String get(Language language, String key, Object... args) {
        String s = bundle(language).getProperty(key);
        if (s == null) {
            s = bundle(Language.EN).getProperty(key, key);
        }
        for (int i = 0; i < args.length; i++) {
            s = s.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return s;
    }

    /**
     * Player names left at a default ("Oyuncu 1", "Player 2", ...) are shown in the current
     * language; any name a player typed is returned unchanged.
     */
    public static String playerName(String name) {
        for (String key : DEFAULT_NAMES) {
            for (Language l : Language.values()) {
                if (get(l, key).equals(name)) {
                    return get(key);
                }
            }
        }
        return name;
    }

    /** A binding that re-translates {@code key} whenever the language changes. */
    public static StringBinding text(String key, Object... args) {
        return Bindings.createStringBinding(() -> get(key, args), LANGUAGE);
    }

    /** All keys defined for {@code language}. */
    public static Set<String> keys(Language language) {
        return bundle(language).stringPropertyNames();
    }

    private static synchronized Properties bundle(Language language) {
        return BUNDLES.computeIfAbsent(language, I18n::load);
    }

    private static Properties load(Language language) {
        String file = "messages_" + language.code() + ".properties";
        Properties p = new Properties();
        try (InputStream in = I18n.class.getResourceAsStream(file)) {
            if (in == null) {
                throw new IllegalStateException("Missing message file " + file);
            }
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return p;
    }
}
