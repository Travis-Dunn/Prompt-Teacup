package com.alchemysoftware.teacup.starfield;

/**
 * What one place's sky looks like. Purely visual: whatever sets these (a
 * system, from its atmospheric values) decides what they mean for play.
 * Read only by Starfield.Build(). There are no defaults: assign every field.
 */
public final class StarfieldParams {
    /** The same seed always builds the same sky. */
    public long seed;

    /** Generated stars relative to a typical sky: 1 is typical, 0 none.
     *  Hero stars and galaxies are counted separately. */
    public float density;

    /** Large, distant stars, 5 to 20 pixels; bigger ones are dimmer. */
    public int heroStars;

    /** Distant galaxies, 9 to 20 pixels, always faint. */
    public int galaxies;

    /** Background haze tint, r, g, b. */
    public final float[] hazeColor = new float[3];

    /** Haze brightness: 0 is jet black. Around 0.1 is soupy. Haze also
     *  veils the stars, the farthest most. */
    public float hazeBrightness;

    /** 0 is an even veil; 1 is patchy and nebulous. */
    public float hazeClumping;

    public StarfieldParams() {}
}
