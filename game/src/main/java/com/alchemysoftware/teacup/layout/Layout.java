package com.alchemysoftware.teacup.layout;

import com.alchemysoftware.whitetail.error.ErrorStrings;
import com.alchemysoftware.whitetail.log.LogLevel;
import com.alchemysoftware.whitetail.log.Logger;

/**
 * Maps the 1920x1080 virtual canvas that every scene is composed on to the
 * framebuffer.
 *
 * The canvas is scaled uniformly to fit the framebuffer and centered, so on
 * every 16:9 screen the composition is identical, and on other aspect ratios
 * it is the same composition with spare space at the sides or top and
 * bottom. Laying out for other aspect ratios is the caller's job; SCREEN
 * gives the whole framebuffer in virtual units for that purpose.
 *
 * Virtual coordinates have a top-left origin, +x right, +y down. Nothing
 * here allocates. Not thread-safe.
 */
public final class Layout {
    public static final float W = 1920.0f, H = 1080.0f;

    /** The virtual canvas, (0, 0, W, H). Must not be modified. */
    public static final Rect CANVAS = NewRect(0.0f, 0.0f, W, H);

    /** The whole framebuffer in virtual units; equal to CANVAS on 16:9
     *  screens. Set by Init(). Must not be modified. */
    public static final Rect SCREEN = new Rect();

    private static boolean init;
    private static float scale;
    /* Framebuffer pixel position of the canvas origin; whole pixels so
       whole virtual pixels at scale 1 land exactly on pixels. */
    private static float offX, offY;
    /* Copies of SCREEN, to catch callers that modify it. */
    private static float screenX, screenY, screenW, screenH;

    private Layout() {}

    /**
     * @param fbW framebuffer width in pixels
     * @param fbH framebuffer height in pixels
     */
    public static void Init(int fbW, int fbH) {
        assert !init : CLASS + ErrorStrings.ERR_STR_DOUBLE_INIT;
        assert fbW > 0 && fbH > 0 : ErrStrFramebuffer(fbW, fbH);

        scale = Math.min(fbW / W, fbH / H);
        offX = Math.round((fbW - W * scale) / 2.0f);
        offY = Math.round((fbH - H * scale) / 2.0f);

        screenX = -offX / scale;
        screenY = -offY / scale;
        screenW = fbW / scale;
        screenH = fbH / scale;
        SCREEN.set(screenX, screenY, screenW, screenH);

        init = true;

        Logger.LogSession(LogLevel.DEBUG, String.format("%s initialized " +
                "[%dx%d, scale %.4f, canvas origin (%.0f, %.0f)].\n", CLASS,
                fbW, fbH, scale, offX, offY));
    }

    /**
     * @return framebuffer pixels per virtual pixel
     */
    public static float GetScale() {
        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        return scale;
    }

    /**
     * Positions r inside parent so that anchor a of r sits on anchor a of
     * parent, then offsets it. Only r.x and r.y are written.
     *
     * @param r the rectangle to position; its w and h are read
     * @param parent the rectangle to position it in
     * @param a which point of r to pin to the same point of parent
     * @param dx offset in virtual pixels, +x right
     * @param dy offset in virtual pixels, +y down
     */
    public static void Place(Rect r, Rect parent, Anchor a, float dx,
                             float dy) {
        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        assert Intact() : ERR_STR_MODIFIED;

        r.x = parent.x + a.fx * (parent.w - r.w) + dx;
        r.y = parent.y + a.fy * (parent.h - r.h) + dy;
    }

    /**
     * @param x virtual x
     * @return framebuffer x in pixels, unrounded
     */
    public static float ToPxX(float x) {
        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        return offX + x * scale;
    }

    /**
     * @param y virtual y
     * @return framebuffer y in pixels from the top, unrounded
     */
    public static float ToPxY(float y) {
        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        return offY + y * scale;
    }

    /**
     * @param px framebuffer x in pixels
     * @return virtual x
     */
    public static float ToVirtualX(float px) {
        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        return (px - offX) / scale;
    }

    /**
     * LWJGL mouse y counts up from the bottom; pass framebuffer height - 1
     * - mouse y.
     *
     * @param py framebuffer y in pixels from the top
     * @return virtual y
     */
    public static float ToVirtualY(float py) {
        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        return (py - offY) / scale;
    }

    private static boolean Intact() {
        return CANVAS.x == 0.0f && CANVAS.y == 0.0f && CANVAS.w == W &&
                CANVAS.h == H && SCREEN.x == screenX &&
                SCREEN.y == screenY && SCREEN.w == screenW &&
                SCREEN.h == screenH;
    }

    private static Rect NewRect(float x, float y, float w, float h) {
        Rect r = new Rect();
        r.set(x, y, w, h);
        return r;
    }

    private static final String CLASS = "Layout";
    private static final String ERR_STR_MODIFIED = CLASS +
            " found CANVAS or SCREEN modified; they are read-only.\n";
    private static String ErrStrFramebuffer(int w, int h) {
        return CLASS + " failed to initialize because framebuffer width [" +
                w + "] and height [" + h + "] must both be greater than " +
                "zero.\n";
    }
}
