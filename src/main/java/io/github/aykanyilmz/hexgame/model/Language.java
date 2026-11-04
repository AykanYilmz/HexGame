package io.github.aykanyilmz.hexgame.model;

import java.util.Locale;

/** Interface languages. */
public enum Language {
    TR("tr"),
    EN("en");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    /** ISO 639-1 code, also the suffix of the message file. */
    public String code() {
        return code;
    }

    /** Turkish on Turkish systems, English everywhere else. */
    public static Language fromLocale(Locale locale) {
        return TR.code.equals(locale.getLanguage()) ? TR : EN;
    }
}
