#version 330 core

/* One quad per star. Every star of every layer is in one mesh; each layer
   wraps over a TILE-sized square, offset by its share of the camera. */

layout(location = 0) in vec3 pos;        /* x, y in the tile; layer */
layout(location = 1) in vec3 normals;    /* galaxy angle, aspect, unused */
layout(location = 2) in vec2 texCoord1;  /* corner, 0 or 1 */
layout(location = 3) in vec2 texCoord2;  /* shape id, size in star pixels */
layout(location = 4) in vec4 colors;     /* r, g, b, brightness */

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
    int layer = int(pos.z + 0.5);
    /* centered on the screen, so the tile covers it whatever the aspect */
    vec2 c = mod(pos.xy - offsets[layer], TILE) - 0.5 * TILE;
    float px = texCoord2.y * starPx;
    /* snap the star's corner to the pixel grid: crisp, never smeared */
    vec2 p0 = floor(origin + c * scale - 0.5 * px + 0.5);
    vec2 p = p0 + texCoord1 * px;

    gl_Position = vec4(p.x / fbSize.x * 2.0 - 1.0,
                       1.0 - p.y / fbSize.y * 2.0, 0.0, 1.0);
    local = texCoord1 * texCoord2.y;
    shape = texCoord2.x;
    size = texCoord2.y;
    color = colors;
    galaxy = normals.xy;
}
