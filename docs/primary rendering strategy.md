# Ship & Weapon Rendering Strategy

Oct 28, 2026 · @human

## Purpose and principles

The core of the ship renderer is one universal system. It's made of a forward shader, a shared material library, meshes that tag each face with a material and a part, and a small set of per-instance values. It draws ships, missiles and separable equipment such as turrets, chunks and the Bourdon's ejected cabin, along with running lights and glass. Shields, engine plumes, beams and most weapon effects are bespoke subsystems that are meant to look different from ship to ship. For those, this doc defines only the common contract they plug into.

**In scope:** the universal system, the contract for effects subsystems, the gameplay camera, the shipyard view, source renders for stills and blueprints, graphics settings, and a record of which OpenGL features the renderer uses.

**Out of scope:** UI, HUD, text, spobs and the starfield. This doc only assumes they draw into the same HDR target, before and after ships. The internal design of each effects subsystem is also out of scope. The sketches under Effects subsystems are starting points, not decisions.

**Taken as fixed:** Java SE 7, the lowest OpenGL version that gives us what we use (3.3 at most, see the OpenGL feature ledger), 60 fps on the benchmark machine, JSON text assets, and mod support through an override directory.

Principles, in priority order:

1. **Space is vast, silent and unforgiving.** Darkness is the default and light is scarce. Ship lighting stays restrained, so the warm, lived-in spobs read as a striking contrast and the over-the-top weapons and shields stand out against it.
2. **One source of truth per fact.** Shape lives in the .blend file, surface response in the material library, colour in the livery, and lore in the parts table. Nothing is copied by hand.
3. **Code over assets.** Anything that can be drawn procedurally is a shader, not a texture, so a shader improvement reaches every ship at once. Textures are only for what nothing else can do, such as logos, insignia and lettering.
4. **Readable at 150–200 px, good up close.** That's the player's ship at the default zoom at 1080p. Zoom follows the player's ship, so an NPC can appear much smaller or much larger than that, and models must hold up at both ends. The viewable area doesn't depend on resolution, so a ship is three times as many pixels wide at 4K as at 720p.
5. **Everything authored is data.** JSON and exported files can be hot-reloaded during development and overridden by mods without extra work.
6. **Anything that can be a setting is a setting.** Costs that scale, such as resolution, anti-aliasing, bloom and light counts, are exposed as graphics settings from the start.
7. **Nothing rendered without a defined purpose** (aesthetic.md). The purpose can be gameplay, lore or readability, but it has to be nameable.

## Pipeline overview

Geometry lives only in the .blend file, and numbers live only in the JSON files. Everything from the loader down is shared by all universal models. Effects subsystems sit beside the ship shader and draw into the same frame.

&#91;embedded content: asset pipeline · authored files, 2 tools, 1 loader, the frame\]

The exporter is the only bridge from Blender to the game. The sync add-on runs the other way, so the Blender viewport and Blender stills use the same library as the game.

## Authoring in Blender

Each ship is one .blend file of named objects. Objects are the parts, Blender material slots name entries in the material library, and empties mark sockets. This takes the place of palette UVs. A material slot is already a per-face ID, Blender displays it natively, and no face needs UVs just to choose a material. At runtime the material table is still the small palette-style lookup it was always meant to be.

**Units and axes.** One unit is one metre. In Blender the nose points to +Y and the top to +Z. The exporter converts to the engine's GL-standard RUB axes by mapping (x, y, z) to (x, z, −y), which puts the nose at −Z and the top at +Y. That mapping is a pure rotation, so winding order and normals need no further fixing. The object origin is the ship's pivot.

**Objects are parts.** An object's name is its part key, such as `hull`, `sphere`, `strut`, `shield_gen`, `engine_top` or `turret`. Anything after the first dot is ignored, so `strut.001` to `strut.003` all belong to part `strut`. The damage model, the blueprint labels and the lore table all refer to parts. A ship can have up to 32.

**Hierarchy is articulation.** Fixed parts are children of the root. A part that moves gets custom properties, and its children move with it. Its origin is the pivot.

| Property | Values | Bourdon example |
| --- | --- | --- |
| `mode` | `rotate` or `translate` | Door assembly: `rotate` |
| `axis` | `x`, `y` or `z` (local) | `z`, a hinge on the cap's edge |
| `min`, `max` | degrees or metres | 0 to 100 |
| `signal` | a signal name (Per-instance data) | `door` |

The sphere is its own node under the door assembly, so it can detach (Chunks and detachable parts). The turret is two nodes, driven by `turret_yaw` and `turret_pitch`, and gimballed engines work the same way. Signal-driven articulation is the only motion the universal system has. There are no keyframes and no skeletal animation.

