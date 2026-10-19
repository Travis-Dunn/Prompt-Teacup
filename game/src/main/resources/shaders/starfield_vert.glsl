#version 330 core

/* Every star is an instance of one unit quad, drawn in a single call. Each
   layer wraps over a tile-sized square, offset by its share of the camera.
   The per-instance record is Generator.LAYOUT.

   Stars are not snapped: the quad covers the star's footprint plus one
   pixel, and the fragment shader shares the light out by coverage. */

layout(location = 0) in vec3 pos;        /* quad corner, 0 or 1 in x and y */
layout(location = 7) in vec3 star;       /* x, y in the tile; layer */
layout(location = 8) in vec2 kind;       /* shape id, size in star pixels */
layout(location = 9) in vec4 colors;     /* r, g, b, brightness */
layout(location = 10) in vec2 galaxyIn;  /* galaxy angle, aspect */

uniform vec2 fbSize;       /* framebuffer pixels */
uniform vec2 origin;       /* whole framebuffer pixel the tile centers on */
uniform float scale;       /* framebuffer pixels per virtual pixel */
uniform float starPx;      /* framebuffer pixels per star pixel, whole */
uniform float tile;        /* wrap square, a whole number of pixels */
uniform vec2 offsets[5];   /* per layer camera offset, wrapped to tile */

out vec2 fbPx;             /* framebuffer position, top-left origin */
flat out vec2 corner;      /* the star's exact top-left, framebuffer px */
flat out float shape;
flat out float size;
flat out vec4 color;
flat out vec2 galaxy;      /* angle, aspect */

void main() {
    int layer = int(star.z + 0.5);
    /* centered on the screen, so the tile covers it whatever the aspect */
    vec2 c = mod(star.xy - offsets[layer], tile) - 0.5 * tile;
    float px = kind.y * starPx;
    /* a whole-pixel half size keeps every star on the layer's phase */
    vec2 p0 = origin + c * scale - floor(0.5 * px);
    /* whole pixels from floor(p0), one wider than the star for spill */
    vec2 p = floor(p0) + pos.xy * (px + 1.0);

    gl_Position = vec4(p.x / fbSize.x * 2.0 - 1.0,
                       1.0 - p.y / fbSize.y * 2.0, 0.0, 1.0);
    fbPx = p;
    corner = p0;
    shape = kind.x;
    size = kind.y;
    color = colors;
    galaxy = galaxyIn;
}
