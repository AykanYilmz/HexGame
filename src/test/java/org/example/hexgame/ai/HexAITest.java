package org.example.hexgame.ai;

import org.example.hexgame.model.Board;
import org.example.hexgame.model.Difficulty;
import org.example.hexgame.model.Player;
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
