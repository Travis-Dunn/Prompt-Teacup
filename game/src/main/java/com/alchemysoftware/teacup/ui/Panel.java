package com.alchemysoftware.teacup.ui;

import org.lwjgl.util.vector.Matrix4f;

import com.alchemysoftware.teacup.layout.Layout;
import com.alchemysoftware.teacup.layout.Rect;
import com.alchemysoftware.whitetail.error.ErrorHandler;
import com.alchemysoftware.whitetail.renderer.Model;
import com.alchemysoftware.whitetail.renderer.ModelInstance;
import com.alchemysoftware.whitetail.renderer.UIRenderer;

/**
 * One procedurally drawn panel: a quad covering a rect plus its style's
 * spill. Owns no GPU resources, so there is nothing to destroy. Drawing
 * allocates nothing.
 */
final class Panel {
    /* Golden-ratio steps spread per-panel seeds evenly over [0, 1). */
    private static final float SEED_STEP = 0.6180339f;
    private static int created;

    private final PanelStyle style;
    private final PanelMaterial mat;
    private final ModelInstance inst;

    private Panel(PanelStyle style, PanelMaterial mat, ModelInstance inst) {
        this.style = style;
        this.mat = mat;
        this.inst = inst;
    }

    /**
     * @return the panel, or null after reporting a fatal error
     */
    static Panel Create(PanelStyle style) {
        PanelMaterial mat;

        try {
            mat = new PanelMaterial(style, (created++ * SEED_STEP) % 1.0f);
            return new Panel(style, mat,
                    new ModelInstance(new Model(style.quad, mat)));
        } catch (OutOfMemoryError e) {
            ErrorHandler.LogFatalAndStop(ERR_STR_CREATE_OOM);
            return null;
        }
    }

    /**
     * @param r the panel rect in virtual pixels; the quad also covers the
     *          style's spill around it. Not snapped: the shader antialiases
     *          its own edges, so it maps exactly instead.
     */
    void setRect(Rect r) {
        Matrix4f m = inst.matModel;
        float s = Layout.GetScale();

        mat.w = r.w;
        mat.h = r.h;
        m.m00 = (r.w + 2.0f * style.padX) * s;
        m.m11 = (r.h + 2.0f * style.padY) * s;
        m.m30 = Layout.ToPxX(r.x - style.padX);
        m.m31 = Layout.ToPxY(r.y - style.padY);
    }

    /**
     * @param hover 0 to 1
     * @param active 0 to 1
     */
    void setState(float hover, float active) {
        mat.hover = hover;
        mat.active = active;
    }

    void draw() {
        UIRenderer.Submit(inst);
    }

    private static final String CLASS = "Panel";
    private static final String ERR_STR_CREATE_OOM = CLASS +
            " failed to create a panel because an OutOfMemoryError was " +
            "encountered.\n";
}
