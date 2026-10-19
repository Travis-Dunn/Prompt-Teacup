#version 330 core

/* A star is a grid of star pixels, each with a weight from its shape. A
   framebuffer pixel receives each star pixel's light in proportion to how
   much of it the framebuffer pixel covers: an exact box filter. On the grid,
   that is the star pixel itself, crisp; between, it is shared, and the total
   is unchanged. Light is shared in linear space, then encoded, so a star
   split across pixels is as bright as one on a single pixel. Output is
   added to what is behind. Shape ids match Generator. */

in vec2 fbPx;
flat in vec2 corner;
flat in float shape;
flat in float size;
flat in vec4 color;
flat in vec2 galaxy;

uniform float starPx;

out vec4 fragColor;

const float DOT = 0.0, BLOCK2 = 1.0, PLUS = 2.0, SOFT3 = 3.0, HERO = 4.0;
const float GAMMA = 2.2;

/* Weight of star pixel `cell`, 0 outside the star. */
float weight(vec2 cell) {
    vec2 q;
    float r, w;

    if (any(lessThan(cell, vec2(0.0))) ||
        any(greaterThanEqual(cell, vec2(size)))) return 0.0;

    /* offset of this star pixel's center from the star's center */
    q = cell + 0.5 - 0.5 * size;
    r = length(q);

    if (shape == DOT) {
        w = 1.0;
    } else if (shape == BLOCK2) {
        w = 0.55;
    } else if (shape == PLUS) {
        float m = abs(q.x) + abs(q.y);
        w = m < 0.5 ? 1.0 : (m < 1.5 ? 0.35 : 0.0);
    } else if (shape == SOFT3) {
        float m = abs(q.x) + abs(q.y);
        w = m < 0.5 ? 1.0 : (m < 1.5 ? 0.45 : 0.18);
    } else if (shape == HERO) {
        /* soft core, faint halo, and thin cross spikes */
        float core = exp(-r * r / (2.0 * pow(size * 0.11, 2.0)));
        float halo = 0.22 * exp(-r / (size * 0.22));
        float spike = 0.35 * max(exp(-abs(q.y) * 1.6) *
                                 exp(-abs(q.x) / (size * 0.28)),
                                 exp(-abs(q.x) * 1.6) *
                                 exp(-abs(q.y) / (size * 0.28)));
        w = min(1.0, core + halo + spike);
    } else {
        /* galaxy: an inclined disk with a bright bulge and faint arms */
        float s = sin(galaxy.x), c = cos(galaxy.x);
        vec2 e = vec2(c * q.x + s * q.y, (-s * q.x + c * q.y) / galaxy.y);
        float re = length(e) + 0.001;
        float bulge = exp(-pow(re / (size * 0.09), 2.0));
        float disk = exp(-re / (size * 0.16));
        float arms = 1.0 + 0.6 * cos(2.0 * atan(e.y, e.x) -
                                     4.0 * log(re / size + 0.05));
        w = min(1.0, bulge + 0.55 * disk * arms);
        /* fade to nothing at the quad's edge */
        w *= clamp(1.0 - re / (0.5 * size), 0.0, 1.0);
    }
    return pow(color.a * w, GAMMA);
}

void main() {
    /* this framebuffer pixel's span, in star pixels from the star's corner */
    vec2 u0 = (fbPx - 0.5 - corner) / starPx;
    vec2 u1 = u0 + 1.0 / starPx;
    vec2 k = floor(u0);
    /* fractions of this pixel lying in star pixel k and in k + 1 */
    vec2 a = (min(u1, k + 1.0) - u0) * starPx;
    vec2 b = max(u1 - (k + 1.0), 0.0) * starPx;
    float lin = a.x * a.y * weight(k) +
                b.x * a.y * weight(k + vec2(1.0, 0.0)) +
                a.x * b.y * weight(k + vec2(0.0, 1.0)) +
                b.x * b.y * weight(k + vec2(1.0, 1.0));

    fragColor = vec4(color.rgb * pow(lin, 1.0 / GAMMA), 1.0);
}
