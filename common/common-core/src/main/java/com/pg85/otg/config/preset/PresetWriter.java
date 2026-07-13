package com.pg85.otg.config.preset;

import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.settings.preset.*;
import com.pg85.otg.constants.Constants;

public class PresetWriter {
    static void writePresetConfig(PresetConfig presetConfig, SettingsMap writer) {
        writer.header1("PresetConfig",
                "Contains settings which affect the entire world, biome specific settings can be found in the Biome Configs.",
                "This file controls biome groupings, ocean and land sizes/rarities, river settings, cave and canyon distribution,",
                "vanilla minecraft structure spawning, sea level, dimension/portal settings and more."
        );

        writer.putSetting(Constants.ConfigVersionSetting, Constants.ConfigVersion);

        writer.header2("Config Writing");

        writer.putSetting(PresetInfo.SETTINGS_MODE, presetConfig.getPresetInfo().getSettingsMode(),
                "Each time " + Constants.MOD_ID + " reads the config files it can also write to them. With this setting you can change how this behaves. Possible modes:",
                "WriteAll - Auto-update settings from old versions, order them, add comments, reset invalid settings and remove custom comments. (Recommended)",
                "WriteWithoutComments - Same as WriteAll, but removes all comments, both the ones added by OTG and custom ones. Removing comments is a recommended optimization for release versions of presets.",
                "WriteDisable - Doesn't write to the config files. Errors are not corrected, old settings are read but are not corrected. Custom comments won't be removed with this mode."
        );

        writer.header2("Preset Identity");

        writer.putSetting(PresetInfo.DISPLAY_NAME, presetConfig.getPresetInfo());

        writer.putSetting(PresetInfo.AUTHOR, presetConfig.getPresetInfo().getAuthor(),
                "The author of this preset"
        );

        writer.putSetting(PresetInfo.DESCRIPTION, presetConfig.getPresetInfo().getDescription(),
                "A short description of this preset"
        );

        writer.putSetting(PresetInfo.MAJOR_VERSION, presetConfig.getPresetInfo().getMajorVersion(),
                "The preset major version. Increasing the minor version makes the PresetPacker overwrite,",
                "while increasing the major version will make the PresetPacker save a new copy"
        );

        writer.putSetting(PresetInfo.MINOR_VERSION, presetConfig.getPresetInfo().getMinorVersion(),
                "The preset minor version. Increasing the minor version makes the PresetPacker overwrite,",
                "while increasing the major version will make the PresetPacker save a new copy"
        );

        writer.putSetting(PresetInfo.REGISTRY_NAME, presetConfig.getPresetInfo().getRegistryName(),
                "The shortened name for the preset, used in biome resource locations and similar"
        );

        writer.putSetting(PresetInfo.SELECTABLE_IN_WORLD_CREATION, presetConfig.getPresetInfo().isSelectableInWorldCreation(),
                "Whether this preset should be selectable in the world creation screen."
        );

        writer.header2("Visual Settings",
                "Controls the world's fog colors. Sky, grass and foliage colors are defined inside the biome configs."
        );

        writer.putSetting(VisualSettings.PRESET_FOG_COLOR, presetConfig.getVisualSettings().getFogColor(),
                "Color of the distance fog, can be overridden per biome."
        );

        writer.header2("Biome Modes");

        writer.putSetting(GenerationSettings.BIOME_MODE, presetConfig.getGenerationSettings().getBiomeMode(),
                "Possible biome modes:",
                "Normal - standard random generation with biome groups, uses all features.",
                "FromImage - biome layout defined by an image file."
        );

        writer.header1("Settings for BiomeMode: Normal");

        writer.putSetting(GenerationSettings.GENERATION_DEPTH, presetConfig.getGenerationSettings().getGenerationDepth(),
                "Defines the maximum number BiomeSize, RiverSize and LandSize can be set to.",
                "All size settings such as Biome Group Size, RiverSize, LandSize (in the PresetConfig.ini), and BiomeSize (in Biome Configs) must be between 0 (largest) and GenerationDepth (smallest).",
                "Increasing GenerationDepth by one will roughly double the size of all biomes, similarly decreasing it by 1 will half the size of all biomes.",
                "Small values (1-2) and Large values (20+) may affect generator performance.",
                "This setting is also used in BiomeMode:FromImage when ImageMode is set to ContinueNormal"
        );

        writer.putSetting(GenerationSettings.BIOME_RARITY_SCALE, presetConfig.getGenerationSettings().getBiomeRarityScale(),
                "Max biome rarity from 1 to infinity. By default this is 100, but you can raise it for fine-grained control, or to create biomes with a chance of occurring smaller than 1/100."
        );

        writer.putSetting(GenerationSettings.OLD_GROUP_RARITY, presetConfig.getGenerationSettings().isOldGroupRarity(),
                "Whether or not OTG should use the old group rarity"
        );

        writer.putSetting(GenerationSettings.OLD_LAND_RARITY, presetConfig.getGenerationSettings().isOldLandRarity(),
                "Whether or not OTG should use the old land rarity. Disabling this will make LandRarity work as a percentage"
        );

        writer.header2("Template Biomes",
                "Template biomes allow you to include non-Open Terrain Generator (OTG) biomes, which can be from vanilla Minecraft or other mods, in your OTG presets.",
                "",
                "Syntax: TemplateBiome(BiomeConfigName, BiomeRegistryName or Tags/Categories[, more BiomeRegistryName or Tags/Categories[, ...]], minTemperature, maxTemperature)",
                "BiomeConfigName: The name of the corresponding biome configuration. This is case sensitive.",
                "BiomeRegistryName: The registry name of a non-OTG biome. For example, \"minecraft:plains\".",
                "Tags/Categories: Instead of using the BiomeRegistryName, you can use Forge Biome Dictionary IDs or Minecraft Biome Categories.",
                "",
                "OTG fetches all non-OTG biomes that match the specified category/tags and associates them with the BiomeConfig. The BiomeConfig must have the setting 'TemplateForBiome' set to true, or it will be ignored.",
                "",
                "Example: TemplateBiome(MCForest, category.forest tag.overworld)",
                "This adds all forest biomes in the overworld to the 'MCForest' biome configuration. Biomes are never added twice.",
                "",
                "Use space as an AND operator. For example, \"category.forest tag.overworld\" matches biomes with both the 'forest' category and the 'overworld' tag.",
                "To target both Minecraft and modded biomes, use \"category.\" or \"tag.\".",
                "To target only modded biomes, use \"modcategory.\" or \"modtag.\".",
                "To target only Minecraft biomes, use \"mccategory.\" or \"mctag.\".",
                "To filter biomes for a specific mod, add \"mod.<namespace>\". For example, \"mod.byg category.plains tag.overworld\".",
                "To exclude specific biome registry names, tags, categories or mods, use \"-\". For example, -tag.overworld to exclude overworld biomes.",
                "",
                "MinTemperature/MaxTemperature: Optional parameters. Only biomes within this temperature range are allowed.",
                "Example: TemplateBiome(TagPlains, category.plains -tag.overworld) or TemplateBiome(TagPlains, category.plains -tag.overworld, -0.2, 0.2)",
                "The first example targets a BiomeConfig named 'TagPlains.bc', and adds to it all non-OTG biomes that are of category \"plains\" but do not have the 'overworld' tag.",
                "The second example does the same, but also includes a temperature range between -0.2 and 0.2.",
                "",
                "Note:",
                "Each biome can only be assigned to one biome config, so the order of TemplateBiome()s is important. Put your most specific TemplateBiome first, and the most generic last.",
                "When using BiomeRegistryName to include or exclude a biome, it must have its own entry. For example: \",minecraft:forest,-minecraft:plains,\""
        );

        writer.addConfigFunctions(presetConfig.getGenerationSettings().getTemplateBiomes());

        writer.header2("Biome Groups",
                "Biome groups are a way to group similar biomes together, ensuring they spawn adjacent to each other. Only standard biomes need to be part of these groups, while isle, border, and river biomes are configured separately.",
                "",
                "Syntax: BiomeGroup(GroupName, GroupSize, GroupRarity, BiomeName or Tags/Categories[, AnotherName[, ...]], minTemperature, maxTemperature)",
                "GroupName: A unique, descriptive name for the group.",
                "GroupSize: A value from 0 to GenerationDepth. A lower number results in a larger group. All biomes in the group must have a BiomeSize number equal to or higher than this value.",
                "GroupRarity: The relative spawn chance of the group.",
                "BiomeName: The name of a corresponding biome configuration. This is case sensitive. It can also be a registry name (e.g., \"minecraft:plains\") if there is an associated TemplateBiome().",
                "If the biome configuration is a template biome, all associated non-OTG biomes are added to the group.",
                "Tags/Categories: Instead of BiomeName, you can use Forge Biome Dictionary IDs or Minecraft Biome Categories.",
                "",
                "OTG fetches all non-OTG biomes that match the specified category/tags and adds them to the biome group. A TemplateBiome() that targets the biome must exist, or it will be ignored.",
                "",
                "Example: BiomeGroup(NormalBiomes, 1, 100, category.plains tag.overworld, tag.hot tag.dry)",
                "This adds two entries: all plains biomes in the overworld, and all hot and dry biomes. Biomes are never added twice.",
                "",
                "Use space as an AND operator. For example, \"category.plains tag.overworld\" matches biomes with both the 'plains' category and the 'overworld' tag.",
                "To target both Minecraft and modded biomes, use \"category.\" or \"tag.\".",
                "To target only modded biomes, use \"modcategory.\" or \"modtag.\".",
                "To target only Minecraft biomes, use \"mccategory.\" or \"mctag.\".",
                "To filter biomes for a specific mod, add \"mod.<namespace>\". For example, \"mod.byg category.plains tag.overworld\".",
                "To exclude specific biome registry names, tags, categories, or mods, use \"-\". For example, -tag.overworld to exclude overworld biomes.",
                "",
                "MinTemperature/MaxTemperature: Optional parameters. When using Tags/Categories, only biomes within this temperature range are used.",
                "Example: BiomeGroup(NormalBiomes, 1, 100, category.plains tag.overworld, tag.hot tag.dry, -1.0, 1.0)",
                "This is the same as the previous example, but it only includes biomes with a temperature between -1.0 and 1.0.",
                "",
                "Note:",
                "When using BiomeRegistryName to include or exclude a biome, it must have its own entry. For example: \",minecraft:forest,-minecraft:plains,\""
        );

        writer.addConfigFunctions(presetConfig.getGenerationSettings().getBiomeGroupManager().getGroups());

        writer.putSetting(GenerationSettings.BLACKLISTED_BIOMES, presetConfig.getGenerationSettings().getBlackListedBiomes(),
                "When using biome dictionary tags and/or biome categories with biome groups, these (non-OTG) biomes are excluded. Example: minecraft:plains."
        );

        writer.header2("Isle & Border Biomes");

        writer.putSetting(GenerationSettings.ISLE_BIOMES, presetConfig.getGenerationSettings().getIsleBiomes(),
                "Isle biomes are biomes which spawn inside another biome (e.g. an island in an ocean). As well as listing every isle biome here, you must set IsleInBiome in each biome config too. Biome name is case sensitive."
        );

        writer.putSetting(GenerationSettings.BORDER_BIOMES, presetConfig.getGenerationSettings().getBorderBiomes(),
                "Biomes used as borders of other biomes. As well as listing every border biome here, you must set BiomeIsBorder in each biome config too. Biome name is case sensitive."
        );

        writer.header2("Landmass Settings");

        writer.putSetting(GenerationSettings.LAND_RARITY, presetConfig.getGenerationSettings().getLandRarity(),
                "Land rarity from 100 to 1. Higher numbers result in more land."
        );

        writer.putSetting(GenerationSettings.LAND_SIZE, presetConfig.getGenerationSettings().getLandSize(),
                "Land size from 0 to GenerationDepth. Higher LandSize numbers will make the size of the land smaller. Landsize number should always be lower than any biome groups."
        );

        writer.putSetting(GenerationSettings.FORCE_LAND_AT_SPAWN, presetConfig.getGenerationSettings().isForceLandAtSpawn(),
                "If enabled, land will always spawn at or near 0,0"
        );

        writer.putSetting(GenerationSettings.OCEAN_BIOME_SIZE, presetConfig.getGenerationSettings().getOceanBiomeSize(),
                "Ocean biome size 0 to GenerationDepth. Higher OceanBiomeSize numbers will make the size of the ocean biomes smaller."
        );

        writer.putSetting(GenerationSettings.LAND_FUZZY, presetConfig.getGenerationSettings().getLandFuzzy(),
                "Generates more lakes (via small ocean biomes) at the edges of continents. As a side effect, the continent will also get a bit larger. Must be from 0 to GenerationDepth minus LandSize."
        );

        writer.putSetting(GenerationSettings.DEFAULT_OCEAN_BIOME, presetConfig.getGenerationSettings().getDefaultOceanBiome(),
                "Set the default Ocean biome for this world."
        );

        writer.putSetting(GenerationSettings.DEFAULT_WARM_OCEAN_BIOME, presetConfig.getGenerationSettings().getDefaultWarmOceanBiome(),
                "Set the default Warm Ocean biome for this world."
        );

        writer.putSetting(GenerationSettings.DEFAULT_LUKEWARM_OCEAN_BIOME, presetConfig.getGenerationSettings().getDefaultLukewarmOceanBiome(),
                "Set the default Lukewarm Ocean biome for this world."
        );

        writer.putSetting(GenerationSettings.DEFAULT_COLD_OCEAN_BIOME, presetConfig.getGenerationSettings().getDefaultColdOceanBiome(),
                "Set the default Cold Ocean biome for this world."
        );

        writer.putSetting(GenerationSettings.DEFAULT_FROZEN_OCEAN_BIOME, presetConfig.getGenerationSettings().getDefaultFrozenOceanBiome(),
                "The default Frozen Ocean biome for this world."
        );

        writer.header2("Ice Area Settings");

        writer.putSetting(GenerationSettings.FROZEN_OCEAN, presetConfig.getGenerationSettings().isFrozenOcean(),
                "Can be true or false, makes the water of the oceans near a cold biome frozen. The definition of 'cold' is controlled by the next setting.",
                "Set this to false to stop the ocean from freezing near when an \"ice area\" intersects with an ocean."
        );

        writer.putSetting(GenerationSettings.FROZEN_OCEAN_TEMPERATURE, presetConfig.getGenerationSettings().getFrozenOceanTemperature(),
                "This is the maximum biome temperature when a biome is still considered cold. Water in oceans nearby cold biomes freezes if FrozenOcean is set to true.",
                "Temperature reference from vanilla Minecraft: < 0.15 for snow, 0.15 - 0.95 for rain, or > 1.0 for dry."
        );

        writer.header2("Rivers");

        writer.putSetting(GenerationSettings.RIVERS_ENABLED, presetConfig.getGenerationSettings().isRiversEnabled(),
                "Set this to false to prevent the river generator from doing anything."
        );

        writer.putSetting(GenerationSettings.RANDOM_RIVERS, presetConfig.getGenerationSettings().isRandomRivers(),
                "When this setting is false, rivers follow the biome borders most of the time. Set this setting to true to disable this behavior."
        );
        writer.putSetting(GenerationSettings.RIVER_RARITY, presetConfig.getGenerationSettings().getRiverRarity(),
                "Controls the rarity of rivers. Must be from 0 to GenerationDepth. A higher number means more rivers. To define which rivers flow through which biomes see the individual biome configs."
        );

        writer.putSetting(GenerationSettings.RIVER_SIZE, presetConfig.getGenerationSettings().getRiverSize(),
                "Controls the size of rivers. Can range from 0 to GenerationDepth minus RiverRarity. Making this larger will make the rivers larger, without affecting how often rivers will spawn."
        );

        writer.header1("Settings For BiomeMode:FromImage",
                "In each of the BiomeConfigs there is a BiomeColor variable, this variable is the hexadecimal color of the biome.",
                "These colors are used to define the biome layout in the input image (as well as the colour of the biome when using the /otg map command). Two biomes must not have the same color.",
                "The settings in this section are for FromImage mode only."
        );

        writer.putSetting(ImageSettings.IMAGE_MODE, presetConfig.getImageSettings().getImageMode(),
                "Defines what to do when terrain is generated outside the boundaries of the image:",
                "Repeat - repeats the image",
                "Mirror - repeats and mirrors the image",
                "ContinueNormal - continues with random generation, using settings for BiomeMode: Normal",
                "FillEmpty - fills the space with one biome (defined below)"
        );

        writer.putSetting(ImageSettings.IMAGE_FILE, presetConfig.getImageSettings().getImageFile(),
                "The image which will provide the Biomes must be a PNG file without transparency, once placed in the same folder as PresetConfig.ini OTG will use it as a reference for the Biomes generation.",
                "Source png file name for FromImage biome mode."
        );

        writer.putSetting(ImageSettings.IMAGE_ORIENTATION, presetConfig.getImageSettings().getImageOrientation(),
                "How the image is oriented: North, South, East or West. When this is set to North, the top of your picture is north (no rotation).",
                "When it is set to East, the image is rotated 90 degrees counter-clockwise, therefore what is on the east in the image becomes north in the world.",
                "Possible values: North, East, South, West."
        );

        writer.putSetting(ImageSettings.IMAGE_FILL_BIOME, presetConfig.getImageSettings().getImageFillBiome(),
                "Biome name for filling outside image boundaries with FillEmpty mode."
        );

        writer.putSetting(ImageSettings.IMAGE_X_OFFSET, presetConfig.getImageSettings().getImageXOffset(),
                "Translates the map origin. This number needs to be multiplied by -1 when using FillEmpty."
        );

        writer.putSetting(ImageSettings.IMAGE_Z_OFFSET, presetConfig.getImageSettings().getImageZOffset(),
                "Translates the map origin. This number needs to be multiplied by -1 when using FillEmpty."
        );

        writer.header1("Terrain Height and Volatility",
                "The settings in this section control terrain settings that are not specific to any biome."
        );

        writer.putSetting(TerrainSettings.WORLD_HEIGHT_SCALE, presetConfig.getTerrainSettings());
        writer.putSetting(TerrainSettings.MIN_Y, presetConfig.getTerrainSettings());
        writer.putSetting(TerrainSettings.HEIGHT, presetConfig.getTerrainSettings());

        writer.putSetting(TerrainSettings.FRACTURE_HORIZONTAL, presetConfig.getTerrainSettings());

        writer.putSetting(TerrainSettings.FRACTURE_VERTICAL, presetConfig.getTerrainSettings());

        writer.putSetting(TerrainSettings.CONTINENTAL_SCALE, presetConfig.getTerrainSettings());
        writer.putSetting(TerrainSettings.CONTINENTAL_BIAS, presetConfig.getTerrainSettings());
        writer.putSetting(TerrainSettings.BASE_HEIGHT_FRACTION, presetConfig.getTerrainSettings());
        writer.putSetting(TerrainSettings.BIOME_HEIGHT_WEIGHT, presetConfig.getTerrainSettings());
        writer.putSetting(TerrainSettings.CONTINENTAL_HEIGHT_WEIGHT, presetConfig.getTerrainSettings());
        writer.putSetting(TerrainSettings.FALLOFF_STEEPNESS, presetConfig.getTerrainSettings());
        writer.putSetting(TerrainSettings.NOISE_AMPLITUDE, presetConfig.getTerrainSettings());

        writer.header1("Blocks");

        writer.putSetting(BlockSettings.REMOVE_SURFACE_STONE, presetConfig.getBlockSettings().isRemoveSurfaceStone(),
                "Set this to true to place the biome surface block on top of all exposed stone."
        );

        writer.header2("Bedrock");

        writer.putSetting(BlockSettings.BEDROCK_BLOCK, presetConfig.getBlockSettings().getBedrockBlock(),
                "Block used as bedrock."
        );

        writer.putSetting(BlockSettings.DISABLE_BEDROCK, presetConfig.getBlockSettings().isDisableBedrock(),
                "Disable bottom of map bedrock generation. Doesn't affect bedrock on the ceiling of the map."
        );

        writer.putSetting(BlockSettings.CEILING_BEDROCK, presetConfig.getBlockSettings().isCeilingBedrock(),
                "Enable ceiling of map bedrock generation."
        );

        writer.putSetting(BlockSettings.FLAT_BEDROCK, presetConfig.getBlockSettings().isFlatBedrock(),
                "Make a single flat layer of bedrock."
        );

        writer.header2("Water / Lava / Frozen States");

        writer.putSetting(TerrainSettings.WATER_LEVEL_MAX, presetConfig.getTerrainSettings().getWaterLevelMax(),
                "Set water level. Every empty block under this level will be fill water or another block from WaterBlock."
        );

        writer.putSetting(TerrainSettings.WATER_LEVEL_MIN, presetConfig.getTerrainSettings().getWaterLevelMin());

        writer.putSetting(BlockSettings.WATER_BLOCK, presetConfig.getBlockSettings().getWaterBlock(),
                "Block used as water in WaterLevel."
        );

        writer.putSetting(BlockSettings.ICE_BLOCK, presetConfig.getBlockSettings().getIceBlock(),
                "Block used as ice."
        );

        writer.putSetting(BlockSettings.COOLED_LAVA_BLOCK, presetConfig.getBlockSettings().getCooledLavaBlock(),
                "Block used as cooled or frozen lava.",
                "Set this to OBSIDIAN for \"frozen\" lava lakes in cold biomes"
        );

        writer.putSetting(BlockSettings.DEFAULT_STONE_BLOCK, presetConfig.getBlockSettings().getDefaultStoneBlock(),
                "Block used as stone in biomes where stone block is not specified."
        );

        writer.putSetting(TerrainSettings.BETTER_SNOW_FALL, presetConfig.getTerrainSettings().isBetterSnowFall(),
                "When set to false, 1 layer of snow falls on the highest block only.",
                "When set to true, the number of layers (1-8) is dependent on biome temperature.",
                "Higher altitudes have lower temperatures, so snow becomes deeper higher up.",
                "Also causes snow to fall through leaves, leaves can carry 3 layers while the rest falls through."
        );

        writer.header1("Resources");

        writer.putSetting(ResourceSettings.DISABLE_OREGEN, presetConfig.getResourceSettings().isDisableOreGen(),
                "Disables Ore(), UnderWaterOre() and Vein() biome resources that use any type of ore block."
        );

        // Structures

        writer.header1("Structures",
                "These are global on/off toggles and spacing/separation settings for the entire world for each",
                "vanilla structure type. Spacing/separation work the same way as they do for datapacks.",
                "When set to true, structures configured in biome configs are able to spawn.",
                "Check the biome configs for customisation options per structure type per biome (size etc)."
        );
        var structureSettings = presetConfig.getStructureSettings();
        writer.putSetting(StructureSettings.VILLAGES_ENABLED, structureSettings.isVillagesEnabled());
        writer.putSetting(StructureSettings.VILLAGE_SPACING, structureSettings.getVillageSpacing());
        writer.putSetting(StructureSettings.VILLAGE_SEPARATION, structureSettings.getVillageSeparation());
        writer.putSetting(StructureSettings.MINESHAFTS_ENABLED, structureSettings.isMineshaftsEnabled());
        writer.putSetting(StructureSettings.MINESHAFT_SPACING, structureSettings.getMineshaftSpacing());
        writer.putSetting(StructureSettings.MINESHAFT_SEPARATION, structureSettings.getMineshaftSeparation());
        writer.putSetting(StructureSettings.STRONGHOLDS_ENABLED, structureSettings.isStrongholdsEnabled());
        writer.putSetting(StructureSettings.STRONGHOLD_SPACING, structureSettings.getStrongholdSpacing());
        writer.putSetting(StructureSettings.STRONGHOLD_SEPARATION, structureSettings.getStrongholdSeparation());
        writer.putSetting(StructureSettings.STRONGHOLD_DISTANCE, structureSettings.getStrongholdDistance());
        writer.putSetting(StructureSettings.STRONGHOLD_SPREAD, structureSettings.getStrongholdSpread());
        writer.putSetting(StructureSettings.STRONGHOLD_COUNT, structureSettings.getStrongholdCount());
        writer.putSetting(StructureSettings.RARE_BUILDINGS_ENABLED, structureSettings.isRareBuildingsEnabled());
        writer.putSetting(StructureSettings.DESERTPYRAMID_SPACING, structureSettings.getDesertPyramidSpacing());
        writer.putSetting(StructureSettings.DESERTPYRAMID_SEPARATION, structureSettings.getDesertPyramidSeparation());
        writer.putSetting(StructureSettings.IGLOO_SPACING, structureSettings.getIglooSpacing());
        writer.putSetting(StructureSettings.IGLOO_SEPARATION, structureSettings.getIglooSeparation());
        writer.putSetting(StructureSettings.JUNGLETEMPLE_SPACING, structureSettings.getJungleTempleSpacing());
        writer.putSetting(StructureSettings.JUNGLETEMPLE_SEPARATION, structureSettings.getJungleTempleSeparation());
        writer.putSetting(StructureSettings.SWAMPHUT_SPACING, structureSettings.getSwampHutSpacing());
        writer.putSetting(StructureSettings.SWAMPHUT_SEPARATION, structureSettings.getSwampHutSeparation());
        writer.putSetting(StructureSettings.WOODLAND_MANSIONS_ENABLED, structureSettings.isWoodlandMansionsEnabled());
        writer.putSetting(StructureSettings.WOODLANDMANSION_SPACING, structureSettings.getWoodlandMansionSpacing());
        writer.putSetting(StructureSettings.WOODLANDMANSION_SEPARATION, structureSettings.getWoodlandMansionSeparation());
        writer.putSetting(StructureSettings.OCEAN_MONUMENTS_ENABLED, structureSettings.isOceanMonumentsEnabled());
        writer.putSetting(StructureSettings.OCEANMONUMENT_SPACING, structureSettings.getOceanMonumentSpacing());
        writer.putSetting(StructureSettings.OCEANMONUMENT_SEPARATION, structureSettings.getOceanMonumentSeparation());
        writer.putSetting(StructureSettings.NETHER_FORTRESSES_ENABLED, structureSettings.isNetherFortressesEnabled());
        writer.putSetting(StructureSettings.NETHERFORTRESS_SPACING, structureSettings.getNetherFortressSpacing());
        writer.putSetting(StructureSettings.NETHERFORTRESS_SEPARATION, structureSettings.getNetherFortressSeparation());
        writer.putSetting(StructureSettings.BURIED_TREASURE_ENABLED, structureSettings.isBuriedTreasureEnabled());
        writer.putSetting(StructureSettings.BURIEDTREASURE_SPACING, structureSettings.getBuriedTreasureSpacing());
        writer.putSetting(StructureSettings.BURIEDTREASURE_SEPARATION, structureSettings.getBuriedTreasureSeparation());
        writer.putSetting(StructureSettings.OCEAN_RUINS_ENABLED, structureSettings.isOceanRuinsEnabled());
        writer.putSetting(StructureSettings.OCEANRUIN_SPACING, structureSettings.getOceanRuinSpacing());
        writer.putSetting(StructureSettings.OCEANRUIN_SEPARATION, structureSettings.getOceanRuinSeparation());
        writer.putSetting(StructureSettings.PILLAGER_OUTPOSTS_ENABLED, structureSettings.isPillagerOutpostsEnabled());
        writer.putSetting(StructureSettings.PILLAGEROUTPOST_SPACING, structureSettings.getPillagerOutpostSpacing());
        writer.putSetting(StructureSettings.PILLAGEROUTPOST_SEPARATION, structureSettings.getPillagerOutpostSeparation());
        writer.putSetting(StructureSettings.BASTION_REMNANTS_ENABLED, structureSettings.isBastionRemnantsEnabled());
        writer.putSetting(StructureSettings.BASTIONREMNANT_SPACING, structureSettings.getBastionRemnantSpacing());
        writer.putSetting(StructureSettings.BASTIONREMNANT_SEPARATION, structureSettings.getBastionRemnantSeparation());
        writer.putSetting(StructureSettings.NETHER_FOSSILS_ENABLED, structureSettings.isNetherFossilsEnabled());
        writer.putSetting(StructureSettings.NETHERFOSSIL_SPACING, structureSettings.getNetherFossilSpacing());
        writer.putSetting(StructureSettings.NETHERFOSSIL_SEPARATION, structureSettings.getNetherFossilSeparation());
        writer.putSetting(StructureSettings.END_CITIES_ENABLED, structureSettings.isEndCitiesEnabled());
        writer.putSetting(StructureSettings.ENDCITY_SPACING, structureSettings.getEndCitySpacing());
        writer.putSetting(StructureSettings.ENDCITY_SEPARATION, structureSettings.getEndCitySeparation());
        writer.putSetting(StructureSettings.RUINED_PORTALS_ENABLED, structureSettings.isRuinedPortalsEnabled());
        writer.putSetting(StructureSettings.RUINEDPORTAL_SPACING, structureSettings.getRuinedPortalSpacing());
        writer.putSetting(StructureSettings.RUINEDPORTAL_SEPARATION, structureSettings.getRuinedPortalSeparation());
        writer.putSetting(StructureSettings.SHIPWRECKS_ENABLED, structureSettings.isShipwrecksEnabled());
        writer.putSetting(StructureSettings.SHIPWRECK_SPACING, structureSettings.getShipwreckSpacing());
        writer.putSetting(StructureSettings.SHIPWRECK_SEPARATION, structureSettings.getShipwreckSeparation());

        writer.header2("OTG Custom structures and objects (BO2/BO3/BO4)");

        writer.putSetting(ResourceSettings.CUSTOM_STRUCTURE_TYPE, presetConfig.getResourceSettings().getCustomStructureType(),
                "Sets the type of structures the world should spawn, BO3 or BO4.",
                "Allowed values: BO3/BO4.",
                "BO4's allow for collision detection, fine control over structure distribution, advanced branching mechanics for",
                "procedurally generated structures, smoothing areas, extremely large structures, settings for blending structures",
                "with surrounding terrain, etc. BO3's are simpler, seed based CustomStructures, more like vanilla mc structures.",
                "Worlds currently can only use one type of structure."
        );

        writer.putSetting(ResourceSettings.BO3_AT_SPAWN, presetConfig.getResourceSettings().getBO3AtSpawn(),
                "This BO3 will be spawned at the world's spawn point as a CustomObject (Max size 32x32)."
        );

        writer.header2("BO3 Custom structures");

        writer.putSetting(ResourceSettings.USE_OLD_BO3_STRUCTURE_RARITY, presetConfig.getResourceSettings().isUseOldBO3StructureRarity(),
                "For 1.12.2 v9.0_r11 and earlier, BO3 customstructures used 2 rarity rolls,",
                "one for the rarity in the CustomStructure() tag, one for the rarity in the BO3 itself.",
                "For 1.16, we use only the rarity roll from the CustomStructure() tag. Set this to true",
                "to use the old system."
        );

        writer.putSetting(ResourceSettings.MAXIMUM_CUSTOM_STRUCTURE_RADIUS, presetConfig.getResourceSettings().getMaximumCustomStructureRadius(),
                "Maximum radius of custom structures in chunks. Custom structures are spawned by",
                "the CustomStructure resource in the biome configuration files. Not used for BO4's."
        );

        writer.putSetting(ResourceSettings.DECORATION_BOUNDS_CHECK, presetConfig.getResourceSettings().isDecorationBoundsCheck(),
                "Set this to false to disable the bounds check during chunk decoration.",
                "While this allows you to spawn objects larger than 32x32, it also makes terrain generation dependent on the direction you explored the world in."
        );

        writer.header1("Carvers: Caves and Ravines");

        writer.putSetting(BlockSettings.CARVER_LAVA_BLOCK, presetConfig.getBlockSettings().getCarverLavaBlock(),
                "Block that replaces all air blocks from Y0 up to CarverLavaBlockHeight.",
                "For example, vanilla replaces air in caves with lava up to Y10.",
                "Defaults to: LAVA"
        );

        writer.putSetting(TerrainSettings.CARVER_LAVA_BLOCK_HEIGHT, presetConfig.getTerrainSettings().getCarverLavaBlockHeight(),
                "All air blocks are replaced to CarverLavaBlock from Y0 up to CarverLavaBlockHeight.",
                "For example, vanilla replaces air in caves with lava up to Y10.",
                "Defaults to: 10"
        );

        writer.header2("Caves");

        writer.putSetting(CarverSettings.CAVES_ENABLED, presetConfig.getCarverSettings().isCavesEnabled(),
                "Enables/disables OTG caves. OTG should automatically disable caves/carvers for biomes when modded carvers are detected."
        );

        writer.putSetting(CarverSettings.CAVE_RARITY, presetConfig.getCarverSettings().getCaveRarity(),
                "This controls the odds that a given chunk will host a single cave and/or the start of a cave system."
        );

        writer.putSetting(CarverSettings.CAVE_FREQUENCY, presetConfig.getCarverSettings().getCaveFrequency(),
                "The number of times the cave generation algorithm will attempt to create single caves and cave",
                "systems in the given chunk. This value is larger because the likelihood for the cave generation",
                "algorithm to bailout is fairly high and it is used in a randomizer that trends towards lower",
                "random numbers. With an input of 40 (default) the randomizer will result in an average random",
                "result of 5 to 6. This can be turned off by setting evenCaveDistribution (below) to true."
        );

        writer.putSetting(CarverSettings.CAVE_MIN_ALTITUDE, presetConfig.getCarverSettings().getCaveMinAltitude(),
                "Sets the minimum and maximum altitudes at which caves will be generated. These values are",
                "used in a randomizer that trends towards lower numbers so that caves become more frequent",
                "the closer you get to the bottom of the map. Setting even cave distribution (above) to true",
                "will turn off this randomizer and use a flat random number generator that will create an even",
                "density of caves at all altitudes."
        );
        writer.putSetting(CarverSettings.CAVE_MAX_ALTITUDE, presetConfig.getCarverSettings().getCaveMaxAltitude());

        writer.putSetting(CarverSettings.INDIVIDUAL_CAVE_RARITY, presetConfig.getCarverSettings().getIndividualCaveRarity(),
                "The odds that the cave generation algorithm will generate a single cavern without an accompanying",
                "cave system. Note that whenever the algorithm generates an individual cave it will also attempt to",
                "generate a pocket of cave systems in the vicinity (no guarantee of connection or that the cave system",
                "will actually be created)."
        );

        writer.putSetting(CarverSettings.CAVE_SYSTEM_FREQUENCY, presetConfig.getCarverSettings().getCaveSystemFrequency(),
                "The number of times the algorithm will attempt to start a cave system in a given chunk per cycle of",
                "the cave generation algorithm (see cave frequency setting above). Note that setting this value too",
                "high with an accompanying high cave frequency value can cause extremely long world generation time."
        );

        writer.putSetting(CarverSettings.CAVE_SYSTEM_POCKET_CHANCE, presetConfig.getCarverSettings().getCaveSystemPocketChance(),
                "This can be set to create an additional chance that a cave system pocket (a higher than normal",
                "density of cave systems) being started in a given chunk. Normally, a cave pocket will only be",
                "attempted if an individual cave is generated, but this will allow more cave pockets to be generated",
                "in addition to the individual cave trigger."
        );

        writer.putSetting(CarverSettings.CAVE_SYSTEM_POCKET_MIN_SIZE, presetConfig.getCarverSettings().getCaveSystemPocketMinSize(),
                "The minimum and maximum size that a cave system pocket can be. This modifies/overrides the",
                "cave system frequency setting (above) when triggered."
        );
        writer.putSetting(CarverSettings.CAVE_SYSTEM_POCKET_MAX_SIZE, presetConfig.getCarverSettings().getCaveSystemPocketMaxSize());

        writer.putSetting(CarverSettings.EVEN_CAVE_DISTRIBUTION, presetConfig.getCarverSettings().isEvenCaveDistribution(),
                "Setting this to true will turn off the randomizer for cave frequency (above). Do note that",
                "if you turn this on you will probably want to adjust the cave frequency down to avoid long",
                "load times at world creation."
        );

        writer.header2("Vanilla noise caves");

        writer.putSetting(CarverSettings.VANILLA_CAVES_ENABLED, presetConfig.getCarverSettings().isVanillaCavesEnabled(),
                "Enables vanilla 1.18+ noise caves (cheese/spaghetti/noodle/pillar caves), carved into OTG's",
                "base terrain via vanilla's density function system. Independent of CavesEnabled/RavinesEnabled,",
                "which control OTG's legacy worm carvers; both can be enabled at once.",
                "Note: with this enabled, stone block replacement comes from surface/ground control layers only,",
                "and caves below a biome's WaterLevelMax will flood (aquifers are not yet supported)."
        );
        writer.putSetting(CarverSettings.VANILLA_CAVE_DENSITY_SCALE, presetConfig.getCarverSettings().getVanillaCaveDensityScale(),
                "Divisor mapping OTG's raw terrain noise onto vanilla's density scale for noise caves.",
                "This controls how strongly terrain density resists carving near the surface (cave",
                "entrances and cave/terrain blending); the depth at which caves open up is controlled",
                "by VanillaCaveDepthGradient instead."
        );
        writer.putSetting(CarverSettings.VANILLA_CAVE_DEPTH_GRADIENT, presetConfig.getCarverSettings().getVanillaCaveDepthGradient(),
                "Density-per-block depth gradient for vanilla noise caves, measured from the blended",
                "terrain center height of each column. This makes cave depth independent of biome",
                "volatility, so caves open up at the same depth in flat and mountainous biomes alike.",
                "Full-size caves start at roughly 1.56/value blocks below the terrain center height",
                "(default 0.05 -> ~31 blocks); cheese caves are fully open from ~2.34/value blocks",
                "(default -> ~47 blocks). Higher values move caves closer to the surface."
        );

        writer.header2("Ravines");

        writer.putSetting(CarverSettings.RAVINES_ENABLED, presetConfig.getCarverSettings().isRavinesEnabled(),
                "Enables/disables OTG ravines. OTG should automatically disable ravines/carvers for biomes when modded carvers are detected."
        );

        writer.putSetting(CarverSettings.RAVINE_RARITY, presetConfig.getCarverSettings().getRavineRarity());
        writer.putSetting(CarverSettings.RAVINE_MIN_ALTITUDE, presetConfig.getCarverSettings().getRavineMinAltitude());
        writer.putSetting(CarverSettings.RAVINE_MAX_ALTITUDE, presetConfig.getCarverSettings().getRavineMaxAltitude());
        writer.putSetting(CarverSettings.RAVINE_MIN_LENGTH, presetConfig.getCarverSettings().getRavineMinLength());
        writer.putSetting(CarverSettings.RAVINE_MAX_LENGTH, presetConfig.getCarverSettings().getRavineMaxLength());
        writer.putSetting(CarverSettings.RAVINE_DEPTH, presetConfig.getCarverSettings().getRavineDepth());

        writer.header1("Spawn point settings");

        writer.putSetting(SpawnSettings.FIXED_SPAWN_POINT, presetConfig.getSpawnSettings().isSpawnPointSet(),
                "Set this to true to enable SpawnPointX/SpawnPointY/SpawnPointZ/SpawnPointAngle."
        );
        writer.putSetting(SpawnSettings.SPAWN_POINT_X, presetConfig.getSpawnSettings().getSpawnPointX(),
                "When FixedSpawnPoint: true, this sets the world's spawn point."
        );
        writer.putSetting(SpawnSettings.SPAWN_POINT_Y, presetConfig.getSpawnSettings().getSpawnPointY(),
                "When FixedSpawnPoint: true, this sets the world's spawn point."
        );
        writer.putSetting(SpawnSettings.SPAWN_POINT_Z, presetConfig.getSpawnSettings().getSpawnPointZ(),
                "When FixedSpawnPoint: true, this sets the world's spawn point."
        );
        writer.putSetting(SpawnSettings.SPAWN_POINT_ANGLE, presetConfig.getSpawnSettings().getSpawnPointAngle(),
                "When FixedSpawnPoint: true, this sets the angle the player is looking when spawned at the spawn point."
        );

        writer.header2("Portal settings (Forge)");

        writer.putSetting(PortalSettings.PORTAL_BLOCKS, presetConfig.getPortalSettings().getPortalBlocks(),
                "A list of one or more portal blocks used to build a portal to this dimension, or back to the overworld.",
                "Only applies for dimensions, not overworld/nether/end."
        );
        writer.putSetting(PortalSettings.PORTAL_COLOR, presetConfig.getPortalSettings().getPortalColor(),
                "The portal color used for this world's portals, only applies for dimensions, not overworld/nether/end.",
                "Options: beige, black, blue, crystalblue, darkblue, darkgreen, darkred, emerald, flame, gold,",
                "green, grey, lightblue, lightgreen, orange, pink, red, white, yellow, default."
        );
        writer.putSetting(PortalSettings.PORTAL_MOB, presetConfig.getPortalSettings().getPortalMob(),
                "The mob that spawns from this portal, minecraft:zombified_piglin by default.",
                "Only applies for dimensions, not overworld/nether/end."
        );
        writer.putSetting(PortalSettings.PORTAL_IGNITION_SOURCE, presetConfig.getPortalSettings().getPortalIgnitionSource(),
                "The ignition source for this portal, minecraft:flint_and_steel by default.",
                "Only applies for dimensions, not overworld/nether/end."
        );

        writer.header1("Dimension settings");

        presetConfig.getDimensionSettings().writeSettings(writer);

        writer.header1("Game rules (Forge)",
                "See: https://minecraft.fandom.com/wiki/Game_rule",
                "Since game rules are shared across all dimensions, these settings only apply if this preset is used as the overworld.",
                "These settings can be overridden via a DimensionConfig with a GameRules entry."
        );

        writer.putSetting(GameRuleSettings.OVERRIDE_GAME_RULES, presetConfig.getGameRuleSettings().isOverrideGameRules(),
                "Set this to true to enable the settings below."
        );
        var gameRuleSettings = presetConfig.getGameRuleSettings();
        writer.putSetting(GameRuleSettings.DO_FIRE_TICK, gameRuleSettings.isDoFireTick());
        writer.putSetting(GameRuleSettings.MOB_GRIEFING, gameRuleSettings.isMobGriefing());
        writer.putSetting(GameRuleSettings.KEEP_INVENTORY, gameRuleSettings.isKeepInventory());
        writer.putSetting(GameRuleSettings.DO_MOB_SPAWNING, gameRuleSettings.isDoMobSpawning());
        writer.putSetting(GameRuleSettings.DO_MOB_LOOT, gameRuleSettings.isDoMobLoot());
        writer.putSetting(GameRuleSettings.DO_TILE_DROPS, gameRuleSettings.isDoTileDrops());
        writer.putSetting(GameRuleSettings.DO_ENTITY_DROPS, gameRuleSettings.isDoEntityDrops());
        writer.putSetting(GameRuleSettings.COMMAND_BLOCK_OUTPUT, gameRuleSettings.isCommandBlockOutput());
        writer.putSetting(GameRuleSettings.NATURAL_REGENERATION, gameRuleSettings.isNaturalRegeneration());
        writer.putSetting(GameRuleSettings.DO_DAY_LIGHT_CYCLE, gameRuleSettings.isNaturalRegeneration());
        writer.putSetting(GameRuleSettings.LOG_ADMIN_COMMANDS, gameRuleSettings.isLogAdminCommands());
        writer.putSetting(GameRuleSettings.SHOW_DEATH_MESSAGES, gameRuleSettings.isShowDeathMessages());
        writer.putSetting(GameRuleSettings.RANDOM_TICK_SPEED, gameRuleSettings.getRandomTickSpeed());
        writer.putSetting(GameRuleSettings.SEND_COMMAND_FEEDBACK, gameRuleSettings.isSendCommandFeedback());
        writer.putSetting(GameRuleSettings.SPECTATORS_GENERATE_CHUNKS, gameRuleSettings.isSpectatorsGenerateChunks());
        writer.putSetting(GameRuleSettings.SPAWN_RADIUS, gameRuleSettings.getSpawnRadius());
        writer.putSetting(GameRuleSettings.DISABLE_ELYTRA_MOVEMENT_CHECK, gameRuleSettings.isDisableElytraMovementCheck());
        writer.putSetting(GameRuleSettings.MAX_ENTITY_CRAMMING, gameRuleSettings.getMaxEntityCramming());
        writer.putSetting(GameRuleSettings.DO_WEATHER_CYCLE, gameRuleSettings.isDoWeatherCycle());
        writer.putSetting(GameRuleSettings.DO_LIMITED_CRAFTING, gameRuleSettings.isDoLimitedCrafting());
        writer.putSetting(GameRuleSettings.MAX_COMMAND_CHAIN_LENGTH, gameRuleSettings.getMaxCommandChainLength());
        writer.putSetting(GameRuleSettings.ANNOUNCE_ADVANCEMENTS, gameRuleSettings.isAnnounceAdvancements());
        writer.putSetting(GameRuleSettings.DISABLE_RAIDS, gameRuleSettings.isDisableRaids());
        writer.putSetting(GameRuleSettings.DO_INSOMNIA, gameRuleSettings.isDoInsomnia());
        writer.putSetting(GameRuleSettings.DROWNING_DAMAGE, gameRuleSettings.isDrowningDamage());
        writer.putSetting(GameRuleSettings.FALL_DAMAGE, gameRuleSettings.isFallDamage());
        writer.putSetting(GameRuleSettings.FIRE_DAMAGE, gameRuleSettings.isFireDamage());
        writer.putSetting(GameRuleSettings.DO_PATROL_SPAWNING, gameRuleSettings.isDoPatrolSpawning());
        writer.putSetting(GameRuleSettings.DO_TRADER_SPAWNING, gameRuleSettings.isDoTraderSpawning());
        writer.putSetting(GameRuleSettings.FORGIVE_DEAD_PLAYERS, gameRuleSettings.isForgiveDeadPlayers());
        writer.putSetting(GameRuleSettings.UNIVERSAL_ANGER, gameRuleSettings.isUniversalAnger());
        writer.putSetting(GameRuleSettings.DO_IMMEDIATE_RESPAWN, gameRuleSettings.isDoImmediateRespawn());
        writer.putSetting(GameRuleSettings.REDUCED_DEBUG_INFO, gameRuleSettings.isReducedDebugInfo());
        writer.putSetting(GameRuleSettings.FREEZE_DAMAGE, gameRuleSettings.isFreezeDamage());
        writer.putSetting(GameRuleSettings.PLAYERS_SLEEPING_PERCENTAGE, gameRuleSettings.getPlayersSleepingPercentage());
        writer.putSetting(GameRuleSettings.DO_WARDEN_SPAWNING, gameRuleSettings.isDoWardenSpawning());
        writer.putSetting(GameRuleSettings.BLOCK_EXPLOSION_DROP_DECAY, gameRuleSettings.isBlockExplosionDropDecay());
        writer.putSetting(GameRuleSettings.MOB_EXPLOSION_DROP_DECAY, gameRuleSettings.isMobExplosionDropDecay());
        writer.putSetting(GameRuleSettings.TNT_EXPLOSION_DROP_DECAY, gameRuleSettings.isTntExplosionDropDecay());
        writer.putSetting(GameRuleSettings.SNOW_ACCUMULATION_HEIGHT, gameRuleSettings.getSnowAccumulationHeight());
        writer.putSetting(GameRuleSettings.COMMAND_MODIFICATION_BLOCK_LIMIT, gameRuleSettings.getCommandModificationBlockLimit());
        writer.putSetting(GameRuleSettings.DO_VINES_SPREAD, gameRuleSettings.isDoVinesSpread());
        writer.putSetting(GameRuleSettings.GLOBAL_SOUND_EVENTS, gameRuleSettings.isGlobalSoundEvents());
        writer.putSetting(GameRuleSettings.LAVA_SOURCE_CONVERSION, gameRuleSettings.isLavaSourceConversion());
        writer.putSetting(GameRuleSettings.WATER_SOURCE_CONVERSION, gameRuleSettings.isWaterSourceConversion());
    }
}
