#version 330 core

/* Star shapes, evaluated at star-pixel centers so every star, however big,
   is made of whole star pixels. Shape ids match Generator. Output is added
   to what is behind. */

in vec2 local;
flat in float shape;
flat in float size;
flat in vec4 color;
flat in vec2 galaxy;

out vec4 fragColor;

const float DOT = 0.0, BLOCK2 = 1.0, PLUS = 2.0, SOFT3 = 3.0, HERO = 4.0;

void main() {
    /* offset of this star pixel's center from the star's center */
    vec2 q = floor(local) + 0.5 - 0.5 * size;
    float r = length(q);
    float w;

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

    fragColor = vec4(color.rgb * color.a * w, 1.0);
}
