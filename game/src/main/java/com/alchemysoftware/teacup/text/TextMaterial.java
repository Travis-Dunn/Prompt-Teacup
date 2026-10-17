package com.alchemysoftware.teacup.text;

import org.lwjgl.opengl.GL13;

import com.alchemysoftware.whitetail.renderer.BlendMode;
import com.alchemysoftware.whitetail.renderer.GLTexture;
import com.alchemysoftware.whitetail.renderer.Material;
import com.alchemysoftware.whitetail.renderer.ShaderProgram;
import com.alchemysoftware.whitetail.renderer.ShaderUniform;

/**
 * Per-label material: the label's coverage texture and tint. UIRenderer
 * binds once per material, so each label needs its own.
 */
final class TextMaterial extends Material {
    private final ShaderUniform uniformColor;

    GLTexture texture;
    float r, g, b, a;

    TextMaterial(ShaderProgram shaderProgram, ShaderUniform uniformColor) {
        super(shaderProgram, BlendMode.ALPHA);
        this.uniformColor = uniformColor;
    }

    @Override
    protected void bindProperties() {
        /* The sampler is fixed to unit 0; don't inherit another material's
           active unit. */
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        texture.bind();
        shaderProgram.setUniform4f(uniformColor, r, g, b, a);
    }
}
