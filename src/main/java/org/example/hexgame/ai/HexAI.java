package org.example.hexgame.ai;

import org.example.hexgame.model.Board;
import org.example.hexgame.model.Difficulty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Hex computer opponent.
 * <p>
 * Positions are scored with a "resistance-free" shortest connection metric: for each player
 * a 0-1 BFS measures how many more empty cells it needs to link its edges (own stones cost 0,
 * empty cells 1, enemy stones are walls). The secondary term counts the empty cells lying on
 * <i>some</i> shortest path - a measure of how many ways a player still has to finish.
 * <ul>
 *     <li>Easy: 1-ply with heavy noise and the occasional sloppy move.</li>
 *     <li>Medium: 1-ply, deterministic apart from tie-breaking.</li>
 *     <li>Hard: 2-ply minimax over the best 1-ply candidates.</li>
 * </ul>
 * Works purely on {@code int[]} snapshots so it can run off the FX thread.
 */
public final class HexAI {

    private static final int INF = 1_000_000;
    private static final double WIN = 1e7;
    /** Tie-breakers: one "distance step" is worth 100. */
    private static final double CENTER_WEIGHT = 2.5;
    private static final double FLEX_WEIGHT = 0.2;

    private final int n;
    private final int[][] neighbours;
    private final Random rng;

