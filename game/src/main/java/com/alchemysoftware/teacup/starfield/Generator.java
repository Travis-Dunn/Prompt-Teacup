package com.alchemysoftware.teacup.starfield;

import java.util.Random;

import com.alchemysoftware.whitetail.image.Image;
import com.alchemysoftware.whitetail.mesh.Mesh;

/**
 * Builds a sky from StarfieldParams: one mesh holding every star of every
 * layer, and a tileable haze noise image. Deterministic for a given seed.
 * Load-time only; allocates freely.
 */
final class Generator {
    /* Shape ids, matched by starfield_frag.glsl. */
    static final int DOT = 0, BLOCK2 = 1, PLUS = 2, SOFT3 = 3, HERO = 4,
            GALAXY = 5;

    /* Generated stars per million square virtual pixels at density 1:
       roughly 700 on a 1920x1080 screen, across all layers. */
    private static final float STARS_PER_MPX = 340.0f;
    /* Share of the generated stars in each layer, far to near. Far layers
       hold more, smaller, dimmer stars. */
    private static final float[] LAYER_SHARE =
            { 0.40f, 0.27f, 0.17f, 0.10f, 0.06f };
    private static final float[] LAYER_MAX_BRIGHTNESS =
            { 0.45f, 0.60f, 0.75f, 0.90f, 1.00f };
    /* How much haze veils each layer, far to near. */
    private static final float[] LAYER_VEIL =
            { 1.00f, 0.85f, 0.65f, 0.45f, 0.30f };
    /* Haze brightness at which the farthest layer is fully veiled. */
    private static final float VEIL_FULL = 0.16f;

    /* Star color temperatures, hot to cool, and how often each occurs. */
    private static final float[][] PALETTE = {
            { 0.70f, 0.82f, 1.00f },
            { 1.00f, 1.00f, 1.00f },
            { 1.00f, 0.95f, 0.82f },
            { 1.00f, 0.80f, 0.58f },
            { 1.00f, 0.62f, 0.46f } };
    private static final float[] PALETTE_WEIGHT =
            { 0.15f, 0.40f, 0.25f, 0.13f, 0.07f };
    /* Generated stars are mostly near-white; the tint is a hint. */
    private static final float TINT = 0.45f;

    /* Haze noise image: size in texels, octaves of periodic value noise. */
    static final int HAZE_SIZE = 256;
    private static final int[] HAZE_OCTAVE_CELLS = { 4, 8, 16, 32 };
    private static final float[] HAZE_OCTAVE_WEIGHT =
            { 0.50f, 0.27f, 0.15f, 0.08f };

    private static final int VERTS = 4, IDX = 6;

    private Generator() {}

    /**
     * @return how many stars of every kind p makes; Stars() needs at least
     *         one
     */
    static int Count(StarfieldParams p) {
        int total = p.heroStars + p.galaxies, layer;

        for (layer = 0; layer < Starfield.LAYERS; ++layer)
            total += LayerCount(p, layer);
        return total;
    }

    /**
     * @return a mesh of every star, or null after reporting a fatal error
     */
    static Mesh Stars(StarfieldParams p) {
        Random rnd = new Random(p.seed);
        int[] counts = new int[Starfield.LAYERS];
        int layer, i, n;
        Builder b = new Builder(Count(p));

        for (layer = 0; layer < Starfield.LAYERS; ++layer)
            counts[layer] = LayerCount(p, layer);

        for (layer = 0; layer < Starfield.LAYERS; ++layer) {
            float near = layer / (Starfield.LAYERS - 1.0f);
            float bigChance = 0.06f + 0.24f * near;
            float maxB = LAYER_MAX_BRIGHTNESS[layer] * Veil(p, layer);

            for (i = 0; i < counts[layer]; ++i) {
                int shape;
                float r = rnd.nextFloat(), bright;

                if (r >= bigChance) shape = DOT;
                else if (r < bigChance * 0.45f) shape = BLOCK2;
                else if (r < bigChance * 0.80f) shape = PLUS;
                else shape = SOFT3;
                n = shape == DOT ? 1 : shape == BLOCK2 ? 2 : 3;

                /* Most stars are faint. */
                bright = maxB * (0.12f + 0.88f *
                        (float) Math.pow(rnd.nextFloat(), 2.2));
                b.add(rnd, layer, shape, n, bright, TINT, 0.0f, 1.0f);
            }
        }

        /* Hero stars sit in the two farthest-but-one layers; the bigger,
           the dimmer, so they stay background. */
        for (i = 0; i < p.heroStars; ++i) {
            layer = 1 + rnd.nextInt(2);
            n = 5 + rnd.nextInt(16);
            b.add(rnd, layer, HERO, n,
                    0.85f * (float) Math.pow(5.0 / n, 0.8) * Veil(p, layer),
                    0.75f, 0.0f, 1.0f);
        }

        /* Galaxies only in the farthest layer, always faint. */
        for (i = 0; i < p.galaxies; ++i) {
            n = 9 + rnd.nextInt(12);
            b.add(rnd, 0, GALAXY, n,
                    (0.18f + 0.14f * rnd.nextFloat()) * Veil(p, 0), 0.5f,
                    rnd.nextFloat() * (float) Math.PI,
                    0.25f + 0.75f * rnd.nextFloat());
        }

        return Mesh.Create(b.pos, b.normals, b.corner, b.shape, b.colors,
                null, null, b.indices);
    }

