package com.pg85.otg.forge.util;

import com.pg85.otg.interfaces.IModLoadedChecker;
import com.pg85.otg.util.OTGModChecker;
import net.minecraftforge.fml.ModList;

public class ForgeModLoadedChecker implements IModLoadedChecker {
    @Override
    public boolean isModLoaded(String mod) {
        return ModList.get().isLoaded(mod);
    }

    public ForgeModLoadedChecker() {
        OTGModChecker.set(this);
    }
}
