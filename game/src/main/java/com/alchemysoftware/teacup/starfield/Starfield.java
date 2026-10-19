package com.alchemysoftware.teacup.starfield;

import java.io.IOException;
import java.io.InputStream;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.alchemysoftware.teacup.layout.Layout;
import com.alchemysoftware.whitetail.error.ErrorHandler;
import com.alchemysoftware.whitetail.error.ErrorStrings;
import com.alchemysoftware.whitetail.image.Image;
import com.alchemysoftware.whitetail.log.LogLevel;
import com.alchemysoftware.whitetail.log.Logger;
import com.alchemysoftware.whitetail.renderer.GLInstanceBuffer;
import com.alchemysoftware.whitetail.renderer.GLMesh;
import com.alchemysoftware.whitetail.renderer.GLTexture;
import com.alchemysoftware.whitetail.renderer.MasterRenderer;
import com.alchemysoftware.whitetail.renderer.ShaderProgram;
import com.alchemysoftware.whitetail.renderer.ShaderUniform;
import com.alchemysoftware.whitetail.renderer.UIRenderer;
import com.alchemysoftware.whitetail.resource.ResourceStreamResolver;
import com.alchemysoftware.whitetail.window.Window;

/**
 * The parallax star field behind everything: a haze, then five layers of
 * stars, hero stars, and galaxies, each layer sliding a different fraction
 * of the camera's movement.
 *
 * Build() makes a sky from StarfieldParams; the same params always make the
 * same sky. SetCamera() says where the viewer is, in virtual pixels of
 * movement at full parallax; the star field never moves on its own, so a
 * menu that wants drift moves the camera itself. Draw() renders
 * immediately, so call it from a scene's onRender(): everything submitted
 * to MasterRenderer draws after it and therefore in front.
 *
 * Stars are drawn in whole star pixels, one framebuffer pixel each at 1080p
 * and two at 4K, snapped to the pixel grid so they stay crisp. Drawing
 * allocates nothing; Build() is a load-time operation. Not thread-safe.
 */
public final class Starfield {
    /** Layers, far to near. */
    static final int LAYERS = 5;
    /** Side of the square every layer wraps over, in virtual pixels;
     *  larger than any screen, so no star shows twice. */
    static final float TILE = 4096.0f;

    /* Fraction of camera movement each layer follows, far to near, and the
       haze behind them all. */
    private static final double[] PARALLAX =
            { 0.03, 0.06, 0.11, 0.18, 0.28 };
    private static final double HAZE_PARALLAX = 0.015;
    /* Virtual pixels per haze noise texel; the haze repeats every
       HAZE_SIZE of them. */
    private static final float HAZE_TEXEL = 8.0f;

    private static boolean init;
    private static int fbW, fbH;
    private static float starPx;

    private static ShaderProgram starShader, hazeShader;
    private static ShaderUniform uStarFb, uStarOrigin, uStarScale, uStarPx;
    private static ShaderUniform[] uStarOffsets;
    private static ShaderUniform uHazeFb, uHazeOrigin, uHazeScale,
            uHazeOffset, uHazeColor, uHazeBrightness, uHazeClumping;
    /* Unit quad: the haze stretches it over the screen, every star is an
       instance of it. */
    private static GLMesh quad;
    private static GLTexture.Properties hazeProps;

    /* The current sky. */
    private static boolean built;
    private static GLInstanceBuffer stars;
    private static GLTexture haze;
    private static float hazeR, hazeG, hazeB, hazeBrightness, hazeClumping;

    private static double camX, camY;

    private Starfield() {}

