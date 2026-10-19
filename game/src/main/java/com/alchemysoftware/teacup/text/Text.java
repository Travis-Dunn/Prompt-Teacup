package com.alchemysoftware.teacup.text;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.alchemysoftware.teacup.layout.Layout;
import com.alchemysoftware.whitetail.error.ErrorHandler;
import com.alchemysoftware.whitetail.error.ErrorStrings;
import com.alchemysoftware.whitetail.log.LogLevel;
import com.alchemysoftware.whitetail.log.Logger;
import com.alchemysoftware.whitetail.renderer.GLMesh;
import com.alchemysoftware.whitetail.renderer.GLTexture;
import com.alchemysoftware.whitetail.renderer.MasterRenderer;
import com.alchemysoftware.whitetail.renderer.Model;
import com.alchemysoftware.whitetail.renderer.ModelInstance;
import com.alchemysoftware.whitetail.renderer.ShaderProgram;
import com.alchemysoftware.whitetail.renderer.ShaderUniform;
import com.alchemysoftware.whitetail.renderer.UIRenderer;
import com.alchemysoftware.whitetail.resource.ResourceStreamResolver;

/**
 * Static entry point for baked text.
 *
 * A label's text is rasterized once by Java2D into a texture at the exact
 * framebuffer pixel size it is shown at, then drawn each frame as one quad
 * through UIRenderer. Sizes and positions are in Layout's virtual pixels, so
 * text is drawn natively at every resolution rather than stretched.
 *
 * Drawing allocates nothing. Creating, re-texting, and restyling a label
 * rasterize and allocate, so treat them as load-time operations. Label
 * materials are pooled, so creating and destroying labels does not grow
 * UIRenderer's per-material state past the peak live label count.
 *
 * Call Init() after Layout.Init(), once the UI renderer is active and the
 * resource resolver is initialized; EndFrame() once per frame after
 * MasterRenderer.Render(); and Shutdown() before MasterRenderer.Shutdown().
 * Not thread-safe.
 */
public final class Text {
    private static boolean init;
    private static ShaderProgram shader;
    private static ShaderUniform uniformColor;
    private static GLMesh quad;
    private static GLTexture.Properties texProps;

    private static ArrayList<Label> live;
    /* Slots whose labels were destroyed this frame; they may still be
       queued in UIRenderer, so they become reusable only at EndFrame(). */
    private static ArrayList<ModelInstance> released;
    private static ArrayList<ModelInstance> free;
    /* Textures replaced or released this frame, destroyed at EndFrame(). */
    private static ArrayList<GLTexture> retired;

    private Text() {}

    /**
     * @param folder resource folder holding both shaders
     * @param vertFilename vertex shader
     * @param fragFilename fragment shader
     * @return false after reporting a fatal error
     */
    public static boolean Init(String folder, String vertFilename,
                               String fragFilename) {
        InputStream vs, fs;
        ShaderUniform uniformTex;

        assert !init : CLASS + ErrorStrings.ERR_STR_DOUBLE_INIT;

        Logger.LogSession(LogLevel.DEBUG, CLASS + " initializing...\n");

        if ((vs = ResourceStreamResolver.Get(folder, vertFilename)) == null)
            return false;
        if ((fs = ResourceStreamResolver.Get(folder, fragFilename)) == null) {
            Close(vs, vertFilename);
            return false;
        }
        shader = MasterRenderer.CreateShaderProgram(vs, fs);
        Close(vs, vertFilename);
        Close(fs, fragFilename);
        if (shader == null) return false;

        if ((uniformColor = shader.getUniform(UNIFORM_COLOR)) == null ||
                (uniformTex = shader.getUniform(UNIFORM_TEX)) == null) {
            Release();
            return false;
        }
        shader.bind();
        shader.setUniform1i(uniformTex, 0);
        ShaderProgram.Unbind();

        if ((quad = UIRenderer.CreateMesh()) == null) {
            Release();
            return false;
        }

        try {
            texProps = new GLTexture.Properties();
            live = new ArrayList<Label>();
            released = new ArrayList<ModelInstance>();
            free = new ArrayList<ModelInstance>();
            retired = new ArrayList<GLTexture>();
        } catch (OutOfMemoryError e) {
            Release();
            ErrorHandler.LogFatalAndStop(
                    CLASS + ErrorStrings.ERR_STR_FAILED_INIT_OOM);
            return false;
        }
        /* Texels map 1:1 to pixels; bilinear only matters if a label is
           ever placed off the pixel grid. */
        texProps.minFilter = GLTexture.Filter.BILINEAR;
        texProps.magFilter = GLTexture.Filter.BILINEAR;
        texProps.mipmapFilter = GLTexture.MipmapFilter.NONE;
        texProps.wrapS = GLTexture.Wrap.CLAMP_TO_EDGE;
        texProps.wrapT = GLTexture.Wrap.CLAMP_TO_EDGE;

        init = true;

        Logger.LogSession(LogLevel.DEBUG, CLASS + " initialized.\n");
        return true;
    }