**Materials.** Each Blender material's name must match a library key exactly, for example `paint_primary`, `aluminum_bare` or `rubber_black`. An unknown name stops the export. The sync add-on (Shipyard, stills and blueprints) creates these materials in Blender from the library, so the viewport already looks like the game.

**UVs, for decals only.** Faces that carry a logo, insignia or lettering use a decal material (Material library) and are unwrapped over the decal image. Every other face can stay unwrapped, and the exporter writes zero UVs for it.

**Normals.** Use Shade Smooth plus sharp edges, or Smooth by Angle. The exporter writes normals as Blender evaluates them. Curved surfaces like the sphere and cylinder should be smooth, since faceted curves cause most specular flicker on small ships.

**Modifiers stay live.** The exporter reads the evaluated mesh, so bevels, mirrors and arrays never need to be applied by hand.

**Collections.**

- `render` holds the one-piece model.
- `chunks` holds the optional chunked version (Chunks and detachable parts).
- `collide` holds collider meshes, named after their damage zones.
- `shield` holds whatever shield geometry a ship's shield subsystem wants, such as an ellipsoid collider or a shell.

**Optional parts.** Bolt-ons and variant parts are modelled in the same file and tagged `optional=true`. Variants choose which ones appear (Per-instance data).

**Sockets** are empties, and the name prefix gives the type. The empty's +Y axis is the socket's direction.

| Prefix | Example | Purpose |
| --- | --- | --- |
| `gun.` | `gun.turret` | Weapon exit point and firing direction. Muzzle-flash lights spawn here. |
| `glow.` | `glow.nav_port` | Running light or other glow sprite. The `glow` property names a library glow. |
| `exhaust.` | `exhaust.top` | Origin and direction for an engine glow or a bespoke plume subsystem |
| `spark.` | `spark.hull` | Optional emitter for damage sparks on a part. Without one, the part's centroid is used. |

## Export tool and runtime format

A Python script running inside Blender writes the game's model file directly, with no .glb in between. This differs from the .glb-to-proprietary plan in tech.md. Blender's glTF exporter handles per-face integer data, object custom properties and empties unevenly, and its axis conversion isn't one we chose. Blender's own Python API exposes all of these as they are, so the exporter is a few hundred lines with no intermediate format to debug. glTF is still available for other kinds of assets.

It runs in two ways:

- **Headless, for builds:** `blender -b bourdon.blend --python export_ship.py -- assets/ships/`
- **On save, while working:** a `save_post` handler exports every time the .blend is saved, and the game, running with hot reload, picks up the change within a second.

**Files per ship.** Generated files are never edited by hand, and authored files are never written by tools.

| File | Written by | Contents |
| --- | --- | --- |
| `bourdon.model` | Exporter | Node tree with transforms and articulations, the one-piece mesh, the chunk meshes, local material and part name lists, sockets, colliders, shield geometry |
| `bourdon.ship.json` | You | Parts table (damage zone, blueprint label, lore line), liveries, variants, chunk rules |
| `materials.json` | You | The shared material and glow library |
| `decals/*.png` | You | Logos, insignia and lettering, all the same size |
| `effects/*.json` | You | One file per effects subsystem, in whatever shape that subsystem defines |

**Materials and parts are per face.** In the file, each triangle stores one material index and one part index, and vertices store only position, normal and UV. Before GL 3.2 there's no way to give the GPU per-face data directly, so the loader passes them as `flat` integer vertex attributes. `flat` means no interpolation at all: the whole triangle takes the value from one vertex, so the result is per-face in practice. The cost is that the loader duplicates a vertex wherever faces with different materials or parts meet. The copies share position and normal, so smooth shading is unaffected. A true per-face lookup avoids the duplicates, but it needs GL 3.2 and isn't worth it unless vertex counts become a problem (OpenGL feature ledger).

**GPU vertex format, 24 bytes**

| Attribute | Type | Bytes |
| --- | --- | --- |
| Position | 3 × float32 | 12 |
| Normal | 3 × int8, normalized, plus 1 pad | 4 |
| UV | 2 × uint16, normalized (decals only, zero elsewhere) | 4 |
| Material | uint8, flat integer attribute | 1 |
| Part | uint8, flat integer attribute | 1 |
| Padding | — | 2 |

Indices are uint16. Each model gets one VBO, one IBO and one VAO, with one index range per node and per chunk.

**Loading.** The material indices in the file point into the model's own name list. While expanding triangles, the loader swaps them for global library IDs, so library order can change and mods can add materials without anything being re-exported. A name the library doesn't know fails the load with an error naming the ship and the missing name, so a typo shows up in seconds instead of as a pink hull.

## Material library

All ships share one `materials.json` of about fifteen entries, and a new ship usually adds one to three. At load time the library becomes an RGBA32F texture with five texels per material and one row per material, which the shader reads with `texelFetch`. This is the palette-texture idea built from data, with room for HDR values. A UBO would also work, but a texture avoids std140 layout rules and size limits.