    /**
     * Loads the shaders. Call after Layout.Init(), once the UI renderer is
     * active and the resource resolver is initialized.
     *
     * @param folder resource folder holding all four shaders
     * @return false after reporting a fatal error
     */
    public static boolean Init(String folder, String starVert,
                               String starFrag, String hazeVert,
                               String hazeFrag) {
        int i;

        assert !init : CLASS + ErrorStrings.ERR_STR_DOUBLE_INIT;

        Logger.LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        fbW = Window.GetWidth();
        fbH = Window.GetHeight();
        starPx = Math.max(1, Math.round(Layout.GetScale()));

        if ((starShader = Load(folder, starVert, starFrag)) == null) {
            Release();
            return false;
        }
        if ((hazeShader = Load(folder, hazeVert, hazeFrag)) == null) {
            Release();
            return false;
        }

        try {
            uStarOffsets = new ShaderUniform[LAYERS];
            hazeProps = new GLTexture.Properties();
        } catch (OutOfMemoryError e) {
            Release();
            ErrorHandler.LogFatalAndStop(
                    CLASS + ErrorStrings.ERR_STR_FAILED_INIT_OOM);
            return false;
        }
        for (i = 0; i < LAYERS; ++i) {
            if ((uStarOffsets[i] = starShader.getUniform(
                    "offsets[" + i + "]")) == null) {
                Release();
                return false;
            }
        }
        if ((uStarFb = starShader.getUniform("fbSize")) == null ||
                (uStarOrigin = starShader.getUniform("origin")) == null ||
                (uStarScale = starShader.getUniform("scale")) == null ||
                (uStarPx = starShader.getUniform("starPx")) == null ||
                (uHazeFb = hazeShader.getUniform("fbSize")) == null ||
                (uHazeOrigin = hazeShader.getUniform("origin")) == null ||
                (uHazeScale = hazeShader.getUniform("scale")) == null ||
                (uHazeOffset = hazeShader.getUniform("offset")) == null ||
                (uHazeColor = hazeShader.getUniform("color")) == null ||
                (uHazeBrightness = hazeShader.getUniform("brightness"))
                        == null ||
                (uHazeClumping = hazeShader.getUniform("clumping")) == null) {
            Release();
            return false;
        }

        if ((quad = UIRenderer.CreateMesh()) == null) {
            Release();
            return false;
        }

        /* Low-frequency noise, magnified; it must tile seamlessly. */
        hazeProps.minFilter = GLTexture.Filter.BILINEAR;
        hazeProps.magFilter = GLTexture.Filter.BILINEAR;
        hazeProps.mipmapFilter = GLTexture.MipmapFilter.NONE;
        hazeProps.wrapS = GLTexture.Wrap.REPEAT;
        hazeProps.wrapT = GLTexture.Wrap.REPEAT;

        init = true;
        Logger.LogSession(LogLevel.DEBUG, CLASS + " initialized [star " +
                "pixel " + (int) starPx + "x].\n");
        return true;
    }

    /**
     * Releases every GPU resource.
     */
    public static void Shutdown() {
        assert init : CLASS + ErrorStrings.ERR_STR_SHUTDOWN_WITHOUT_INIT;

        Release();
        init = false;
        Logger.LogSession(LogLevel.DEBUG, CLASS + " shut down.\n");
    }

    /**
     * Replaces the sky. Load-time: generates thousands of stars and a noise
     * image, and allocates.
     *
     * @param p the sky to build; read now, not kept
     * @return false after reporting a fatal error
     */
    public static boolean Build(StarfieldParams p) {
        long t0 = System.nanoTime();
        Image image;

        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        assert p != null : ERR_STR_PARAMS_NULL;
        assert p.density >= 0.0f : ErrStrNegative("density", p.density);
        assert p.heroStars >= 0 : ErrStrNegative("hero stars", p.heroStars);
        assert p.galaxies >= 0 : ErrStrNegative("galaxies", p.galaxies);
        assert p.hazeBrightness >= 0.0f :
                ErrStrNegative("haze brightness", p.hazeBrightness);
        assert p.hazeClumping >= 0.0f && p.hazeClumping <= 1.0f :
                ErrStrClumping(p.hazeClumping);

        Unbuild();

        try {
            if ((stars = MasterRenderer.CreateInstanceBuffer(
                    Generator.LAYOUT, Generator.Stars(p))) == null)
                return false;
            if ((image = Generator.Haze(p)) == null) return false;
            if ((haze = MasterRenderer.CreateTexture(image, hazeProps))
                    == null) return false;
        } catch (OutOfMemoryError e) {
            ErrorHandler.LogFatalAndStop(ERR_STR_BUILD_OOM);
            return false;
        }

        hazeR = p.hazeColor[0];
        hazeG = p.hazeColor[1];
        hazeB = p.hazeColor[2];
        hazeBrightness = p.hazeBrightness;
        hazeClumping = p.hazeClumping;
        built = true;

        Logger.LogSession(LogLevel.DEBUG, String.format("%s built a sky " +
                "of [%d] stars from seed [%d] in [%.2f] ms.\n", CLASS,
                stars.count,
                p.seed, (System.nanoTime() - t0) / 1.0e6));
        return true;
    }

    /**
     * @param x camera x, in virtual pixels of movement at full parallax
     * @param y camera y, likewise; +y is down
     */
    public static void SetCamera(double x, double y) {
        camX = x;
        camY = y;
    }

