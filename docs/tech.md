Teacup explicitly targets the following platforms:

- The current version of Microsoft Windows 11
- All mainline desktop Windows iterations going back to XP
- Linux, at least the current version of Mint.

Post-release stretch goals. Worth pursueing if some level of success if achieved.

- MacOS, no idea what version, or what all is involved. But it's worth remembering that Escape Velocity was a Macintosh game, and was only later ported to Windows.
- Steam Deck, possbily other handhelds/consoles? This may make sense financially, but given that we are probably going to be binding half the keyboard, I don't know how this could work.
- Physical release. By far the most fantastical item on the list, but it's something I would personally love to do. That said, this isn't happening unless we absolutely knock it out of the park. Not even worth considering until revenue exceeds 10x current goals.
- DLC, specifically bringing mod support. Whitetail is built with mod support via a standard override directory in mind, and since I grew up in the modding community for various games, I would love to pay it forward. If the game is successful enough to warrant any DLC at all, it will have mod support tacked on as a value add.

Performance is also important. There is one requirement - 40 fps on my thinkpad T420. A lot of things come into this, and it's easy to sort of cheat, but the bottom line is it needs to be at least close to this. Should be pretty easy. Obviously that's average fps, and minimum is as important, but I just want to keep the statement simple. Also, the T420 is a moving target. Right now it has the original dual core sandy bridge i5, and uses the integrated graphics. At some point, it will be upgraded to the discrete nvidia graphics and a faster CPU. It also already has 16gb of memory, and a good SSD, so it's not stock anyway. Either way, these changes are not that big a deal, 10 to 50 per cent for the upgrades I have in mind vs. if I benchmarked only on a contemporary college freshman laptop, which would be at least 600% faster. Also, the benchmarking will be done in linux mint, because the windows drivers for the iGPU only support up to GL 3.1, while Mesa supports 3.3. We'll still be supporting Windows XP - you just need a capable driver.

Shipped game code will be exclusively Java. Dev tooling is probably going to be standardized, but that's not yet decided. Web, python, and java are all on the table.

All source code must be Java SE 7 compatible. This goes for language features, API surface, and dependencies.

Rendering will be via OpenGL 3.3 Core.

IDE - IntelliJ

Build system - Maven

Teacup will run on a bundled JRE

Yet to be decided - packaging, non-source code asset organization during development, and in the shipped game.

File formats and properties for all sound, 3d, image, script, etc. files. Most of these will probably be proprietary binary formats built with dev tooling, from interchange formats such as .glb, .ogg, .png, etc. **Update** 29 oct 29: Ship meshes will go straight from blender to the proprietary binary format, since I can just write a python export script.

Text assets will probably be JSON.

