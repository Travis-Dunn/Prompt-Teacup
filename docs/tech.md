Teacup explicitly targets the following platforms:

- The current version of Microsoft Windows 11
- All mainline desktop Windows iterations going back to XP
- Linux, at least the current version of Mint.

Post-release stretch goals. Worth pursueing if some level of success if achieved.

- MacOS, no idea what version, or what all is involved. But it's worth remembering that Escape Velocity was a Macintosh game, and was only later ported to Windows.
- Steam Deck, possbily other handhelds/consoles? This may make sense financially, but given that we are probably going to be binding half the keyboard, I don't know how this could work.
- Physical release. By far the most fantastical item on the list, but it's something I would personally love to do. That said, this isn't happening unless we absolutely knock it out of the park. Not even worth considering until revenue exceeds 10x current goals.
- DLC, specifically bringing mod support. Whitetail is built with mod support via a standard override directory in mind, and since I grew up in the modding community for various games, I would love to pay it forward. If the game is successful enough to warrant any DLC at all, it will have mod support tacked on as a value add.

Shipped game code will be exclusively Java. Dev tooling is probably going to be standardized, but that's not yet decided. 

All source code must be Java SE 7 compatible. This goes for language features, API surface, and dependencies.

Rendering will be via OpenGL 3.3 Core.

IDE - IntelliJ

Build system - Maven

Teacup will run on a bundled JRE

Yet to be decided - packaging, non-source code asset organization during development, and in the shipped game.

File formats and properties for all sound, 3d, image, script, etc. files. Most of these will probably be proprietary binary formats built with dev tooling, from interchange formats such as .glb, .ogg, .png, etc.