    /**
     * @return HAZE_SIZE square grayscale noise that tiles seamlessly,
     *         spanning the full 0 to 255 range, or null after reporting a
     *         fatal error
     */
    static Image Haze(StarfieldParams p) {
        Random rnd = new Random(p.seed ^ 0x9E3779B97F4A7C15L);
        int size = HAZE_SIZE, o, x, y;
        float[] sum = new float[size * size];
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        byte[] data = new byte[size * size];

        for (o = 0; o < HAZE_OCTAVE_CELLS.length; ++o) {
            int cells = HAZE_OCTAVE_CELLS[o];
            float[] lattice = new float[cells * cells];
            float span = (float) size / cells;

            for (x = 0; x < lattice.length; ++x)
                lattice[x] = rnd.nextFloat();
            for (y = 0; y < size; ++y) {
                for (x = 0; x < size; ++x) {
                    sum[y * size + x] += HAZE_OCTAVE_WEIGHT[o] *
                            Lattice(lattice, cells, x / span, y / span);
                }
            }
        }

        for (x = 0; x < sum.length; ++x) {
            lo = Math.min(lo, sum[x]);
            hi = Math.max(hi, sum[x]);
        }
        for (x = 0; x < sum.length; ++x)
            data[x] = (byte) Math.round(255.0f * (sum[x] - lo) / (hi - lo));

        return Image.Create(size, size, Image.FORMAT_GRAY_8, data);
    }

    private static int LayerCount(StarfieldParams p, int layer) {
        float area = Starfield.TILE * Starfield.TILE / 1.0e6f;

        return Math.round(p.density * STARS_PER_MPX * area *
                LAYER_SHARE[layer]);
    }

    /* How much of a layer's light survives the haze. */
    private static float Veil(StarfieldParams p, int layer) {
        float v = LAYER_VEIL[layer] * p.hazeBrightness / VEIL_FULL;
        return Math.max(0.0f, 1.0f - v);
    }

    /* Smoothly interpolated periodic value noise. */
    private static float Lattice(float[] l, int cells, float fx, float fy) {
        int x0 = (int) fx, y0 = (int) fy;
        int x1 = (x0 + 1) % cells, y1 = (y0 + 1) % cells;
        float tx = Smooth(fx - x0), ty = Smooth(fy - y0);
        float a = l[y0 * cells + x0], b = l[y0 * cells + x1];
        float c = l[y1 * cells + x0], d = l[y1 * cells + x1];

        return (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx))
                * ty;
    }

    private static float Smooth(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    /* Appends one quad per star. Layout, per vertex:
         pos      x, y in the tile; layer
         normals  galaxy angle, galaxy aspect, unused
         corner   0 or 1 in x and y
         shape    shape id, size in star pixels
         colors   r, g, b, brightness */
    private static final class Builder {
        final float[] pos, normals, corner, shape, colors;
        final int[] indices;
        private int stars;

        Builder(int capacity) {
            pos = new float[capacity * VERTS * Mesh.POS_STRIDE];
            normals = new float[capacity * VERTS * Mesh.NORMAL_STRIDE];
            corner = new float[capacity * VERTS * Mesh.TEX_COORD_STRIDE];
            shape = new float[capacity * VERTS * Mesh.TEX_COORD_STRIDE];
            colors = new float[capacity * VERTS * Mesh.COLOR_STRIDE];
            indices = new int[capacity * IDX];
        }

        void add(Random rnd, int layer, int shapeId, int size,
                 float bright, float tint, float angle, float aspect) {
            float x = rnd.nextFloat() * Starfield.TILE;
            float y = rnd.nextFloat() * Starfield.TILE;
            float[] c = PALETTE[Pick(rnd)];
            int v = stars * VERTS, k;

            for (k = 0; k < VERTS; ++k) {
                int vi = v + k;
                Set3(pos, vi, x, y, layer);
                Set3(normals, vi, angle, aspect, 0.0f);
                corner[vi * 2] = k == 1 || k == 2 ? 1.0f : 0.0f;
                corner[vi * 2 + 1] = k >= 2 ? 1.0f : 0.0f;
                shape[vi * 2] = shapeId;
                shape[vi * 2 + 1] = size;
                colors[vi * 4] = 1.0f + (c[0] - 1.0f) * tint;
                colors[vi * 4 + 1] = 1.0f + (c[1] - 1.0f) * tint;
                colors[vi * 4 + 2] = 1.0f + (c[2] - 1.0f) * tint;
                colors[vi * 4 + 3] = bright;
            }

            k = stars * IDX;
            indices[k] = v;
            indices[k + 1] = v + 3;
            indices[k + 2] = v + 2;
            indices[k + 3] = v;
            indices[k + 4] = v + 2;
            indices[k + 5] = v + 1;
            ++stars;
        }

        private static void Set3(float[] a, int vi, float x, float y,
                                 float z) {
            a[vi * 3] = x;
            a[vi * 3 + 1] = y;
            a[vi * 3 + 2] = z;
        }

        private static int Pick(Random rnd) {
            float r = rnd.nextFloat();
            int i;

            for (i = 0; i < PALETTE_WEIGHT.length - 1; ++i) {
                if ((r -= PALETTE_WEIGHT[i]) < 0.0f) return i;
            }
            return PALETTE_WEIGHT.length - 1;
        }
    }
}
