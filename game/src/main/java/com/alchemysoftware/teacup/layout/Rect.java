package com.alchemysoftware.teacup.layout;

/**
 * Axis-aligned rectangle in virtual pixels, top-left origin, +y down.
 * Mutable so callers can keep and reuse instances instead of allocating.
 */
public final class Rect {
    public float x, y, w, h;

    public Rect() {}

    /**
     * @param x left edge
     * @param y top edge
     * @param w width
     * @param h height
     */
    public void set(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
