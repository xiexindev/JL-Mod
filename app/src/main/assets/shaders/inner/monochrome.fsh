/*
 * Emulate LCD backlight color for monochrome games
 *
 * Copyright 2023 Yury Kharchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform vec4 u_setting;
uniform vec2 u_texelDelta;
uniform vec2 u_pixelDelta;
uniform sampler2D sampler0;
varying vec2 v_texcoord0;

void main() {
  float luma = dot(texture2D(sampler0, v_texcoord0.xy).xyz, vec3(0.299, 0.587, 0.114));
  float luma2 = dot(texture2D(sampler0, v_texcoord0.xy - u_texelDelta * u_setting.w).xyz, vec3(0.299, 0.587, 0.114));
  if (luma >= 1.0 / 255.0) {
	luma = mix(luma, luma2, 0.3);
  }
  gl_FragColor.rgb = vec3(luma) * (u_setting.rgb / 255.0);
  gl_FragColor.a = 1.0;
}
