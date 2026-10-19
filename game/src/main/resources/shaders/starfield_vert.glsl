#version 330 core

/* Every star is an instance of one unit quad, drawn in a single call. Each
   layer wraps over a TILE-sized square, offset by its share of the camera.
   The per-instance record is Generator.LAYOUT. */

layout(location = 0) in vec3 pos;        /* quad corner, 0 or 1 in x and y */
layout(location = 7) in vec3 star;       /* x, y in the tile; layer */
layout(location = 8) in vec2 kind;       /* shape id, size in star pixels */
layout(location = 9) in vec4 colors;     /* r, g, b, brightness */
layout(location = 10) in vec2 galaxyIn;  /* galaxy angle, aspect */

uniform vec2 fbSize;       /* framebuffer pixels */
uniform vec2 origin;       /* framebuffer pixel the tile is centered on */
uniform float scale;       /* framebuffer pixels per virtual pixel */
uniform float starPx;      /* framebuffer pixels per star pixel, whole */
uniform vec2 offsets[5];   /* per layer camera offset, wrapped to TILE */

const float TILE = 4096.0;

out vec2 local;            /* position in the star, in star pixels */
flat out float shape;
flat out float size;
flat out vec4 color;
flat out vec2 galaxy;      /* angle, aspect */

void main() {
    int layer = int(star.z + 0.5);
    /* centered on the screen, so the tile covers it whatever the aspect */
    vec2 c = mod(star.xy - offsets[layer], TILE) - 0.5 * TILE;
    float px = kind.y * starPx;
    /* snap the star's corner to the pixel grid: crisp, never smeared */
    vec2 p0 = floor(origin + c * scale - 0.5 * px + 0.5);
    vec2 p = p0 + pos.xy * px;

    gl_Position = vec4(p.x / fbSize.x * 2.0 - 1.0,
                       1.0 - p.y / fbSize.y * 2.0, 0.0, 1.0);
    local = pos.xy * kind.y;
    shape = kind.x;
    size = kind.y;
    color = colors;
    galaxy = galaxyIn;
}
