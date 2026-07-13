package com.pg85.otg.shared.biome;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

import com.mojang.serialization.Lifecycle;
import com.pg85.otg.OTG;
import com.pg85.otg.config.ConfigFunction;
import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.biome.BiomeGroupFunction;
import com.pg85.otg.config.settings.biome.BiomeVisualSettings;
import com.pg85.otg.config.settings.biome.MobSettings;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.gen.biome.BiomeData;
import com.pg85.otg.gen.biome.layers.BiomeLayerData;
import com.pg85.otg.gen.biome.layers.BiomeGroup;
import com.pg85.otg.gen.resource.RegistryResource;
import com.pg85.otg.interfaces.IBiome;
import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.interfaces.IBiomeResourceLocation;
import com.pg85.otg.config.settings.preset.PresetSettings;
import com.pg85.otg.presets.LocalPresetLoader;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.biome.OTGBiomeResourceLocation;
import com.pg85.otg.util.biome.WeightedMobSpawnGroup;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import static com.pg85.otg.util.logging.LogCategory.CONFIGS;


public class SharedLegacyBiomeLoader extends LocalPresetLoader {
    // Static fields to store the references
    public static HolderGetter<PlacedFeature> PLACED_FEATURE_HOLDER;
    public static HolderGetter<ConfiguredWorldCarver<?>> CONFIGURED_CARVER_HOLDER;
    public static boolean BIOME_DATA_INITIALIZED = false;
    private Map<String, List<ResourceKey<Biome>>> biomesByPresetFolderName = new LinkedHashMap<>();
    private HashMap<String, IBiome[]> globalIdMapping = new HashMap<>();
    private Map<String, BiomeLayerData> presetGenerationData = new HashMap<>();
    
    public SharedLegacyBiomeLoader(Path otgRootFolder)
    {
        super(otgRootFolder);
    }

    public List<ResourceKey<Biome>> getBiomeResourceKeys(String presetFolderName)
    {
        return this.biomesByPresetFolderName.get(presetFolderName);
    }

    @Override
    public IBiome[] getGlobalIdMapping(String presetFolderName)
    {
        return globalIdMapping.get(presetFolderName);
    }

    @Override
    public Map<String, BiomeLayerData> getPresetGenerationData()
    {
        return new HashMap<>(this.presetGenerationData);
    }

    // Note: BiomeGen and ChunkGen cache some settings during a session, so they'll only update on world exit/rejoin.
    public void reloadPresetFromDisk(String presetFolderName, WritableRegistry<Biome> biomeRegistry)
    {
        clearCaches();

        if(this.presetsDir.exists() && this.presetsDir.isDirectory())
        {
            for(File presetDir : Objects.requireNonNull(this.presetsDir.listFiles()))
            {
                if(presetDir.isDirectory() && presetDir.getName().equals(presetFolderName))
                {
                    for(File file : Objects.requireNonNull(presetDir.listFiles()))
                    {
                        if(file.getName().equals(Constants.PRESET_CONFIG_FILE))
                        {
                            Preset preset = loadPreset(presetDir.toPath());
                            Preset existingPreset = this.presets.get(preset.getFolderName());
                            existingPreset.update(preset);
                            break;
                        }
                    }
                }
            }
        }
        registerBiomes(biomeRegistry);
    }

    protected void clearCaches()
    {
        this.globalIdMapping = new HashMap<>();
        this.presetGenerationData = new HashMap<>();
        this.biomesByPresetFolderName = new LinkedHashMap<>();
    }

    public void reRegisterBiomes(String presetFolderName, WritableRegistry<Biome> biomeRegistry)
    {
        this.globalIdMapping.remove(presetFolderName);
        this.presetGenerationData.remove(presetFolderName);
        this.biomesByPresetFolderName.remove(presetFolderName);

        registerBiomes(biomeRegistry);
    }

    public void registerBiomes()
    {
        registerBiomes(null);
    }

