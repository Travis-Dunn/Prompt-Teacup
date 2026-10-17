package com.alchemysoftware.teacup.layout;

/**
 * A point on a rectangle, used to pin a child to the matching point of its
 * parent. fx and fy are fractions of the slack between them: 0 is the
 * left/top edge, 0.5 the center, 1 the right/bottom edge.
 */
public enum Anchor {
    TOP_LEFT(0.0f, 0.0f), TOP(0.5f, 0.0f), TOP_RIGHT(1.0f, 0.0f),
    LEFT(0.0f, 0.5f), CENTER(0.5f, 0.5f), RIGHT(1.0f, 0.5f),
    BOTTOM_LEFT(0.0f, 1.0f), BOTTOM(0.5f, 1.0f), BOTTOM_RIGHT(1.0f, 1.0f);

    public final float fx, fy;

    Anchor(float fx, float fy) {
        this.fx = fx;
        this.fy = fy;
    }
}
