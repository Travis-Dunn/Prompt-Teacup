package com.alchemysoftware.teacup.scenes;

import com.alchemysoftware.teacup.layout.Anchor;
import com.alchemysoftware.teacup.layout.Layout;
import com.alchemysoftware.teacup.layout.Rect;
import com.alchemysoftware.teacup.scratch.Data;
import com.alchemysoftware.teacup.text.Label;
import com.alchemysoftware.teacup.text.Text;
import com.alchemysoftware.teacup.text.TextStyle;
import com.alchemysoftware.teacup.ui.MenuButton;
import com.alchemysoftware.whitetail.event.Event;
import com.alchemysoftware.whitetail.scene.Scene;
import com.alchemysoftware.whitetail.scene.SceneManager;

/**
 * Lists the keyboard shortcuts as two columns, key names right-aligned and
 * descriptions left-aligned against a central gutter.
 */
public final class SceneControls extends Scene {
    private Label heading;
    private Label[] keys, actions;
    private MenuButton back;

    @Override
    public void onInit() {
        TextStyle headingStyle = new TextStyle();
        headingStyle.font = Data.fontMichroma;
        headingStyle.size = 56.0f;
        headingStyle.tracking = 0.08f;
        headingStyle.lineSpacing = 1.0f;
        headingStyle.align = TextStyle.Align.CENTER;

        TextStyle keyStyle = new TextStyle();
        keyStyle.font = Data.fontMichroma;
        keyStyle.size = 26.0f;
        keyStyle.tracking = 0.1f;
        keyStyle.lineSpacing = 1.0f;
        keyStyle.align = TextStyle.Align.RIGHT;

        TextStyle actionStyle = new TextStyle();
        actionStyle.font = Data.fontMichroma;
        actionStyle.size = 26.0f;
        actionStyle.tracking = 0.02f;
        actionStyle.lineSpacing = 1.0f;
        actionStyle.align = TextStyle.Align.LEFT;

        Rect keyCol = new Rect(), actionCol = new Rect();
        float top = (Layout.H - KEY_STRS.length * ROW_H) / 2.0f;
        int i;

        if ((heading = Text.Create(headingStyle, HEADING_STR)) == null)
            return;
        heading.place(Layout.CANVAS, Anchor.TOP, 0.0f, 180.0f);
        heading.setColor(0.45f, 0.92f, 1.0f, 1.0f);

        keys = new Label[KEY_STRS.length];
        actions = new Label[KEY_STRS.length];
        for (i = 0; i < KEY_STRS.length; ++i) {
            keyCol.set(0.0f, top + i * ROW_H, Layout.W / 2.0f - GUTTER,
                    ROW_H);
            actionCol.set(Layout.W / 2.0f + GUTTER, top + i * ROW_H,
                    Layout.W / 2.0f - GUTTER, ROW_H);

            if ((keys[i] = Text.Create(keyStyle, KEY_STRS[i])) == null)
                return;
            keys[i].place(keyCol, Anchor.RIGHT, 0.0f, 0.0f);
            keys[i].setColor(1.0f, 0.62f, 0.25f, 1.0f);

            if ((actions[i] = Text.Create(actionStyle, ACTION_STRS[i]))
                    == null) return;
            actions[i].place(actionCol, Anchor.LEFT, 0.0f, 0.0f);
            actions[i].setColor(0.86f, 0.86f, 0.80f, 1.0f);
        }

        if ((back = MenuButton.Create(Data.menuButtonStyle, BACK_STR))
                == null) return;
        back.place(Layout.CANVAS, Anchor.BOTTOM, 0.0f, -180.0f);
    }

    @Override
    public void onEnter() {
        back.reset();
    }

    @Override
    public void onExit() {

    }

    @Override
    public void onUpdate(double v) {

    }

    @Override
    public void onRender() {
        int i;

        Text.Draw(heading);
        for (i = 0; i < keys.length; ++i) {
            Text.Draw(keys[i]);
            Text.Draw(actions[i]);
        }
        back.draw();
    }

    @Override
    public void onInput() {
        if (back.clicked()) SceneManager.TransitionTo(Data.MAIN_MENU_STR);
    }

    @Override
    public void onDestroy() {
        int i;

        if (heading != null) Text.Destroy(heading);
        for (i = 0; keys != null && i < keys.length; ++i) {
            if (keys[i] != null) Text.Destroy(keys[i]);
            if (actions[i] != null) Text.Destroy(actions[i]);
        }
        if (back != null) MenuButton.Destroy(back);
    }

    @Override
    public boolean handleEvent(Event event) {
        return false;
    }

    /* One row per shortcut; KEY_STRS[i] is described by ACTION_STRS[i]. */
    private static final String[] KEY_STRS = { Data.KEY_QUIT_STR };
    private static final String[] ACTION_STRS = { "Quit the game" };

    private static final float ROW_H = 56.0f;
    /* Half the space between the key and action columns. */
    private static final float GUTTER = 24.0f;

    private static final String HEADING_STR = "CONTROLS";
    private static final String BACK_STR = "BACK";
}
