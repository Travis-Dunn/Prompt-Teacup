package com.alchemysoftware.teacup.scenes;

import com.alchemysoftware.teacup.game.Game;
import com.alchemysoftware.teacup.layout.Anchor;
import com.alchemysoftware.teacup.layout.Layout;
import com.alchemysoftware.teacup.scratch.Data;
import com.alchemysoftware.teacup.starfield.Starfield;
import com.alchemysoftware.teacup.starfield.StarfieldParams;
import com.alchemysoftware.teacup.text.Label;
import com.alchemysoftware.teacup.text.Text;
import com.alchemysoftware.teacup.text.TextStyle;
import com.alchemysoftware.teacup.ui.MenuButton;
import com.alchemysoftware.whitetail.event.Event;
import com.alchemysoftware.whitetail.scene.Scene;
import com.alchemysoftware.whitetail.scene.SceneManager;

public final class SceneMainMenu extends Scene {
    /* The menu's sky drifts on its own, in virtual pixels per second at
       full parallax; in flight the camera follows the player instead. */
    private static final double DRIFT_X = 24.0, DRIFT_Y = 9.0;

    private Label title, build;
    private MenuButton play, controls;
    private final StarfieldParams sky = new StarfieldParams();
    private double camX, camY;

    @Override
    public void onInit() {
        /* Literals until systems exist and supply their own. */
        sky.seed = 0x7EAC5L;
        sky.density = 1.0f;
        sky.heroStars = 7;
        sky.galaxies = 4;
        sky.hazeColor[0] = 0.32f;
        sky.hazeColor[1] = 0.22f;
        sky.hazeColor[2] = 0.46f;
        sky.hazeBrightness = 0.05f;
        sky.hazeClumping = 0.7f;

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

        if ((play = MenuButton.Create(Data.constructButtonStyle, PLAY_STR))
                == null) return;
        play.place(Layout.CANVAS, Anchor.CENTER, 0.0f, 60.0f);

        if ((controls = MenuButton.Create(Data.constructButtonStyle,
                CONTROLS_STR)) == null) return;
        controls.place(Layout.CANVAS, Anchor.CENTER, 0.0f, 150.0f);
    }

    @Override
    public void onEnter() {
        /* Other places build their own skies, so rebuild on every visit. */
        Starfield.Build(sky);
        play.reset();
        controls.reset();
    }

    @Override
    public void onExit() {

    }

    @Override
    public void onUpdate(double v) {
        camX += DRIFT_X * v;
        camY += DRIFT_Y * v;
        Starfield.SetCamera(camX, camY);
    }

    @Override
    public void onRender() {
        Starfield.Draw();
        Text.Draw(title);
        Text.Draw(build);
        play.draw();
        controls.draw();
    }

    @Override
    public void onInput() {
        if (play.clicked()) {
            Data.game = Game.New();
            SceneManager.TransitionTo(Data.FLIGHT_STR);
        }
        if (controls.clicked()) SceneManager.TransitionTo(Data.CONTROLS_STR);
    }

    @Override
    public void onDestroy() {
        if (title != null) Text.Destroy(title);
        if (build != null) Text.Destroy(build);
        if (play != null) MenuButton.Destroy(play);
        if (controls != null) MenuButton.Destroy(controls);
    }

    @Override
    public boolean handleEvent(Event event) {
        return false;
    }

    private static final String TITLE_STR = "PROMPT TEACUP";
    private static final String PLAY_STR = "PLAY";
    private static final String CONTROLS_STR = "CONTROLS";
}
