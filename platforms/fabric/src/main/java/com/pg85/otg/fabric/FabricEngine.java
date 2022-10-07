package com.pg85.otg.fabric;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.core.OTGEngine;
import com.pg85.otg.fabric.materials.FabricMaterials;
import com.pg85.otg.fabric.presets.FabricPresetLoader;
import com.pg85.otg.fabric.util.FabricLogger;
import com.pg85.otg.fabric.util.FabricModLoadedChecker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.WritableRegistry;
import net.minecraft.world.level.biome.Biome;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class FabricEngine extends OTGEngine {

    private final OTGPlugin plugin;

    //The constructor
    protected FabricEngine(OTGPlugin plugin) {
        super(
                new FabricLogger(),
                Paths.get(FabricLoader.getInstance().getConfigDir().toString(),
                        File.separator + Constants.MOD_ID),
                new FabricModLoadedChecker(),
                new FabricPresetLoader(Paths.get(FabricLoader.getInstance().getConfigDir().toString(),
                        File.separator + Constants.MOD_ID))
        );

        this.plugin = plugin;
    }

    @Override
    public void onStart() {
        FabricMaterials.init();
        super.onStart();
    }

    public void reloadPreset(String presetFolderName, WritableRegistry<Biome> biomeRegistry)
    {
        ((FabricPresetLoader)this.presetLoader).reloadPresetFromDisk(presetFolderName, this.biomeResourcesManager, this.logger, biomeRegistry);
    }

    //Do we need an onsave and onreload?

    @Override
    public File getJarFile()
    {
        String fileName = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        // URLEncoded string, decode.
        try {
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) { }

        if(fileName != null)
        {
            File modFile = new File(fileName);
            if(modFile.isFile())
            {
                return modFile;
            }
        }
        return null;
    }
}