    private HexAI(int n, Random rng) {
        this.n = n;
        this.rng = rng;
        this.neighbours = new int[n * n][];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                List<Integer> list = new ArrayList<>(6);
                for (int[] d : Board.DIRECTIONS) {
                    int nr = r + d[0];
                    int nc = c + d[1];
                    if (nr >= 0 && nr < n && nc >= 0 && nc < n) {
                        list.add(nr * n + nc);
                    }
                }
                neighbours[r * n + c] = list.stream().mapToInt(Integer::intValue).toArray();
            }
        }
    }

    /**
     * @param cells    row-major board codes (0 empty, 1 red, 2 blue)
     * @param n        board size
     * @param me       code of the player to move
     * @param swapRule whether the opponent may swap our opening stone
     * @return {row, col}
     */
    public static int[] chooseMove(int[] cells, int n, int me, Difficulty difficulty, boolean swapRule, Random rng) {
        return new HexAI(n, rng).choose(cells.clone(), me, difficulty, swapRule);
    }

    /** Pie-rule decision for blue: take red's opening stone if it is too strong to leave alone. */
    public static boolean shouldSwap(int n, int row, int col) {
        double center = (n - 1) / 2.0;
        double dr = row - center;
        double dc = col - center;
        // hex distance in axial coordinates
        double dist = (Math.abs(dr) + Math.abs(dc) + Math.abs(dr + dc)) / 2.0;
        boolean onEdge = row == 0 || col == 0 || row == n - 1 || col == n - 1;
        return !onEdge && dist <= Math.max(1, n / 4.0);
    }

    private int[] choose(int[] b, int me, Difficulty difficulty, boolean swapRule) {
        List<Integer> empties = new ArrayList<>();
        for (int i = 0; i < b.length; i++) {
            if (b[i] == 0) {
                empties.add(i);
            }
        }
        if (empties.isEmpty()) {
            throw new IllegalStateException("Board is full");
        }
        if (empties.size() == b.length) {
            return opening(difficulty, swapRule);
        }

        // 1. Win immediately when possible.
        for (int e : empties) {
            b[e] = me;
            boolean wins = distance(b, me) == 0;
            b[e] = 0;
            if (wins) {
                return rc(e);
            }
        }

        // 2. If the opponent threatens to win next move, only blocking moves are worth looking at
        //    (with several threats the game is lost, but blocking one still looks - and is - sensible).
        int opp = 3 - me;
        List<Integer> threats = new ArrayList<>();
        for (int e : empties) {
            b[e] = opp;
            if (distance(b, opp) == 0) {
                threats.add(e);
            }
            b[e] = 0;
        }
        if (!threats.isEmpty()) {
            empties = threats;
        }

        // 3. One-ply evaluation of every candidate cell.
        double[] score = new double[b.length];
        for (int e : empties) {
            b[e] = me;
            score[e] = evaluate(b, me) - centrality(e) * CENTER_WEIGHT;
            b[e] = 0;
        }
        Integer[] ranked = empties.toArray(new Integer[0]);
        Arrays.sort(ranked, (x, y) -> Double.compare(score[y], score[x]));

        switch (difficulty) {
            case EASY -> {
                if (rng.nextDouble() < 0.2) {
                    return rc(ranked[rng.nextInt(Math.min(ranked.length, 10))]);
                }
                int best = ranked[0];
                double bestNoisy = -Double.MAX_VALUE;
                for (int e : ranked) {
                    double noisy = score[e] + rng.nextGaussian() * 110;
                    if (noisy > bestNoisy) {
                        bestNoisy = noisy;
                        best = e;
                    }
                }
                return rc(best);
            }
            case MEDIUM -> {
                return rc(pickAmongTies(ranked, score, 1.0));
            }
            default -> {
                return rc(twoPly(b, me, ranked, score));
            }
        }
    }

    private int twoPly(int[] b, int me, Integer[] ranked, double[] oneply) {
        int opp = 3 - me;
        int width = Math.min(ranked.length, n <= 7 ? ranked.length : 12);
        int best = ranked[0];
        double bestScore = -Double.MAX_VALUE;
        for (int k = 0; k < width; k++) {
            int m = ranked[k];
            b[m] = me;
            double worst = Double.MAX_VALUE;
            for (int r = 0; r < b.length && worst > -WIN; r++) {
                if (b[r] != 0) {
                    continue;
                }
                b[r] = opp;
                worst = Math.min(worst, evaluate(b, me));
                b[r] = 0;
            }
            b[m] = 0;
            if (worst == Double.MAX_VALUE) {
                worst = oneply[m];
            }
            double s = worst - centrality(m) * CENTER_WEIGHT + rng.nextDouble() * 0.001;
            if (s > bestScore) {
                bestScore = s;
                best = m;
            }
        }
        return best;
    }

    private int pickAmongTies(Integer[] ranked, double[] score, double tolerance) {
        double top = score[ranked[0]];
        int ties = 1;
        while (ties < ranked.length && top - score[ranked[ties]] <= tolerance) {
            ties++;
        }
        return ranked[rng.nextInt(ties)];
    }

    private int[] opening(Difficulty difficulty, boolean swapRule) {
        int mid = n / 2;
        if (!swapRule && difficulty != Difficulty.EASY) {
            return new int[]{mid, mid};
        }
        // With the swap rule a dead-centre opening would just get stolen: play a modest cell instead.
        List<int[]> options = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                boolean modest = swapRule ? !shouldSwap(n, r, c) && r > 0 && r < n - 1 && c > 0 && c < n - 1
                        : Math.abs(r - mid) + Math.abs(c - mid) <= 2;
                if (modest) {
                    options.add(new int[]{r, c});
                }
            }
        }
        return options.isEmpty() ? new int[]{mid, mid} : options.get(rng.nextInt(options.size()));
    }

    // ------------------------------------------------------------------ evaluation

    private double evaluate(int[] b, int me) {
        int opp = 3 - me;
        int[] mine = analyse(b, me);
        int[] theirs = analyse(b, opp);
        if (mine[0] == 0) {
            return WIN;
        }
        if (theirs[0] == 0) {
            return -WIN;
        }
        if (mine[0] >= INF) {
            return -WIN / 2;
        }
        if (theirs[0] >= INF) {
            return WIN / 2;
        }
        return 100.0 * (theirs[0] - mine[0]) + FLEX_WEIGHT * (mine[1] - theirs[1]);
    }

    /** @return {shortest connection distance, number of empty cells on any shortest path} */
    private int[] analyse(int[] b, int p) {
        int[] fromStart = bfs(b, p, true);
        int[] fromEnd = bfs(b, p, false);
        int best = INF;
        for (int i = 0; i < n; i++) {
            best = Math.min(best, fromStart[edge(p, false, i)]);
        }
        int flex = 0;
        if (best < INF) {
            for (int i = 0; i < b.length; i++) {
                if (b[i] == 0 && fromStart[i] < INF && fromEnd[i] < INF && fromStart[i] + fromEnd[i] - 1 == best) {
                    flex++;
                }
            }
        }
        return new int[]{best, flex};
    }

    private int distance(int[] b, int p) {
        int[] d = bfs(b, p, true);
        int best = INF;
        for (int i = 0; i < n; i++) {
            best = Math.min(best, d[edge(p, false, i)]);
        }
        return best;
    }

    /** 0-1 BFS from one of the player's edges; a cell's distance includes its own cost. */
    private int[] bfs(int[] b, int p, boolean fromStart) {
        int[] d = new int[b.length];
        Arrays.fill(d, INF);
        ArrayDeque<Integer> dq = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            int idx = edge(p, fromStart, i);
            int cost = cost(b[idx], p);
            if (cost < INF && cost < d[idx]) {
                d[idx] = cost;
                if (cost == 0) {
                    dq.addFirst(idx);
                } else {
                    dq.addLast(idx);
                }
            }
        }
        while (!dq.isEmpty()) {
            int u = dq.pollFirst();
            for (int v : neighbours[u]) {
                int cost = cost(b[v], p);
                if (cost >= INF) {
                    continue;
                }
                int nd = d[u] + cost;
                if (nd < d[v]) {
                    d[v] = nd;
                    if (cost == 0) {
                        dq.addFirst(v);
                    } else {
                        dq.addLast(v);
                    }
                }
            }
        }
        return d;
    }

    private static int cost(int cell, int p) {
        if (cell == p) {
            return 0;
        }
        return cell == 0 ? 1 : INF;
    }

    /** Red (1) links column 0 to column n-1, blue (2) links row 0 to row n-1. */
    private int edge(int p, boolean start, int i) {
        int line = start ? 0 : n - 1;
        return p == 1 ? i * n + line : line * n + i;
    }

    private double centrality(int idx) {
        double center = (n - 1) / 2.0;
        double dr = idx / n - center;
        double dc = idx % n - center;
        return (Math.abs(dr) + Math.abs(dc) + Math.abs(dr + dc)) / 2.0;
    }

    private int[] rc(int idx) {
        return new int[]{idx / n, idx % n};
    }
}
