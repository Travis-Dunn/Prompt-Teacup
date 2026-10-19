package com.alchemysoftware.teacup.scratch;

import com.alchemysoftware.teacup.scenes.SceneControls;
import com.alchemysoftware.teacup.scenes.SceneMainMenu;
import com.alchemysoftware.teacup.text.TextStyle;
import com.alchemysoftware.teacup.ui.ButtonStyle;
import com.alchemysoftware.teacup.ui.PanelStyle;
import com.alchemysoftware.whitetail.config.ConfigEntry;
import com.alchemysoftware.whitetail.input.Button;
import com.alchemysoftware.whitetail.window.GLProfile;

import java.awt.Font;
import java.io.File;
import java.util.ArrayList;

public final class Data {
    public static String platformStr;
    public static final String VENDOR_STR = "Alchemy Software";
    public static final String APP_STR = "Teacup";
    public static File userDir;
    public static final String WIN_TITLE_STR = "Teacup development build";
    public static final int GL_MAJOR = 3;
    public static final int GL_MINOR = 3;
    public static final GLProfile GL_PROFILE = GLProfile.CORE;

    public static float globalVolume;
    public static boolean borderless, fullscreen, vSync;
    public static int winW, winH;
    public static int screenW, screenH;
    public static int fpsTarget;

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Config ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    public static final String CFG_STR_F_GLOBAL_VOLUME = "fGlobalVolume";
    public static final float CFG_DEF_F_GLOBAL_VOLUME = 0.5f;

    public static final String CFG_STR_B_BORDERLESS = "bBorderless";
    public static final boolean CFG_DEF_B_BORDERLESS = false;

    public static final String CFG_STR_B_FULLSCREEN = "bFullscreen";
    public static final boolean CFG_DEF_B_FULLSCREEN = false;

    public static final String CFG_STR_I_WIN_W = "iWinWidth";
    public static final int CFG_DEF_I_WIN_W = 1280;
    public static final int CFG_MIN_I_WIN_W = 320;
    public static final int CFG_MAX_I_WIN_W = 15360;

    public static final String CFG_STR_I_WIN_H = "iWinHeight";
    public static final int CFG_DEF_I_WIN_H = 720;
    public static final int CFG_MIN_I_WIN_H = 200;
    public static final int CFG_MAX_I_WIN_H = 5120;

    public static final String CFG_STR_B_VSYNC = "bVsync";
    public static final boolean CFG_DEF_B_VSYNC = true;

    public static final String CFG_STR_I_FPS_TARGET = "iFpsTarget";
    public static final int CFG_DEF_I_FPS_TARGET = 0;

    public static ArrayList<ConfigEntry> CfgList() {
        ArrayList<ConfigEntry> ret = new ArrayList<ConfigEntry>();

        ret.add(new ConfigEntry(
                CFG_STR_F_GLOBAL_VOLUME,
                ConfigEntry.TYPE_FLOAT,
                0.0f,
                1.0f,
                CFG_DEF_F_GLOBAL_VOLUME));

        ret.add(new ConfigEntry(
                CFG_STR_B_BORDERLESS,
                ConfigEntry.TYPE_BOOL,
                null,
                null,
                CFG_DEF_B_BORDERLESS));

        ret.add(new ConfigEntry(
                CFG_STR_B_FULLSCREEN,
                ConfigEntry.TYPE_BOOL,
                null,
                null,
                CFG_DEF_B_FULLSCREEN));

        ret.add(new ConfigEntry(
                CFG_STR_I_WIN_W,
                ConfigEntry.TYPE_INT,
                CFG_MIN_I_WIN_W,
                CFG_MAX_I_WIN_W,
                CFG_DEF_I_WIN_W));

        ret.add(new ConfigEntry(
                CFG_STR_I_WIN_H,
                ConfigEntry.TYPE_INT,
                CFG_MIN_I_WIN_H,
                CFG_MAX_I_WIN_H,
                CFG_DEF_I_WIN_H));

        ret.add(new ConfigEntry(
                CFG_STR_B_VSYNC,
                ConfigEntry.TYPE_BOOL,
                null,
                null,
                CFG_DEF_B_VSYNC));

        ret.add(new ConfigEntry(
                CFG_STR_I_FPS_TARGET,
                ConfigEntry.TYPE_INT,
                0,
                0x7FFFFFFF,
                CFG_DEF_I_FPS_TARGET));

        return ret;
    }

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Text ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    public static final String FONT_FOLDER = "fonts";
    public static final String FONT_MICHROMA_FILENAME = "Michroma-Regular.ttf";
    public static Font fontMichroma;

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Shaders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    public static final String SHADER_FOLDER = "shaders";
    /* Shared by every UI quad: text and panels. */
    public static final String UI_VERT_FILENAME = "ui_vert.glsl";
    public static final String TEXT_FRAG_FILENAME = "text_frag.glsl";
    /* Shared head of every panel style's fragment shader. */
    public static final String PANEL_FRAG_HEAD_FILENAME =
            "panel_frag_head.glsl";
    public static final String CONSTRUCT_FRAG_FILENAME = "construct_frag.glsl";

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Styles ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    public static ButtonStyle menuButtonStyle;
    public static PanelStyle constructPanel;
    public static ButtonStyle constructButtonStyle;

