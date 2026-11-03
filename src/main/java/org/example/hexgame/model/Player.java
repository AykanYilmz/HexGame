package org.example.hexgame.model;

/**
 * The two sides of a Hex game.
 * <ul>
 *     <li>{@link #RED} always moves first and connects the <b>left</b> and <b>right</b> edges.</li>
 *     <li>{@link #BLUE} connects the <b>top</b> and <b>bottom</b> edges.</li>
 * </ul>
 */
public enum Player {
    RED, BLUE;

    public Player opponent() {
        return this == RED ? BLUE : RED;
    }

    /** Compact numeric code used by the AI (1 = red, 2 = blue, 0 = empty). */
    public int code() {
        return this == RED ? 1 : 2;
    }
}
