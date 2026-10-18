package com.alchemysoftware.teacup.scenes;

import com.alchemysoftware.teacup.layout.Anchor;
import com.alchemysoftware.teacup.layout.Layout;
import com.alchemysoftware.teacup.scratch.Data;
import com.alchemysoftware.teacup.text.Label;
import com.alchemysoftware.teacup.text.Text;
import com.alchemysoftware.teacup.text.TextStyle;
import com.alchemysoftware.teacup.ui.MenuButton;
import com.alchemysoftware.whitetail.event.Event;
import com.alchemysoftware.whitetail.scene.Scene;
import com.alchemysoftware.whitetail.scene.SceneManager;

public final class SceneMainMenu extends Scene {
    private Label title, build;
    private MenuButton controls;

    @Override
    public void onInit() {
        TextStyle titleStyle = new TextStyle();
        titleStyle.font = Data.fontMichroma;
        titleStyle.size = 88.0f;
        titleStyle.tracking = 0.08f;
        titleStyle.lineSpacing = 1.0f;
        titleStyle.align = TextStyle.Align.CENTER;

        TextStyle buildStyle = new TextStyle();
        buildStyle.font = Data.fontMichroma;
        buildStyle.size = 14.0f;
        buildStyle.lineSpacing = 1.0f;
        buildStyle.align = TextStyle.Align.LEFT;

        if ((title = Text.Create(titleStyle, TITLE_STR)) == null) return;
        title.place(Layout.CANVAS, Anchor.CENTER, 0.0f, -90.0f);
        title.setColor(0.45f, 0.92f, 1.0f, 1.0f);

        if ((build = Text.Create(buildStyle, Data.WIN_TITLE_STR)) == null)
            return;
        /* A watermark belongs in the real screen corner on any aspect. */
        build.place(Layout.SCREEN, Anchor.BOTTOM_LEFT, 24.0f, -20.0f);
        build.setColor(1.0f, 0.62f, 0.25f, 0.85f);

        if ((controls = MenuButton.Create(Data.menuButtonStyle,
                CONTROLS_STR)) == null) return;
        controls.place(Layout.CANVAS, Anchor.CENTER, 0.0f, 90.0f);
    }

    @Override
    public void onEnter() {

    }

    @Override
    public void onExit() {

    }

    @Override
    public void onUpdate(double v) {

    }

    @Override
    public void onRender() {
        Text.Draw(title);
        Text.Draw(build);
        controls.draw();
    }

    @Override
    public void onInput() {
        if (controls.clicked()) SceneManager.TransitionTo(Data.CONTROLS_STR);
    }

    @Override
    public void onDestroy() {
        if (title != null) Text.Destroy(title);
        if (build != null) Text.Destroy(build);
        if (controls != null) MenuButton.Destroy(controls);
    }

    @Override
    public boolean handleEvent(Event event) {
        return false;
    }

    private static final String TITLE_STR = "PROMPT TEACUP";
    private static final String CONTROLS_STR = "CONTROLS";
}
