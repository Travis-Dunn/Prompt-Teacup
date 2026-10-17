package com.alchemysoftware.teacup.scratch;

import com.alchemysoftware.whitetail.config.ConfigEntry;
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

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Scenes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

    public static SceneMainMenu mainMenuScene;
    public static final String MAIN_MENU_STR = "main menu";
}
