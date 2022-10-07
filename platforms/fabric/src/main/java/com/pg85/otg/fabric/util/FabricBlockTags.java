package com.pg85.otg.fabric.util;

import com.pg85.otg.constants.Constants;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class FabricBlockTags extends FabricTagProvider.BlockTagProvider {

    private static final Tag.Named<Block> STONE =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "stone"));

    private static final Tag.Named<Block> DIRT =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "dirt"));

    private static final Tag.Named<Block> STAINED_CLAY =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "stained_clay"));

    private static final Tag.Named<Block> LOG =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "log"));

    private static final Tag.Named<Block> AIR =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "air"));

    private static final Tag.Named<Block> SANDSTONE =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "sandstone"));

    private static final Tag.Named<Block> RED_SANDSTONE =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "red_sandstone"));

    private static final Tag.Named<Block> LONG_GRASS =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "long_grass"));

    private static final Tag.Named<Block> RED_FLOWER =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "red_flower"));

    private static final Tag.Named<Block> QUARTZ_BLOCK =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "quartz_block"));

    private static final Tag.Named<Block> PRISMARINE =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "prismarine"));

    private static final Tag.Named<Block> CONCRETE =
            TagFactory.BLOCK.create(new ResourceLocation(Constants.MOD_ID_SHORT, "concrete"));

    public FabricBlockTags(FabricDataGenerator fdg) {
        super(fdg);
    }

    @Override
    protected void generateTags() {
        getOrCreateTagBuilder(STONE)
                .add(Blocks.STONE)
                .add(Blocks.GRANITE)
                .add(Blocks.DIORITE)
                .add(Blocks.ANDESITE);

        getOrCreateTagBuilder(DIRT)
                .add(Blocks.DIRT)
                .add(Blocks.COARSE_DIRT)
                .add(Blocks.PODZOL);

        getOrCreateTagBuilder(STAINED_CLAY)
                .add(Blocks.WHITE_TERRACOTTA)
                .add(Blocks.ORANGE_TERRACOTTA)
                .add(Blocks.MAGENTA_TERRACOTTA)
                .add(Blocks.LIGHT_BLUE_TERRACOTTA)
                .add(Blocks.YELLOW_TERRACOTTA)
                .add(Blocks.LIME_TERRACOTTA)
                .add(Blocks.PINK_TERRACOTTA)
                .add(Blocks.GRAY_TERRACOTTA)
                .add(Blocks.LIGHT_GRAY_TERRACOTTA)
                .add(Blocks.CYAN_TERRACOTTA)
                .add(Blocks.PURPLE_TERRACOTTA)
                .add(Blocks.BLUE_TERRACOTTA)
                .add(Blocks.BROWN_TERRACOTTA)
                .add(Blocks.GREEN_TERRACOTTA)
                .add(Blocks.RED_TERRACOTTA)
                .add(Blocks.BLACK_TERRACOTTA);

        getOrCreateTagBuilder(LOG)
                .add(Blocks.DARK_OAK_LOG)
                .add(Blocks.DARK_OAK_WOOD)
                .add(Blocks.STRIPPED_DARK_OAK_LOG)
                .add(Blocks.STRIPPED_DARK_OAK_WOOD)
                .add(Blocks.OAK_LOG)
                .add(Blocks.OAK_WOOD)
                .add(Blocks.STRIPPED_OAK_LOG)
                .add(Blocks.STRIPPED_OAK_WOOD)
                .add(Blocks.ACACIA_LOG)
                .add(Blocks.ACACIA_WOOD)
                .add(Blocks.STRIPPED_ACACIA_LOG)
                .add(Blocks.STRIPPED_ACACIA_WOOD)
                .add(Blocks.BIRCH_LOG)
                .add(Blocks.BIRCH_WOOD)
                .add(Blocks.STRIPPED_BIRCH_LOG)
                .add(Blocks.STRIPPED_BIRCH_WOOD)
                .add(Blocks.JUNGLE_LOG)
                .add(Blocks.STRIPPED_JUNGLE_LOG)
                .add(Blocks.STRIPPED_JUNGLE_WOOD)
                .add(Blocks.SPRUCE_LOG)
                .add(Blocks.SPRUCE_WOOD)
                .add(Blocks.STRIPPED_SPRUCE_LOG)
                .add(Blocks.STRIPPED_SPRUCE_WOOD)
                .add(Blocks.CRIMSON_STEM)
                .add(Blocks.STRIPPED_CRIMSON_STEM)
                .add(Blocks.CRIMSON_HYPHAE)
                .add(Blocks.STRIPPED_CRIMSON_HYPHAE)
                .add(Blocks.WARPED_STEM)
                .add(Blocks.STRIPPED_WARPED_STEM)
                .add(Blocks.WARPED_HYPHAE)
                .add(Blocks.STRIPPED_WARPED_HYPHAE);

        getOrCreateTagBuilder(AIR)
                .add(Blocks.AIR)
                .add(Blocks.CAVE_AIR);

        getOrCreateTagBuilder(SANDSTONE)
                .add(Blocks.SANDSTONE)
                .add(Blocks.CHISELED_SANDSTONE)
                .add(Blocks.SMOOTH_SANDSTONE);

        getOrCreateTagBuilder(RED_SANDSTONE)
                .add(Blocks.RED_SANDSTONE)
                .add(Blocks.CHISELED_RED_SANDSTONE)
                .add(Blocks.SMOOTH_RED_SANDSTONE);

        getOrCreateTagBuilder(LONG_GRASS)
                .add(Blocks.DEAD_BUSH)
                .add(Blocks.TALL_GRASS)
                .add(Blocks.FERN);

        getOrCreateTagBuilder(RED_FLOWER)
                .add(Blocks.POPPY)
                .add(Blocks.BLUE_ORCHID)
                .add(Blocks.ALLIUM)
                .add(Blocks.AZURE_BLUET)
                .add(Blocks.RED_TULIP)
                .add(Blocks.ORANGE_TULIP)
                .add(Blocks.WHITE_TULIP)
                .add(Blocks.PINK_TULIP)
                .add(Blocks.OXEYE_DAISY);

        getOrCreateTagBuilder(QUARTZ_BLOCK)
                .add(Blocks.QUARTZ_BLOCK)
                .add(Blocks.CHISELED_QUARTZ_BLOCK)
                .add(Blocks.QUARTZ_PILLAR);

        getOrCreateTagBuilder(PRISMARINE)
                .add(Blocks.PRISMARINE)
                .add(Blocks.PRISMARINE_BRICKS)
                .add(Blocks.DARK_PRISMARINE);

        getOrCreateTagBuilder(CONCRETE)
                .add(Blocks.WHITE_CONCRETE)
                .add(Blocks.ORANGE_CONCRETE)
                .add(Blocks.MAGENTA_CONCRETE)
                .add(Blocks.LIGHT_BLUE_CONCRETE)
                .add(Blocks.YELLOW_CONCRETE)
                .add(Blocks.LIME_CONCRETE)
                .add(Blocks.PINK_CONCRETE)
                .add(Blocks.GRAY_CONCRETE)
                .add(Blocks.LIGHT_GRAY_CONCRETE)
                .add(Blocks.CYAN_CONCRETE)
                .add(Blocks.PURPLE_CONCRETE)
                .add(Blocks.BLUE_CONCRETE)
                .add(Blocks.BROWN_CONCRETE)
                .add(Blocks.GREEN_CONCRETE)
                .add(Blocks.RED_CONCRETE)
                .add(Blocks.BLACK_CONCRETE);

    }
}
