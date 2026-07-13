package com.pg85.otg.config.settings.biome;

import com.pg85.otg.config.ConfigFile;
import com.pg85.otg.config.ConfigFunction;
import com.pg85.otg.config.io.IConfigFunctionProvider;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.settings.ConfigSection;
import com.pg85.otg.config.settings.biome.generated.BiomePlacementSettings;
import com.pg85.otg.config.settings.biome.generated.BiomeStructureTagSettings;
import com.pg85.otg.config.settings.preset.PresetSettings;
import com.pg85.otg.config.settingtype.Setting;
import com.pg85.otg.interfaces.ICustomStructureGen;
import com.pg85.otg.interfaces.ISaplingSpawner;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.minecraft.SaplingType;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BiomeSettings - represents any BiomeConfig-style config file
 * Settings are stored in, written from and read to ConfigSections.
 * Must be instantiated either as a BiomeConfig, with all config sections present
 * or as a BiomeTemplate, with any sections present.
 */
@Getter
public abstract class BiomeSettings implements ConfigFile {
    @Getter
    protected final Path configPath;
    protected TemplateConfig templateConfig = null;
    protected IdentitySettings identitySettings = null;
    protected MobSettings mobSettings = null;
    protected BiomePlacementConfig generationSettings = null;
    protected BiomeStructureSettings structureSettings = null;
    protected BiomeTerrainSettings terrainSettings = null;
    protected BiomeVisualSettings visualSettings = null;
    protected SurfaceSettings surfaceSettings = null;
    protected BiomeResourceSettings resourceSettings = null;

    protected BiomeTagSettings biomeTagSettings = null;
    protected BiomeStructureTagConfig biomeStructureTagConfig = null;

    private final String configName;
    @Getter
    @Setter
    protected ICustomStructureGen structureGen;

    protected BiomeSettings(SettingsMap reader, PresetSettings presetSettings, IConfigFunctionProvider provider) {
        this.configName = reader.getName();
        this.configPath = reader.getPath();
        renameOldSettings(reader);
        readDefaultSettings(reader, presetSettings, provider);
    }

    @Override
    public String getConfigName() {
        return configName;
    }

    // Misc

    // PresetConfig getters
    // TODO: Ideally, don't contain presetConfig within biomeconfig,
    // use a parent object that holds both, like a worldgenregion.

    abstract public boolean biomeConfigsHaveReplacement();

    // Inheritance

    // Height / volatility
    public double getCHCData(int controlLayer) {
        var chc = this.getTerrainSettings().getCustomHeightControl();
        if (controlLayer < 0 || controlLayer >= chc.size()) {
            // Legacy presets carry CHC arrays sized for 256-high worlds; extended
            // height worlds index past them. Treat missing layers as no control.
            return 0.0;
        }
        return chc.get(controlLayer);
    }

    // OTG Custom structures (BO's)

    public List<List<String>> getCustomStructureNames() {
        List<List<String>> customStructureNamesByGen = new ArrayList<>();
        for (ICustomStructureGen structureGens : this.getResourceSettings().getCustomStructures()) {
            List<String> customStructureNames = Arrays.asList(structureGens.getObjectNames());
            customStructureNamesByGen.add(customStructureNames);
        }
        return customStructureNamesByGen;
    }

    // Saplings

    public ISaplingSpawner getSaplingGen(SaplingType type) {
        ISaplingSpawner gen = this.resourceSettings.getSaplingGrowers().get(type);
        if (gen == null && type.growsTree()) {
            gen = this.resourceSettings.getSaplingGrowers().get(SaplingType.All);
        }
        return gen;
    }

