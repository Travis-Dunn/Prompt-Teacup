#version 330 core

layout(location = 0) in vec3 pos;
layout(location = 2) in vec2 texCoord1;

uniform mat4 matProj;
uniform mat4 matModel;

out vec2 uv;

void main() {
    uv = texCoord1;
    gl_Position = matProj * matModel * vec4(pos, 1.0);
}
