package com.alchemysoftware.teacup.ui;

import com.alchemysoftware.teacup.layout.Layout;
import com.alchemysoftware.whitetail.renderer.BlendMode;
import com.alchemysoftware.whitetail.renderer.Material;
import com.alchemysoftware.whitetail.renderer.ShaderProgram;

/**
 * Per-panel material: the panel's size and interaction state, plus its
 * style's theme. UIRenderer binds once per material, so each panel needs
 * its own.
 */
final class PanelMaterial extends Material {
    /* Shared clock for animated styles, wrapped so float precision holds. */
    private static final long EPOCH = System.nanoTime();
    private static final long TIME_WRAP_NS = 600L * 1000000000L;

    private final PanelStyle style;
    final float seed;
    float w, h, hover, active;

    PanelMaterial(PanelStyle style, float seed) {
        super(style.shader, BlendMode.PREMULTIPLIED);
        this.style = style;
        this.seed = seed;
    }

    @Override
    protected void bindProperties() {
        ShaderProgram sp = shaderProgram;
        PanelStyle s = style;
        float t = ((System.nanoTime() - EPOCH) % TIME_WRAP_NS) / 1.0e9f;

        sp.setUniform2f(s.uSize, w, h);
        sp.setUniform2f(s.uPad, s.padX, s.padY);
        sp.setUniform1f(s.uTime, t);
        sp.setUniform1f(s.uSeed, seed);
        sp.setUniform1f(s.uAa, 1.0f / Layout.GetScale());
        sp.setUniform3f(s.uState, hover, active, 0.0f);
        sp.setUniform3f(s.uColA, s.colA[0], s.colA[1], s.colA[2]);
        sp.setUniform3f(s.uColB, s.colB[0], s.colB[1], s.colB[2]);
        sp.setUniform4f(s.uFill, s.fill[0], s.fill[1], s.fill[2], s.fill[3]);
    }
}
