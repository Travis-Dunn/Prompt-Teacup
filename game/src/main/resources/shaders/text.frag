#version 330 core

in vec2 uv;

uniform sampler2D tex;
uniform vec4 color;

out vec4 fragColor;

/* Coverage is blended in gamma space, which makes light text on a dark
   background look thinner than it was rasterized. An exponent below 1
   restores stem weight; 1.0 disables the correction. Tune by eye. */
const float COVERAGE_EXP = 0.8;

void main() {
    float coverage = pow(texture(tex, uv).r, COVERAGE_EXP);
    fragColor = vec4(color.rgb, color.a * coverage);
}
