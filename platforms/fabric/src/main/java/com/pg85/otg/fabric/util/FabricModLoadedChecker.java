package com.pg85.otg.fabric.util;

import com.pg85.otg.interfaces.IModLoadedChecker;
import net.fabricmc.loader.api.FabricLoader;

public class FabricModLoadedChecker implements IModLoadedChecker {
    @Override
    public boolean isModLoaded(String mod) {
        return FabricLoader.getInstance().isModLoaded(mod);
    }
}
