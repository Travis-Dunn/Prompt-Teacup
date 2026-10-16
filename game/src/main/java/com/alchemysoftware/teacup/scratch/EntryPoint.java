package com.alchemysoftware.teacup.scratch;

import com.alchemysoftware.whitetail.os.OsChecker;

public final class EntryPoint {
    public static void main(String args[]) {
        Data.platformStr = OsChecker.NAME;
        System.out.println("Preparing to start on " + Data.platformStr);

        if (!Engine.Init()) {
            System.err.println(ERR_STR_FAIL_INIT);
            return;
        }

        Engine.Run();
        Engine.Shutdown();
    }

    private static final String CLASS = "EntryPoint";
    private static final String ERR_STR_FAIL_INIT = CLASS +
            " failed to initialize.\n";
}