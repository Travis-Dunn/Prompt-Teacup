#version 330 core

/* The sky's background: a tint whose brightness is modulated by tileable
   low-frequency noise, sliding with the slowest parallax. Dithered, since
   dim gradients band badly in 8 bits. */

in vec2 px;

uniform sampler2D noise;
uniform vec2 origin;       /* framebuffer pixel of the screen center */
uniform float scale;       /* framebuffer pixels per virtual pixel */
uniform vec2 offset;       /* camera offset, wrapped to the noise period */
uniform vec3 color;
uniform float brightness;  /* 0 is black */
uniform float clumping;    /* 0 even, 1 patchy */

out vec4 fragColor;

const float PERIOD = 2048.0;  /* virtual pixels: 256 texels of 8 */

void main() {
    vec2 v = (px - origin) / scale + offset;
    float n = texture(noise, v / PERIOD).r;
    /* n*n averages about 0.28; squaring sharpens patches into wisps while
       keeping the average brightness the same at any clumping */
    float lum = brightness * mix(1.0, 3.6 * n * n, clumping);
    /* interleaved gradient noise: cheap, even, and free of the patterns a
       sin() hash shows on some GPUs */
    float d = fract(52.9829189 * fract(dot(floor(px),
                                           vec2(0.06711056, 0.00583715))))
              - 0.5;

    fragColor = vec4(color * lum + d / 255.0, 1.0);
}