    /**
     * Draws the haze and stars now. Call from a scene's onRender().
     * Allocates nothing.
     */
    public static void Draw() {
        float sc, ox, oy;
        double period;
        int i;

        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        assert built : ERR_STR_NOT_BUILT;

        sc = Layout.GetScale();
        ox = fbW * 0.5f;
        oy = fbH * 0.5f;

        try {
            /* MasterRenderer sets its own state before each of its passes,
               so none of this leaks into them. */
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);

            if (hazeBrightness > 0.0f) {
                period = Generator.HAZE_SIZE * (double) HAZE_TEXEL;
                GL11.glDisable(GL11.GL_BLEND);
                hazeShader.bind();
                hazeShader.setUniform2f(uHazeFb, fbW, fbH);
                hazeShader.setUniform2f(uHazeOrigin, ox, oy);
                hazeShader.setUniform1f(uHazeScale, sc);
                hazeShader.setUniform2f(uHazeOffset,
                        (float) Wrap(camX * HAZE_PARALLAX, period),
                        (float) Wrap(camY * HAZE_PARALLAX, period));
                hazeShader.setUniform3f(uHazeColor, hazeR, hazeG, hazeB);
                hazeShader.setUniform1f(uHazeBrightness, hazeBrightness);
                hazeShader.setUniform1f(uHazeClumping, hazeClumping);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                haze.bind();
                quad.draw();
            }

            if (stars.count > 0) {
                /* Light adds up: overlapping stars brighten. */
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                starShader.bind();
                starShader.setUniform2f(uStarFb, fbW, fbH);
                starShader.setUniform2f(uStarOrigin, ox, oy);
                starShader.setUniform1f(uStarScale, sc);
                starShader.setUniform1f(uStarPx, starPx);
                /* Offsets are wrapped here in double precision, so the sky
                   never jitters however far the camera travels. */
                for (i = 0; i < LAYERS; ++i) {
                    starShader.setUniform2f(uStarOffsets[i],
                            (float) Wrap(camX * PARALLAX[i], TILE),
                            (float) Wrap(camY * PARALLAX[i], TILE));
                }
                quad.drawInstanced(stars);
            }
        } catch (RuntimeException e) {
            ErrorHandler.LogFatalExcpAndStop(ERR_STR_DRAW_EXCP, e);
        }
    }

    private static double Wrap(double v, double period) {
        double w = v % period;
        return w < 0.0 ? w + period : w;
    }

    private static ShaderProgram Load(String folder, String vert,
                                      String frag) {
        InputStream vs, fs;
        ShaderProgram sp;

        if ((vs = ResourceStreamResolver.Get(folder, vert)) == null)
            return null;
        if ((fs = ResourceStreamResolver.Get(folder, frag)) == null) {
            Close(vs, vert);
            return null;
        }
        sp = MasterRenderer.CreateShaderProgram(vs, fs);
        Close(vs, vert);
        Close(fs, frag);
        return sp;
    }

    private static void Unbuild() {
        if (stars != null) MasterRenderer.DestroyInstanceBuffer(stars);
        if (haze != null) MasterRenderer.DestroyTexture(haze);
        stars = null;
        haze = null;
        built = false;
    }

    private static void Release() {
        Unbuild();
        if (quad != null) MasterRenderer.DestroyMesh(quad);
        if (starShader != null) MasterRenderer.DestroyShaderProgram(starShader);
        if (hazeShader != null) MasterRenderer.DestroyShaderProgram(hazeShader);
        quad = null;
        starShader = hazeShader = null;
        uStarOffsets = null;
        hazeProps = null;
    }

    private static void Close(InputStream is, String filename) {
        try {
            is.close();
        } catch (IOException e) {
            Logger.LogSessionExcp(LogLevel.WARNING, ErrStrClose(filename), e);
        }
    }

    private static final String CLASS = "Starfield";
    private static final String ERR_STR_PARAMS_NULL = CLASS +
            " failed to build a sky because the params were null.\n";
    private static final String ERR_STR_NOT_BUILT = CLASS +
            " failed to draw because no sky has been built.\n";
    private static final String ERR_STR_BUILD_OOM = CLASS +
            " failed to build a sky because an OutOfMemoryError was " +
            "encountered.\n";
    private static final String ERR_STR_DRAW_EXCP = CLASS +
            " failed to draw because LWJGL rejected an OpenGL operation.\n";
    private static String ErrStrNegative(String what, float v) {
        return CLASS + " failed to build a sky because " + what + " [" + v +
                "] was negative.\n";
    }
    private static String ErrStrClumping(float v) {
        return CLASS + " failed to build a sky because haze clumping [" + v +
                "] was out of bounds [0,1,inclusive].\n";
    }
    private static String ErrStrClose(String filename) {
        return CLASS + " failed to close the stream for [" + filename +
                "].\n";
    }
}