| Field | Type | Meaning | Texel |
| --- | --- | --- | --- |
| `color` | sRGB hex | Base colour, converted to linear at load. Ignored for paint. | 0.rgb |
| `roughness` | 0–1 | Microsurface roughness | 0.a |
| `emit` | sRGB hex × `intensity` | HDR emission | 1.rgb |
| `signal` | Signal name | Scales emission. Absent means constant. | 1.a |
| `metal` | Bool | Metal tints its reflections and has no diffuse | 2.r |
| `paint` | `primary`, `secondary`, `stripe`, `trim` | Takes its colour from the instance livery | 2.g |
| `wear` | 0–1 | Weathering susceptibility, if weathering is used | 2.b |
| `decal` | Image name, or `instance` | RGBA image composited over the material (below) | 2.a |
| `coat` | sRGB hex | Mirror-coating reflection colour facing the viewer | 3.rgb |
| `coatEdge` | sRGB hex | Mirror-coating reflection colour at grazing angles | 4.rgb |
| `base` | Material key | Copies every field from another entry, then overrides | — |

**Decals.** A material with `decal` composites its image over itself using the UVs, and where the image's alpha is 0 the face looks like the plain material. A CGF logo on the painted hull is `{ "base": "paint_primary", "decal": "cgf_logo" }`. All decal images live in one texture array, mipmapped so they stay clean when a ship is small. `"decal": "instance"` takes the image from the instance instead, so one model can carry each faction's insignia. If logos should follow the livery, a later `decalTint` field could tint a white mask by a paint slot.

**Glass** is opaque and very shiny, like cheap polarized sunglasses. The body is near black. The `coat` colour sets a strong, coloured mirror reflection even when looked at head-on, and the hue shifts toward `coatEdge` at the rim. The analytic environment, the key light and muzzle-flash point lights give it something to reflect. Glass is universal: every ship uses this material type, tuned per entry.

```json
{
  "materials": {
    "paint_primary": { "paint": "primary", "roughness": 0.35, "wear": 0.6 },
    "aluminum_bare": { "color": "#C8CCD0", "metal": true, "roughness": 0.4 },
    "rubber_black":  { "color": "#0B0B0C", "roughness": 0.8 },
    "canopy_glass":  { "color": "#050608", "roughness": 0.05,
                       "coat": "#3E6F7A", "coatEdge": "#C9A64B" },
    "decal_cgf":     { "base": "paint_primary", "decal": "cgf_logo" },
    "thruster_grid": { "color": "#151820", "roughness": 0.6,
                       "emit": "#9FA8FF", "intensity": 4.0, "signal": "throttle" }
  },
  "glows": {
    "nav_red":     { "color": "#FF2A1A", "intensity": 6,  "size": 0.25, "minPx": 2.5, "signal": "nav" },
    "strobe":      { "color": "#FFFFFF", "intensity": 12, "size": 0.3,  "minPx": 3,   "signal": "strobe" },
    "exhaust_cgf": { "color": "#9FA8FF", "intensity": 3,  "size": 1.2,  "minPx": 4,   "signal": "throttle" }
  }
}
```

**Starter library**

| Key | On the Bourdon | Notes |
| --- | --- | --- |
| `paint_primary` … `paint_trim` | Hull, sphere, stripe, trim | Four entries, one per livery slot. Dielectric, roughness about 0.35. |
| `aluminum_bare` | Struts | Metal, roughness 0.4 |
| `rubber_black` | Shield generator | Black in every livery, like a real bumper. Albedo about 0.04, not 0. |
| `canopy_glass` | Canopy | Coated glass, as above |
| `cabin_glow` | Just inside the canopy | Faint warm emission on `cabin` |
| `machinery_dark` | Engine housings, turret base | Dark metal |
| `tank_steel` | Mercury spheres | Metal, lighter than machinery |
| `mli_gold` | Life support pack | Thermal blanket on equipment outside the pressure hull |
| `thruster_grid` | Engine faces | Pale blue-violet emission on `throttle` |
| `decal_cgf` | Flanks of the cargo cylinder | CGF logo over the primary paint |
| `primer_grey` | Aftermarket bolt-ons | Mismatched parts that tell the upgrade story for free |
| `chunk_interior` | Cut faces of chunks | Dark and rough, with emission on `heat` so fresh breaks glow, then cool |

**Glows** are the second half of the file and are universal too. A glow is a camera-facing sprite at a `glow.` or `exhaust.` socket, with a physical size in metres and a minimum size in pixels, so a 25 cm nav light still reads on a distant ship. Red on port and green on starboard show which way a small ship is facing. An engine that only needs a glow uses one, while an engine with a plume gets its own effects subsystem at the same socket.

Not in the library, by design: normal maps, textures other than decals, anisotropy, subsurface scattering and real transparency.

