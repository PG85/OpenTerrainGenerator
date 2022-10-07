package com.pg85.otg.fabric.util;

import com.pg85.otg.constants.Constants;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.tags.Tag;

//Biome tag manager
public class FabricBiomeTags extends FabricTagProvider<Biome> {

    private static final String TARGET_FOLDER = "biomes";
    private static final String TAG_MAN_NAME = "FabricBiomeTags";

    private static final Tag.Named<Biome> BIOME_TAG_KEYS =
            TagFactory.BIOME.create(new ResourceLocation(Constants.MOD_ID_SHORT, "biomes"));

    public FabricBiomeTags(FabricDataGenerator fdg) {
        super(fdg, BuiltinRegistries.BIOME, TARGET_FOLDER, TAG_MAN_NAME);
    }

    @Override
    protected void generateTags() {
        return;
    }

    public void addBiomeTag(Biome b) {
        getOrCreateTagBuilder(BIOME_TAG_KEYS).add(b);
    }
}
