#version 330 core

/* A unit quad stretched over the whole framebuffer. */

layout(location = 0) in vec3 pos;

uniform vec2 fbSize;

out vec2 px;               /* framebuffer pixel, top-left origin */

void main() {
    px = pos.xy * fbSize;
    gl_Position = vec4(pos.x * 2.0 - 1.0, 1.0 - pos.y * 2.0, 0.0, 1.0);
}
