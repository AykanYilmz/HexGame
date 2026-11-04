package io.github.aykanyilmz.hexgame.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    /** Plays red at the given cells, with blue answering on the given cells. */
    private static Board play(int size, int[][] red, int[][] blue) {
        Board b = new Board(size);
        for (int i = 0; i < red.length; i++) {
            b.play(red[i][0], red[i][1]);
            if (i < blue.length) {
                b.play(blue[i][0], blue[i][1]);
            }
        }
        return b;
    }

    @Test
    void redMovesFirstAndTurnsAlternate() {
        Board b = new Board(5);
        assertEquals(Player.RED, b.currentPlayer());
        b.play(2, 2);
        assertEquals(Player.BLUE, b.currentPlayer());
        assertEquals(Player.RED, b.get(2, 2));
    }

    @Test
    void redWinsWithLeftToRightRow() {
        Board b = play(5, new int[][]{{2, 0}, {2, 1}, {2, 2}, {2, 3}, {2, 4}},
                new int[][]{{0, 0}, {0, 1}, {0, 2}, {0, 3}});
        assertEquals(Player.RED, b.winner());
        assertEquals(5, b.winningPath(Player.RED).size());
    }

    @Test
    void blueWinsWithTopToBottomColumn() {
        Board b = play(4, new int[][]{{0, 0}, {1, 0}, {2, 0}, {3, 3}},
                new int[][]{{0, 2}, {1, 2}, {2, 2}, {3, 2}});
        assertEquals(Player.BLUE, b.winner());
    }

    @Test
    void diagonalHexNeighbourConnects() {
        // (r, c) -> (r+1, c-1) is a hex neighbour: a blue anti-diagonal must win
        Board b = play(3, new int[][]{{0, 0}, {1, 0}, {2, 2}},
                new int[][]{{0, 2}, {1, 1}, {2, 0}});
        assertEquals(Player.BLUE, b.winner());
        List<int[]> path = b.winningPath(Player.BLUE);
        assertArrayEquals(new int[]{0, 2}, path.get(0));
        assertArrayEquals(new int[]{2, 0}, path.get(path.size() - 1));
    }

    @Test
    void nonNeighbourDiagonalDoesNotConnect() {
        // (r, c) -> (r+1, c+1) is NOT adjacent on a hex grid
        Board b = play(3, new int[][]{{0, 0}, {1, 1}, {2, 2}}, new int[][]{{0, 2}, {2, 0}});
        assertNull(b.winner());
    }

    @Test
    void occupiedCellIsRejected() {
        Board b = new Board(5);
        b.play(1, 1);
        assertThrows(IllegalStateException.class, () -> b.play(1, 1));
    }

    @Test
    void cannotPlayAfterWin() {
        Board b = play(2, new int[][]{{0, 0}, {0, 1}}, new int[][]{{1, 0}});
        assertEquals(Player.RED, b.winner());
        assertThrows(IllegalStateException.class, () -> b.play(1, 1));
    }

    @Test
    void undoRestoresState() {
        Board b = new Board(5);
        b.play(1, 1);
        b.play(2, 2);
        b.undo();
        assertTrue(b.isEmpty(2, 2));
        assertEquals(Player.BLUE, b.currentPlayer());
        assertEquals(1, b.moveCount());
    }

    @Test
    void swapMirrorsStoneAndUndoRevertsIt() {
        Board b = new Board(5);
        b.play(1, 3);
        assertTrue(b.canSwap());
        b.swap();
        assertTrue(b.isEmpty(1, 3));
        assertEquals(Player.BLUE, b.get(3, 1));
        assertEquals(Player.RED, b.currentPlayer());
        assertFalse(b.canSwap());

        b.undo();
        assertEquals(Player.RED, b.get(1, 3));
        assertTrue(b.isEmpty(3, 1));
        assertEquals(Player.BLUE, b.currentPlayer());
    }

    @Test
    void swapOnDiagonalCellKeepsCell() {
        Board b = new Board(5);
        b.play(2, 2);
        b.swap();
        assertEquals(Player.BLUE, b.get(2, 2));
        b.undo();
        assertEquals(Player.RED, b.get(2, 2));
    }

    @Test
    void fullBoardAlwaysHasExactlyOneWinner() {
        // Hex has no draws: fill randomly until someone wins and check consistency.
        java.util.Random rng = new java.util.Random(7);
        for (int game = 0; game < 50; game++) {
            Board b = new Board(7);
            while (b.winner() == null) {
                int r;
                int c;
                do {
                    r = rng.nextInt(7);
                    c = rng.nextInt(7);
                } while (!b.isEmpty(r, c));
                b.play(r, c);
            }
            Player w = b.winner();
            assertNotNull(b.winningPath(w));
            assertNull(b.winningPath(w.opponent()));
        }
    }
    @Test
    void undoingTheWinningMoveReopensTheGame() {
        Board b = play(5, new int[][]{{2, 0}, {2, 1}, {2, 2}, {2, 3}, {2, 4}},
                new int[][]{{0, 0}, {0, 1}, {0, 2}, {0, 3}});
        assertEquals(Player.RED, b.winner());
        b.undo();
        assertNull(b.winner());
        assertEquals(Player.RED, b.currentPlayer());
        b.play(4, 4); // a different (non-winning) move is accepted again
        assertNull(b.winner());
    }

    @Test
    void swappedStoneCountsForBlueAndUndoingPastItRestoresRed() {
        Board b = new Board(5);
        b.play(0, 2);           // red on the top edge...
        b.swap();               // ...becomes blue's (2,0)
        assertEquals(Player.BLUE, b.get(2, 0));
        assertEquals(1, b.movesBy(Player.BLUE));
        assertEquals(1, b.movesBy(Player.RED)); // red did make a move, it just lost the stone
        assertEquals(Player.RED, b.currentPlayer());
        assertFalse(b.canSwap());

        b.play(4, 4);
        b.undo();
        b.undo();               // takes back the swap
        assertTrue(b.canSwap());
        assertEquals(Player.RED, b.get(0, 2));
        assertNull(b.get(2, 0));
        b.undo();               // and red's first stone
        assertEquals(0, b.moveCount());
        assertNull(b.get(0, 2));
    }

    @Test
    void undoOnEmptyBoardIsRejected() {
        assertThrows(IllegalStateException.class, () -> new Board(5).undo());
    }

    @Test
    void swapIsOnlyAvailableRightAfterTheFirstStone() {
        Board b = new Board(5);
        assertFalse(b.canSwap());
        assertThrows(IllegalStateException.class, b::swap);
        b.play(1, 1);
        b.play(3, 3);
        assertFalse(b.canSwap());
        assertThrows(IllegalStateException.class, b::swap);
    }
}
