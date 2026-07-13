package com.pg85.otg.fabric;

import com.pg85.otg.OTG;
import com.pg85.otg.OTGEngine;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.shared.biome.SharedLegacyBiomeLoader;
import com.pg85.otg.shared.materials.SharedMaterials;
import com.pg85.otg.fabric.util.FabricModLoadedChecker;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FabricEngine extends OTGEngine {
    protected FabricEngine() {
        super(
                OTGLog.getLogger(),
                FabricLoader.getInstance().getConfigDir().resolve(Constants.MOD_ID),
                new FabricModLoadedChecker(),
                new SharedLegacyBiomeLoader(FabricLoader.getInstance().getConfigDir().resolve(Constants.MOD_ID))
        );
    }

    @Override
    public void onStart() {
        SharedMaterials.init();
        super.onStart();
    }

    @Override
    public File getJarFile() {
        // get the jar file of the mod from fabric itself
        List<File> jarFiles = new ArrayList<>();
        FabricLoader.getInstance().getModContainer(Constants.MOD_ID_SHORT).ifPresent(modContainer -> {
            for (var path : modContainer.getOrigin().getPaths()) {
                jarFiles.add(path.toFile());
            }
        });
        if (jarFiles.size() > 1) {
            OTG.log(LogLevel.WARN, LogCategory.MAIN, "Found multiple paths for mod jar, using the first one.");
        }
        return jarFiles.get(0);
    }
}
