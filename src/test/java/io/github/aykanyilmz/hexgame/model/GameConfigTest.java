package io.github.aykanyilmz.hexgame.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    private static GameConfig ai(Player humanColor) {
        return new GameConfig(9, GameConfig.Mode.AI, Difficulty.HARD, true, "Ada", "ignored", humanColor);
    }

    private static GameConfig pvp(Player seatAColor) {
        return new GameConfig(9, GameConfig.Mode.PVP, Difficulty.MEDIUM, false, "Ada", "Can", seatAColor);
    }

    @Test
    void aiModeGivesTheOtherColourToHexBot() {
        GameConfig c = ai(Player.RED);
        assertFalse(c.isAI(Player.RED));
        assertTrue(c.isAI(Player.BLUE));
        assertEquals("Ada", c.nameOf(Player.RED));
        assertEquals(GameConfig.AI_NAME, c.nameOf(Player.BLUE));
    }

    @Test
    void humanPlayingBlueMakesHexBotMoveFirst() {
        GameConfig c = ai(Player.BLUE);
        assertTrue(c.isAI(Player.RED));
        assertEquals(GameConfig.AI_NAME, c.nameOf(Player.RED));
        assertEquals("Ada", c.nameOf(Player.BLUE));
    }

    @Test
    void pvpHasNoComputerPlayer() {
        GameConfig c = pvp(Player.RED);
        for (Player p : Player.values()) {
            assertFalse(c.isAI(p));
        }
        assertEquals("Ada", c.nameOf(Player.RED));
        assertEquals("Can", c.nameOf(Player.BLUE));
    }

    @Test
    void rematchSwapsColoursButSeatsKeepTheirNames() {
        GameConfig first = pvp(Player.RED);
        GameConfig rematch = first.withSeatAColor(Player.BLUE);
        assertEquals("Can", rematch.nameOf(Player.RED));
        assertEquals("Ada", rematch.nameOf(Player.BLUE));
        // seat indices stay with the people, so the score follows them
        assertEquals(0, first.seatOf(Player.RED));
        assertEquals(0, rematch.seatOf(Player.BLUE));
        assertEquals(1, rematch.seatOf(Player.RED));
        // nothing else changes
        assertEquals(first.size(), rematch.size());
        assertEquals(first.mode(), rematch.mode());
        assertEquals(first.swapRule(), rematch.swapRule());
    }

    @Test
    void defaultsUseAValidBoardSize() {
        int size = GameConfig.defaults().size();
        boolean listed = false;
        for (int s : GameConfig.SIZES) {
            listed |= s == size;
        }
        assertTrue(listed);
    }
}
