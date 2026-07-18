package com.pg85.otg.shared.biome;

import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.config.settings.preset.PresetSettings;
import com.pg85.otg.presets.Preset;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.WritableRegistry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

public class SharedBiomeLoader {

    public static HolderGetter<PlacedFeature> PLACED_FEATURE_HOLDER;
    public static HolderGetter<ConfiguredWorldCarver<?>> CONFIGURED_CARVER_HOLDER;
    public static boolean BIOME_DATA_INITIALIZED = false;

    public static void registerBiomesForPreset(Preset preset, WritableRegistry<Biome> biomeRegistry) {

        if (!BIOME_DATA_INITIALIZED) {
            // Should always be initialized, but better safe than sorry. Would rather have a sensical error message than nonsensical
            throw new IllegalStateException("BiomeDataMixin not initialized");
        }
        HolderGetter<PlacedFeature> featureHolder = PLACED_FEATURE_HOLDER;
        HolderGetter<ConfiguredWorldCarver<?>> carverHolder = CONFIGURED_CARVER_HOLDER;

        List<BiomeConfig> biomeConfigs = preset.getBiomeConfigList();

        // Handle templates

        OTGTemplateHandler.handleTemplates(preset, biomeRegistry);

        // Handle ocean biomes

        // Handle mob inheritance
        MobInheritanceHandler.handleMobInheritance(biomeRegistry, biomeConfigs);





    }

}
