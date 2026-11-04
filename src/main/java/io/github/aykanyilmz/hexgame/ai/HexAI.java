package io.github.aykanyilmz.hexgame.ai;

import io.github.aykanyilmz.hexgame.model.Board;
import io.github.aykanyilmz.hexgame.model.Difficulty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Hex computer opponent.
 * <p>
 * Two position metrics are used:
 * <ul>
 *     <li><b>Shortest distance</b> (Easy): a 0-1 BFS measures how many more empty cells a player
 *     needs to link its edges (own stones cost 0, empty cells 1, enemy stones are walls).</li>
 *     <li><b>Two-distance</b> (Medium, Hard): a cell is only as close to an edge as its
 *     <i>second best</i> neighbour, because the opponent will always cut the best one. Single-file
 *     routes therefore count as weak and double routes such as bridges as strong - the classic
 *     evaluation of Hex programs like Queenbee.</li>
 * </ul>
 * Search:
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
    private static final double MOBILITY_WEIGHT = 1.0;

    private final int n;
    private final int[][] neighbours;
    private final Random rng;
    /** Scratch space of {@link #groups}: adjacent empty cells and edge contacts per group. */
    private final List<int[]> groupEmpties = new ArrayList<>();
    private final List<Integer> groupTouches = new ArrayList<>();

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

    /**
     * Pie-rule decision as a player of the given strength would make it: Hard always gets it
     * right, Medium slips now and then and Easy is close to a coin flip.
     */
    public static boolean shouldSwap(int n, int row, int col, Difficulty difficulty, Random rng) {
        boolean ideal = shouldSwap(n, row, col);
        double accuracy = switch (difficulty) {
            case EASY -> 0.6;
            case MEDIUM -> 0.9;
            case HARD -> 1.0;
        };
        return rng.nextDouble() < accuracy ? ideal : !ideal;
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
        boolean strong = difficulty != Difficulty.EASY;
        double[] score = new double[b.length];
        for (int e : empties) {
            b[e] = me;
            score[e] = (strong ? evaluateTwoDistance(b, me) : evaluate(b, me)) - centrality(e) * CENTER_WEIGHT;
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
                worst = Math.min(worst, evaluateTwoDistance(b, me));
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

    /**
     * Two-distance evaluation from {@code me}'s point of view. One potential step is worth 100;
     * having more cells that realise the best potential (more ways to finish) breaks ties.
     */
    private double evaluateTwoDistance(int[] b, int me) {
        int opp = 3 - me;
        int mineDist = distance(b, me);
        int theirDist = distance(b, opp);
        if (mineDist == 0) {
            return WIN;
        }
        if (theirDist == 0) {
            return -WIN;
        }
        if (mineDist >= INF) {
            return -WIN / 2;
        }
        if (theirDist >= INF) {
            return WIN / 2;
        }
        int[] mine = potential(b, me, mineDist);
        int[] theirs = potential(b, opp, theirDist);
        return 100.0 * (theirs[0] - mine[0]) + MOBILITY_WEIGHT * (mine[1] - theirs[1]);
    }

    /**
     * @param plain the player's ordinary shortest distance, used when no two-distance route exists
     * @return {best two-distance potential, number of empty cells achieving it}
     */
    private int[] potential(int[] b, int p, int plain) {
        int[] group = groups(b, p);
        int[] fromStart = twoDistance(b, p, group, true);
        int[] fromEnd = twoDistance(b, p, group, false);
        int best = INF;
        int count = 0;
        for (int i = 0; i < b.length; i++) {
            if (b[i] != 0 || fromStart[i] >= INF || fromEnd[i] >= INF) {
                continue;
            }
            int pot = fromStart[i] + fromEnd[i];
            if (pot < best) {
                best = pot;
                count = 1;
            } else if (pot == best) {
                count++;
            }
        }
        if (best >= INF) {
            // Only single-file routes are left: still reachable, but every one of them can be cut.
            return new int[]{plain + n, 0};
        }
        return new int[]{best, count};
    }

    /**
     * Labels the connected groups of {@code p}'s stones and returns, per cell, its group id or -1.
     * Each group's adjacent empty cells and edge contacts go to the scratch lists.
     */
    private int[] groups(int[] b, int p) {
        int[] id = new int[b.length];
        Arrays.fill(id, -1);
        groupEmpties.clear();
        groupTouches.clear();
        int[] stack = new int[b.length];
        int[] seen = new int[b.length];
        int stamp = 0;
        for (int s = 0; s < b.length; s++) {
            if (b[s] != p || id[s] >= 0) {
                continue;
            }
            int g = groupEmpties.size();
            stamp++;
            List<Integer> empties = new ArrayList<>();
            int touches = 0;
            int top = 0;
            stack[top++] = s;
            id[s] = g;
            while (top > 0) {
                int u = stack[--top];
                touches |= edgeMask(p, u);
                for (int v : neighbours[u]) {
                    if (b[v] == p && id[v] < 0) {
                        id[v] = g;
                        stack[top++] = v;
                    } else if (b[v] == 0 && seen[v] != stamp) {
                        seen[v] = stamp;
                        empties.add(v);
                    }
                }
            }
            groupEmpties.add(empties.stream().mapToInt(Integer::intValue).toArray());
            groupTouches.add(touches);
        }
        return id;
    }

    /** Bit 1: the cell lies on the player's start edge, bit 2: on its end edge. */
    private int edgeMask(int p, int idx) {
        int line = p == 1 ? idx % n : idx / n;
        return (line == 0 ? 1 : 0) | (line == n - 1 ? 2 : 0);
    }

    /**
     * Two-distance of every empty cell to one of {@code p}'s edges. Own stone groups are
     * transparent: a cell next to a group neighbours every empty cell around that group.
     */
    private int[] twoDistance(int[] b, int p, int[] group, boolean fromStart) {
        int bit = fromStart ? 1 : 2;
        int[] d = new int[b.length];
        Arrays.fill(d, INF);
        int[] hits = new int[b.length];
        int[] seen = new int[b.length];
        int[] queue = new int[b.length];
        int head = 0;
        int tail = 0;

        // Cells touching the edge, directly or through an own group, are one step away.
        for (int i = 0; i < b.length; i++) {
            if (b[i] != 0) {
                continue;
            }
            boolean touching = (edgeMask(p, i) & bit) != 0;
            for (int k = 0; !touching && k < neighbours[i].length; k++) {
                int g = group[neighbours[i][k]];
                touching = g >= 0 && (groupTouches.get(g) & bit) != 0;
            }
            if (touching) {
                d[i] = 1;
                queue[tail++] = i;
            }
        }

        // Values leave the queue in non-decreasing order, so the second neighbour to reach a
        // cell is its second best one.
        while (head < tail) {
            int u = queue[head++];
            int stamp = u + 1;
            for (int v : neighbours[u]) {
                if (b[v] == 0) {
                    tail = relax(v, u, stamp, d, hits, seen, queue, tail);
                } else if (b[v] == p) {
                    for (int x : groupEmpties.get(group[v])) {
                        tail = relax(x, u, stamp, d, hits, seen, queue, tail);
                    }
                }
            }
        }
        return d;
    }

    private static int relax(int x, int from, int stamp, int[] d, int[] hits, int[] seen, int[] queue, int tail) {
        if (x == from || seen[x] == stamp || d[x] < INF) {
            return tail;
        }
        seen[x] = stamp;
        if (++hits[x] == 2) {
            d[x] = d[from] + 1;
            queue[tail++] = x;
        }
        return tail;
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
