package com.alchemysoftware.teacup.text;

import java.awt.Font;

/**
 * How a label's text is laid out and rasterized. Read only when a label is
 * baked; changing a style afterwards does not affect labels already baked
 * with it. There are no defaults: font, size, lineSpacing, and align must be
 * assigned before use. Sizes are in reference units (1080p pixels).
 */
public final class TextStyle {
    /**
     * Horizontal alignment. Sets both how lines are justified against the
     * widest line and which point of the text a label's x position names:
     * the left edge, the center, or the right edge.
     */
    public enum Align { LEFT, CENTER, RIGHT }

    /** Base font, as returned by Text.LoadFont(). */
    public Font font;

    /** Em size in reference units. Must be greater than zero. */
    public float size;

    /** Extra space between glyphs as a fraction of the em size. 0 is the
     *  font's own spacing; negative values tighten. */
    public float tracking;

    /** Multiplier of the font's natural line height. Must be greater than
     *  zero. */
    public float lineSpacing;

    /** Wrap width in reference units. 0 breaks lines only at '\n'. */
    public float wrapW;

    public Align align;

    public TextStyle() {}
}
