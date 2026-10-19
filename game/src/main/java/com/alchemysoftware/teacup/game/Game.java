package com.alchemysoftware.teacup.game;

/**
 * One playthrough: everything that is true of the world right now.
 */
public final class Game {
    /** The system the player is in. */
    public Syst syst;

    public Game() {}

    /**
     * Starts a fresh playthrough in the first system.
     */
    public static Game New() {
        Game g = new Game();

        /* Literals until systems are loaded from data. */
        g.syst = new Syst();
        g.syst.sky.seed = 0x5157L;
        g.syst.sky.density = 0.8f;
        g.syst.sky.heroStars = 5;
        g.syst.sky.galaxies = 3;
        g.syst.sky.hazeColor[0] = 0.18f;
        g.syst.sky.hazeColor[1] = 0.30f;
        g.syst.sky.hazeColor[2] = 0.42f;
        g.syst.sky.hazeBrightness = 0.04f;
        g.syst.sky.hazeClumping = 0.5f;

        return g;
    }
}
