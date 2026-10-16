Teacup code must conform to standards, but these standards are not the same as those of Whitetail. They are in most cases more lenient.

1. Error handling in Whitetail is exclusively through ErrorHandler; Teacup may define it's own policy, and it must observe that policy. Whatever it is, it must discriminate between errors that must all be caught and addressed during development, and errors that the player may see, after the game is released.
