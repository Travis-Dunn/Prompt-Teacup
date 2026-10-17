package com.alchemysoftware.teacup.text;

import org.lwjgl.util.vector.Matrix4f;

import com.alchemysoftware.whitetail.renderer.ModelInstance;

/**
 * One piece of baked text, created by Text.Create() and released by
 * Text.Destroy().
 *
 * The position names the label's anchor point: on the first line's
 * baseline, at the left edge, center, or right edge of the text depending on
 * the style's alignment. Positions are in reference units.
 *
 * Position and color changes are free. setText() and setStyle() re-bake,
 * which allocates; treat them as load-time operations.
 */
public final class Label {
    /* Null once destroyed. */
    ModelInstance inst;
    TextMaterial mat;

    TextStyle style;
    String text;

    /* Baked geometry in framebuffer pixels. The anchor is relative to the
       texture's top-left corner. */
    int texW, texH;
    float anchorX, anchorY;
    float boxW, boxH, ascent;

    private float x, y;

    Label(ModelInstance inst, TextStyle style, String text) {
        this.inst = inst;
        this.mat = (TextMaterial) inst.model.material;
        this.style = style;
        this.text = text;
    }

    /**
     * Re-bakes with new text. Does nothing if the text is unchanged.
     *
     * @param text the new text; '\n' starts a new line
     */
    public void setText(String text) {
        assert inst != null : ERR_STR_DESTROYED;
        assert text != null : ERR_STR_TEXT_NULL;

        if (text.equals(this.text)) return;
        this.text = text;
        Text.Bake(this);
    }

    /**
     * Re-bakes with a new style, keeping the position.
     *
     * @param style the new style
     */
    public void setStyle(TextStyle style) {
        assert inst != null : ERR_STR_DESTROYED;

        this.style = style;
        Text.Bake(this);
    }

    /**
     * @param x anchor x in reference units
     * @param y anchor y (first baseline) in reference units
     */
    public void setPosition(float x, float y) {
        assert inst != null : ERR_STR_DESTROYED;

        this.x = x;
        this.y = y;
        updateMatrix();
    }

    /**
     * Sets the tint. Components are not premultiplied.
     *
     * @param r red, 0 to 1
     * @param g green, 0 to 1
     * @param b blue, 0 to 1
     * @param a opacity, 0 to 1
     */
    public void setColor(float r, float g, float b, float a) {
        assert inst != null : ERR_STR_DESTROYED;

        mat.r = r;
        mat.g = g;
        mat.b = b;
        mat.a = a;
    }

    /**
     * @return width of the widest line in reference units
     */
    public float getW() { return boxW / Text.scale; }

    /**
     * @return height from the first line's ascent to the last line's
     *         descent, in reference units
     */
    public float getH() { return boxH / Text.scale; }

    /**
     * @return distance from the top of the text box down to the first
     *         baseline, in reference units
     */
    public float getAscent() { return ascent / Text.scale; }

    /* Snaps the texture to whole framebuffer pixels so each texel lands on
       exactly one pixel. */
    void updateMatrix() {
        Matrix4f m = inst.matModel;

        m.m00 = texW;
        m.m11 = texH;
        m.m30 = Math.round(x * Text.scale - anchorX);
        m.m31 = Math.round(y * Text.scale - anchorY);
    }

    private static final String CLASS = Label.class.getSimpleName();
    private static final String ERR_STR_DESTROYED = CLASS +
            " was used after it was destroyed.\n";
    private static final String ERR_STR_TEXT_NULL = CLASS +
            " failed to set text because the text was null.\n";
}
