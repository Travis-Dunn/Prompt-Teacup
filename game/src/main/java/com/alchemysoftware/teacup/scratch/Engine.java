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
    private static final int STAGE_NONE = 0, STAGE_LOGGER = 1, STAGE_WINDOW = 2,
    STAGE_INPUT_SYS = 3, STAGE_LWJGL_INPUT = 4, STAGE_RENDERER = 5,
    STAGE_AUDIO = 6, STAGE_GC_PRIEST = 7, STAGE_SCENE = 8;
    private static int stage;

    static boolean Init() {
        assert (!init);

        if ((Data.userDir = UserDir.Resolve(Data.VENDOR_STR, Data.APP_STR))
                == null) {
            System.err.println(ERR_STR_FAIL_USER_DIR);
            _Shutdown();
            return false;
        }

        if (!Logger.Init(Data.userDir)) {
            System.err.println(ERR_STR_FAIL_LOG);
            _Shutdown();
            return false;
        } stage = STAGE_LOGGER;

        if (!ConfigFileParser.Init(Data.userDir, Data.CfgList())) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_CFG);
            _Shutdown();
            return false;
        }

        Data.borderless = ConfigFileParser.GetBool(Data.CFG_STR_B_BORDERLESS);
        Data.fullscreen = ConfigFileParser.GetBool(Data.CFG_STR_B_FULLSCREEN);
        Data.vSync = ConfigFileParser.GetBool(Data.CFG_STR_B_VSYNC);
        Data.winW = ConfigFileParser.GetInt(Data.CFG_STR_I_WIN_W);
        Data.winH = ConfigFileParser.GetInt(Data.CFG_STR_I_WIN_H);
        Data.fpsTarget = ConfigFileParser.GetInt(Data.CFG_STR_I_FPS_TARGET);
        Data.globalVolume =
                ConfigFileParser.GetFloat(Data.CFG_STR_F_GLOBAL_VOLUME);

        if (((Data.screenW = Window.GetDesktopWidth())
                < Data.CFG_MIN_I_WIN_W)) {
            ErrorHandler.LogFatalAndStop(ErrStrScreenW());
            _Shutdown();
            return false;
        }
        if (((Data.screenH = Window.GetDesktopHeight())
                < Data.CFG_MIN_I_WIN_H)) {
            ErrorHandler.LogFatalAndStop(ErrStrScreenH());
            _Shutdown();
            return false;
        }

        if ((Data.vSync && !(Data.fpsTarget == 0)) ||
                (!Data.vSync && Data.fpsTarget < 0)) {
            ErrorHandler.LogFatalAndStop(ERR_STR_VSYNC_FPS_INCOMPATIBLE);
            _Shutdown();
            return false;
        }

        if (!Window.Init(Data.WIN_TITLE_STR, Data.winW, Data.winH, Data.vSync,
                Data.fullscreen, Data.borderless, Data.GL_MAJOR, Data.GL_MINOR,
                Data.GL_PROFILE)) {
            ErrorHandler.LogFatalAndStop(ErrStrFailedInitWin());
            _Shutdown();
            return false;
        } stage = STAGE_WINDOW;

        if (!InputSys.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_INPUT_SYS);
            _Shutdown();
            return false;
        } stage = STAGE_INPUT_SYS;

        if (!LwjglInput.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_LWJGL_INPUT);
            _Shutdown();
            return false;
        } stage = STAGE_LWJGL_INPUT;

        if (!ResourceStreamResolver.Init(null,
                ResourceStreamResolver.MODE_CLASSPATH)) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_RESOURCE);
            _Shutdown();
            return false;
        }

        if (!(MasterRenderer.Init(Data.winW, Data.winH))) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_MASTER_RENDERER);
            _Shutdown();
            return false;
        } stage = STAGE_RENDERER;

        if (!MasterRenderer.ActivateModelRenderer()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_MODEL_RENDERER);
            _Shutdown();
            return false;
        }

        if (!MasterRenderer.ActivateUIRenderer()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_UI_RENDERER);
            _Shutdown();
            return false;
        }

        if (!Timer.Init(0L)) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_TIMER);
            _Shutdown();
            return false;
        }

        if (!AudioContext.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_AUDIO);
            _Shutdown();
            return false;
        } stage = STAGE_AUDIO;

        if (!GCPriest.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_GC_PRIEST);
            _Shutdown();
            return false;
        } stage = STAGE_GC_PRIEST;

        if (!SceneManager.Init()) {
            ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_SCENE);
            _Shutdown();
            return false;
        } stage = STAGE_SCENE;

        try {
            if (!InitGame()) {
                ErrorHandler.LogFatalAndStop(ERR_STR_FAIL_INIT_GAME);
                _Shutdown();
                return false;
            }
        } catch (OutOfMemoryError e) {
            ErrorHandler.LogFatalExcpAndStop(ERR_STR_FAIL_INIT_GAME_OOM, e);
            _Shutdown();
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

        _Shutdown();

        init = false;
    }

    static void _Shutdown() {
        if (stage >= STAGE_SCENE)       SceneManager.Shutdown();
        if (stage >= STAGE_GC_PRIEST)   GCPriest.Shutdown();
        if (stage >= STAGE_AUDIO)       AudioContext.Shutdown();
        if (stage >= STAGE_RENDERER)    MasterRenderer.Shutdown();
        if (stage >= STAGE_LWJGL_INPUT) LwjglInput.Shutdown();
        if (stage >= STAGE_INPUT_SYS)   InputSys.Shutdown();
        if (stage >= STAGE_WINDOW)      Window.Shutdown();
        if (stage >= STAGE_LOGGER)      Logger.Shutdown();
        stage = STAGE_NONE;
    }

    private static final String CLASS = "Engine";
    private static final String ERR_STR_FAIL_USER_DIR = CLASS +
            " failed to initialize because it failed to resolve the user " +
            "directory.\n";
    private static final String ERR_STR_FAIL_LOG = CLASS +
            " failed to initialize because the logger " +
            "failed to initialize.\n";
    private static final String ERR_STR_FAIL_CFG = CLASS +
            " failed to initialize because the config file parser " +
            "failed to initialize.\n";
    private static final String ERR_STR_FAIL_INPUT_SYS = CLASS +
            " failed to initialize because the input system " +
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
            "initialize.\n";
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
    private static String ErrStrFailedInitWin() {
        return String.format("%s failed to initialize " +
                "because the window failed to initialize.\n", CLASS);
    }
}