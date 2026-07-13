package com.pg85.otg.forge;

import com.pg85.otg.OTGEngine;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.forge.util.ForgeModLoadedChecker;
import com.pg85.otg.shared.biome.SharedLegacyBiomeLoader;
import com.pg85.otg.shared.materials.SharedMaterials;
import com.pg85.otg.util.OTGLog;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.ModList;

import java.io.File;

public class ForgeEngine extends OTGEngine {
    protected ForgeEngine() {
        super(
                OTGLog.getLogger(),
                FMLPaths.CONFIGDIR.get().resolve(Constants.MOD_ID),
                new ForgeModLoadedChecker(),
                new SharedLegacyBiomeLoader(FMLPaths.CONFIGDIR.get().resolve(Constants.MOD_ID))
        );
    }

    @Override
    public void onStart() {
        SharedMaterials.init();
        super.onStart();
    }

    @Override
    public File getJarFile() {
        return ModList.get().getModFileById(Constants.MOD_ID_SHORT).getFile().getFilePath().toFile();
    }
}
