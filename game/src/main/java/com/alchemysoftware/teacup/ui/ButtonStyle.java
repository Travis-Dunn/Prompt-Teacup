package com.alchemysoftware.teacup.ui;

import com.alchemysoftware.teacup.text.TextStyle;

/**
 * How a MenuButton looks. Shared by every button of a kind; text is read
 * when a button is created, the colors every frame. There are no defaults:
 * text must be assigned and each color filled in before use.
 */
public final class ButtonStyle {
    /** Style of the button's label. */
    public TextStyle text;

    /** Clickable margin around the label's text box, in virtual pixels. */
    public float padX, padY;

    /** Label tints as r, g, b, a, not premultiplied. idle when the mouse is
     *  elsewhere, hover when it is over the button, down while the button
     *  is being pressed. */
    public final float[] idle = new float[4];
    public final float[] hover = new float[4];
    public final float[] down = new float[4];

    public ButtonStyle() {}
}
