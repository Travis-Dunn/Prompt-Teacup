package com.alchemysoftware.teacup.scratch;

import com.alchemysoftware.teacup.text.Label;
import com.alchemysoftware.teacup.text.Text;
import com.alchemysoftware.teacup.text.TextStyle;
import com.alchemysoftware.whitetail.event.Event;
import com.alchemysoftware.whitetail.scene.Scene;

public final class SceneMainMenu extends Scene {
    private Label title, blurb, build;

    @Override
    public void onInit() {
        TextStyle titleStyle = new TextStyle();
        titleStyle.font = Data.fontMichroma;
        titleStyle.size = 88.0f;
        titleStyle.tracking = 0.08f;
        titleStyle.lineSpacing = 1.0f;
        titleStyle.align = TextStyle.Align.CENTER;

        TextStyle blurbStyle = new TextStyle();
        blurbStyle.font = Data.fontMichroma;
        blurbStyle.size = 22.0f;
        blurbStyle.tracking = 0.02f;
        blurbStyle.lineSpacing = 1.5f;
        blurbStyle.wrapW = 1100.0f;
        blurbStyle.align = TextStyle.Align.CENTER;

        TextStyle buildStyle = new TextStyle();
        buildStyle.font = Data.fontMichroma;
        buildStyle.size = 14.0f;
        buildStyle.lineSpacing = 1.0f;
        buildStyle.align = TextStyle.Align.LEFT;

        if ((title = Text.Create(titleStyle, "PROMPT TEACUP")) == null) return;
        title.setPosition(960.0f, 420.0f);
        title.setColor(0.45f, 0.92f, 1.0f, 1.0f);

        if ((blurb = Text.Create(blurbStyle, BLURB_STR)) == null) return;
        blurb.setPosition(960.0f, 520.0f);
        blurb.setColor(0.86f, 0.86f, 0.80f, 1.0f);

        if ((build = Text.Create(buildStyle, Data.WIN_TITLE_STR)) == null)
            return;
        build.setPosition(24.0f, 1056.0f);
        build.setColor(1.0f, 0.62f, 0.25f, 0.85f);
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
        Text.Draw(blurb);
        Text.Draw(build);
    }

    @Override
    public void onInput() {

    }

    @Override
    public void onDestroy() {
        if (title != null) Text.Destroy(title);
        if (blurb != null) Text.Destroy(blurb);
        if (build != null) Text.Destroy(build);
    }

    @Override
    public boolean handleEvent(Event event) {
        return false;
    }

    private static final String BLURB_STR = "Baked text: rasterized once " +
            "by Java2D at the exact pixel size it is shown at, then drawn " +
            "as a single quad. Kerning, tracking, and word wrapping come " +
            "from a real text layout engine.\n\nSizes are in 1080p " +
            "reference units, so this paragraph is native at every " +
            "resolution.";
}
