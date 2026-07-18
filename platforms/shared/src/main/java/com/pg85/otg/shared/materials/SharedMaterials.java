package com.pg85.otg.shared.materials;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.util.materials.LocalMaterials;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SharedMaterials extends LocalMaterials
{
	// Default blocks in given tags.
	// Tags aren't loaded until datapacks are loaded, on world creation. We mirror the vanilla copy of the tag to solve this.
	private static final Block[] CORAL_BLOCKS_TAG = { Blocks.TUBE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK };
	private static final Block[] WALL_CORALS_TAG = { Blocks.TUBE_CORAL_WALL_FAN, Blocks.BRAIN_CORAL_WALL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN, Blocks.FIRE_CORAL_WALL_FAN, Blocks.HORN_CORAL_WALL_FAN };
	private static final Block[] CORALS_TAG = { Blocks.TUBE_CORAL, Blocks.BRAIN_CORAL, Blocks.BUBBLE_CORAL, Blocks.FIRE_CORAL, Blocks.HORN_CORAL, Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN };

	public static void init()
	{
		// Coral
		CORAL_BLOCKS = Arrays.stream(CORAL_BLOCKS_TAG).map(block -> SharedMaterialData.ofBlockState(block.defaultBlockState())).collect(Collectors.toList());
		WALL_CORALS = Arrays.stream(WALL_CORALS_TAG).map(block -> SharedMaterialData.ofBlockState(block.defaultBlockState())).collect(Collectors.toList());
		CORALS = Arrays.stream(CORALS_TAG).map(block -> SharedMaterialData.ofBlockState(block.defaultBlockState())).collect(Collectors.toList());
				
		// Blocks used in OTG code
		
		AIR = SharedMaterialData.ofBlockState(Blocks.AIR.defaultBlockState());
		CAVE_AIR = SharedMaterialData.ofBlockState(Blocks.CAVE_AIR.defaultBlockState());
		STRUCTURE_VOID = SharedMaterialData.ofBlockState(Blocks.STRUCTURE_VOID.defaultBlockState());
		COMMAND_BLOCK = SharedMaterialData.ofBlockState(Blocks.COMMAND_BLOCK.defaultBlockState());
		STRUCTURE_BLOCK = SharedMaterialData.ofBlockState(Blocks.STRUCTURE_BLOCK.defaultBlockState());
		GRASS = SharedMaterialData.ofBlockState(Blocks.GRASS.defaultBlockState());
		DIRT = SharedMaterialData.ofBlockState(Blocks.DIRT.defaultBlockState());
		CLAY = SharedMaterialData.ofBlockState(Blocks.CLAY.defaultBlockState());
		TERRACOTTA = SharedMaterialData.ofBlockState(Blocks.TERRACOTTA.defaultBlockState());
		WHITE_TERRACOTTA = SharedMaterialData.ofBlockState(Blocks.WHITE_TERRACOTTA.defaultBlockState());
		ORANGE_TERRACOTTA = SharedMaterialData.ofBlockState(Blocks.ORANGE_TERRACOTTA.defaultBlockState());
		YELLOW_TERRACOTTA = SharedMaterialData.ofBlockState(Blocks.YELLOW_TERRACOTTA.defaultBlockState());
		BROWN_TERRACOTTA = SharedMaterialData.ofBlockState(Blocks.BROWN_TERRACOTTA.defaultBlockState());
		RED_TERRACOTTA = SharedMaterialData.ofBlockState(Blocks.RED_TERRACOTTA.defaultBlockState());
		SILVER_TERRACOTTA = SharedMaterialData.ofBlockState(Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState());
		STONE = SharedMaterialData.ofBlockState(Blocks.STONE.defaultBlockState());
		NETHERRACK = SharedMaterialData.ofBlockState(Blocks.NETHERRACK.defaultBlockState());
		END_STONE = SharedMaterialData.ofBlockState(Blocks.END_STONE.defaultBlockState());
		SAND = SharedMaterialData.ofBlockState(Blocks.SAND.defaultBlockState());
		RED_SAND = SharedMaterialData.ofBlockState(Blocks.RED_SAND.defaultBlockState());
		SANDSTONE = SharedMaterialData.ofBlockState(Blocks.SANDSTONE.defaultBlockState());
		RED_SANDSTONE = SharedMaterialData.ofBlockState(Blocks.RED_SANDSTONE.defaultBlockState());
		GRAVEL = SharedMaterialData.ofBlockState(Blocks.GRAVEL.defaultBlockState());
		MOSSY_COBBLESTONE = SharedMaterialData.ofBlockState(Blocks.MOSSY_COBBLESTONE.defaultBlockState());
		SNOW = SharedMaterialData.ofBlockState(Blocks.SNOW.defaultBlockState());
		SNOW_BLOCK = SharedMaterialData.ofBlockState(Blocks.SNOW_BLOCK.defaultBlockState());
		TORCH = SharedMaterialData.ofBlockState(Blocks.TORCH.defaultBlockState());
		BEDROCK = SharedMaterialData.ofBlockState(Blocks.BEDROCK.defaultBlockState());
		MAGMA = SharedMaterialData.ofBlockState(Blocks.MAGMA_BLOCK.defaultBlockState());
		ICE = SharedMaterialData.ofBlockState(Blocks.ICE.defaultBlockState());
		PACKED_ICE = SharedMaterialData.ofBlockState(Blocks.PACKED_ICE.defaultBlockState());
		BLUE_ICE = SharedMaterialData.ofBlockState(Blocks.BLUE_ICE.defaultBlockState());
		FROSTED_ICE = SharedMaterialData.ofBlockState(Blocks.FROSTED_ICE.defaultBlockState());
		GLOWSTONE = SharedMaterialData.ofBlockState(Blocks.GLOWSTONE.defaultBlockState());
		MYCELIUM = SharedMaterialData.ofBlockState(Blocks.MYCELIUM.defaultBlockState());
		STONE_SLAB = SharedMaterialData.ofBlockState(Blocks.STONE_SLAB.defaultBlockState());

		// Liquids
		WATER = SharedMaterialData.ofBlockState(Blocks.WATER.defaultBlockState());
		LAVA = SharedMaterialData.ofBlockState(Blocks.LAVA.defaultBlockState());

		// Trees
		ACACIA_LOG = SharedMaterialData.ofBlockState(Blocks.ACACIA_LOG.defaultBlockState());
		BIRCH_LOG = SharedMaterialData.ofBlockState(Blocks.BIRCH_LOG.defaultBlockState());
		DARK_OAK_LOG = SharedMaterialData.ofBlockState(Blocks.DARK_OAK_LOG.defaultBlockState());
		OAK_LOG = SharedMaterialData.ofBlockState(Blocks.OAK_LOG.defaultBlockState());
		SPRUCE_LOG = SharedMaterialData.ofBlockState(Blocks.SPRUCE_LOG.defaultBlockState());
		JUNGLE_LOG = SharedMaterialData.ofBlockState(Blocks.JUNGLE_LOG.defaultBlockState());
		ACACIA_WOOD = SharedMaterialData.ofBlockState(Blocks.ACACIA_WOOD.defaultBlockState());
		BIRCH_WOOD = SharedMaterialData.ofBlockState(Blocks.BIRCH_WOOD.defaultBlockState());
		DARK_OAK_WOOD = SharedMaterialData.ofBlockState(Blocks.DARK_OAK_WOOD.defaultBlockState());
		OAK_WOOD = SharedMaterialData.ofBlockState(Blocks.OAK_WOOD.defaultBlockState());
		SPRUCE_WOOD = SharedMaterialData.ofBlockState(Blocks.SPRUCE_WOOD.defaultBlockState());
		JUNGLE_WOOD = SharedMaterialData.ofBlockState(Blocks.JUNGLE_WOOD.defaultBlockState());

		STRIPPED_ACACIA_LOG = SharedMaterialData.ofBlockState(Blocks.STRIPPED_ACACIA_LOG.defaultBlockState());
		STRIPPED_BIRCH_LOG = SharedMaterialData.ofBlockState(Blocks.STRIPPED_BIRCH_LOG.defaultBlockState());
		STRIPPED_DARK_OAK_LOG = SharedMaterialData.ofBlockState(Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
		STRIPPED_JUNGLE_LOG = SharedMaterialData.ofBlockState(Blocks.STRIPPED_JUNGLE_LOG.defaultBlockState());
		STRIPPED_OAK_LOG = SharedMaterialData.ofBlockState(Blocks.STRIPPED_OAK_LOG.defaultBlockState());
		STRIPPED_SPRUCE_LOG = SharedMaterialData.ofBlockState(Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
		
		ACACIA_LEAVES = SharedMaterialData.ofBlockState(Blocks.ACACIA_LEAVES.defaultBlockState());
		BIRCH_LEAVES = SharedMaterialData.ofBlockState(Blocks.BIRCH_LEAVES.defaultBlockState());
		DARK_OAK_LEAVES = SharedMaterialData.ofBlockState(Blocks.DARK_OAK_LEAVES.defaultBlockState());
		JUNGLE_LEAVES = SharedMaterialData.ofBlockState(Blocks.JUNGLE_LEAVES.defaultBlockState());
		OAK_LEAVES = SharedMaterialData.ofBlockState(Blocks.OAK_LEAVES.defaultBlockState());
		SPRUCE_LEAVES = SharedMaterialData.ofBlockState(Blocks.SPRUCE_LEAVES.defaultBlockState());

		// Plants
		POPPY = SharedMaterialData.ofBlockState(Blocks.POPPY.defaultBlockState());
		BLUE_ORCHID = SharedMaterialData.ofBlockState(Blocks.BLUE_ORCHID.defaultBlockState());
		ALLIUM = SharedMaterialData.ofBlockState(Blocks.ALLIUM.defaultBlockState());
		AZURE_BLUET = SharedMaterialData.ofBlockState(Blocks.AZURE_BLUET.defaultBlockState());
		RED_TULIP = SharedMaterialData.ofBlockState(Blocks.RED_TULIP.defaultBlockState());
		ORANGE_TULIP = SharedMaterialData.ofBlockState(Blocks.ORANGE_TULIP.defaultBlockState());
		WHITE_TULIP = SharedMaterialData.ofBlockState(Blocks.WHITE_TULIP.defaultBlockState());
		PINK_TULIP = SharedMaterialData.ofBlockState(Blocks.PINK_TULIP.defaultBlockState());
		OXEYE_DAISY = SharedMaterialData.ofBlockState(Blocks.OXEYE_DAISY.defaultBlockState());		
		YELLOW_FLOWER = SharedMaterialData.ofBlockState(Blocks.DANDELION.defaultBlockState());
		DEAD_BUSH = SharedMaterialData.ofBlockState(Blocks.DEAD_BUSH.defaultBlockState());
		FERN = SharedMaterialData.ofBlockState(Blocks.FERN.defaultBlockState());
		LONG_GRASS = SharedMaterialData.ofBlockState(Blocks.GRASS.defaultBlockState());
		
		RED_MUSHROOM_BLOCK = SharedMaterialData.ofBlockState(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState());
		BROWN_MUSHROOM_BLOCK = SharedMaterialData.ofBlockState(Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState());		
		RED_MUSHROOM = SharedMaterialData.ofBlockState(Blocks.RED_MUSHROOM.defaultBlockState());
		BROWN_MUSHROOM = SharedMaterialData.ofBlockState(Blocks.BROWN_MUSHROOM.defaultBlockState());

		DOUBLE_TALL_GRASS_LOWER = SharedMaterialData.ofBlockState(Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		DOUBLE_TALL_GRASS_UPPER = SharedMaterialData.ofBlockState(Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
		LARGE_FERN_LOWER = SharedMaterialData.ofBlockState(Blocks.LARGE_FERN.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		LARGE_FERN_UPPER = SharedMaterialData.ofBlockState(Blocks.LARGE_FERN.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));		
		LILAC_LOWER = SharedMaterialData.ofBlockState(Blocks.LILAC.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		LILAC_UPPER = SharedMaterialData.ofBlockState(Blocks.LILAC.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));		
		PEONY_LOWER = SharedMaterialData.ofBlockState(Blocks.PEONY.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		PEONY_UPPER = SharedMaterialData.ofBlockState(Blocks.PEONY.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));		
		ROSE_BUSH_LOWER = SharedMaterialData.ofBlockState(Blocks.ROSE_BUSH.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		ROSE_BUSH_UPPER = SharedMaterialData.ofBlockState(Blocks.ROSE_BUSH.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));	
		SUNFLOWER_LOWER = SharedMaterialData.ofBlockState(Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		SUNFLOWER_UPPER = SharedMaterialData.ofBlockState(Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));

		ACACIA_SAPLING = SharedMaterialData.ofBlockState(Blocks.ACACIA_SAPLING.defaultBlockState());
		BAMBOO_SAPLING = SharedMaterialData.ofBlockState(Blocks.BAMBOO_SAPLING.defaultBlockState());
		BIRCH_SAPLING = SharedMaterialData.ofBlockState(Blocks.BIRCH_SAPLING.defaultBlockState());
		DARK_OAK_SAPLING = SharedMaterialData.ofBlockState(Blocks.DARK_OAK_SAPLING.defaultBlockState());
		JUNGLE_SAPLING = SharedMaterialData.ofBlockState(Blocks.JUNGLE_SAPLING.defaultBlockState());
		OAK_SAPLING = SharedMaterialData.ofBlockState(Blocks.OAK_SAPLING.defaultBlockState());
		SPRUCE_SAPLING = SharedMaterialData.ofBlockState(Blocks.SPRUCE_SAPLING.defaultBlockState());
		
		PUMPKIN = SharedMaterialData.ofBlockState(Blocks.PUMPKIN.defaultBlockState());
		CACTUS = SharedMaterialData.ofBlockState(Blocks.CACTUS.defaultBlockState());
		MELON_BLOCK = SharedMaterialData.ofBlockState(Blocks.MELON.defaultBlockState());
		VINE = SharedMaterialData.ofBlockState(Blocks.VINE.defaultBlockState());
		WATER_LILY = SharedMaterialData.ofBlockState(Blocks.LILY_PAD.defaultBlockState());
		SUGAR_CANE_BLOCK = SharedMaterialData.ofBlockState(Blocks.SUGAR_CANE.defaultBlockState());
		
		BlockState bambooState = Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.AGE, 1).setValue(BambooStalkBlock.LEAVES, BambooLeaves.NONE).setValue(BambooStalkBlock.STAGE, 0);
		BAMBOO = SharedMaterialData.ofBlockState(bambooState);
		BAMBOO_SMALL = SharedMaterialData.ofBlockState(bambooState.setValue(BambooStalkBlock.LEAVES, BambooLeaves.SMALL));
		BAMBOO_LARGE = SharedMaterialData.ofBlockState(bambooState.setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE));
		BAMBOO_LARGE_GROWING = SharedMaterialData.ofBlockState(bambooState.setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE).setValue(BambooStalkBlock.STAGE, 1));
		PODZOL = SharedMaterialData.ofBlockState(Blocks.PODZOL.defaultBlockState());
		SEAGRASS = SharedMaterialData.ofBlockState(Blocks.SEAGRASS.defaultBlockState());
		TALL_SEAGRASS_LOWER = SharedMaterialData.ofBlockState(Blocks.TALL_SEAGRASS.defaultBlockState().setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.LOWER));
		TALL_SEAGRASS_UPPER = SharedMaterialData.ofBlockState(Blocks.TALL_SEAGRASS.defaultBlockState().setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER));
		KELP = SharedMaterialData.ofBlockState(Blocks.KELP.defaultBlockState());
		KELP_PLANT = SharedMaterialData.ofBlockState(Blocks.KELP_PLANT.defaultBlockState());
		VINE_SOUTH = SharedMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true));
		VINE_NORTH = SharedMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.NORTH, true));
		VINE_WEST = SharedMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true));
		VINE_EAST = SharedMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true));
		SEA_PICKLE = SharedMaterialData.ofBlockState(Blocks.SEA_PICKLE.defaultBlockState());

		// Ores
		COAL_ORE = SharedMaterialData.ofBlockState(Blocks.COAL_ORE.defaultBlockState());
		DIAMOND_ORE = SharedMaterialData.ofBlockState(Blocks.DIAMOND_ORE.defaultBlockState());
		EMERALD_ORE = SharedMaterialData.ofBlockState(Blocks.EMERALD_ORE.defaultBlockState());
		GOLD_ORE = SharedMaterialData.ofBlockState(Blocks.GOLD_ORE.defaultBlockState());
		IRON_ORE = SharedMaterialData.ofBlockState(Blocks.IRON_ORE.defaultBlockState());
		LAPIS_ORE = SharedMaterialData.ofBlockState(Blocks.LAPIS_ORE.defaultBlockState());
		QUARTZ_ORE = SharedMaterialData.ofBlockState(Blocks.NETHER_QUARTZ_ORE.defaultBlockState());
		REDSTONE_ORE = SharedMaterialData.ofBlockState(Blocks.REDSTONE_ORE.defaultBlockState());

		// Ore blocks
		GOLD_BLOCK = SharedMaterialData.ofBlockState(Blocks.GOLD_BLOCK.defaultBlockState());
		IRON_BLOCK = SharedMaterialData.ofBlockState(Blocks.IRON_BLOCK.defaultBlockState());
		REDSTONE_BLOCK = SharedMaterialData.ofBlockState(Blocks.REDSTONE_BLOCK.defaultBlockState());
		DIAMOND_BLOCK = SharedMaterialData.ofBlockState(Blocks.DIAMOND_BLOCK.defaultBlockState());
		LAPIS_BLOCK = SharedMaterialData.ofBlockState(Blocks.LAPIS_BLOCK.defaultBlockState());
		COAL_BLOCK = SharedMaterialData.ofBlockState(Blocks.COAL_BLOCK.defaultBlockState());
		QUARTZ_BLOCK = SharedMaterialData.ofBlockState(Blocks.QUARTZ_BLOCK.defaultBlockState());
		EMERALD_BLOCK = SharedMaterialData.ofBlockState(Blocks.EMERALD_BLOCK.defaultBlockState());

		BERRY_BUSH = SharedMaterialData.ofBlockState(Blocks.SWEET_BERRY_BUSH.defaultBlockState());
	}
}
