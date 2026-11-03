package org.example.hexgame.model;

public enum Difficulty {
    EASY("Kolay"),
    MEDIUM("Orta"),
    HARD("Zor");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