    public void registerBiomes(WritableRegistry<Biome> biomeRegistry)
    {
        for(Preset preset : this.presets.values())
        {
            registerBiomesForPreset(preset, biomeRegistry);
        }
    }

    private void registerBiomesForPreset(Preset preset, WritableRegistry<Biome> biomeRegistry)
    {
        if (!BIOME_DATA_INITIALIZED) {
            // Should always be initialized, but better safe than sorry. Would rather have a sensical error message than nonsensical
            throw new IllegalStateException("BiomeDataMixin not initialized");
        }
        HolderGetter<PlacedFeature> featureHolder = PLACED_FEATURE_HOLDER;
        HolderGetter<ConfiguredWorldCarver<?>> carverHolder = CONFIGURED_CARVER_HOLDER;

        // Index BiomeColors for FromImageMode and /otg map
        HashMap<Integer, Integer> biomeColorMap = new HashMap<>();

        // Start at 1, 0 is the fallback for the biome generator (the world's ocean biome).
        int currentId = 1;

        List<ResourceKey<Biome>> presetBiomes = new ArrayList<>();
        this.biomesByPresetFolderName.put(preset.getFolderName(), presetBiomes);

        PresetSettings presetConfig = preset.getPresetConfig();
        BiomeSettings oceanBiomeConfig = null;
        int[] oceanTemperatures = new int[]{0, 0, 0, 0};

        List<BiomeConfig> biomeConfigs = preset.getBiomeConfigList();

        Map<Integer, List<BiomeData>> isleBiomesAtDepth = new HashMap<>();
        Map<Integer, List<BiomeData>> borderBiomesAtDepth = new HashMap<>();

        Map<String, List<Integer>> worldBiomes = new HashMap<>();
        Map<String, BiomeConfig> biomeConfigsByName = new HashMap<>();

        // Create registry keys for each biomeconfig, create template 
        // biome configs for any non-otg biomes targeted via TemplateForBiome.
        Map<IBiomeResourceLocation, BiomeConfig> biomeConfigsByResourceLocation = new LinkedHashMap<>();
        List<String> blackListedBiomes = presetConfig.getGenerationSettings().getBlackListedBiomes();

        //processTemplateBiomes(preset.getFolderName(), presetConfig, biomeConfigs, biomeConfigsByResourceLocation, biomeConfigsByName, blackListedBiomes);

        for(BiomeConfig biomeConfig : biomeConfigs)
        {
            if(!biomeConfig.getIdentitySettings().isTemplateForBiome())
            {
                // Normal OTG biome, not a template biome.
                IBiomeResourceLocation otgLocation = new OTGBiomeResourceLocation(preset.getPresetFolder(), preset.getPresetRegistryName(), biomeConfig.getIdentitySettings().getBiomeName());
                biomeConfig.setRegistryKey(otgLocation);
                biomeConfigsByResourceLocation.put(otgLocation, biomeConfig);
                biomeConfigsByName.put(biomeConfig.getIdentitySettings().getBiomeName(), biomeConfig);
            }
        }

        MobInheritanceHandler.handleMobInheritance(biomeRegistry, biomeConfigs);

        IBiome[] presetIdMapping = new IBiome[biomeConfigsByResourceLocation.size()];
        boolean hasOceanBiome = false;
        for(Entry<IBiomeResourceLocation, BiomeConfig> biomeConfigEntry : biomeConfigsByResourceLocation.entrySet())
        {
            IBiomeResourceLocation iBiomeResourceLocation = biomeConfigEntry.getKey();
            BiomeConfig biomeConfig = biomeConfigEntry.getValue();
            boolean isOceanBiome = false;
            // Biome id 0 is reserved for ocean, used when a land column has 
            // no biome assigned, which can happen due to biome group rarity.
            if(biomeConfig.getIdentitySettings().getBiomeName().equals(presetConfig.getGenerationSettings().getDefaultOceanBiome()))
            {
                oceanBiomeConfig = biomeConfig;
                isOceanBiome = true;
                hasOceanBiome = true;
            }

            int otgBiomeId = isOceanBiome ? 0 : currentId;

            // Some legacy presets have invalid ocean biomes
            // This check forces the final biome into ID 0 to be ocean biome
            // This avoids a crash on modern versions
            if(otgBiomeId == presetIdMapping.length && !hasOceanBiome) {
                otgBiomeId = 0;
            }

            if(otgBiomeId > presetIdMapping.length)
            {
                OTGLog.fatal(CONFIGS, "Fatal error while registering OTG biome id's for preset " + preset.getFolderName());

                OTGLog.info(CONFIGS, "Total number of biomes to register: " + presetIdMapping.length);
                OTGLog.info(CONFIGS, "Current id: " + otgBiomeId);
                OTGLog.info(CONFIGS, "List of biomes: " + Arrays.toString(presetIdMapping));

                throw new RuntimeException("Fatal error while registering OTG biome id's for preset " + preset.getFolderName());
            }

            // When using TemplateForBiome, we'll fetch the non-OTG biome from the registry, including any settings registered to it.
            // For normal biomes we create our own new OTG biome and apply settings from the biome config.
            ResourceLocation resourceLocation = new ResourceLocation(iBiomeResourceLocation.toResourceLocationString());
            ResourceKey<Biome> resourceKey;
            Biome biome;
            Holder.Reference<Biome> ref;
            // templates, and non-developer refresh, both just get the biome from the registry
            if(
                biomeConfig.getIdentitySettings().isTemplateForBiome()
            ) {
                biome = biomeRegistry.get(resourceLocation);
                if (biome == null) {
                    if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.BIOME_REGISTRY))
                    {
                        OTG.log(LogLevel.ERROR, LogCategory.BIOME_REGISTRY, "Could not find biome " + resourceLocation + " for biomeconfig " + biomeConfig.getIdentitySettings().getBiomeName());
                    }
                    continue;
                }
                Optional<ResourceKey<Biome>> key = biomeRegistry.getResourceKey(biome);
                resourceKey = key.orElse(null);
                if (resourceKey == null) {
                    if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.BIOME_REGISTRY))
                    {
                        OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.BIOME_REGISTRY, "Could not find resource key for biome " + resourceLocation + " for biomeconfig " + biomeConfig.getIdentitySettings().getBiomeName());
                    }
                    continue;
                }
                ref = biomeRegistry.getHolder(resourceKey).orElseThrow();

            } else {
                if(!(iBiomeResourceLocation instanceof OTGBiomeResourceLocation))
                {
                    if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.BIOME_REGISTRY))
                    {
                        OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.BIOME_REGISTRY, "Could not process template biomeconfig " + biomeConfig.getIdentitySettings().getBiomeName() + ", did you set TemplateForBiome:true in the BiomeConfig?");
                    }
                    continue;
                }
                resourceKey = ResourceKey.create(Registries.BIOME, resourceLocation);
                // For OTG biomes, add Fabric biome dictionary tags.

                biome = SharedLegacyBiomeLoader.createOTGBiome(preset.getPresetConfig(), biomeConfig, featureHolder, carverHolder);

                ref = biomeRegistry.register(resourceKey, biome, Lifecycle.stable());
            }
            presetBiomes.add(resourceKey);

            biomeConfig.setOTGBiomeId(otgBiomeId);

            // Populate our map for syncing
            //OTGClientSyncManager.getSyncedData().put(resourceLocation.toString(), new SharedBiomeSyncWrapper(biomeConfig));

            // Ocean temperature mappings. Probably a better way to do this?
            if (biomeConfig.getIdentitySettings().getBiomeName().equals(presetConfig.getGenerationSettings().getDefaultWarmOceanBiome()))
            {
                oceanTemperatures[0] = otgBiomeId;
            }
            if (biomeConfig.getIdentitySettings().getBiomeName().equals(presetConfig.getGenerationSettings().getDefaultLukewarmOceanBiome()))
            {
                oceanTemperatures[1] = otgBiomeId;
            }
            if (biomeConfig.getIdentitySettings().getBiomeName().equals(presetConfig.getGenerationSettings().getDefaultColdOceanBiome()))
            {
                oceanTemperatures[2] = otgBiomeId;
            }
            if (biomeConfig.getIdentitySettings().getBiomeName().equals(presetConfig.getGenerationSettings().getDefaultFrozenOceanBiome()))
            {
                oceanTemperatures[3] = otgBiomeId;
            }

            IBiome otgBiome = new SharedBiome(biomeConfig, biome, ref);

            presetIdMapping[otgBiomeId] = otgBiome;

            List<Integer> idsForBiome = worldBiomes.computeIfAbsent(biomeConfig.getIdentitySettings().getBiomeName(), k -> new ArrayList<>());
            idsForBiome.add(otgBiomeId);

            // Make a list of isle and border biomes per generation depth
            if(biomeConfig.getGenerationSettings().isIsleBiome())
            {
                // Make or get a list for this group depth, then add
                List<BiomeData> biomesAtDepth = isleBiomesAtDepth.getOrDefault(biomeConfig.getGenerationSettings().getBiomeSizeWhenIsle(), new ArrayList<>());
                biomesAtDepth.add(
                        new BiomeData(
                                otgBiomeId,
                                biomeConfig.getGenerationSettings().getBiomeRarityWhenIsle(),
                                biomeConfig.getGenerationSettings().getBiomeSizeWhenIsle(),
                                biomeConfig.getVisualSettings().getBiomeTemperature(),
                                biomeConfig.getGenerationSettings().getIsleInBiomes(),
                                biomeConfig.getGenerationSettings().getBorderInBiomes(),
                                biomeConfig.getGenerationSettings().getOnlyBorderNear(),
                                biomeConfig.getGenerationSettings().getNotBorderNear()
                        )
                );
                isleBiomesAtDepth.put(biomeConfig.getGenerationSettings().getBiomeSizeWhenIsle(), biomesAtDepth);
            }

            if(biomeConfig.getGenerationSettings().isBorderBiome())
            {
                // Make or get a list for this group depth, then add
                List<BiomeData> biomesAtDepth = borderBiomesAtDepth.getOrDefault(biomeConfig.getGenerationSettings().getBiomeSizeWhenBorder(), new ArrayList<>());
                biomesAtDepth.add(
                        new BiomeData(
                                otgBiomeId,
                                biomeConfig.getGenerationSettings().getBiomeRarity(),
                                biomeConfig.getGenerationSettings().getBiomeSizeWhenBorder(),
                                biomeConfig.getVisualSettings().getBiomeTemperature(),
                                biomeConfig.getGenerationSettings().getIsleInBiomes(),
                                biomeConfig.getGenerationSettings().getBorderInBiomes(),
                                biomeConfig.getGenerationSettings().getOnlyBorderNear(),
                                biomeConfig.getGenerationSettings().getNotBorderNear()
                        )
                );
                borderBiomesAtDepth.put(biomeConfig.getGenerationSettings().getBiomeSizeWhenBorder(), biomesAtDepth);
            }

            // Index BiomeColor for FromImageMode and /otg map
            biomeColorMap.put(biomeConfig.getGenerationSettings().getBiomeMapColor().getColor(), otgBiomeId);

            if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.BIOME_REGISTRY))
            {
                OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.BIOME_REGISTRY, "Registered biome " + resourceLocation.toString() + " | " + biomeConfig.getIdentitySettings().getBiomeName() + " with OTG id " + otgBiomeId);
            }

            currentId += isOceanBiome ? 0 : 1;
        }

        // If the ocean config is null, shift the array downwards to fill id 0
        if (oceanBiomeConfig == null)
        {
            System.arraycopy(presetIdMapping, 1, presetIdMapping, 0, presetIdMapping.length - 1);
        }

        this.globalIdMapping.put(preset.getFolderName(), presetIdMapping);

        preset.updateBiomeIds();

        Set<Integer> biomeDepths = new HashSet<>();
        Map<Integer, List<BiomeGroup>> groupDepths = new HashMap<>();

        // Iterate through the groups and add it to the layer data
        Map<Integer, BiomeGroup> groupRegistry = processBiomeGroups(preset.getFolderName(), presetConfig, biomeConfigsByResourceLocation, biomeConfigsByName, blackListedBiomes, biomeDepths, groupDepths);

        // Set the base data
        BiomeLayerData data = new BiomeLayerData(preset.getPresetFolder(), presetConfig, oceanBiomeConfig, oceanTemperatures, groupRegistry, biomeDepths, groupDepths, isleBiomesAtDepth, borderBiomesAtDepth, worldBiomes, biomeColorMap, presetIdMapping);

        // Set data for this preset
        this.presetGenerationData.put(preset.getFolderName(), data);
    }

    public static Biome createOTGBiome(PresetSettings presetConfig, BiomeConfig biomeConfig, HolderGetter<PlacedFeature> featureHolderGetter, HolderGetter<ConfiguredWorldCarver<?>> carverHolderGetter) {

        BiomeGenerationSettings.Builder generationSettings = new BiomeGenerationSettings.Builder(featureHolderGetter, carverHolderGetter);

        // Mob spawning
        MobSpawnSettings.Builder mobSpawnSettings = createMobSpawnSettings(biomeConfig);

        //BiomeDefaultFeatures.addDefaultCarversAndLakes(generationSettings);


        // Register any Registry() resources to the biome, to be handled by MC.
        for (ConfigFunction<BiomeSettings> res : biomeConfig.getResourceQueue())
        {
            if (res instanceof RegistryResource registryResource)
            {
                GenerationStep.Decoration stage = GenerationStep.Decoration.valueOf(registryResource.getDecorationStage());
                Optional<Holder.Reference<PlacedFeature>> placedFeatureReference = featureHolderGetter.get(ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(registryResource.getFeatureKey())));
                if(
                        placedFeatureReference.isPresent()
                        && placedFeatureReference.get().isBound()
                        && placedFeatureReference.get().unwrapKey().isPresent()
                ) {
                    generationSettings.addFeature(stage, placedFeatureReference.get().unwrapKey().get());
                } else {
                    if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.DECORATION))
                    {
                        OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.DECORATION, "Registry() " + registryResource.getFeatureKey() + " could not be found for biomeconfig " + biomeConfig.getIdentitySettings().getBiomeName());
                    }
                }
            }
        }

        // Add default structures
        // TODO: Find a way to add our biomes to the relevant structure biome tags...


        float temperature = biomeConfig.getVisualSettings().getBiomeTemperature();
        
        float downfall = biomeConfig.getVisualSettings().getBiomeWetness();

        BiomeSpecialEffects.Builder specialEffects = getSpecialEffects(presetConfig, biomeConfig);

        return new Biome.BiomeBuilder()
                .generationSettings(generationSettings.build())
                .mobSpawnSettings(mobSpawnSettings.build())
                .specialEffects(specialEffects.build())
                .downfall(downfall)
                .temperature(temperature)
                .build();
    }

    private static BiomeSpecialEffects.Builder getSpecialEffects(PresetSettings presetConfig, BiomeSettings biomeConfig) {
        BiomeVisualSettings biomeVisualSettings = biomeConfig.getVisualSettings();
        float safeTemperature = biomeConfig.getVisualSettings().getBiomeTemperature();
        if (safeTemperature >= 0.1 && safeTemperature <= 0.2)
        {
            // Avoid temperatures between 0.1 and 0.2, Minecraft restriction
            safeTemperature = safeTemperature >= 1.5 ? 0.2f : 0.1f;
        }

        BiomeSpecialEffects.Builder specialEffects =
                new BiomeSpecialEffects.Builder()
                        .fogColor(
                                (!Objects.equals(biomeVisualSettings.getFogColor(), BiomeVisualSettings.FOG_COLOR.getDefaultValue())
                                        ? biomeVisualSettings.getFogColor()
                                        : presetConfig.getVisualSettings().getFogColor()
                                ).intValue()
                        )
                        .waterFogColor(
                                !Objects.equals(biomeVisualSettings.getWaterFogColor(), BiomeVisualSettings.WATER_FOG_COLOR.getDefaultValue())
                                        ? biomeVisualSettings.getWaterFogColor().intValue()
                                        : 329011
                        )
                        .waterColor(
                                !Objects.equals(biomeVisualSettings.getWaterColor(), BiomeVisualSettings.WATER_COLOR.getDefaultValue())
                                        ? biomeVisualSettings.getWaterColor().intValue()
                                        : 4159204
                        )
                        .skyColor(
                                !Objects.equals(biomeVisualSettings.getSkyColor(), BiomeVisualSettings.SKY_COLOR.getDefaultValue())
                                        ? biomeVisualSettings.getSkyColor().intValue()
                                        : getSkyColorForTemp(safeTemperature)
                        ) // TODO: Sky color is normally based on temp, make a setting for that?
                ;
        //Optional<Holder.Reference<ParticleType<?>>> ambientParticle = getFromRegistry(BuiltInRegistries.PARTICLE_TYPE, Registries.PARTICLE_TYPE, biomeVisualSettings.getParticleType());
        // TODO: Particles have become incredibly tricky to work with, let's avoid this for now

        Optional<Holder.Reference<SoundEvent>> ambientLoopSoundEvent = getFromRegistry(BuiltInRegistries.SOUND_EVENT, Registries.SOUND_EVENT, biomeVisualSettings.getAmbientSound());
        ambientLoopSoundEvent.ifPresent(specialEffects::ambientLoopSound);
        Optional<Holder.Reference<SoundEvent>> ambientMoodSound = getFromRegistry(BuiltInRegistries.SOUND_EVENT, Registries.SOUND_EVENT, biomeVisualSettings.getMoodSound());
        if (ambientMoodSound.isPresent()) {
            AmbientMoodSettings ambientMoodSettings = new AmbientMoodSettings(
                    ambientMoodSound.get(),
                    biomeVisualSettings.getMoodSoundDelay(),
                    biomeVisualSettings.getMoodSearchRange(),
                    biomeVisualSettings.getMoodOffset()
            );
            specialEffects.ambientMoodSound(ambientMoodSettings);
        }
        Optional<Holder.Reference<SoundEvent>> ambientAdditionsSound = getFromRegistry(BuiltInRegistries.SOUND_EVENT, Registries.SOUND_EVENT, biomeVisualSettings.getAdditionsSound());
        if (ambientAdditionsSound.isPresent()) {
            AmbientAdditionsSettings ambientAdditionsSettings = new AmbientAdditionsSettings(
                    ambientAdditionsSound.get(),
                    biomeVisualSettings.getAdditionsTickChance()
            );
            specialEffects.ambientAdditionsSound(ambientAdditionsSettings);
        }
        Optional<Holder.Reference<SoundEvent>> backgroundMusic = getFromRegistry(BuiltInRegistries.SOUND_EVENT, Registries.SOUND_EVENT, biomeVisualSettings.getMusic());
        if (backgroundMusic.isPresent()) {
            Music music = new Music(
                    backgroundMusic.get(),
                    biomeVisualSettings.getMusicMinDelay(),
                    biomeVisualSettings.getMusicMaxDelay(),
                    biomeVisualSettings.isReplaceCurrentMusic()
            );
            specialEffects.backgroundMusic(music);
        }

        if(biomeVisualSettings.getFoliageColor().intValue() != 0xffffff) {
            specialEffects.foliageColorOverride(biomeVisualSettings.getFoliageColor().intValue());
        }

        if(biomeVisualSettings.getGrassColor().intValue() != 0xffffff) {
            specialEffects.grassColorOverride(biomeVisualSettings.getGrassColor().intValue());
        }

        switch(biomeVisualSettings.getGrassColorModifier()) {
            case Swamp:
                specialEffects.grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP);
                break;
            case DarkForest:
                specialEffects.grassColorModifier(BiomeSpecialEffects.GrassColorModifier.DARK_FOREST);
                break;
            default:
                break;
        }

        return specialEffects;
    }

    private static <T> Optional<Holder.Reference<T>> getFromRegistry(Registry<T> registry, ResourceKey<Registry<T>> registryResourceKey, String locationString) {
        try {
            return registry.getHolder(ResourceKey.create(registryResourceKey, new ResourceLocation(locationString)));
        } catch (Exception e) {
            OTGLog.error(CONFIGS, "Could not find registry entry for '" + locationString + "'");
            return Optional.empty();
        }
    }


    private static MobSpawnSettings.Builder createMobSpawnSettings(BiomeConfig biomeConfig) {
        MobSettings mobSettings = biomeConfig.getMergedMobSettings();
        String biomeName = biomeConfig.getIdentitySettings().getBiomeName();
        MobSpawnSettings.Builder mobSpawnInfoBuilder = new MobSpawnSettings.Builder();

        addMobGroup(MobCategory.MONSTER, mobSpawnInfoBuilder, mobSettings.getMonsters(), biomeName);
        addMobGroup(MobCategory.CREATURE, mobSpawnInfoBuilder, mobSettings.getCreatures(), biomeName);
        addMobGroup(MobCategory.WATER_CREATURE, mobSpawnInfoBuilder, mobSettings.getWaterCreatures(), biomeName);
        addMobGroup(MobCategory.AMBIENT, mobSpawnInfoBuilder, mobSettings.getAmbientCreatures(), biomeName);
        addMobGroup(MobCategory.WATER_AMBIENT, mobSpawnInfoBuilder, mobSettings.getWaterAmbientCreatures(), biomeName);
        addMobGroup(MobCategory.MISC, mobSpawnInfoBuilder, mobSettings.getMiscCreatures(), biomeName);
        return mobSpawnInfoBuilder;
    }

    private static void addMobGroup(MobCategory entitiClassification, MobSpawnSettings.Builder mobSpawnInfoBuilder, List<WeightedMobSpawnGroup> mobSpawnGroupList, String biomeName)
    {
        for(WeightedMobSpawnGroup mobSpawnGroup : mobSpawnGroupList)
        {
            Optional<EntityType<?>> entityType = EntityType.byString(mobSpawnGroup.internalName());
            if(entityType.isPresent())
            {
                mobSpawnInfoBuilder.addSpawn(entitiClassification, new MobSpawnSettings.SpawnerData(entityType.get(), mobSpawnGroup.getWeight(), mobSpawnGroup.getMin(), mobSpawnGroup.getMax()));
            } else {
                if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.MOBS))
                {
                    OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.MOBS, "Could not find entity for mob: " + mobSpawnGroup.getMob() + " in BiomeConfig " + biomeName);
                }
            }
        }
    }

    private Map<Integer, BiomeGroup> processBiomeGroups(String presetFolderName, PresetSettings presetConfig, Map<IBiomeResourceLocation, BiomeConfig> biomeConfigsByResourceLocation, Map<String, BiomeConfig> biomeConfigsByName, List<String> blackListedBiomes, Set<Integer> biomeDepths, Map<Integer, List<BiomeGroup>> groupDepths)
    {
        int genDepth = presetConfig.getGenerationSettings().getGenerationDepth();
        Map<Integer, BiomeGroup> groupRegistry = new HashMap<>();
        for (BiomeGroupFunction group : presetConfig.getGenerationSettings().getBiomeGroupManager().getGroups())
        {
            if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.BIOME_REGISTRY))
            {
                OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.BIOME_REGISTRY, "Processing " + group.toString());
            }

            // Initialize biome group data
            List<BiomeData> biomes = new ArrayList<>();

            // init to genDepth as it will have one value per depth
            var totalDepthRarity = new int[genDepth + 1];
            var maxRarityPerDepth = new int[genDepth + 1];

            float totalTemp = 0;

            HashMap<String, BiomeConfig> groupBiomes = new LinkedHashMap<>();

            for (String biomeGroupEntry : group.getBiomes()) {
                BiomeConfig biomeConfig = biomeConfigsByName.get(biomeGroupEntry);
                if(biomeConfig == null)
                {
                    if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.BIOME_REGISTRY))
                    {
                        OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.BIOME_REGISTRY, "Could not find biome " + biomeGroupEntry + " in biome group " + group.getGroupId());
                    }
                    continue;
                }
                groupBiomes.put(biomeGroupEntry, biomeConfig);
            }


            // Add each biome to the group
            for (Entry<String, BiomeConfig> biome : groupBiomes.entrySet())
            {
                if(biome.getValue() != null)
                {
                    BiomeConfig config = biome.getValue();
                    // Make and add the generation data
                    BiomeData newBiomeData = new BiomeData(
                            config.getOTGBiomeID().id(),
                            config.getGenerationSettings().getBiomeRarity(),
                            config.getGenerationSettings().getBiomeSize(),
                            config.getVisualSettings().getBiomeTemperature(),
                            config.getGenerationSettings().getIsleInBiomes(),
                            config.getGenerationSettings().getBorderInBiomes(),
                            config.getGenerationSettings().getOnlyBorderNear(),
                            config.getGenerationSettings().getNotBorderNear()
                    );
                    biomes.add(newBiomeData);

                    // Add the biome size- if it's already there, nothing is done
                    biomeDepths.add(config.getGenerationSettings().getBiomeSize());

                    totalTemp += config.getVisualSettings().getBiomeTemperature();

                    // Add this biome's rarity to the total for its depth in the group
                    totalDepthRarity[config.getGenerationSettings().getBiomeSize()] += config.getGenerationSettings().getBiomeRarity();
                }
            }

            // We have filled out the biome group's totalDepthRarity array, use it to fill the maxRarityPerDepth array
            for (int depth = 0; depth < totalDepthRarity.length; depth++)
            {
                // maxRarityPerDepth is the sum of totalDepthRarity for this and subsequent depths
                for (int j = depth; j < totalDepthRarity.length; j++)
                {
                    maxRarityPerDepth[depth] += totalDepthRarity[j];
                }
            }

            float avgTemp = totalTemp / group.getBiomes().size();
            BiomeGroup bg = new BiomeGroup(group.getGroupId(), group.getGroupRarity(), biomes, avgTemp, totalDepthRarity, maxRarityPerDepth);

            int groupSize = group.getGenerationDepth();

            // Make or get a list for this group depth, then add
            List<BiomeGroup> groupsAtDepth = groupDepths.getOrDefault(groupSize, new ArrayList<>());
            groupsAtDepth.add(bg);

            // Replace entry
            groupDepths.put(groupSize, groupsAtDepth);

            // Register group id
            groupRegistry.put(bg.id, bg);
        }
        return groupRegistry;
    }

    private static int getSkyColorForTemp(float temp) {
        float skyColor = temp / 3.0F;
        skyColor = Mth.clamp(skyColor, -1.0F, 1.0F);
        return Mth.hsvToRgb(0.62222224F - skyColor * 0.05F, 0.5F + skyColor * 0.1F, 1.0F);
    }
}
