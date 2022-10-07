package com.pg85.otg.fabric.materials;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.util.materials.LocalMaterials;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FabricMaterials extends LocalMaterials {
    // Default blocks in given tags
    // Tags aren't loaded until datapacks are loaded, on world creation. We mirror the vanilla copy of the tag to solve this.
    private static final Block[] CORAL_BLOCKS_TAG = {Blocks.TUBE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK};
    private static final Block[] WALL_CORALS_TAG = {Blocks.TUBE_CORAL_WALL_FAN, Blocks.BRAIN_CORAL_WALL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN, Blocks.FIRE_CORAL_WALL_FAN, Blocks.HORN_CORAL_WALL_FAN};
    private static final Block[] CORALS_TAG = {Blocks.TUBE_CORAL, Blocks.BRAIN_CORAL, Blocks.BUBBLE_CORAL, Blocks.FIRE_CORAL, Blocks.HORN_CORAL, Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN};

    public static void init() {

        //Adding tags handled in util.FabricBlockTags class

        // Coral
        CORAL_BLOCKS = Arrays.stream(CORAL_BLOCKS_TAG).map(block -> FabricMaterialData.ofBlockState(block.defaultBlockState())).collect(Collectors.toList());
        WALL_CORALS = Arrays.stream(WALL_CORALS_TAG).map(block -> FabricMaterialData.ofBlockState(block.defaultBlockState())).collect(Collectors.toList());
        CORALS = Arrays.stream(CORALS_TAG).map(block -> FabricMaterialData.ofBlockState(block.defaultBlockState())).collect(Collectors.toList());

        // Blocks used in OTG code

        AIR = FabricMaterialData.ofBlockState(Blocks.AIR.defaultBlockState());
        CAVE_AIR = FabricMaterialData.ofBlockState(Blocks.CAVE_AIR.defaultBlockState());
        STRUCTURE_VOID = FabricMaterialData.ofBlockState(Blocks.STRUCTURE_VOID.defaultBlockState());
        COMMAND_BLOCK = FabricMaterialData.ofBlockState(Blocks.COMMAND_BLOCK.defaultBlockState());
        STRUCTURE_BLOCK = FabricMaterialData.ofBlockState(Blocks.STRUCTURE_BLOCK.defaultBlockState());
        GRASS = FabricMaterialData.ofBlockState(Blocks.GRASS_BLOCK.defaultBlockState());
        DIRT = FabricMaterialData.ofBlockState(Blocks.DIRT.defaultBlockState());
        CLAY = FabricMaterialData.ofBlockState(Blocks.CLAY.defaultBlockState());
        TERRACOTTA = FabricMaterialData.ofBlockState(Blocks.TERRACOTTA.defaultBlockState());
        WHITE_TERRACOTTA = FabricMaterialData.ofBlockState(Blocks.WHITE_TERRACOTTA.defaultBlockState());
        ORANGE_TERRACOTTA = FabricMaterialData.ofBlockState(Blocks.ORANGE_TERRACOTTA.defaultBlockState());
        YELLOW_TERRACOTTA = FabricMaterialData.ofBlockState(Blocks.YELLOW_TERRACOTTA.defaultBlockState());
        BROWN_TERRACOTTA = FabricMaterialData.ofBlockState(Blocks.BROWN_TERRACOTTA.defaultBlockState());
        RED_TERRACOTTA = FabricMaterialData.ofBlockState(Blocks.RED_TERRACOTTA.defaultBlockState());
        SILVER_TERRACOTTA = FabricMaterialData.ofBlockState(Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState());
        STONE = FabricMaterialData.ofBlockState(Blocks.STONE.defaultBlockState());
        DEEPSLATE = FabricMaterialData.ofBlockState(Blocks.DEEPSLATE.defaultBlockState());
        NETHERRACK = FabricMaterialData.ofBlockState(Blocks.NETHERRACK.defaultBlockState());
        END_STONE = FabricMaterialData.ofBlockState(Blocks.END_STONE.defaultBlockState());
        SAND = FabricMaterialData.ofBlockState(Blocks.SAND.defaultBlockState());
        RED_SAND = FabricMaterialData.ofBlockState(Blocks.RED_SAND.defaultBlockState());
        SANDSTONE = FabricMaterialData.ofBlockState(Blocks.SANDSTONE.defaultBlockState());
        RED_SANDSTONE = FabricMaterialData.ofBlockState(Blocks.RED_SANDSTONE.defaultBlockState());
        GRAVEL = FabricMaterialData.ofBlockState(Blocks.GRAVEL.defaultBlockState());
        MOSSY_COBBLESTONE = FabricMaterialData.ofBlockState(Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        SNOW = FabricMaterialData.ofBlockState(Blocks.SNOW.defaultBlockState());
        SNOW_BLOCK = FabricMaterialData.ofBlockState(Blocks.SNOW_BLOCK.defaultBlockState());
        TORCH = FabricMaterialData.ofBlockState(Blocks.TORCH.defaultBlockState());
        BEDROCK = FabricMaterialData.ofBlockState(Blocks.BEDROCK.defaultBlockState());
        MAGMA = FabricMaterialData.ofBlockState(Blocks.MAGMA_BLOCK.defaultBlockState());
        ICE = FabricMaterialData.ofBlockState(Blocks.ICE.defaultBlockState());
        PACKED_ICE = FabricMaterialData.ofBlockState(Blocks.PACKED_ICE.defaultBlockState());
        BLUE_ICE = FabricMaterialData.ofBlockState(Blocks.BLUE_ICE.defaultBlockState());
        FROSTED_ICE = FabricMaterialData.ofBlockState(Blocks.FROSTED_ICE.defaultBlockState());
        GLOWSTONE = FabricMaterialData.ofBlockState(Blocks.GLOWSTONE.defaultBlockState());
        MYCELIUM = FabricMaterialData.ofBlockState(Blocks.MYCELIUM.defaultBlockState());
        STONE_SLAB = FabricMaterialData.ofBlockState(Blocks.STONE_SLAB.defaultBlockState());
        AMETHYST_BLOCK = FabricMaterialData.ofBlockState(Blocks.AMETHYST_BLOCK.defaultBlockState());
        BUDDING_AMETHYST = FabricMaterialData.ofBlockState(Blocks.BUDDING_AMETHYST.defaultBlockState());
        CALCITE = FabricMaterialData.ofBlockState(Blocks.CALCITE.defaultBlockState());
        SMOOTH_BASALT = FabricMaterialData.ofBlockState(Blocks.SMOOTH_BASALT.defaultBlockState());
        SMALL_AMETHYST_BUD = FabricMaterialData.ofBlockState(Blocks.SMALL_AMETHYST_BUD.defaultBlockState());
        MEDIUM_AMETHYST_BUD = FabricMaterialData.ofBlockState(Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState());
        LARGE_AMETHYST_BUD = FabricMaterialData.ofBlockState(Blocks.LARGE_AMETHYST_BUD.defaultBlockState());
        AMETHYST_CLUSTER = FabricMaterialData.ofBlockState(Blocks.AMETHYST_CLUSTER.defaultBlockState());
        GRANITE = FabricMaterialData.ofBlockState(Blocks.GRANITE.defaultBlockState());
        TUFF = FabricMaterialData.ofBlockState(Blocks.TUFF.defaultBlockState());

        // Liquids
        WATER = FabricMaterialData.ofBlockState(Blocks.WATER.defaultBlockState());
        LAVA = FabricMaterialData.ofBlockState(Blocks.LAVA.defaultBlockState());

        // Trees
        ACACIA_LOG = FabricMaterialData.ofBlockState(Blocks.ACACIA_LOG.defaultBlockState());
        BIRCH_LOG = FabricMaterialData.ofBlockState(Blocks.BIRCH_LOG.defaultBlockState());
        DARK_OAK_LOG = FabricMaterialData.ofBlockState(Blocks.DARK_OAK_LOG.defaultBlockState());
        OAK_LOG = FabricMaterialData.ofBlockState(Blocks.OAK_LOG.defaultBlockState());
        SPRUCE_LOG = FabricMaterialData.ofBlockState(Blocks.SPRUCE_LOG.defaultBlockState());
        ACACIA_WOOD = FabricMaterialData.ofBlockState(Blocks.ACACIA_WOOD.defaultBlockState());
        BIRCH_WOOD = FabricMaterialData.ofBlockState(Blocks.BIRCH_WOOD.defaultBlockState());
        DARK_OAK_WOOD = FabricMaterialData.ofBlockState(Blocks.DARK_OAK_WOOD.defaultBlockState());
        OAK_WOOD = FabricMaterialData.ofBlockState(Blocks.OAK_WOOD.defaultBlockState());
        SPRUCE_WOOD = FabricMaterialData.ofBlockState(Blocks.SPRUCE_WOOD.defaultBlockState());
        STRIPPED_ACACIA_LOG = FabricMaterialData.ofBlockState(Blocks.STRIPPED_ACACIA_LOG.defaultBlockState());
        STRIPPED_BIRCH_LOG = FabricMaterialData.ofBlockState(Blocks.STRIPPED_BIRCH_LOG.defaultBlockState());
        STRIPPED_DARK_OAK_LOG = FabricMaterialData.ofBlockState(Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
        STRIPPED_JUNGLE_LOG = FabricMaterialData.ofBlockState(Blocks.STRIPPED_JUNGLE_LOG.defaultBlockState());
        STRIPPED_OAK_LOG = FabricMaterialData.ofBlockState(Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        STRIPPED_SPRUCE_LOG = FabricMaterialData.ofBlockState(Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());

        ACACIA_LEAVES = FabricMaterialData.ofBlockState(Blocks.ACACIA_LEAVES.defaultBlockState());
        BIRCH_LEAVES = FabricMaterialData.ofBlockState(Blocks.BIRCH_LEAVES.defaultBlockState());
        DARK_OAK_LEAVES = FabricMaterialData.ofBlockState(Blocks.DARK_OAK_LEAVES.defaultBlockState());
        JUNGLE_LEAVES = FabricMaterialData.ofBlockState(Blocks.JUNGLE_LEAVES.defaultBlockState());
        OAK_LEAVES = FabricMaterialData.ofBlockState(Blocks.OAK_LEAVES.defaultBlockState());
        SPRUCE_LEAVES = FabricMaterialData.ofBlockState(Blocks.SPRUCE_LEAVES.defaultBlockState());

        // Plants
        POPPY = FabricMaterialData.ofBlockState(Blocks.POPPY.defaultBlockState());
        BLUE_ORCHID = FabricMaterialData.ofBlockState(Blocks.BLUE_ORCHID.defaultBlockState());
        ALLIUM = FabricMaterialData.ofBlockState(Blocks.ALLIUM.defaultBlockState());
        AZURE_BLUET = FabricMaterialData.ofBlockState(Blocks.AZURE_BLUET.defaultBlockState());
        RED_TULIP = FabricMaterialData.ofBlockState(Blocks.RED_TULIP.defaultBlockState());
        ORANGE_TULIP = FabricMaterialData.ofBlockState(Blocks.ORANGE_TULIP.defaultBlockState());
        WHITE_TULIP = FabricMaterialData.ofBlockState(Blocks.WHITE_TULIP.defaultBlockState());
        PINK_TULIP = FabricMaterialData.ofBlockState(Blocks.PINK_TULIP.defaultBlockState());
        OXEYE_DAISY = FabricMaterialData.ofBlockState(Blocks.OXEYE_DAISY.defaultBlockState());
        YELLOW_FLOWER = FabricMaterialData.ofBlockState(Blocks.DANDELION.defaultBlockState());
        DEAD_BUSH = FabricMaterialData.ofBlockState(Blocks.DEAD_BUSH.defaultBlockState());
        FERN = FabricMaterialData.ofBlockState(Blocks.FERN.defaultBlockState());
        LONG_GRASS = FabricMaterialData.ofBlockState(Blocks.GRASS.defaultBlockState());

        RED_MUSHROOM_BLOCK = FabricMaterialData.ofBlockState(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState());
        BROWN_MUSHROOM_BLOCK = FabricMaterialData.ofBlockState(Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState());
        RED_MUSHROOM = FabricMaterialData.ofBlockState(Blocks.RED_MUSHROOM.defaultBlockState());
        BROWN_MUSHROOM = FabricMaterialData.ofBlockState(Blocks.BROWN_MUSHROOM.defaultBlockState());

        DOUBLE_TALL_GRASS_LOWER = FabricMaterialData.ofBlockState(Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        DOUBLE_TALL_GRASS_UPPER = FabricMaterialData.ofBlockState(Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        LARGE_FERN_LOWER = FabricMaterialData.ofBlockState(Blocks.LARGE_FERN.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        LARGE_FERN_UPPER = FabricMaterialData.ofBlockState(Blocks.LARGE_FERN.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        LILAC_LOWER = FabricMaterialData.ofBlockState(Blocks.LILAC.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        LILAC_UPPER = FabricMaterialData.ofBlockState(Blocks.LILAC.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        PEONY_LOWER = FabricMaterialData.ofBlockState(Blocks.PEONY.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        PEONY_UPPER = FabricMaterialData.ofBlockState(Blocks.PEONY.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        ROSE_BUSH_LOWER = FabricMaterialData.ofBlockState(Blocks.ROSE_BUSH.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        ROSE_BUSH_UPPER = FabricMaterialData.ofBlockState(Blocks.ROSE_BUSH.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        SUNFLOWER_LOWER = FabricMaterialData.ofBlockState(Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        SUNFLOWER_UPPER = FabricMaterialData.ofBlockState(Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));

        ACACIA_SAPLING = FabricMaterialData.ofBlockState(Blocks.ACACIA_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        BAMBOO_SAPLING = FabricMaterialData.ofBlockState(Blocks.BAMBOO_SAPLING.defaultBlockState());
        BIRCH_SAPLING = FabricMaterialData.ofBlockState(Blocks.BIRCH_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        DARK_OAK_SAPLING = FabricMaterialData.ofBlockState(Blocks.DARK_OAK_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        JUNGLE_SAPLING = FabricMaterialData.ofBlockState(Blocks.JUNGLE_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        OAK_SAPLING = FabricMaterialData.ofBlockState(Blocks.OAK_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        SPRUCE_SAPLING = FabricMaterialData.ofBlockState(Blocks.SPRUCE_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1));

        PUMPKIN = FabricMaterialData.ofBlockState(Blocks.PUMPKIN.defaultBlockState());
        CACTUS = FabricMaterialData.ofBlockState(Blocks.CACTUS.defaultBlockState());
        MELON_BLOCK = FabricMaterialData.ofBlockState(Blocks.MELON.defaultBlockState());
        VINE = FabricMaterialData.ofBlockState(Blocks.VINE.defaultBlockState());
        WATER_LILY = FabricMaterialData.ofBlockState(Blocks.LILY_PAD.defaultBlockState());
        SUGAR_CANE_BLOCK = FabricMaterialData.ofBlockState(Blocks.SUGAR_CANE.defaultBlockState());
        BlockState bambooState = Blocks.BAMBOO.defaultBlockState().setValue(BambooBlock.AGE, 1).setValue(BambooBlock.LEAVES, BambooLeaves.NONE).setValue(BambooBlock.STAGE, 0);
        BAMBOO = FabricMaterialData.ofBlockState(bambooState);
        BAMBOO_SMALL = FabricMaterialData.ofBlockState(bambooState.setValue(BambooBlock.LEAVES, BambooLeaves.SMALL));
        BAMBOO_LARGE = FabricMaterialData.ofBlockState(bambooState.setValue(BambooBlock.LEAVES, BambooLeaves.LARGE));
        BAMBOO_LARGE_GROWING = FabricMaterialData.ofBlockState(bambooState.setValue(BambooBlock.LEAVES, BambooLeaves.LARGE).setValue(BambooBlock.STAGE, 1));
        PODZOL = FabricMaterialData.ofBlockState(Blocks.PODZOL.defaultBlockState());
        SEAGRASS = FabricMaterialData.ofBlockState(Blocks.SEAGRASS.defaultBlockState());
        TALL_SEAGRASS_LOWER = FabricMaterialData.ofBlockState(Blocks.TALL_SEAGRASS.defaultBlockState().setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.LOWER));
        TALL_SEAGRASS_UPPER = FabricMaterialData.ofBlockState(Blocks.TALL_SEAGRASS.defaultBlockState().setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER));
        KELP = FabricMaterialData.ofBlockState(Blocks.KELP.defaultBlockState());
        KELP_PLANT = FabricMaterialData.ofBlockState(Blocks.KELP_PLANT.defaultBlockState());
        VINE_SOUTH = FabricMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true));
        VINE_NORTH = FabricMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.NORTH, true));
        VINE_WEST = FabricMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true));
        VINE_EAST = FabricMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true));
        SEA_PICKLE = FabricMaterialData.ofBlockState(Blocks.SEA_PICKLE.defaultBlockState());

        // Ores
        COAL_ORE = FabricMaterialData.ofBlockState(Blocks.COAL_ORE.defaultBlockState());
        DIAMOND_ORE = FabricMaterialData.ofBlockState(Blocks.DIAMOND_ORE.defaultBlockState());
        EMERALD_ORE = FabricMaterialData.ofBlockState(Blocks.EMERALD_ORE.defaultBlockState());
        GOLD_ORE = FabricMaterialData.ofBlockState(Blocks.GOLD_ORE.defaultBlockState());
        IRON_ORE = FabricMaterialData.ofBlockState(Blocks.IRON_ORE.defaultBlockState());
        COPPER_ORE = FabricMaterialData.ofBlockState(Blocks.COPPER_ORE.defaultBlockState());
        LAPIS_ORE = FabricMaterialData.ofBlockState(Blocks.LAPIS_ORE.defaultBlockState());
        QUARTZ_ORE = FabricMaterialData.ofBlockState(Blocks.NETHER_QUARTZ_ORE.defaultBlockState());
        REDSTONE_ORE = FabricMaterialData.ofBlockState(Blocks.REDSTONE_ORE.defaultBlockState());

        // Ore blocks
        GOLD_BLOCK = FabricMaterialData.ofBlockState(Blocks.GOLD_BLOCK.defaultBlockState());
        IRON_BLOCK = FabricMaterialData.ofBlockState(Blocks.IRON_BLOCK.defaultBlockState());
        REDSTONE_BLOCK = FabricMaterialData.ofBlockState(Blocks.REDSTONE_BLOCK.defaultBlockState());
        DIAMOND_BLOCK = FabricMaterialData.ofBlockState(Blocks.DIAMOND_BLOCK.defaultBlockState());
        LAPIS_BLOCK = FabricMaterialData.ofBlockState(Blocks.LAPIS_BLOCK.defaultBlockState());
        COAL_BLOCK = FabricMaterialData.ofBlockState(Blocks.COAL_BLOCK.defaultBlockState());
        QUARTZ_BLOCK = FabricMaterialData.ofBlockState(Blocks.QUARTZ_BLOCK.defaultBlockState());
        EMERALD_BLOCK = FabricMaterialData.ofBlockState(Blocks.EMERALD_BLOCK.defaultBlockState());
        BERRY_BUSH = FabricMaterialData.ofBlockState(Blocks.SWEET_BERRY_BUSH.defaultBlockState());
        RAW_IRON_BLOCK = FabricMaterialData.ofBlockState(Blocks.RAW_IRON_BLOCK.defaultBlockState());
        RAW_COPPER_BLOCK = FabricMaterialData.ofBlockState(Blocks.RAW_COPPER_BLOCK.defaultBlockState());
    }
}