    public ISaplingSpawner getCustomSaplingGen(LocalMaterialData materialData, boolean wideTrunk) {
        if (wideTrunk) {
            ISaplingSpawner spawner = this.resourceSettings.getCustomBigSaplingGrowers().get(materialData);
            if (spawner != null) {
                return spawner;
            }
        }
        return this.resourceSettings.getCustomSaplingGrowers().get(materialData);
    }
    // Misc
    public List<ConfigFunction<BiomeSettings>> getResourceQueue() {
        return this.getResourceSettings().getResourceQueue();
    }
    @Override
    public void renameOldSettings(SettingsMap settings)
    {
        settings.renameOldSetting("DisableNotchHeightControl", BiomeTerrainSettings.DISABLE_BIOME_HEIGHT);
        settings.renameOldSetting("BiomeDictId", BiomeTagSettings.BIOME_TAGS);
        settings.renameOldSetting("BiomeDictTags", BiomeTagSettings.BIOME_TAGS);
        settings.renameOldSetting("TemplateBiomeType", BiomeTagSettings.BIOME_TYPE);
        settings.renameOldSetting("IsleInBiome", BiomePlacementSettings.ISLE_IN_BIOMES);
        settings.renameOldSetting("BiomeIsBorder", BiomePlacementSettings.BORDER_IN_BIOMES);
        settings.renameOldSetting("BiomeColor", BiomePlacementSettings.BIOME_MAP_COLOR);
        settings.renameOldSetting("MaxAverageHeight", BiomeTerrainSettings.PEAK_FACTOR);
        settings.renameOldSetting("MaxAverageDepth", BiomeTerrainSettings.VALLEY_FACTOR);
    }

    protected void readDefaultSettings(SettingsMap settingsMap, PresetSettings presetSettings, IConfigFunctionProvider provider) {
        templateConfig = TemplateConfig.buildTemplateSettings(settingsMap);
        identitySettings = IdentitySettings.buildIdentitySettings(settingsMap, presetSettings);
        mobSettings = MobSettings.getMobSettings(settingsMap);
        generationSettings = BiomePlacementConfig.getGenerationSettings(settingsMap, presetSettings.getGenerationSettings());
        structureSettings = BiomeStructureSettings.getBiomeStructureSettings(settingsMap, presetSettings.getStructureSettings());
        terrainSettings = BiomeTerrainSettings.getBiomeTerrainSettings(settingsMap, presetSettings.getTerrainSettings());
        visualSettings = BiomeVisualSettings.getBiomeVisualSettings(settingsMap, presetSettings.getVisualSettings());

        biomeTagSettings = BiomeTagSettings.getBiomeTagConfig(settingsMap, identitySettings);
        biomeStructureTagConfig = BiomeStructureTagConfig.getBiomeStructureTagConfig(settingsMap, structureSettings);

        surfaceSettings = SurfaceSettings.getSurfaceSettings(
                settingsMap,
                presetSettings.getBlockSettings(),
                presetSettings.getTerrainSettings()
                );

        resourceSettings = BiomeResourceSettings.getResourceSettings(
                presetSettings.getResourceSettings(),
                new ArrayList<>(
                        settingsMap.getConfigFunctions(
                                this,
                                provider,
                                presetSettings.getConfigName()
                        )));
    }

    @Override
    public void writeConfigSettings(SettingsMap writer) {
        if (identitySettings != null)
            writeConfigSection(writer, identitySettings);
        if (generationSettings != null)
            writeConfigSection(writer, generationSettings);
        if (terrainSettings != null)
            writeConfigSection(writer, terrainSettings);
        if (surfaceSettings != null)
            writeConfigSection(writer, surfaceSettings);
        if (visualSettings != null)
            writeConfigSection(writer, visualSettings);
        if (resourceSettings != null)
            resourceSettings.write(writer);
        if (structureSettings != null)
            writeConfigSection(writer, structureSettings);
        if (mobSettings != null)
            writeConfigSection(writer, mobSettings);
        if (biomeTagSettings != null) {
            writeConfigSection(writer, biomeTagSettings);
        }
        if (biomeStructureTagConfig != null) {
            if (biomeStructureTagConfig.isDisplayAllStructureTags()) {
                writeConfigSection(writer, biomeStructureTagConfig);
            } else {
                writer.putSetting(BiomeStructureTagSettings.DISPLAY_ALL_STRUCTURE_TAGS, biomeStructureTagConfig);
                writeAlteredConfigSection(writer, biomeStructureTagConfig);
            }
        }
    }

    public void writeConfigSection(SettingsMap writer, ConfigSection section) {
        writer.header1(section.getSectionName(), section.getSectionComment());

        for (Setting<?> setting : section.getSettingsList()) {
            writer.putSetting(setting, section);
        }
    }

    public void writeAlteredConfigSection(SettingsMap writer, ConfigSection section) {
        writer.header2(section.getSectionName(), section.getSectionComment());

        for (Setting<?> setting : section.getAlteredSettings()) {
            writer.putSetting(setting, section);
        }
    }
}
