package com.alchemysoftware.teacup.ui;

import com.alchemysoftware.teacup.layout.Anchor;
import com.alchemysoftware.teacup.layout.Layout;
import com.alchemysoftware.teacup.layout.Rect;
import com.alchemysoftware.teacup.text.Label;
import com.alchemysoftware.teacup.text.Text;
import com.alchemysoftware.teacup.text.TextStyle;
import com.alchemysoftware.whitetail.input.Button;
import com.alchemysoftware.whitetail.input.InputSys;
import com.alchemysoftware.whitetail.timer.Timer;
import com.alchemysoftware.whitetail.window.Window;

/**
 * A text button, optionally on a panel. A click is a press and release of
 * the left mouse button both over the button; dragging off before releasing
 * cancels it.
 *
 * The owning scene calls clicked() from onInput() and draw() from
 * onRender() every frame it is active, acts on clicked() returning true,
 * and calls reset() from onEnter(). Neither clicked() nor draw()
 * allocates.
 *
 * Create and Destroy are load-time operations, like Text.Create and
 * Text.Destroy. Not thread-safe.
 */
public final class MenuButton {
    /* Seconds for the hover highlight to fade fully in, and fully out. */
    private static final float HOVER_IN = 0.10f, HOVER_OUT = 0.22f;

    private Label label;
    /* Null for a bare label. */
    private final Panel panel;
    private final ButtonStyle style;
    /* Clickable area, in virtual pixels; also the panel rect. */
    private final Rect hit = new Rect();
    /* The current press started on this button. */
    private boolean armed;
    /* Hover highlight, 0 to 1, eased toward the mouse state. */
    private float hover;

    private MenuButton(Label label, Panel panel, ButtonStyle style) {
        this.label = label;
        this.panel = panel;
        this.style = style;
        hit.w = Math.max(style.minW, label.getW() + 2.0f * style.padX);
        hit.h = label.getH() + 2.0f * style.padY;
    }

    /**
     * @param style the button's look; kept by reference
     * @param text the label text
     * @return the button at (0, 0), or null after reporting a fatal error
     */
    public static MenuButton Create(ButtonStyle style, String text) {
        Label label;
        Panel panel = null;

        assert style != null : ERR_STR_STYLE_NULL;
        assert style.text != null : ERR_STR_TEXT_STYLE_NULL;

        if (style.panel != null &&
                (panel = Panel.Create(style.panel)) == null) return null;
        if ((label = Text.Create(style.text, text)) == null) return null;
        return new MenuButton(label, panel, style);
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
     * Positions the button and places the label in it according to the
     * label's alignment.
     *
     * @param parent the rectangle to position the button in
     * @param a which point of the button to pin to the same point of parent
     * @param dx offset in virtual pixels, +x right
     * @param dy offset in virtual pixels, +y down
     */
    public void place(Rect parent, Anchor a, float dx, float dy) {
        TextStyle.Align align = style.text.align;

        assert label != null : ERR_STR_DESTROYED;

        Layout.Place(hit, parent, a, dx, dy);
        if (align == TextStyle.Align.LEFT)
            label.place(hit, Anchor.LEFT, style.padX, 0.0f);
        else if (align == TextStyle.Align.RIGHT)
            label.place(hit, Anchor.RIGHT, -style.padX, 0.0f);
        else label.place(hit, Anchor.CENTER, 0.0f, 0.0f);
        if (panel != null) panel.setRect(hit);
    }

    /**
     * Snaps the hover highlight to the current mouse state and forgets any
     * press in progress. Call from the owning scene's onEnter(), so a scene
     * doesn't fade out a highlight left over from its last visit.
     */
    public void reset() {
        assert label != null : ERR_STR_DESTROYED;

        armed = false;
        hover = mouseOver() && !InputSys.IsDown(Button.MOUSE_LEFT)
                ? 1.0f : 0.0f;
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
     * Eases the hover highlight, tints the label, and queues the panel and
     * label. Call once per frame from the owning scene's onRender().
     */
    public void draw() {
        boolean over, down, lit;
        float dt, t;
        float[] c0, c1;

        assert label != null : ERR_STR_DESTROYED;

        /* A press that started elsewhere doesn't highlight buttons it drags
           over; dragging off a pressed button unlights it. */
        over = mouseOver();
        down = armed && over;
        lit = armed ? over : over && !InputSys.IsDown(Button.MOUSE_LEFT);

        dt = (float) Timer.GetDt();
        hover = lit ? Math.min(1.0f, hover + dt / HOVER_IN)
                : Math.max(0.0f, hover - dt / HOVER_OUT);

        if (down) {
            c0 = c1 = style.down;
            t = 1.0f;
        } else {
            c0 = style.idle;
            c1 = style.hover;
            t = hover;
        }
        label.setColor(c0[0] + (c1[0] - c0[0]) * t,
                c0[1] + (c1[1] - c0[1]) * t,
                c0[2] + (c1[2] - c0[2]) * t,
                c0[3] + (c1[3] - c0[3]) * t);

        if (panel != null) {
            panel.setState(hover, down ? 1.0f : 0.0f);
            panel.draw();
        }
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