    /**
     * Builds the styles shared between scenes. Fonts must be loaded, and the
     * UI renderer active, first.
     *
     * @return false after reporting a fatal error
     */
    public static boolean InitStyles() {
        TextStyle text = new TextStyle();
        TextStyle constructText = new TextStyle();

        text.font = fontMichroma;
        text.size = 32.0f;
        text.tracking = 0.1f;
        text.lineSpacing = 1.0f;
        text.align = TextStyle.Align.CENTER;

        menuButtonStyle = new ButtonStyle();
        menuButtonStyle.text = text;
        menuButtonStyle.padX = 32.0f;
        menuButtonStyle.padY = 16.0f;
        Rgba(menuButtonStyle.idle, 0x8c999e, 1.0f);
        Rgba(menuButtonStyle.hover, 0x73ebff, 1.0f);
        Rgba(menuButtonStyle.down, 0xffffff, 1.0f);

        /* Construct, "red october" theme. Pads cover the lean and the drop
           shadow, which reach past the panel rect. */
        if ((constructPanel = PanelStyle.Load(SHADER_FOLDER,
                UI_VERT_FILENAME, PANEL_FRAG_HEAD_FILENAME,
                CONSTRUCT_FRAG_FILENAME)) == null) return false;
        Rgb(constructPanel.colA, 0xc22f3b);
        Rgb(constructPanel.colB, 0xe8d6b0);
        Rgba(constructPanel.fill, 0x1c1416, 0.90f);
        constructPanel.padX = 20.25f;
        constructPanel.padY = 13.5f;

        constructText.font = fontMichroma;
        constructText.size = 26.0f;
        constructText.tracking = 0.1f;
        constructText.lineSpacing = 1.0f;
        constructText.align = TextStyle.Align.LEFT;

        constructButtonStyle = new ButtonStyle();
        constructButtonStyle.text = constructText;
        constructButtonStyle.panel = constructPanel;
        constructButtonStyle.padX = 22.5f;
        constructButtonStyle.padY = 13.0f;
        constructButtonStyle.minW = 328.5f;
        /* Hover floods the slab with accent A, so the label goes dark. */
        Rgba(constructButtonStyle.idle, 0xe8d6b0, 1.0f);
        Rgba(constructButtonStyle.hover, 0x14090b, 1.0f);
        Rgba(constructButtonStyle.down, 0x14090b, 1.0f);

        return true;
    }

    private static void Rgb(float[] c, int rgb) {
        c[0] = ((rgb >> 16) & 0xff) / 255.0f;
        c[1] = ((rgb >> 8) & 0xff) / 255.0f;
        c[2] = (rgb & 0xff) / 255.0f;
    }

    private static void Rgba(float[] c, int rgb, float a) {
        Rgb(c, rgb);
        c[3] = a;
    }

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Controls ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    /* Keep each key and its on-screen name together so the Controls scene
       can't drift from what the game actually does. */
    public static final int KEY_QUIT = Button.KEY_ESCAPE;
    public static final String KEY_QUIT_STR = "ESC";

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Scenes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    public static SceneMainMenu mainMenuScene;
    public static final String MAIN_MENU_STR = "main menu";

    public static SceneControls controlsScene;
    public static final String CONTROLS_STR = "controls";
}
