package com.pg85.otg.fabric.presets;

import com.mojang.serialization.Lifecycle;
import com.pg85.otg.config.biome.BiomeConfigFinder;
import com.pg85.otg.config.biome.BiomeGroup;
import com.pg85.otg.config.biome.TemplateBiome;
import com.pg85.otg.config.io.IConfigFunctionProvider;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.constants.SettingsEnums;
import com.pg85.otg.core.OTG;
import com.pg85.otg.core.config.world.WorldConfig;
import com.pg85.otg.core.presets.LocalPresetLoader;
import com.pg85.otg.core.presets.Preset;
import com.pg85.otg.fabric.biome.FabricBiome;
import com.pg85.otg.fabric.materials.FabricMaterialReader;
import com.pg85.otg.fabric.network.BiomeSettingSyncWrapper;
import com.pg85.otg.fabric.network.OTGClientSyncManager;
import com.pg85.otg.gen.biome.BiomeData;
import com.pg85.otg.gen.biome.layers.BiomeLayerData;
import com.pg85.otg.gen.biome.layers.NewBiomeGroup;
import com.pg85.otg.interfaces.*;
import com.pg85.otg.util.biome.MCBiomeResourceLocation;
import com.pg85.otg.util.biome.OTGBiomeResourceLocation;
import com.pg85.otg.util.biome.WeightedMobSpawnGroup;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import com.pg85.otg.util.minecraft.EntityCategory;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FabricPresetLoader extends LocalPresetLoader {

    private Map<String, List<ResourceKey<Biome>>> biomesByPresetFolderName = new LinkedHashMap<>();
    private HashMap<String, IBiome[]> globalIdMapping = new HashMap<>();
    private Map<String, BiomeLayerData> presetGenerationData = new HashMap<>();

    public FabricPresetLoader(Path otgRootFolder) {
        super(otgRootFolder);
    }

    // Creates a preset-specific materialreader, have to do this
    // only when loading each preset since each preset may have
    // its own block fallbacks / block dictionaries.
    @Override
    protected IMaterialReader createMaterialReader() {
        return new FabricMaterialReader();
    }

    public List<ResourceKey<Biome>> getBiomeRegistryKeys(String presetFolderName)
    {
        return this.biomesByPresetFolderName.get(presetFolderName);
    }

    public IBiome[] getGlobalIdMapping(String presetFolderName)
    {
        return globalIdMapping.get(presetFolderName);
    }

    public Map<String, BiomeLayerData> getPresetGenerationData()
    {
        Map<String, BiomeLayerData> clonedData = new HashMap<>();
        for(Map.Entry<String, BiomeLayerData> entry : this.presetGenerationData.entrySet())
        {
            clonedData.put(entry.getKey(), new BiomeLayerData(entry.getValue()));
        }
        return clonedData;
    }

    public void reloadPresetFromDisk(String presetFolderName, IConfigFunctionProvider biomeResourcesManager, ILogger logger, WritableRegistry<Biome> biomeRegistry)
    {
        clearCaches();

        if(this.presetsDir.exists() && this.presetsDir.isDirectory())
        {
            for(File presetDir : this.presetsDir.listFiles())
            {
                if(presetDir.isDirectory() && presetDir.getName().equals(presetFolderName))
                {
                    for(File file : presetDir.listFiles())
                    {
                        if(file.getName().equals(Constants.WORLD_CONFIG_FILE))
                        {
                            Preset preset = loadPreset(presetDir.toPath(), biomeResourcesManager, logger);
                            Preset existingPreset = this.presets.get(preset.getFolderName());
                            existingPreset.update(preset);
                            break;
                        }
                    }
                }
            }
        }
        registerBiomes(true, biomeRegistry);
    }

    protected void clearCaches()
    {
        this.globalIdMapping = new HashMap<>();
        this.presetGenerationData = new HashMap<>();
        this.biomesByPresetFolderName = new LinkedHashMap<>();
        this.materialReaderByPresetFolderName = new HashMap<>();
    }

    public void reRegisterBiomes(String presetFolderName, WritableRegistry<Biome> biomeRegistry)
    {
        this.globalIdMapping.remove(presetFolderName);
        this.presetGenerationData.remove(presetFolderName);
        this.biomesByPresetFolderName.remove(presetFolderName);

        registerBiomes(true, biomeRegistry);
    }

    @Override
    public void registerBiomes()
    {
        registerBiomes(false, null);
    }

    private void registerBiomes(boolean refresh, WritableRegistry<Biome> biomeRegistry)
    {
        for(Preset preset : this.presets.values())
        {
            registerBiomesForPreset(refresh, preset, biomeRegistry);
        }
    }

    private void registerBiomesForPreset(boolean refresh, Preset preset, WritableRegistry<Biome> biomeRegistry)
    {
        // Index BiomeColors for FromImageMode and /otg map
        HashMap<Integer, Integer> biomeColorMap = new HashMap<Integer, Integer>();

        // Start at 1, 0 is the fallback for the biome generator (the world's ocean biome).
        int currentId = 1;

        List<ResourceKey<Biome>> presetBiomes = new ArrayList<>();
        this.biomesByPresetFolderName.put(preset.getFolderName(), presetBiomes);

        IWorldConfig worldConfig = preset.getWorldConfig();
        IBiomeConfig oceanBiomeConfig = null;
        int[] oceanTemperatures = new int[]{0, 0, 0, 0};

        List<IBiomeConfig> biomeConfigs = preset.getAllBiomeConfigs();

        Map<Integer, List<BiomeData>> isleBiomesAtDepth = new HashMap<>();
        Map<Integer, List<BiomeData>> borderBiomesAtDepth = new HashMap<>();

        Map<String, List<Integer>> worldBiomes = new HashMap<>();
        Map<String, IBiomeConfig> biomeConfigsByName = new HashMap<>();

        // Create registry keys for each biomeconfig, create template
        // biome configs for any non-otg biomes targeted via TemplateForBiome.
        Map<IBiomeResourceLocation, IBiomeConfig> biomeConfigsByResourceLocation = new LinkedHashMap<>();
        List<String> blackListedBiomes = worldConfig.getBlackListedBiomes();

        processTemplateBiomes(preset.getFolderName(), worldConfig, biomeConfigs, biomeConfigsByResourceLocation, biomeConfigsByName, blackListedBiomes);

        for(IBiomeConfig biomeConfig : biomeConfigs)
        {
            if(!biomeConfig.getIsTemplateForBiome())
            {
                // Normal OTG biome, not a template biome.
                IBiomeResourceLocation otgLocation = new OTGBiomeResourceLocation(preset.getPresetFolder(), preset.getShortPresetName(), preset.getMajorVersion(), biomeConfig.getName());
                biomeConfigsByResourceLocation.put(otgLocation, biomeConfig);
                biomeConfigsByName.put(biomeConfig.getName(), biomeConfig);
            }
        }

        IBiome[] presetIdMapping = new IBiome[biomeConfigsByResourceLocation.entrySet().size()];
        for(Map.Entry<IBiomeResourceLocation, IBiomeConfig> biomeConfig : biomeConfigsByResourceLocation.entrySet())
        {
            boolean isOceanBiome = false;
            // Biome id 0 is reserved for ocean, used when a land column has
            // no biome assigned, which can happen due to biome group rarity.
            if(biomeConfig.getValue().getName().equals(worldConfig.getDefaultOceanBiome()))
            {
                oceanBiomeConfig = biomeConfig.getValue();
                isOceanBiome = true;
            }

            int otgBiomeId = isOceanBiome ? 0 : currentId;

            // When using TemplateForBiome, we'll fetch the non-OTG biome from the registry, including any settings registered to it.
            // For normal biomes we create our own new OTG biome and apply settings from the biome config.
            ResourceLocation resourceLocation = new ResourceLocation(biomeConfig.getKey().toResourceLocationString());
            ResourceKey<Biome> registryKey;
            Biome biome;
            if(biomeConfig.getValue().getIsTemplateForBiome())
            {
                if(refresh)
                {
                    biome = biomeRegistry.get(resourceLocation);
                    Optional<ResourceKey<Biome>> key = biomeRegistry.getResourceKey(biome);
                    registryKey = key.isPresent() ? key.get() : null;
                } else {
                    biome = BuiltinRegistries.BIOME.get(resourceLocation);
                    // TODO: Can we not fetch an existing key?
                    registryKey = ResourceKey.create(Registry.BIOME_REGISTRY, resourceLocation);
                }
            } else {
                if(!(biomeConfig.getKey() instanceof OTGBiomeResourceLocation))
                {
                    if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.BIOME_REGISTRY))
                    {
                        OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.BIOME_REGISTRY, "Could not process template biomeconfig " + biomeConfig.getValue().getName() + ", did you set TemplateForBiome:true in the BiomeConfig?");
                    }
                    continue;
                }
                if(OTG.getEngine().getPluginConfig().getDeveloperModeEnabled())
                {
                    registryKey = ResourceKey.create(Registry.BIOME_REGISTRY, resourceLocation);
                    if(registryKey != null)
                    {
                        // TODO: Biome tag
                        /*
                        // For OTG biomes, add Forge biome dictionary tags.
                        biomeConfig.getValue().getBiomeDictTags().forEach(biomeDictId -> {
                            if(biomeDictId != null && biomeDictId.trim().length() > 0)
                            {
                                BiomeDictionary.addTypes(registryKey, BiomeDictionary.Type.getType(biomeDictId.trim()));
                            }
                        });
                        */




                        // For developer-mode, always re-create OTG biomes, to pick up any config changes.
                        // This does break any kind of datapack support we might implement for OTG biomes.
                        biome = FabricBiome.createOTGBiome(isOceanBiome, preset.getWorldConfig(), biomeConfig.getValue());
                        if(refresh)
                        {
                            biomeRegistry.registerOrOverride(OptionalInt.empty(), registryKey, biome, Lifecycle.stable());
                        } else {
                            Registry.register(BuiltinRegistries.BIOME, registryKey, biome);
                        }
                    } else {
                        biome = null;
                    }
                } else {
                    if(refresh)
                    {
                        biome = biomeRegistry.get(resourceLocation);
                        Optional<ResourceKey<Biome>> key = biomeRegistry.getResourceKey(biome);
                        registryKey = key.isPresent() ? key.get() : null;
                    } else {
                        registryKey = ResourceKey.create(Registry.BIOME_REGISTRY, resourceLocation);
                        if(registryKey != null)
                        {
                            /*
                            // For OTG biomes, add Forge biome dictionary tags.
                            biomeConfig.getValue().getBiomeDictTags().forEach(biomeDictId -> {
                                if(biomeDictId != null && biomeDictId.trim().length() > 0)
                                {
                                    BiomeDictionary.addTypes(registryKey, BiomeDictionary.Type.getType(biomeDictId.trim()));
                                }
                            });

                             */

                            biome = FabricBiome.createOTGBiome(isOceanBiome, preset.getWorldConfig(), biomeConfig.getValue());
                            Registry.register(BuiltinRegistries.BIOME, registryKey, biome);
                        } else {
                            biome = null;
                        }
                    }
                }
            }
            if(biome == null || registryKey == null)
            {
                if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.BIOME_REGISTRY))
                {
                    OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.BIOME_REGISTRY, "Could not find biome " + resourceLocation.toString() + " for biomeconfig " + biomeConfig.getValue().getName());
                }
                continue;
            }
            presetBiomes.add(registryKey);
            biomeConfig.getValue().setRegistryKey(biomeConfig.getKey());
            biomeConfig.getValue().setOTGBiomeId(otgBiomeId);

            // Populate our map for syncing
            OTGClientSyncManager.getSyncedData().put(resourceLocation.toString(), new BiomeSettingSyncWrapper(biomeConfig.getValue()));

            // Ocean temperature mappings. Probably a better way to do this?
            if (biomeConfig.getValue().getName().equals(worldConfig.getDefaultWarmOceanBiome()))
            {
                oceanTemperatures[0] = otgBiomeId;
            }
            if (biomeConfig.getValue().getName().equals(worldConfig.getDefaultLukewarmOceanBiome()))
            {
                oceanTemperatures[1] = otgBiomeId;
            }
            if (biomeConfig.getValue().getName().equals(worldConfig.getDefaultColdOceanBiome()))
            {
                oceanTemperatures[2] = otgBiomeId;
            }
            if (biomeConfig.getValue().getName().equals(worldConfig.getDefaultFrozenOceanBiome()))
            {
                oceanTemperatures[3] = otgBiomeId;
            }

            IBiome otgBiome = new FabricBiome(biome, biomeConfig.getValue());
            if(otgBiomeId >= presetIdMapping.length)
            {
                OTG.getEngine().getLogger().log(LogLevel.FATAL, LogCategory.CONFIGS, "Fatal error while registering OTG biome id's for preset " + preset.getFolderName() + ", most likely you've assigned a DefaultOceanBiome that doesn't exist.");
                throw new RuntimeException("Fatal error while registering OTG biome id's for preset " + preset.getFolderName() + ", most likely you've assigned a DefaultOceanBiome that doesn't exist.");
            }
            presetIdMapping[otgBiomeId] = otgBiome;

            List<Integer> idsForBiome = worldBiomes.get(biomeConfig.getValue().getName());
            if(idsForBiome == null)
            {
                idsForBiome = new ArrayList<Integer>();
                worldBiomes.put(biomeConfig.getValue().getName(), idsForBiome);
            }
            idsForBiome.add(otgBiomeId);

            // Make a list of isle and border biomes per generation depth
            if(biomeConfig.getValue().isIsleBiome())
            {
                // Make or get a list for this group depth, then add
                List<BiomeData> biomesAtDepth = isleBiomesAtDepth.getOrDefault(worldConfig.getBiomeMode() == SettingsEnums.BiomeMode.NoGroups ? biomeConfig.getValue().getBiomeSize() : biomeConfig.getValue().getBiomeSizeWhenIsle(), new ArrayList<>());
                biomesAtDepth.add(
                        new BiomeData(
                                otgBiomeId,
                                worldConfig.getBiomeMode() == SettingsEnums.BiomeMode.NoGroups ? biomeConfig.getValue().getBiomeRarity() : biomeConfig.getValue().getBiomeRarityWhenIsle(),
                                worldConfig.getBiomeMode() == SettingsEnums.BiomeMode.NoGroups ? biomeConfig.getValue().getBiomeSize() : biomeConfig.getValue().getBiomeSizeWhenIsle(),
                                biomeConfig.getValue().getBiomeTemperature(),
                                biomeConfig.getValue().getIsleInBiomes(),
                                biomeConfig.getValue().getBorderInBiomes(),
                                biomeConfig.getValue().getOnlyBorderNearBiomes(),
                                biomeConfig.getValue().getNotBorderNearBiomes()
                        )
                );
                isleBiomesAtDepth.put(worldConfig.getBiomeMode() == SettingsEnums.BiomeMode.NoGroups ? biomeConfig.getValue().getBiomeSize() : biomeConfig.getValue().getBiomeSizeWhenIsle(), biomesAtDepth);
            }

            if(biomeConfig.getValue().isBorderBiome())
            {
                // Make or get a list for this group depth, then add
                List<BiomeData> biomesAtDepth = borderBiomesAtDepth.getOrDefault(worldConfig.getBiomeMode() == SettingsEnums.BiomeMode.NoGroups ? biomeConfig.getValue().getBiomeSize() : biomeConfig.getValue().getBiomeSizeWhenBorder(), new ArrayList<>());
                biomesAtDepth.add(
                        new BiomeData(
                                otgBiomeId,
                                biomeConfig.getValue().getBiomeRarity(),
                                worldConfig.getBiomeMode() == SettingsEnums.BiomeMode.NoGroups ? biomeConfig.getValue().getBiomeSize() : biomeConfig.getValue().getBiomeSizeWhenBorder(),
                                biomeConfig.getValue().getBiomeTemperature(),
                                biomeConfig.getValue().getIsleInBiomes(),
                                biomeConfig.getValue().getBorderInBiomes(),
                                biomeConfig.getValue().getOnlyBorderNearBiomes(),
                                biomeConfig.getValue().getNotBorderNearBiomes()
                        )
                );
                borderBiomesAtDepth.put(worldConfig.getBiomeMode() == SettingsEnums.BiomeMode.NoGroups ? biomeConfig.getValue().getBiomeSize() : biomeConfig.getValue().getBiomeSizeWhenBorder(), biomesAtDepth);
            }

            // Index BiomeColor for FromImageMode and /otg map
            biomeColorMap.put(biomeConfig.getValue().getBiomeColor(), otgBiomeId);

            if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.BIOME_REGISTRY))
            {
                OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.BIOME_REGISTRY, "Registered biome " + resourceLocation.toString() + " | " + biomeConfig.getValue().getName() + " with OTG id " + otgBiomeId);
            }

            currentId += isOceanBiome ? 0 : 1;
        }

        // If the ocean config is null, shift the array downwards to fill id 0
        if (oceanBiomeConfig == null)
        {
            System.arraycopy(presetIdMapping, 1, presetIdMapping, 0, presetIdMapping.length - 1);
        }

        this.globalIdMapping.put(preset.getFolderName(), presetIdMapping);

        // Set the base data
        BiomeLayerData data = new BiomeLayerData(preset.getPresetFolder(), worldConfig, oceanBiomeConfig, oceanTemperatures);

        Set<Integer> biomeDepths = new HashSet<>();
        Map<Integer, List<NewBiomeGroup>> groupDepths = new HashMap<>();

        // Iterate through the groups and add it to the layer data
        processBiomeGroups(preset.getFolderName(), worldConfig, biomeConfigsByResourceLocation, biomeConfigsByName, blackListedBiomes, biomeDepths, groupDepths, data);

        // Add the data and process isle/border biomes
        data.init(biomeDepths, groupDepths, isleBiomesAtDepth, borderBiomesAtDepth, worldBiomes, biomeColorMap, presetIdMapping);

        // Set data for this preset
        this.presetGenerationData.put(preset.getFolderName(), data);
    }

    private void processTemplateBiomes(String presetFolderName, IWorldConfig worldConfig, List<IBiomeConfig> biomeConfigs, Map<IBiomeResourceLocation, IBiomeConfig> biomeConfigsByResourceLocation, Map<String, IBiomeConfig> biomeConfigsByName, List<String> blackListedBiomes)
    {
        // TODO
    }

    private void processBiomeGroups(String presetFolderName, IWorldConfig worldConfig, Map<IBiomeResourceLocation, IBiomeConfig> biomeConfigsByResourceLocation, Map<String, IBiomeConfig> biomeConfigsByName, List<String> blackListedBiomes, Set<Integer> biomeDepths, Map<Integer, List<NewBiomeGroup>> groupDepths, BiomeLayerData data)
    {
        // TODO
    }

    @Override
    protected void mergeVanillaBiomeMobSpawnSettings(BiomeConfigFinder.BiomeConfigStub biomeConfigStub, String biomeResourceLocation)
    {
        String[] resourceLocationArr = biomeResourceLocation.split(":");
        String resourceDomain = resourceLocationArr.length > 1 ? resourceLocationArr[0] : null;
        String resourceLocation = resourceLocationArr.length > 1 ? resourceLocationArr[1] : resourceLocationArr[0];

        Biome biome = null;
        try
        {
            ResourceLocation location = new ResourceLocation(resourceDomain, resourceLocation);
            biome = BuiltinRegistries.BIOME.get(location);
        }
        catch(ResourceLocationException ex)
        {
            // Can happen when no biome is registered or input is otherwise invalid.
        }
        if(biome != null)
        {
            // Merge the vanilla biome's mob spawning lists with the mob spawning lists from the BiomeConfig.
            // Mob spawning settings for the same creature will not be inherited (so BiomeConfigs can override vanilla mob spawning settings).
            // We also inherit any mobs that have been added to vanilla biomes' mob spawning lists by other mods.
            biomeConfigStub.mergeMobs(getListFromMinecraftBiome(biome, MobCategory.MONSTER), EntityCategory.MONSTER);
            biomeConfigStub.mergeMobs(getListFromMinecraftBiome(biome, MobCategory.AMBIENT), EntityCategory.AMBIENT);
            biomeConfigStub.mergeMobs(getListFromMinecraftBiome(biome, MobCategory.CREATURE), EntityCategory.CREATURE);
            biomeConfigStub.mergeMobs(getListFromMinecraftBiome(biome, MobCategory.UNDERGROUND_WATER_CREATURE), EntityCategory.UNDERGROUND_WATER_CREATURE);
            biomeConfigStub.mergeMobs(getListFromMinecraftBiome(biome, MobCategory.WATER_AMBIENT), EntityCategory.WATER_AMBIENT);
            biomeConfigStub.mergeMobs(getListFromMinecraftBiome(biome, MobCategory.WATER_CREATURE), EntityCategory.WATER_CREATURE);
            biomeConfigStub.mergeMobs(getListFromMinecraftBiome(biome, MobCategory.MISC), EntityCategory.MISC);
        } else {
            if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.MOBS))
            {
                OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.MOBS, "Could not inherit mobs for unrecognised biome \"" +  biomeResourceLocation + "\" in " + biomeConfigStub.getBiomeName() + Constants.BiomeConfigFileExtension);
            }
        }
    }

    private List<WeightedMobSpawnGroup> getListFromMinecraftBiome(Biome biome, MobCategory type)
    {
        WeightedRandomList<MobSpawnSettings.SpawnerData> mobList = biome.getMobSettings().getMobs(type);
        List<WeightedMobSpawnGroup> result = new ArrayList<WeightedMobSpawnGroup>();
        for (MobSpawnSettings.SpawnerData spawner : mobList.unwrap())
        {
            WeightedMobSpawnGroup wMSG = new WeightedMobSpawnGroup(spawner.type.getDescriptionId().replace("entity.minecraft.", ""), spawner.getWeight().asInt(), spawner.minCount, spawner.maxCount);
            if(wMSG != null)
            {
                result.add(wMSG);
            }
        }
        return result;
    }

}