## Per-instance data

Anything that can differ between two Bourdons on the same screen is per-instance data: the livery, the insignia, sixteen named signals, damage per part, and age. Gameplay code fills these values every frame, and the renderer never makes gameplay decisions.

| Uniform | Size | Set by |
| --- | --- | --- |
| `uModel` | mat4 | Physics: position, heading and bank (colliders use the same transform) |
| `uNode` | mat4 per node | Articulation, from signals |
| `uLivery` | 4 × vec3 | The ship's livery: primary, secondary, stripe, trim |
| `uInsignia` | int | Decal layer for `"decal": "instance"` materials |
| `uSignal` | 16 floats | Ship systems, every frame |
| `uDamage` | 32 floats, one per part | Damage model, 0 = intact, 1 = destroyed |
| `uAge` | float | The ship record, if weathering is used |

**Signals** are the one mechanism behind emission, glows, moving parts and effects subsystems. A material, glow, articulation or subsystem names a signal, the loader resolves each name to an index once, and gameplay writes plain floats.

| Signal | Range | Drives |
| --- | --- | --- |
| `throttle` | 0–1 | Thruster grid emission, exhaust glow, any plume subsystem |
| `nav` | 0 or 1 | Red and green running lights, off when running dark |
| `strobe` | 0–1 | White strobe, with the blink pattern computed on the CPU and a per-ship phase offset |
| `cabin` | 0–1 | Cabin glow |
| `charge` | 0–1 | Weapon charge glow on turrets and emitters |
| `door` | 0–1 | Cargo door assembly, in the shipyard and while docking |
| `turret_yaw`, `turret_pitch` | Degrees | Turret articulation |
| `heat` | 1, decaying to 0 | Glow on fresh chunk cut faces |

That leaves seven spare slots. Signal names are per ship type, so a later ship can add `gimbal`, `radiator`, or whatever its shield subsystem needs, without touching the renderer.

**Liveries and variants** live in the ship file. A livery is four colours. A variant is a livery, an optional insignia, and a list of optional parts to show, so each minor variant costs one JSON block.

```json
{
  "liveries": {
    "stock":  { "primary": "#F2B705", "secondary": "#F2B705", "stripe": "#111111", "trim": "#2B2B2B" },
    "postal": { "primary": "#E9E4D8", "secondary": "#1F3A93", "stripe": "#C8102E", "trim": "#2B2B2B" }
  },
  "variants": {
    "bourdon":        { "livery": "stock" },
    "bourdon_long":   { "livery": "stock", "show": ["aux_tank"] },
    "bourdon_pirate": { "livery": "pirate_red", "insignia": "skull", "show": ["gun_pod", "armor_plate"] }
  }
}
```

To leave a slot out, a livery sets it equal to another slot. A player repaint is just a livery stored in the save game.

## Chunks and detachable parts

A ship can be drawn in one of two ways. It uses its one-piece mesh while it's intact, and switches to its chunk set once anything has come off. Both go through the same shader with the same instance data, so the switch can't be seen as long as the chunks line up with the one-piece mesh. Ships without chunks only ever use the one-piece mesh.

