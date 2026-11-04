package io.github.aykanyilmz.hexgame.model;

/**
 * Everything needed to start a match.
 *
 * @param size       board edge length
 * @param mode       local two-player or versus the computer
 * @param difficulty AI strength (ignored in {@link Mode#PVP})
 * @param swapRule   whether blue may take over red's first stone
 * @param nameA      first seat (the human in AI mode)
 * @param nameB      second seat (ignored in AI mode)
 * @param seatAColor colour played by seat A in this game
 */
public record GameConfig(int size, Mode mode, Difficulty difficulty, boolean swapRule,
                         String nameA, String nameB, Player seatAColor) {

    public enum Mode {PVP, AI}

    public static final String AI_NAME = "HexBot";
    public static final int[] SIZES = {5, 7, 9, 11, 13, 17};

    public static GameConfig defaults() {
        return new GameConfig(11, Mode.PVP, Difficulty.MEDIUM, false, "Oyuncu 1", "Oyuncu 2", Player.RED);
    }

    public GameConfig withSeatAColor(Player color) {
        return new GameConfig(size, mode, difficulty, swapRule, nameA, nameB, color);
    }

    public String nameOf(Player p) {
        if (p == seatAColor) {
            return nameA;
        }
        return mode == Mode.AI ? AI_NAME : nameB;
    }

    public boolean isAI(Player p) {
        return mode == Mode.AI && p != seatAColor;
    }

    /** 0 for seat A, 1 for seat B. */
    public int seatOf(Player p) {
        return p == seatAColor ? 0 : 1;
    }
}
