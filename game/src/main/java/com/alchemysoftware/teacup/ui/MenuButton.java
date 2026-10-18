package com.alchemysoftware.teacup.ui;

import com.alchemysoftware.teacup.layout.Anchor;
import com.alchemysoftware.teacup.layout.Layout;
import com.alchemysoftware.teacup.layout.Rect;
import com.alchemysoftware.teacup.text.Label;
import com.alchemysoftware.teacup.text.Text;
import com.alchemysoftware.whitetail.input.Button;
import com.alchemysoftware.whitetail.input.InputSys;
import com.alchemysoftware.whitetail.window.Window;

/**
 * A text button. A click is a press and release of the left mouse button
 * both over the button; dragging off before releasing cancels it.
 *
 * The owning scene calls clicked() from onInput() and draw() from
 * onRender() every frame it is active, and acts on clicked() returning
 * true. Neither allocates.
 *
 * Create and Destroy are load-time operations, like Text.Create and
 * Text.Destroy. Not thread-safe.
 */
public final class MenuButton {
    private Label label;
    private final ButtonStyle style;
    /* Clickable area, in virtual pixels. */
    private final Rect hit = new Rect();
    /* The current press started on this button. */
    private boolean armed;

    private MenuButton(Label label, ButtonStyle style) {
        this.label = label;
        this.style = style;
        hit.w = label.getW() + 2.0f * style.padX;
        hit.h = label.getH() + 2.0f * style.padY;
    }

    /**
     * @param style the button's look; kept by reference
     * @param text the label text
     * @return the button at (0, 0), or null after reporting a fatal error
     */
    public static MenuButton Create(ButtonStyle style, String text) {
        Label label;

        assert style != null : ERR_STR_STYLE_NULL;
        assert style.text != null : ERR_STR_TEXT_STYLE_NULL;

        if ((label = Text.Create(style.text, text)) == null) return null;
        return new MenuButton(label, style);
    }

    /**
     * @param button a live button; it must not be used afterwards
     */
    public static void Destroy(MenuButton button) {
        assert button != null : ERR_STR_BUTTON_NULL;
        assert button.label != null : ERR_STR_DESTROYED;

        Text.Destroy(button.label);
        button.label = null;
    }

    /**
     * Positions the clickable area (the label's box plus padding) and
     * centers the label in it.
     *
     * @param parent the rectangle to position the button in
     * @param a which point of the button to pin to the same point of parent
     * @param dx offset in virtual pixels, +x right
     * @param dy offset in virtual pixels, +y down
     */
    public void place(Rect parent, Anchor a, float dx, float dy) {
        assert label != null : ERR_STR_DESTROYED;

        Layout.Place(hit, parent, a, dx, dy);
        label.place(hit, Anchor.CENTER, 0.0f, 0.0f);
    }

    /**
     * Tracks the left mouse button. Call exactly once per frame from the
     * owning scene's onInput(), including frames where the result is not
     * needed; a skipped frame can miss the press or release. Beware
     * short-circuiting: in a.clicked() || b.clicked(), b is skipped
     * whenever a is clicked.
     *
     * @return true on the frame the button is clicked
     */
    public boolean clicked() {
        boolean over, click;

        assert label != null : ERR_STR_DESTROYED;

        over = mouseOver();
        if (InputSys.WasPressed(Button.MOUSE_LEFT)) armed = over;
        if (!InputSys.WasReleased(Button.MOUSE_LEFT)) return false;

        click = armed && over;
        armed = false;
        return click;
    }

    /**
     * Tints the label for the current mouse state and queues it. Call once
     * per frame from the owning scene's onRender().
     */
    public void draw() {
        boolean over;
        float[] c;

        assert label != null : ERR_STR_DESTROYED;

        /* Hover is read fresh rather than kept from clicked(), so a scene
           entered this frame doesn't show last visit's state. A press
           that started elsewhere doesn't highlight buttons it drags over. */
        over = mouseOver();
        if (armed) c = over ? style.down : style.idle;
        else c = over && !InputSys.IsDown(Button.MOUSE_LEFT)
                ? style.hover : style.idle;

        label.setColor(c[0], c[1], c[2], c[3]);
        Text.Draw(label);
    }

    /**
     * @return the clickable area in virtual pixels; must not be modified
     */
    public Rect getRect() {
        return hit;
    }

    /* LWJGL reports window pixels from the bottom-left; sample the center
       of the pixel under the cursor. */
    private boolean mouseOver() {
        float x = Layout.ToVirtualX(InputSys.GetMouseX() + 0.5f);
        float y = Layout.ToVirtualY(Window.GetHeight() - 0.5f -
                InputSys.GetMouseY());

        return x >= hit.x && x < hit.x + hit.w &&
                y >= hit.y && y < hit.y + hit.h;
    }

    private static final String CLASS = "MenuButton";
    private static final String ERR_STR_STYLE_NULL = CLASS +
            " failed to create a button because the style was null.\n";
    private static final String ERR_STR_TEXT_STYLE_NULL = CLASS +
            " failed to create a button because the style's text style " +
            "was null.\n";
    private static final String ERR_STR_BUTTON_NULL = CLASS +
            " was given a null button.\n";
    private static final String ERR_STR_DESTROYED = CLASS +
            " was used after it was destroyed.\n";
}
