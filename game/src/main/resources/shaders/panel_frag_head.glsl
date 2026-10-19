#version 330 core

/* Shared head of every panel style's fragment shader; PanelStyle prepends
   it to the style's own file. The quad covers the panel rect grown by pad
   on each side, so shadows and glow can spill past it. Output is
   premultiplied alpha (BlendMode.PREMULTIPLIED): solid paint writes alpha,
   light writes color with no alpha. */

in vec2 uv;

uniform vec2 size;   /* panel rect, virtual pixels */
uniform vec2 pad;    /* spill on each side, virtual pixels */
uniform float time;  /* seconds, wrapped every 600 */
uniform float seed;  /* per panel, 0 to 1, decorrelates effects */
uniform float aa;    /* one framebuffer pixel, in virtual pixels */
uniform vec3 state;  /* hover 0 to 1, active 0 to 1, disabled 0 to 1 */
uniform vec3 colA;   /* primary accent */
uniform vec3 colB;   /* secondary accent */
uniform vec4 fill;   /* panel interior; alpha is interior opacity */

out vec4 fragColor;

/* Virtual-pixel position with the panel rect at [0, size], y up. */
vec2 localP() {
    vec2 ext = size + 2.0 * pad;
    return vec2(uv.x, 1.0 - uv.y) * ext - pad;
}
