/* CONSTRUCT: mission-control signage as screen-printed constructivist
   slabs. Forward-leaning sheared panels with one sliced corner, near-solid
   duotone fills under animated film grain, a hard offset drop shadow
   instead of any glow, an accent underline bar, and a thin print keyline.
   Hover floods the slab with the accent and the label flips dark: a poster
   inverting, not a lamp brightening. Emits no light.

   Ported from Farwake's UI zoo. Its numbers are in 1280x960 units; U
   converts them to ours, so they stay comparable with the original. */

const float U = 1.125;

const float SKEW = 0.16;          /* lean, as a fraction of height */
const float CUT = 13.0 * U;       /* sliced corner */
const float SHADOW = 6.0 * U;     /* drop shadow offset, right and down */
const float GRAIN = 0.9 / U;      /* grain cells per virtual pixel */
const float BAR_H = 5.0 * U;      /* underline bar */
const float KEYLINE = 1.1 * U;    /* print keyline half-width */

float sdBox(vec2 p, vec2 b) {
    vec2 q = abs(p) - b;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0);
}

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

/* The slab: a box with the top-right corner sliced off. */
float slab(vec2 sp, vec2 sz, float cut) {
    float d = sdBox(sp - 0.5 * sz, 0.5 * sz);
    float c = (sp.x - sz.x + sp.y - sz.y + cut) * 0.70710678;
    return max(d, c);
}

void main() {
    vec2 p = localP();
    float hover = state.x;
    float act = state.y;
    float disabled = state.z;
    float lit = max(hover, act);

    /* constructivist lean: shear verticals about the mid-height */
    vec2 sp = vec2(p.x - (p.y - 0.5 * size.y) * SKEW, p.y);

    float d = slab(sp, size, CUT);
    float inside = 1.0 - smoothstep(-aa, aa, d);

    /* hard offset drop shadow: print registration, not light */
    float ds = slab(sp - vec2(SHADOW, -SHADOW), size, CUT);
    float shadow = 1.0 - smoothstep(-aa, aa, ds);

    /* film grain with a slow projector flicker */
    float salt = floor(time * 8.0) * 0.37 + seed * 61.0;
    float grain = (hash2(floor(sp * GRAIN) + vec2(salt)) - 0.5) * 0.07;

    float gy = clamp(sp.y / max(size.y, 1.0), 0.0, 1.0);
    vec3 base = fill.rgb * mix(0.8, 1.15, gy) + vec3(grain);
    /* hover floods the slab with the accent */
    vec3 fc = mix(base, colA + vec3(grain * 1.5), 0.9 * lit);
    float fa = fill.a * inside;

    /* disabled: drained of ink */
    float luma = dot(fc, vec3(0.299, 0.587, 0.114));
    fc = mix(fc, vec3(luma), 0.8 * disabled);
    fa *= 1.0 - 0.45 * disabled;

    /* accent underline bar along the slab's bottom */
    float bar = inside * (1.0 - smoothstep(BAR_H - aa, BAR_H + aa, sp.y))
              * (1.0 - disabled);

    /* thin print keyline */
    float stroke = (1.0 - smoothstep(KEYLINE - aa, KEYLINE + aa, abs(d)))
                 * (0.35 + 0.65 * lit) * (1.0 - 0.7 * disabled);

    /* composite back to front, premultiplied: shadow, fill, bar, keyline */
    vec4 acc = vec4(vec3(0.0), shadow * 0.5 * (1.0 - 0.5 * disabled));
    acc.rgb = fc * fa + acc.rgb * (1.0 - fa);
    acc.a = fa + acc.a * (1.0 - fa);
    float ba = bar * 0.95;
    acc.rgb = colB * ba + acc.rgb * (1.0 - ba);
    acc.a = ba + acc.a * (1.0 - ba);
    vec3 lineCol = mix(colB, vec3(1.0), 0.2);
    acc.rgb = lineCol * stroke + acc.rgb * (1.0 - stroke);
    acc.a = stroke + acc.a * (1.0 - stroke);

    fragColor = acc;
}
