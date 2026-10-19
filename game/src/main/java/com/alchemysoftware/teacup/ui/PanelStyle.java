package com.alchemysoftware.teacup.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

import com.alchemysoftware.whitetail.log.LogLevel;
import com.alchemysoftware.whitetail.log.Logger;
import com.alchemysoftware.whitetail.renderer.GLMesh;
import com.alchemysoftware.whitetail.renderer.MasterRenderer;
import com.alchemysoftware.whitetail.renderer.ShaderProgram;
import com.alchemysoftware.whitetail.renderer.ShaderUniform;
import com.alchemysoftware.whitetail.renderer.UIRenderer;
import com.alchemysoftware.whitetail.resource.ResourceStreamResolver;

/**
 * One look for UI panels: a fragment shader that draws the whole panel
 * procedurally, with no textures, plus its theme colors.
 *
 * The shader decides the shape; the shapes' dimensions are part of a
 * style's identity and live in its shader file. Colors are the theme and
 * live here, so retheming never turns one style into another. There are no
 * defaults: fill in every color and both pads before use. Colors are read
 * every frame.
 *
 * Load once, after the UI renderer is active and the resource resolver is
 * initialized. GPU resources belong to MasterRenderer and are released at
 * its shutdown. Not thread-safe.
 */
public final class PanelStyle {
    /** Primary accent, r, g, b. */
    public final float[] colA = new float[3];
    /** Secondary accent, r, g, b. */
    public final float[] colB = new float[3];
    /** Panel interior, r, g, b, and opacity. */
    public final float[] fill = new float[4];

    /** How far the drawn quad extends past the panel rect on each side,
     *  in virtual pixels, so shadows and glow are not clipped. */
    public float padX, padY;

    final ShaderProgram shader;
    final GLMesh quad;
    final ShaderUniform uSize, uPad, uTime, uSeed, uAa, uState, uColA, uColB,
            uFill;

    private PanelStyle(ShaderProgram shader, GLMesh quad,
                       ShaderUniform[] u) {
        this.shader = shader;
        this.quad = quad;
        uSize = u[0];
        uPad = u[1];
        uTime = u[2];
        uSeed = u[3];
        uAa = u[4];
        uState = u[5];
        uColA = u[6];
        uColB = u[7];
        uFill = u[8];
    }

    /**
     * @param folder resource folder holding all three shaders
     * @param vertFilename vertex shader shared by UI quads
     * @param headFilename fragment shader head shared by every panel style
     * @param fragFilename the style's own fragment shader, appended to the
     *                     head
     * @return the style, or null after reporting a fatal error
     */
    public static PanelStyle Load(String folder, String vertFilename,
                                  String headFilename, String fragFilename) {
        InputStream vs, hs, fs;
        ShaderProgram shader;
        ShaderUniform[] u;
        GLMesh quad;
        int i;

        if ((vs = ResourceStreamResolver.Get(folder, vertFilename)) == null)
            return null;
        if ((hs = ResourceStreamResolver.Get(folder, headFilename)) == null) {
            Close(vs, vertFilename);
            return null;
        }
        if ((fs = ResourceStreamResolver.Get(folder, fragFilename)) == null) {
            Close(vs, vertFilename);
            Close(hs, headFilename);
            return null;
        }

        /* GLSL has no #include; the head and the style read as one file. */
        shader = MasterRenderer.CreateShaderProgram(vs,
                new SequenceInputStream(hs, fs));
        Close(vs, vertFilename);
        Close(hs, headFilename);
        Close(fs, fragFilename);
        if (shader == null) return null;

        u = new ShaderUniform[UNIFORMS.length];
        for (i = 0; i < UNIFORMS.length; ++i) {
            if ((u[i] = shader.getUniform(UNIFORMS[i])) == null) {
                MasterRenderer.DestroyShaderProgram(shader);
                return null;
            }
        }

        if ((quad = UIRenderer.CreateMesh()) == null) {
            MasterRenderer.DestroyShaderProgram(shader);
            return null;
        }

        Logger.LogSession(LogLevel.DEBUG, CLASS + " loaded [" +
                fragFilename + "].\n");
        return new PanelStyle(shader, quad, u);
    }

    private static void Close(InputStream is, String filename) {
        try {
            is.close();
        } catch (IOException e) {
            Logger.LogSessionExcp(LogLevel.WARNING, ErrStrClose(filename), e);
        }
    }

    /* Order matches the constructor. Every style must use all of them. */
    private static final String[] UNIFORMS = { "size", "pad", "time",
            "seed", "aa", "state", "colA", "colB", "fill" };

    private static final String CLASS = "PanelStyle";
    private static String ErrStrClose(String filename) {
        return CLASS + " failed to close the stream for [" + filename +
                "].\n";
    }
}