**Authoring.** The `chunks` collection holds objects that together make up the whole ship, cut along the lines it should break on. Cut faces use `chunk_interior`. Chunks don't need to match damage zones one-for-one. Which chunk comes off, and when, is gameplay data in the ship file. Chunks need no manual part tagging, because the exporter copies each chunk face's part from the nearest face of the one-piece mesh (a BVH lookup in Blender's `mathutils`).

**Articulated nodes are shared.** Nodes such as the door assembly, turret and sphere are separate nodes in both versions. Chunks replace only the static geometry under the root. Each articulated node rides on the chunk nearest its pivot, unless it names one with a `chunk` property.

**Detaching** is one operation for every case. It takes a chunk or an articulated node together with its children and makes it a new entity:

1. The new entity starts at the node's current world transform, with the parent's velocity plus an impulse.
2. It gets a copy of the parent's instance data: livery, insignia, signals and damage.
3. If the parent was still drawn as one piece, the parent switches to its chunk set.
4. `heat` is set to 1 on both, so the fresh breaks glow and then cool.

**The Bourdon's sphere** is the first real case. A manual ejection and a lucky shot to the struts both call detach on the sphere node, which is why they look the same. The struts stay with the hull, as the brief says. The detached cabin keeps its own signals, so the cabin glow stays on and the strobe keeps blinking. That reads as someone still being inside.

**Cost.** Chunk mode is one draw per chunk instead of one for the hull, and only for ships that have broken. Debris that has drifted off screen or outlived a time limit is removed, and that limit is a graphics setting.

## Shading and lighting

Shading and lighting are one universal system with a single forward shader. Everything that makes a syst feel like itself comes in through uniforms from a per-syst lighting profile. The shader combines Lambert diffuse and GGX specular from one key light, up to a dozen small point lights, a two-colour ambient, an analytic environment for reflections, a rim term and emission. All of it is linear HDR, tonemapped at the end.

**Syst lighting profile**

| Field | Meaning |
| --- | --- |
| `keyColor`, `keyIntensity` | The star |
| `keyElevation` | Degrees above the gameplay plane. The horizontal direction always points from the star to the ship, so lighting shifts as you fly through a syst. |
| `keySize` | The star's apparent size, which widens the specular highlight |
| `ambientUp`, `ambientDown` | Hemisphere fill colours |
| `envSky`, `envBand` | What metal and glass reflect |
| `rim` | Rim colour and strength |
| `exposure`, `bloom` | Tonemap exposure and bloom strength |

These example systs show how much range the profile covers:

| Syst | Character |
| --- | --- |
| Clean, empty space | Small bright key, near-black ambient and environment, hard contrast. The baseline for vast and unforgiving. |
| White dwarf | Tiny, harsh, blue-white key with pinpoint highlights. Almost no fill. |
| Red giant | Large, soft orange key with broad highlights. Warm ambient. |
| Gas cloud | Dim, diffuse key, strongly coloured ambient and environment, low contrast, more bloom |

**Terms**

- **Environment.** Without something to reflect, metal and glass in space look black. The reflection vector reads a function of the profile colours instead of a cubemap: dark sky, a soft brighter band, and a lobe toward the star. No asset is needed.
- **Point lights.** Muzzle flashes, impacts and explosions light the hull. Any subsystem can add a light with a position, colour, radius and lifetime. Each frame the CPU gives each ship the lights whose radius reaches its bounding sphere, up to the limit set in graphics settings (0, 4, 8 or 12). Each light falls smoothly to zero at its radius, so the cost stays local.
- **Specular.** Metal reflects its own colour. Coated glass reflects `coat` head-on, shifting toward `coatEdge` at the rim. Everything else reflects at 4%, rising to white at grazing angles.
- **Specular anti-aliasing.** Roughness is widened by the screen-space change in the normal. This is the main fix for flat faces flashing as a ship turns, and it matters at both ends of the zoom range.
- **Rim.** A faint Fresnel term that separates dark materials such as rubber from black space. Its strength is per preset.
- **Emission.** The material's `emit` times its signal. Damaged parts can flicker it.
- **Damage and weathering.** Both are cheap to build, so they're in. Object-space value noise, multiplied by `uDamage[part]` or by `wear` × age, darkens and roughens the surface. The noise is 3D and in object space, so it needs no UVs and stays fixed to the hull. A per-preset strength of 0 turns either one off until we decide whether it makes the game better.
- **Shadows.** None in gameplay. The shipyard and still renders get one shadow map.
- **Tonemap.** Render into RGBA16F, apply a filmic curve with the profile's exposure in the final pass, then write sRGB.

```glsl
#version 140   // GLSL for GL 3.1, see OpenGL feature ledger
flat in int vMat; flat in int vPart;
in vec3 vN; in vec3 vPosW; in vec3 vPosObj; in vec2 vUV;
uniform sampler2D      uMaterials;          // RGBA32F, 5 texels x N materials
uniform sampler2DArray uDecals;
uniform vec3  uLivery[4];  uniform int uInsignia;
uniform float uSignal[16]; uniform float uDamage[32];
uniform vec4  uLightPos[12], uLightColor[12]; uniform int uLightCount;   // pos.w = radius
out vec4 fragColor;

vec4 mat(int t) { return texelFetch(uMaterials, ivec2(t, vMat), 0); }

void main() {
  vec4 m0 = mat(0), m1 = mat(1), m2 = mat(2), m3 = mat(3), m4 = mat(4);
  vec3 base = m2.g > 0.5 ? uLivery[int(m2.g) - 1] : m0.rgb;

  // decal: 2.a = layer, -1 none, -2 instance insignia; sample outside the branch
  float layer = m2.a < -1.5 ? float(uInsignia) : m2.a;
  vec4 d = texture(uDecals, vec3(vUV, max(layer, 0.0)));
  if (layer >= 0.0) base = mix(base, d.rgb, d.a);

  // specular anti-aliasing
  vec3 N = normalize(vN), du = dFdx(N), dv = dFdy(N);
  float rough = sqrt(clamp(m0.a * m0.a + min(0.5 * (dot(du, du) + dot(dv, dv)), 0.18), 0.0, 1.0));

  // reflectance at normal incidence and at grazing angles
  bool coated = dot(m3.rgb, vec3(1.0)) > 0.0;
  vec3 F0  = m2.r > 0.5 ? base : (coated ? m3.rgb : vec3(0.04));
  vec3 F90 = coated ? m4.rgb : vec3(1.0);

  // + key light, point lights (loop to uLightCount), ambient, environment, rim
  vec3 emit = m1.rgb * (m1.a < 0.0 ? 1.0 : uSignal[int(m1.a)]);
}
```

## Gameplay view

Gameplay uses a perspective camera tilted back from straight down, so a cube shows its top and a little of the side facing the camera. Zoom follows the player's ship, putting it at about 150–200 px at 1080p whatever its size. The camera's tilt, field of view and height are uniforms.

- **Size range.** The smallest on-screen ship is a small NPC seen from the largest player ship. The largest is a big NPC seen from the Bourdon, which can fill much of the screen. The brief's 2 px rule for struts applies at the small end. Glow minimum sizes, mipmapped decals and specular anti-aliasing cover both ends.
- **Mapping screen and plane.** Picking and HUD anchors go through the camera matrices, which is a few lines of math on the engine side.
- **Depth slices.** Ships in the 2D game can overlap. Their geometry must not intersect, but in perspective, moving a ship toward the camera would change its size and position. Instead, each ship gets its own slice of the depth buffer through `glDepthRange`. That remaps only the depth values, so nothing moves on screen. Ships are sorted each frame and the front-most gets the nearest slice. A 24-bit depth buffer split 256 ways still leaves 65,536 levels per slice, which is plenty when the near and far planes are fitted closely around the play plane. Each ship depth-tests only against itself, and one ship cleanly covers another.
- **Banking.** Roll of up to about 20°, proportional to turn rate. Colliders get the same transform. It shows that the ships are 3D models, and highlights slide across the hull during a turn.
- **Readability.** Dark materials don't go below about 0.04 albedo, the rim term outlines ships against space, and the gameplay preset keeps specular calmer than the showcase preset.

**Frame order**

1. Starfield and spobs (another system) draw into the RGBA16F MSAA target.
2. Universal geometry: ships, chunks, missiles, separable equipment and debris, each in its own depth slice.
3. Universal glows: running lights and simple engine glows. Additive, depth test on, depth write off, in their ship's slice, so a glow behind the hull stays hidden.
4. Effects subsystems, each in a slot it declares: inside its ship's slice for attached effects such as shields and plumes, or free above everything for bolts, beams and explosions.
5. Resolve the MSAA.
6. Bloom: threshold, then a downsample chain from half resolution, blur, and add back. This is what makes the neon, VFD and laser look work.
7. Tonemap and convert to sRGB in the backbuffer.
8. UI (another system).

**Performance budget.** Ships are cheap. The cost is in full-screen work, and because the view doesn't depend on resolution, that work scales with it: a 4K frame has nine times the pixels of a 720p one. The MSAA level, bloom resolution and render scale are therefore settings, and steps 5–7 get profiled on the benchmark machine before anything else. Draw calls run about one per node per ship, a few hundred in a big battle, which is fine for GL. Add instancing only if a profile asks for it.

## Effects subsystems

Shields, engine plumes, beams and most weapon effects are bespoke on purpose. There may be about one shield system for every two distinct ships. Many narrow systems stay affordable if they share a contract and a small, optional toolkit, so this section fixes only those two things. Everything under Tentative sketches is a starting point.

**The contract.** Every subsystem receives:

| Input | From |
| --- | --- |
| Ship and node transforms, bounds, depth slice | Universal system |
| Sockets, shield geometry, the ship's own mesh | Model file, via the loader |
| Signals | Ship instance |
| Hit events: point, normal, part, projectile type | Gameplay |
| Point-light API | Lighting |
| Quality tier | Graphics settings |
| Its own JSON | `effects/<name>.json` |

In return, every subsystem draws into the HDR target in the frame slot it declares, attached (inside its ship's depth slice) or free (above everything). It also offers a low tier and an off tier, and reads all its numbers from JSON so they can be tuned and hot-reloaded.

**The toolkit**, which subsystems can use or ignore:

- **Sprite batch.** Camera-facing quads, which can be stretched along velocity.
- **Ribbon batch.** Quad strips along a polyline, with width and colour varying along the length.
- **Mesh pass.** Draws a ship's own geometry again with a custom shader, for example inflated along its normals.
- **Shared GLSL.** Noise functions and Fresnel.
- **HDR plus bloom.** This is what makes over-the-top cheap, since intensity alone produces glare.

Sprites and ribbons from all subsystems go into one streaming vertex buffer per frame, so many small systems don't mean many draw calls.

**Tentative sketches**

- **Shield, projected plane.** An ellipsoid collider that isn't drawn until something hits it. On a hit, a small flat disc of energy appears at the impact point, facing the projectile, and joins the ship's array of active planes, fading over about 500 ms. The explosion, the sound and the projectile's removal all happen at that moment.
- **Shield, conformal.** No separate collider. The ship's mesh is drawn again slightly outside the hull, faint or invisible at rest. A hit flashes it, and a shockwave ring travels across the surface from the hit point, using object-space distance from the hit. One practical point: inflate along the normals rather than scaling by 3%. Scaling about the pivot moves the Bourdon's nose out about 18 cm but barely moves anything near the middle, so those parts would poke through. Inflating needs normals without hard-edge splits, so for ships that use this, the exporter can write an extra per-position averaged "shell normal".
- **Engine plumes.** One subsystem per engine design, at the `exhaust.` sockets and driven by `throttle`. The Bourdon needs none, since its brief calls for a faint glow and no plume.
- **Blasters.** A stretched HDR sprite with core and glow colours, a muzzle-flash sprite with a point light, and an impact flare with sparks.
- **Beams.** A ribbon from the `gun.` socket to the hit point. Tesla-style arcs are jagged polylines regenerated every few frames. A point light sits at the hit.
- **Missiles.** Universal models, plus an exhaust glow and a trail ribbon.
- **Kinetic weapons** such as railguns, the weakest visually. If kept, they get a thin bright tracer, a strong muzzle flash with a light, and sparks and a flash on impact, possibly with a faint ionised wake. With the toolkit this is cheap either way, so the keep or cut-to-DLC decision can rest on gameplay and scope.
- **Explosions.** A flash sprite, a point light, particles, and detaching chunks.

## Shipyard, stills and blueprints

The shipyard turntable runs live in the engine with a showcase preset. Stills come from Blender, using materials generated from the same library, and blueprints come from an orthographic engine render plus the parts table. None of the three needs a separate material setup.

- **Shipyard view.** The same shader, with a showcase preset: a long-lens perspective camera, a showcase lighting profile (key, fill and rim), stronger specular, one shadow map and a slow turntable. Articulations demo themselves. The Bourdon's door assembly swings open like a Super Guppy's nose, and the turret tracks. Animating the `door` signal is the whole shipyard animation.
- **Blender sync add-on.** It reads `materials.json`, the decal images and a chosen livery, then creates or updates a Principled BSDF material for each library key, including coated glass and emission. Authoring needs this anyway so the viewport matches the game, and it also makes Cycles or EEVEE the renderer for stills, marketing shots, and reference renders for "Artist's impression" images.
- **Blueprints.** Running the game with `--blueprint bourdon` renders orthographic top, side and front views into normal, depth and part-ID buffers. Edge detection on those buffers gives clean linework for silhouettes, creases and part boundaries. Each part's centroid becomes a label anchor, written as JSON next to the PNGs. Labels and lore lines come from the parts table and are drawn by the game's own text renderer, never by an image model, which garbles text. Any AI styling of the line plates is optional and sits underneath the labels.

The parts table is one more block in the ship file:

```json
"parts": {
  "sphere":     { "zone": "cockpit", "label": "Crew sphere",      "lore": "2.8 m pressure vessel. Detaches as a lifeboat." },
  "hg_tank":    { "zone": "engines", "label": "Mercury tank",     "lore": "Propellant for the gridded thrusters." },
  "shield_gen": { "zone": "shield",  "label": "Shield generator", "lore": "Covers the sides and rear only. Cost, 50 years ago." }
}
```

## Graphics settings

Every cost that scales is a setting from the start. Each one is a uniform, a loop count, a texture size or a pass toggle, so changing a setting never needs a shader rebuild. The default preset is whatever holds 60 fps on the benchmark machine.

| Setting | Options | Affects |
| --- | --- | --- |
| Render scale | 50–100% | Every scene pass. UI stays at native resolution. |
| Anti-aliasing | Off, 2×, 4×, 8× MSAA | Scene target and resolve |
| Bloom | Off, low, high | Number and resolution of blur levels |
| Point lights per ship | 0, 4, 8, 12 | Loop count in the ship shader |
| Decal resolution | Full, half, quarter | Top mip levels skipped at load |
| Damage and weathering detail | Off, on | Noise term in the ship shader |
| Effects quality | Each subsystem's tiers | Whatever each subsystem defines |
| Debris lifetime | Short, long | How long detached chunks stay |
| Shipyard shadows | Off, on | The showcase shadow map |

Specular anti-aliasing and glow minimum sizes aren't settings. They're part of what makes ships readable, so they're always on.

## OpenGL feature ledger

Everything in this doc fits in GL 3.1 with GLSL 1.40, so that's the version to request until a bump buys something specific. Add a row here whenever the renderer starts using a new feature.

**In use**

| Feature | Core since | Used for |
| --- | --- | --- |
| VAOs, VBOs, `glMapBufferRange` | 3.0 | All geometry, streaming sprite and ribbon buffers |
| Integer vertex attributes, `flat`, `texelFetch` | 3.0 | Per-face material and part IDs, material table |
| RGBA32F and RGBA16F textures and render targets | 3.0 | Material table, HDR scene target |
| 2D texture arrays | 3.0 | Decals and insignia |
| FBOs, multisample renderbuffers, `glBlitFramebuffer` | 3.0 | MSAA scene and resolve |
| sRGB framebuffer | 3.0 | Final output |
| `glDepthRange`, `dFdx`/`dFdy` | 1.x | Depth slices, specular anti-aliasing |

**Candidates for a bump**

| Feature | Core since | What it would buy | Verdict |
| --- | --- | --- | --- |
| Multisample textures | 3.2 | A custom resolve that tonemaps each sample before averaging. Very bright emissive edges such as lights, neon and plumes then antialias properly instead of stair-stepping, which a plain HDR resolve can't fix. | The strongest case. Bump to 3.2 if bright edges look jagged, which is likely given the neon look. 3.2 also brings a real core profile. |
| `gl_PrimitiveID` in fragment shaders | 3.2 | True per-face lookup with no vertex duplication | Not needed |
| Geometry shaders | 3.2 | Expanding sprites and ribbons on the GPU | Not needed, since CPU batching is simpler |
| Fence sync | 3.2 | Streaming buffers without stalls | Buffer orphaning works without it |
| Instanced arrays | 3.3 | Cheap instancing for swarms of identical missiles or particles | Only if a profile asks for it |
| Timer queries | 3.3 | GPU time per pass | Development only. Use the extension where it exists. |
| Packed 10-bit normals, explicit attribute locations, sampler objects | 3.3 | Convenience | Not needed |

## Per-ship checklist and build order

Once the pipeline exists, a ship's universal side costs its modelling plus a short list of data tasks, with no texture painting and no per-ship shader. Its bespoke effects, such as the shield and any plume, are where the per-ship engineering goes. A variant costs one JSON block.

**Per ship**

- [ ] Model the parts as named objects in `render`, smoothing curved surfaces
- [ ] Assign library materials by slot, adding a library entry only when nothing fits
- [ ] Unwrap only the decal faces, and add any new decal images
- [ ] Place `gun.`, `glow.`, `exhaust.` and any `spark.` sockets
- [ ] Set articulation properties on moving parts
- [ ] Cut the chunk set in `chunks`, if the ship breaks up
- [ ] Model the colliders (`collide`) and whatever shield geometry its shield subsystem uses (`shield`)
- [ ] Write the ship file: parts table, stock livery, variants, chunk rules
- [ ] Build or reuse its shield subsystem, and its plume subsystem if it has one
- [ ] Export, then check at the smallest and largest on-screen sizes and in the showcase preset
- [ ] Render stills in Blender and generate the blueprint plates

**Engine build order.** Each step leaves something visible:

1. Exporter and loader, with flat base colours. The Bourdon appears on screen.
2. Material texture, lighting shader, syst lighting profiles, liveries.
3. Gameplay camera, depth slices, MSAA, HDR and tonemap. Profile on the benchmark machine here.
4. Signals: emission, glow sprites, articulations.
5. Decals and insignia.
6. The effects contract and toolkit, bloom, point lights, then the first blaster.
7. Chunks and detachment, starting with the Bourdon's sphere.
8. The Bourdon's shield subsystem.
9. Showcase preset with shadow map, the Blender sync add-on, blueprint mode.
10. Damage and weathering noise, built cheaply and switched off until judged.

## Open questions and deferred features

The hardware baseline is still open, so the renderer asks for the lowest GL version it actually uses (OpenGL feature ledger). At 3.1 it runs on the T420's integrated HD 3000 on Windows, which Intel caps at GL 3.1 ([Intel forum](https://forums.intel.com/t5/Graphics/Intel-HD-3000-will-intel-provide-a-update-to-support-opengl-3-3/m-p/452037/highlight/true)), and on Linux, where Mesa reached 3.2 on that chip in 2014 ([Phoronix](https://www.phoronix.com/news/MTc5MTg)). A bump to 3.2 would rule out the first of those.

- [ ] Choose the benchmark configuration and the XP machine. That decides whether a bump to 3.2 is free.
- [ ] Decide the depth-slice order: does the player's ship always draw on top, or is everything sorted by distance from the camera?
- [ ] Set the camera tilt, field of view, and zoom-to-ship-size rule.
- [ ] Decide whether kinetic weapons stay in the game or move to the DLC list.
- [ ] Decide whether insignia and logos follow the livery (`decalTint`).
- [ ] Define chunk rules: which damage detaches which chunk.
- [ ] Decide whether mods can add materials, glows and decals. The loader's name remapping already allows it.

**Built, decision deferred:** damage and weathering noise.

**Later, if a profile asks:** instancing, a custom HDR resolve (GL 3.2).

**Ruled out:** normal maps, textures other than decals, real transparency, shadows during gameplay, and keyframed or skeletal animation.
