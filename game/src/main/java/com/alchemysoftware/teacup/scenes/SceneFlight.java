package com.alchemysoftware.teacup.scenes;

import com.alchemysoftware.teacup.scratch.Data;
import com.alchemysoftware.teacup.starfield.Starfield;
import com.alchemysoftware.whitetail.event.Event;
import com.alchemysoftware.whitetail.scene.Scene;

/**
 * Flying in the current system, Data.game.syst. Enter only once a game
 * exists.
 */
public final class SceneFlight extends Scene {
    @Override
    public void onInit() {

    }

    @Override
    public void onEnter() {
        Starfield.Build(Data.game.syst.sky);
        /* No player to follow yet, so the sky holds still. */
        Starfield.SetCamera(0.0, 0.0);
    }

    @Override
    public void onExit() {

    }

    @Override
    public void onUpdate(double v) {

    }

    @Override
    public void onRender() {
        Starfield.Draw();
    }

    @Override
    public void onInput() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public boolean handleEvent(Event event) {
        return false;
    }
}
