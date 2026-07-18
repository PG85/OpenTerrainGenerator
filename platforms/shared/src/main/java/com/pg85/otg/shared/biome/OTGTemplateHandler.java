package com.pg85.otg.shared.biome;

import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.biome.BiomeTemplate;
import com.pg85.otg.presets.Preset;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public class OTGTemplateHandler {
    public static void handleTemplates(Preset preset, Registry<Biome> registry) {

        List<BiomeTemplate> biomeTemplates = preset.getBiomeTemplateList();

        List<BiomeConfig> biomeConfigs = preset.getBiomeConfigList();



    }
}