    /**
     * Destroys every remaining GPU resource. Labels still alive are a leak
     * and are logged as a warning.
     */
    public static void Shutdown() {
        int i;

        assert init : CLASS + ErrorStrings.ERR_STR_SHUTDOWN_WITHOUT_INIT;

        if (live.size() > 0) {
            Logger.LogSession(LogLevel.WARNING, ErrStrLeak(live.size()));
            for (i = live.size() - 1; i >= 0; --i) Destroy(live.get(i));
        }
        EndFrame();
        Release();
        init = false;

        Logger.LogSession(LogLevel.DEBUG, CLASS + " shut down.\n");
    }

    /**
     * Loads a TrueType font through ResourceStreamResolver.
     *
     * @param folder resource folder
     * @param filename font file name
     * @return the font at size 1, or null after reporting a fatal error
     */
    public static Font LoadFont(String folder, String filename) {
        InputStream is;
        Font font = null;

        if ((is = ResourceStreamResolver.Get(folder, filename)) == null)
            return null;

        try {
            font = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (FontFormatException e) {
            ErrorHandler.LogFatalExcpAndStop(
                    ErrStrFontFormat(folder, filename), e);
        } catch (IOException e) {
            ErrorHandler.LogFatalExcpAndStop(
                    ErrStrFontRead(folder, filename), e);
        } catch (RuntimeException e) {
            ErrorHandler.LogFatalExcpAndStop(
                    ErrStrFontRead(folder, filename), e);
        }
        Close(is, filename);

        if (font != null) {
            Logger.LogSession(LogLevel.DEBUG, CLASS + " loaded font [" +
                    font.getFontName() + "] from [" + folder + "/" +
                    filename + "].\n");
        }
        return font;
    }

    /**
     * Creates and bakes a label. Its position is (0, 0) and its color is
     * opaque white until set.
     *
     * @param style layout and font; kept by reference and read again
     *              whenever this label re-bakes
     * @param text the text; '\n' starts a new line
     * @return the label, or null after reporting a fatal error
     */
    public static Label Create(TextStyle style, String text) {
        ModelInstance inst;
        Label label;

        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        assert text != null : ERR_STR_TEXT_NULL;

        if ((inst = TakeSlot()) == null) return null;

        try {
            label = new Label(inst, style, text);
            live.add(label);
        } catch (OutOfMemoryError e) {
            free.add(inst);
            ErrorHandler.LogFatalAndStop(ERR_STR_CREATE_OOM);
            return null;
        }
        label.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (!Bake(label)) {
            Destroy(label);
            return null;
        }
        return label;
    }

    /**
     * Releases a label. Its texture is destroyed at the next EndFrame(), so
     * destroying a label already drawn this frame is safe.
     *
     * @param label a live label; it must not be used afterwards
     */
    public static void Destroy(Label label) {
        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        assert label != null : ERR_STR_LABEL_NULL;
        assert label.inst != null : ERR_STR_DOUBLE_DESTROY;

        if (label.mat.texture != null) retired.add(label.mat.texture);
        released.add(label.inst);
        live.remove(label);
        label.inst = null;
        label.mat = null;
    }

    /**
     * Queues a label for this frame's UI pass. Allocates nothing.
     *
     * @param label a live label
     */
    public static void Draw(Label label) {
        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;
        assert label != null : ERR_STR_LABEL_NULL;
        assert label.inst != null : ERR_STR_DRAW_DESTROYED;

        UIRenderer.Submit(label.inst);
    }

    /**
     * Destroys textures retired this frame and makes released labels'
     * resources reusable. Call once per frame after MasterRenderer.Render().
     * Does nothing, and allocates nothing, when nothing was retired.
     */
    public static void EndFrame() {
        int i;

        assert init : CLASS + ErrorStrings.ERR_STR_NOT_INIT;

        for (i = 0; i < retired.size(); ++i)
            MasterRenderer.DestroyTexture(retired.get(i));
        retired.clear();

        for (i = 0; i < released.size(); ++i) free.add(released.get(i));
        released.clear();
    }

    /* Re-bakes a label, retiring its old texture. */
    static boolean Bake(Label label) {
        long t0 = System.nanoTime();
        GLTexture old = label.mat.texture;
        GLTexture texture;

        if ((texture = Baker.Bake(label, Layout.GetScale(), texProps))
                == null)
            return false;

        if (old != null) retired.add(old);
        label.mat.texture = texture;
        label.updateMatrix();

        Logger.LogSession(LogLevel.DEBUG, LogStrBaked(label,
                (System.nanoTime() - t0) / 1.0e6));
        return true;
    }

    private static ModelInstance TakeSlot() {
        int last = free.size() - 1;
        TextMaterial mat;

        if (last >= 0) {
            ModelInstance inst = free.remove(last);
            ((TextMaterial) inst.model.material).texture = null;
            return inst;
        }

        try {
            mat = new TextMaterial(shader, uniformColor);
            return new ModelInstance(new Model(quad, mat));
        } catch (OutOfMemoryError e) {
            ErrorHandler.LogFatalAndStop(ERR_STR_CREATE_OOM);
            return null;
        }
    }

    private static void Release() {
        if (quad != null) MasterRenderer.DestroyMesh(quad);
        if (shader != null) MasterRenderer.DestroyShaderProgram(shader);
        quad = null;
        shader = null;
        uniformColor = null;
        texProps = null;
        live = null;
        released = null;
        free = null;
        retired = null;
    }

    private static void Close(InputStream is, String filename) {
        try {
            is.close();
        } catch (IOException e) {
            Logger.LogSessionExcp(LogLevel.WARNING, ErrStrClose(filename), e);
        }
    }

    private static final String UNIFORM_COLOR = "color";
    private static final String UNIFORM_TEX = "tex";

    private static final String CLASS = "Text";
    private static final String ERR_STR_TEXT_NULL = CLASS +
            " failed to create a label because the text was null.\n";
    private static final String ERR_STR_LABEL_NULL = CLASS +
            " was given a null label.\n";
    private static final String ERR_STR_DOUBLE_DESTROY = CLASS +
            " failed to destroy a label because it was already destroyed.\n";
    private static final String ERR_STR_DRAW_DESTROYED = CLASS +
            " failed to draw a label because it was destroyed.\n";
    private static final String ERR_STR_CREATE_OOM = CLASS +
            " failed to create a label because an OutOfMemoryError was " +
            "encountered.\n";
    private static String ErrStrLeak(int n) {
        return CLASS + " shut down with [" + n + "] labels never " +
                "destroyed; destroying them now.\n";
    }
    private static String ErrStrFontFormat(String folder, String filename) {
        return CLASS + " failed to load the font [" + folder + "/" +
                filename + "] because it is not a valid TrueType font.\n";
    }
    private static String ErrStrFontRead(String folder, String filename) {
        return CLASS + " failed to load the font [" + folder + "/" +
                filename + "] because it could not be read.\n";
    }
    private static String ErrStrClose(String filename) {
        return CLASS + " failed to close the stream for [" + filename +
                "].\n";
    }
    private static String LogStrBaked(Label label, double ms) {
        return String.format("%s baked [%s] into a [%dx%d] texture in " +
                "[%.2f] ms.\n", CLASS, label.text, label.texW, label.texH, ms);
    }
}
