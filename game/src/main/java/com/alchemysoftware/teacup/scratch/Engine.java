package com.alchemysoftware.teacup.scratch;

import com.alchemysoftware.whitetail.audio.openal.AudioContext;
import com.alchemysoftware.whitetail.error.ErrorHandler;
import com.alchemysoftware.whitetail.error.RunFlag;
import com.alchemysoftware.whitetail.gc.GCPriest;
import com.alchemysoftware.whitetail.input.lwjgl.LwjglInput;
import com.alchemysoftware.whitetail.renderer.MasterRenderer;
import com.alchemysoftware.whitetail.resource.ResourceStreamResolver;
import com.alchemysoftware.whitetail.scene.SceneManager;
import com.alchemysoftware.whitetail.timer.Timer;
import com.alchemysoftware.whitetail.userdir.UserDir;
import com.alchemysoftware.whitetail.log.Logger;
import com.alchemysoftware.whitetail.config.ConfigFileParser;
import com.alchemysoftware.whitetail.window.Window;
import com.alchemysoftware.whitetail.input.InputSys;

public final class Engine {
    private static boolean init, initGame;

    static boolean Init() {
        assert (!init);

        if ((Data.userDir = UserDir.Resolve(Data.VENDOR_STR, Data.APP_STR))
                == null) {
            System.err.println(ERR_STR_FAIL_USER_DIR);
            return false;
        }

        if (!Logger.Init(Data.userDir)) {
            System.err.println(ERR_STR_FAIL_LOG);
            return false;
        }

        if (!ConfigFileParser.Init(Data.userDir, Data.CfgList())) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_CFG);
            return false;
        }

        Data.borderless = ConfigFileParser.GetBool(Data.CFG_STR_B_BORDERLESS);
        Data.fullscreen = ConfigFileParser.GetBool(Data.CFG_STR_B_FULLSCREEN);
        Data.undecorated = ConfigFileParser.GetBool(Data.CFG_STR_B_UNDECORATED);
        Data.vSync = ConfigFileParser.GetBool(Data.CFG_STR_B_VSYNC);
        Data.winW = ConfigFileParser.GetInt(Data.CFG_STR_I_WIN_W);
        Data.winH = ConfigFileParser.GetInt(Data.CFG_STR_I_WIN_H);
        Data.fpsTarget = ConfigFileParser.GetInt(Data.CFG_STR_I_FPS_TARGET);
        Data.globalVolume =
                ConfigFileParser.GetFloat(Data.CFG_STR_F_GLOBAL_VOLUME);

        if (((Data.screenW = Window.GetDesktopWidth())
                < Data.CFG_MIN_I_WIN_W)) {
            ErrorHandler.LogFatalAndStop(ErrStrScreenW());
            return false;
        }
        if (((Data.screenH = Window.GetDesktopHeight())
                < Data.CFG_MIN_I_WIN_H)) {
            ErrorHandler.LogFatalAndStop(ErrStrScreenH());
            return false;
        }

        if ((Data.vSync && !(Data.fpsTarget == 0)) ||
                (!Data.vSync && Data.fpsTarget < 0)) {
            ErrorHandler.LogFatalAndStop(ERR_STR_VSYNC_FPS_INCOMPATIBLE);
            return false;
        }

        if (!Window.Init(Data.WIN_TITLE_STR, Data.winW, Data.winH, Data.vSync,
                Data.fullscreen, Data.undecorated, Data.GL_MAJOR, Data.GL_MINOR,
                Data.GL_PROFILE)) {
            ErrorHandler.LogFatalAndStop(ErrStrFailedInitSecondWin());
            return false;
        }

        if (!InputSys.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_INPUT_SYS);
            return false;
        }

        if (!LwjglInput.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_LWJGL_INPUT);
            return false;
        }

        if (!ResourceStreamResolver.Init(null,
                ResourceStreamResolver.MODE_CLASSPATH)) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_RESOURCE);
            return false;
        }

        if (!(MasterRenderer.Init(Data.winW, Data.winH))) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_MASTER_RENDERER);
            return false;
        }

        if (!MasterRenderer.ActivateModelRenderer()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_MODEL_RENDERER);
            return false;
        }

        if (!MasterRenderer.ActivateUIRenderer()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_UI_RENDERER);
            return false;
        }

        if (!Timer.Init(0L)) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_TIMER);
            return false;
        }

        if (!AudioContext.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_AUDIO);
            return false;
        }

        if (!GCPriest.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_GC_PRIEST);
            return false;
        }

        if (!SceneManager.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_SCENE);
            return false;
        }

        try {
            if (!InitGame()) {
                ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_INIT_GAME);
                return false;
            }
        } catch (OutOfMemoryError e) {
            ErrorHandler.LogFatalExcpAndStop(ERR_STR_FAIL_INIT_GAME_OOM, e);
            return false;
        }

        return init = true;
    }

    static boolean InitGame() {
        Data.mainMenuScene = new SceneMainMenu();

        SceneManager.RegisterScene(Data.MAIN_MENU_STR, Data.mainMenuScene);

        SceneManager.InitScenes();

        SceneManager.TransitionTo(Data.MAIN_MENU_STR);

        return initGame = true;
    }

    static void Run() {
        assert(init);

        RunFlag.Start();
        while (RunFlag.IsRunning() && !Window.ShouldClose()) {
            Input();
            Update();
            Render();
        }
        RunFlag.Stop();
    }

    static void Input() {
        Window.PollEvents();
        LwjglInput.Update();
        SceneManager.Input();
    }

    static void Update() {
        Timer.Update();
        SceneManager.Update(Timer.GetDt());
    }

    static void Render() {
        SceneManager.Render();
        MasterRenderer.Render();
        Window.SwapBuffers();
    }

    static void Shutdown() {
        assert (init);

        SceneManager.Shutdown();
        GCPriest.Shutdown();
        AudioContext.Shutdown();
        MasterRenderer.Shutdown();
        LwjglInput.Shutdown();
        InputSys.Shutdown();
        Window.Shutdown();
        Logger.Shutdown();
    }

    private static final String CLASS = "Engine";
    private static final String ERR_STR_FAIL_USER_DIR = CLASS +
            " failed to initialize because it failed to resolve the user " +
            "directory.\n";
    private static final String ERR_STR_FAIL_LOG = CLASS +
            " failed to initialize because the logger + " +
            "failed to initialize.\n";
    private static final String ERR_STR_FAIL_CFG = CLASS +
            " failed to initialize because the config file parser " +
            "failed to initialize.\n";
    private static final String ERR_STR_FAIL_INPUT_SYS = CLASS +
            " failed to initialize because the input system failed " +
            "failed to initialize.\n";
    private static final String ERR_STR_FAIL_LWJGL_INPUT = CLASS +
            " failed to initialize because the lwjgl input system failed " +
            "to initialize.\n";
    private static final String ERR_STR_FAIL_RESOURCE = CLASS +
            " failed to initialize because the resource system failed to " +
            "initialize.\n";
    private static final String ERR_STR_FAIL_MASTER_RENDERER = CLASS +
            " failed to initialize because the master renderer failed to " +
            "initialize.\n";
    private static final String ERR_STR_FAIL_MODEL_RENDERER = CLASS +
            " failed to initialize because the model renderer failed to " +
            "initialize.\n";
    private static final String ERR_STR_FAIL_UI_RENDERER = CLASS +
            " failed to initialize because the ui renderer failed to " +
            "initialize.\n";
    private static final String ERR_STR_FAIL_TIMER = CLASS +
            " failed to initialize because the timer failed to initialize.\n";
    private static final String ERR_STR_FAIL_AUDIO = CLASS +
            " failed to initialize because the audio system failed to " +
            "initialize.";
    private static final String ERR_STR_FAIL_GC_PRIEST = CLASS +
            " failed to initialize because the garbage collector monitoring " +
            "system failed to initialize.\n";
    private static final String ERR_STR_FAIL_SCENE = CLASS +
            " failed to initialize because the scene system failed to " +
            "initialize.\n";
    private static final String ERR_STR_FAIL_INIT_GAME = CLASS +
            " failed to initialize because the game failed to initialize.\n";
    private static final String ERR_STR_FAIL_INIT_GAME_OOM = CLASS +
            " failed to initialize because an OutOfMemoryError was " +
            "encountered while initializing the game.\n";
    private static String ErrStrScreenW() {
        return String.format("%s failed to initialize because the screen " +
                "width [%d] was less than the minimum [%d].\n", CLASS,
                Data.screenW, Data.CFG_MIN_I_WIN_W);
    }
    private static String ErrStrScreenH() {
        return String.format("%s failed to initialize because the screen " +
                        "height [%d] was less than the minimum [%d].\n", CLASS,
                Data.screenH, Data.CFG_MIN_I_WIN_H);
    }
    private static final String ERR_STR_VSYNC_FPS_INCOMPATIBLE = CLASS +
            " failed to initialize because VSync and Fps settings are " +
            "incompatible. \n If VSync is enabled, Fps must be set to 0" +
            ". If vSync is disabled, Fps must be greater than zero.\n";
    private static String ErrStrFailedInitSecondWin() {
        return String.format("%s failed to initialize " +
                "because the window failed to initialize.\n", CLASS);
    }
}