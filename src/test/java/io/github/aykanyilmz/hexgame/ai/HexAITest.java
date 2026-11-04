package io.github.aykanyilmz.hexgame.ai;

import io.github.aykanyilmz.hexgame.model.Board;
import io.github.aykanyilmz.hexgame.model.Difficulty;
import io.github.aykanyilmz.hexgame.model.Player;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class HexAITest {

    private static int[] move(Board b, Difficulty d) {
        return HexAI.chooseMove(b.toCodes(), b.size(), b.currentPlayer().code(), d, false, new Random(1));
    }

    @Test
    void takesImmediateWin() {
        Board b = new Board(5);
        // red: row 2 cols 0..3, blue elsewhere; red to move should finish at (2,4)
        int[][] red = {{2, 0}, {2, 1}, {2, 2}, {2, 3}};
        int[][] blue = {{0, 0}, {0, 1}, {4, 4}, {4, 3}};
        for (int i = 0; i < 4; i++) {
            b.play(red[i][0], red[i][1]);
            b.play(blue[i][0], blue[i][1]);
        }
        // (2,4) and (1,4) both complete the chain (hex neighbours of (2,3))
        for (Difficulty d : Difficulty.values()) {
            int[] m = move(b, d);
            b.play(m[0], m[1]);
            assertEquals(Player.RED, b.winner(), "difficulty " + d);
            b.undo();
        }
    }

    @Test
    void blocksOpponentsOnlyWinningCell() {
        Board b = new Board(5);
        // Blue owns column 2, rows 0..3. Its hex neighbours on the last row are (4,1) and (4,2);
        // red already sits on (4,1), so (4,2) is the single winning cell red must take.
        int[][] red = {{0, 0}, {4, 1}, {1, 0}, {3, 0}};
        int[][] blue = {{0, 2}, {1, 2}, {2, 2}, {3, 2}};
        for (int i = 0; i < 4; i++) {
            b.play(red[i][0], red[i][1]);
            b.play(blue[i][0], blue[i][1]);
        }
        assertEquals(Player.RED, b.currentPlayer());
        for (Difficulty d : Difficulty.values()) {
            assertArrayEquals(new int[]{4, 2}, move(b, d), "difficulty " + d);
        }
    }

    @Test
    void blocksOneOfSeveralThreatsEvenWhenLost() {
        Board b = new Board(5);
        // same shape but (4,1) and (4,2) are both open: red cannot stop both, it must still block one
        int[][] red = {{0, 0}, {4, 4}, {1, 0}, {3, 0}};
        int[][] blue = {{0, 2}, {1, 2}, {2, 2}, {3, 2}};
        for (int i = 0; i < 4; i++) {
            b.play(red[i][0], red[i][1]);
            b.play(blue[i][0], blue[i][1]);
        }
        int[] m = move(b, Difficulty.HARD);
        assertTrue(m[0] == 4 && (m[1] == 1 || m[1] == 2), "played " + m[0] + "," + m[1]);
    }

    @Test
    void openingIsCentralWithoutSwapRule() {
        Board b = new Board(9);
        assertArrayEquals(new int[]{4, 4}, move(b, Difficulty.HARD));
    }

    @Test
    void swapDecisionPrefersCentralOpenings() {
        assertTrue(HexAI.shouldSwap(11, 5, 5));
        assertFalse(HexAI.shouldSwap(11, 0, 0));
        assertFalse(HexAI.shouldSwap(11, 10, 3));
    }

    @Test
    void swapAccuracyDependsOnDifficulty() {
        Random rng = new Random(7);
        int trials = 2000;
        int[] swaps = new int[Difficulty.values().length];
        for (Difficulty d : Difficulty.values()) {
            for (int i = 0; i < trials; i++) {
                // the centre is a textbook swap: the right answer is always "yes"
                if (HexAI.shouldSwap(11, 5, 5, d, rng)) {
                    swaps[d.ordinal()]++;
                }
            }
        }
        assertEquals(trials, swaps[Difficulty.HARD.ordinal()]);
        double medium = swaps[Difficulty.MEDIUM.ordinal()] / (double) trials;
        double easy = swaps[Difficulty.EASY.ordinal()] / (double) trials;
        assertTrue(medium > 0.85 && medium < 0.95, "medium swapped " + medium);
        assertTrue(easy > 0.53 && easy < 0.67, "easy swapped " + easy);
    }

    @Test
    void savesBridgeWhenOpponentIntrudes() {
        // Red: (2,0)-(2,1) and (3,2)-(3,3) joined by a bridge over (2,2)/(3,1); (3,3) reaches the
        // right edge through (2,4) or (3,4). Blue just intruded at (2,2), so red must answer (3,1):
        // afterwards it has two unstoppable ways to finish, otherwise blue cuts at (3,1).
        Board b = new Board(5);
        int[][] red = {{2, 0}, {2, 1}, {3, 2}, {3, 3}};
        int[][] blue = {{0, 3}, {0, 4}, {1, 4}, {2, 2}};
        for (int i = 0; i < 4; i++) {
            b.play(red[i][0], red[i][1]);
            b.play(blue[i][0], blue[i][1]);
        }
        for (Difficulty d : new Difficulty[]{Difficulty.MEDIUM, Difficulty.HARD}) {
            for (int seed = 0; seed < 10; seed++) {
                int[] m = HexAI.chooseMove(b.toCodes(), 5, Player.RED.code(), d, false, new Random(seed));
                assertArrayEquals(new int[]{3, 1}, m, d + " seed " + seed);
            }
        }
    }

    @Test
    void mediumBeatsEasyMostOfTheTime() {
        assertTrue(wins(Difficulty.MEDIUM, Difficulty.EASY, 7, 6) >= 5);
    }

    @Test
    void hardBeatsMediumMoreOftenThanNot() {
        assertTrue(wins(Difficulty.HARD, Difficulty.MEDIUM, 9, 8) >= 5);
    }

    /** Games won by {@code a} against {@code b}, alternating colours, from a random first stone. */
    private static int wins(Difficulty a, Difficulty b, int n, int games) {
        Random rng = new Random(42);
        int won = 0;
        for (int game = 0; game < games; game++) {
            Board board = new Board(n);
            Player pa = game % 2 == 0 ? Player.RED : Player.BLUE;
            board.play(rng.nextInt(n), rng.nextInt(n));
            while (board.winner() == null) {
                Difficulty d = board.currentPlayer() == pa ? a : b;
                int[] m = HexAI.chooseMove(board.toCodes(), n, board.currentPlayer().code(), d, false, rng);
                board.play(m[0], m[1]);
            }
            if (board.winner() == pa) {
                won++;
            }
        }
        return won;
    }

    @Test
    void hardBeatsEasyMostOfTheTime() {
        int hardWins = 0;
        Random rng = new Random(42);
        for (int game = 0; game < 6; game++) {
            Board b = new Board(7);
            Player hard = game % 2 == 0 ? Player.RED : Player.BLUE;
            while (b.winner() == null) {
                Difficulty d = b.currentPlayer() == hard ? Difficulty.HARD : Difficulty.EASY;
                int[] m = HexAI.chooseMove(b.toCodes(), 7, b.currentPlayer().code(), d, false, rng);
                b.play(m[0], m[1]);
            }
            if (b.winner() == hard) {
                hardWins++;
            }
        }
        assertTrue(hardWins >= 5, "hard won only " + hardWins + "/6");
    }
}
