package com.pg85.otg.shared.biome;

import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.settings.biome.MobSettings;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.biome.WeightedMobSpawnGroup;
import com.pg85.otg.util.logging.LogCategory;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.*;

public class MobInheritanceHandler {
    private final Set<String> processedBiomes = new HashSet<>();
    private final Registry<Biome> biomeRegistry;
    private final List<BiomeConfig> biomeConfigs;
    private List<String> inheritanceChain;

    private MobInheritanceHandler(Registry<Biome> biomeRegistry, List<BiomeConfig> biomeConfigs) {
        this.biomeRegistry = biomeRegistry;
        this.biomeConfigs = biomeConfigs;
    }

    public static void handleMobInheritance(Registry<Biome> biomeRegistry, List<BiomeConfig> biomeConfigs) {
        MobInheritanceHandler handler = new MobInheritanceHandler(biomeRegistry, biomeConfigs);
        for (BiomeConfig biomeConfig : biomeConfigs) {
            handler.inheritanceChain = new ArrayList<>();
            try {
                handler.handleMobInheritance(biomeConfig);
            } catch (InvalidConfigException e) {
                OTGLog.error(LogCategory.CONFIGS, "Error while handling mob inheritance for biome " + biomeConfig.getIdentitySettings().getBiomeName() + ": " + e.getMessage());
            }
        }
    }

    private void handleMobInheritance(BiomeConfig biomeConfig) throws InvalidConfigException {
        String biomeName = biomeConfig.getIdentitySettings().getBiomeName();
        if (alreadyProcessed(biomeName)) return;

        String parentName = biomeConfig.getMobSettings().getInheritMobsBiomeName();
        if (parentName.isBlank()) {
            biomeConfig.setMergedMobSettings(biomeConfig.getMobSettings());
            return;
        }

        MobSettings child = biomeConfig.getMobSettings();
        MobSettings parent;

        if (parentName.startsWith("otg:") || !parentName.contains(":")) {
            parent = getOTGMobSettings(biomeConfig, parentName);
        } else {
            parent = getVanillaMobSettings(biomeConfig, parentName);
        }

        if (parent == null) {
            biomeConfig.setMergedMobSettings(child);
            return;
        }

        biomeConfig.setMergedMobSettings(
                MobSettings.builder()
                        .monsters(combine("monster", child.getMonsters(), parent.getMonsters()))
                        .creatures(combine("creature", child.getCreatures(), parent.getCreatures()))
                        .waterCreatures(combine("water creature", child.getWaterCreatures(), parent.getWaterCreatures()))
                        .ambientCreatures(combine("ambient creature", child.getAmbientCreatures(), parent.getAmbientCreatures()))
                        .waterAmbientCreatures(combine("water ambient creature", child.getWaterAmbientCreatures(), parent.getWaterAmbientCreatures()))
                        .miscCreatures(combine("misc creatures", child.getMiscCreatures(), parent.getMiscCreatures()))
                        .build()
        );

    }

    private List<WeightedMobSpawnGroup> combine(String type, List<WeightedMobSpawnGroup> child, List<WeightedMobSpawnGroup> parent) {
        List<WeightedMobSpawnGroup> combined = new ArrayList<>();
        combined.addAll(child);
        combined.addAll(parent);
        if (combined.isEmpty()) {
            OTGLog.info(LogCategory.CONFIGS, "No " + type + " spawn data found for biome " + inheritanceChain.get(inheritanceChain.size() - 1));
        }
        return combined;
    }

    private MobSettings getOTGMobSettings(BiomeConfig biomeConfig, String parentName) throws InvalidConfigException {
        BiomeConfig parent = biomeConfigs.stream().filter(b -> b.getIdentitySettings().getBiomeName().equals(parentName)).findFirst().orElse(null);
        if (parent == null) {
            parent = biomeConfigs.stream().filter(b -> b.getRegistryKey().toResourceLocationString().equalsIgnoreCase(parentName)).findFirst().orElse(null);
        }
        if (parent == null) {
            OTGLog.error("Parent biome " + parentName + " not found for biome " + biomeConfig.getIdentitySettings().getBiomeName());
            return null;
        } else {
            handleMobInheritance(parent);
            return parent.getMergedMobSettings();
        }
    }

    private MobSettings getVanillaMobSettings(BiomeConfig biomeConfig, String parentName) {
        Optional<Biome> optionalParent = biomeRegistry.getOptional(ResourceKey.create(Registries.BIOME, new ResourceLocation(parentName.toLowerCase(Locale.ROOT))));
        if (optionalParent.isPresent()) {
            MobSettings.MobSettingsBuilder builder = MobSettings.builder();
            Biome parent = optionalParent.get();
            for (MobCategory category : MobCategory.values()) {
                switch (category) {
                    case MONSTER -> builder.monsters(
                            mapToMobSpawnGroup(parent.getMobSettings().getMobs(MobCategory.MONSTER).unwrap())
                    );
                    case CREATURE -> builder.creatures(
                            mapToMobSpawnGroup(parent.getMobSettings().getMobs(MobCategory.CREATURE).unwrap())
                    );
                    case WATER_CREATURE -> builder.waterCreatures(
                            mapToMobSpawnGroup(parent.getMobSettings().getMobs(MobCategory.WATER_CREATURE).unwrap())
                    );
                    case AMBIENT -> builder.ambientCreatures(
                            mapToMobSpawnGroup(parent.getMobSettings().getMobs(MobCategory.AMBIENT).unwrap())
                    );
                    case WATER_AMBIENT -> builder.waterAmbientCreatures(
                            mapToMobSpawnGroup(parent.getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap())
                    );
                    case MISC -> builder.miscCreatures(
                            mapToMobSpawnGroup(parent.getMobSettings().getMobs(MobCategory.MISC).unwrap())
                    );
                }
            }
            return builder.build();
        }
        OTGLog.warn("Parent biome " + parentName + " not found for biome " + biomeConfig.getIdentitySettings().getBiomeName());
        return null;
    }

    private List<WeightedMobSpawnGroup> mapToMobSpawnGroup(List<MobSpawnSettings.SpawnerData> spawnerData) {
        List<WeightedMobSpawnGroup> mobSpawnGroups = new ArrayList<>();
        for (MobSpawnSettings.SpawnerData data : spawnerData) {
            mobSpawnGroups.add(new WeightedMobSpawnGroup(
                    BuiltInRegistries.ENTITY_TYPE.getKey(data.type).toString(),
                    data.getWeight().asInt(),
                    data.minCount,
                    data.maxCount));
        }
        return mobSpawnGroups;
    }

    private boolean alreadyProcessed(String biomeName) throws InvalidConfigException {
        inheritanceChain.add(biomeName);
        if (inheritanceChain.stream().filter(biomeName::equals).count() > 1) {
            throw new InvalidConfigException("Biome inheritance loop detected: " + String.join(" -> ", inheritanceChain));
        }

        if (processedBiomes.contains(biomeName)) {
            return true;
        }
        processedBiomes.add(biomeName);
        return false;
    }



}
