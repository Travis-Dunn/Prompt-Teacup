package com.alchemysoftware.teacup.ui;

import com.alchemysoftware.teacup.text.TextStyle;

/**
 * How a MenuButton looks. Shared by every button of a kind; text, panel,
 * and sizes are read when a button is created, the colors every frame.
 * There are no defaults: text must be assigned and each color filled in
 * before use.
 */
public final class ButtonStyle {
    /** Style of the button's label. Its alignment also places the label in
     *  the button: LEFT and RIGHT inset it by padX, CENTER centers it. */
    public TextStyle text;

    /** Drawn behind the label, or null for a bare label. */
    public PanelStyle panel;

    /** Margin around the label's text box, in virtual pixels. The button is
     *  the text box plus this margin, and at least minW wide. */
    public float padX, padY, minW;

    /** Label tints as r, g, b, a, not premultiplied. idle when the mouse is
     *  elsewhere, hover when it is over the button (faded in and out),
     *  down while the button is being pressed. */
    public final float[] idle = new float[4];
    public final float[] hover = new float[4];
    public final float[] down = new float[4];

    public ButtonStyle() {}
}
