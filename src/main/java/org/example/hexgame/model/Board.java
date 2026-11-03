package org.example.hexgame.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Rhombus shaped Hex board with full move history (undo + swap rule support).
 * <p>
 * Cell (r, c) touches (r, c±1), (r-1, c), (r-1, c+1), (r+1, c) and (r+1, c-1).
 */
public final class Board {

    public static final int[][] DIRECTIONS = {{0, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}};

    /** A single entry of the move history. {@code swap} marks a pie-rule swap made by blue. */
    public record Move(int row, int col, Player player, boolean swap) {
    }

    private final int size;
    private final Player[][] cells;
    private final List<Move> history = new ArrayList<>();

    public Board(int size) {
        if (size < 2) {
            throw new IllegalArgumentException("Board size must be at least 2");
        }
        this.size = size;
        this.cells = new Player[size][size];
    }

    public int size() {
        return size;
    }

    public Player get(int row, int col) {
        return cells[row][col];
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    public boolean isEmpty(int row, int col) {
        return cells[row][col] == null;
    }

    public Player currentPlayer() {
        return history.size() % 2 == 0 ? Player.RED : Player.BLUE;
    }

    public int moveCount() {
        return history.size();
    }

    public List<Move> history() {
        return Collections.unmodifiableList(history);
    }

    public Optional<Move> lastMove() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.get(history.size() - 1));
    }

    public int movesBy(Player player) {
        int count = 0;
        for (Move m : history) {
            if (m.player() == player) {
                count++;
            }
        }
        return count;
    }

    public Move play(int row, int col) {
        if (!inBounds(row, col)) {
            throw new IllegalArgumentException("Out of bounds: " + row + "," + col);
        }
        if (!isEmpty(row, col)) {
            throw new IllegalStateException("Cell already taken: " + row + "," + col);
        }
        if (winner() != null) {
            throw new IllegalStateException("Game is already over");
        }
        Player p = currentPlayer();
        cells[row][col] = p;
        Move move = new Move(row, col, p, false);
        history.add(move);
        return move;
    }

    /** Swap (pie) rule is available to blue right after red's very first stone. */
    public boolean canSwap() {
        return history.size() == 1;
    }

    /**
     * Blue takes over red's opening stone. To keep the edge goals consistent the stone
     * is mirrored across the long diagonal: red's (r, c) becomes blue's (c, r).
     */
    public Move swap() {
        if (!canSwap()) {
            throw new IllegalStateException("Swap is not available");
        }
        Move first = history.get(0);
        cells[first.row()][first.col()] = null;
        cells[first.col()][first.row()] = Player.BLUE;
        Move move = new Move(first.col(), first.row(), Player.BLUE, true);
        history.add(move);
        return move;
    }

    public Move undo() {
        if (history.isEmpty()) {
            throw new IllegalStateException("Nothing to undo");
        }
        Move move = history.remove(history.size() - 1);
        cells[move.row()][move.col()] = null;
        if (move.swap()) {
            Move first = history.get(0);
            cells[first.row()][first.col()] = first.player();
        }
        return move;
    }

    public Player winner() {
        for (Player p : Player.values()) {
            if (winningPath(p) != null) {
                return p;
            }
        }
        return null;
    }

    /**
     * Shortest chain of {@code player}'s stones linking its two edges, ordered from the
     * starting edge (left for red, top for blue), or {@code null} if none exists.
     */
    public List<int[]> winningPath(Player player) {
        int n = size;
        int[] prev = new int[n * n];
        Arrays.fill(prev, -2);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            int r = player == Player.RED ? i : 0;
            int c = player == Player.RED ? 0 : i;
            if (cells[r][c] == player) {
                prev[r * n + c] = -1;
                queue.add(r * n + c);
            }
        }
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            int r = cur / n;
            int c = cur % n;
            if (player == Player.RED ? c == n - 1 : r == n - 1) {
                LinkedList<int[]> path = new LinkedList<>();
                for (int x = cur; x != -1; x = prev[x]) {
                    path.addFirst(new int[]{x / n, x % n});
                }
                return path;
            }
            for (int[] d : DIRECTIONS) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (inBounds(nr, nc) && cells[nr][nc] == player && prev[nr * n + nc] == -2) {
                    prev[nr * n + nc] = cur;
                    queue.add(nr * n + nc);
                }
            }
        }
        return null;
    }

    /** Row-major snapshot: 0 = empty, 1 = red, 2 = blue. */
    public int[] toCodes() {
        int[] out = new int[size * size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                out[r * size + c] = cells[r][c] == null ? 0 : cells[r][c].code();
            }
        }
        return out;
    }
}
