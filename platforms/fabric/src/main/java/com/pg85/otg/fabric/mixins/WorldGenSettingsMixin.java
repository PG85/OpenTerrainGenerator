package com.pg85.otg.fabric.mixins;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.fabric.gen.OTGNoiseChunkGenerator;
import com.pg85.otg.util.logging.LogCategory;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.dedicated.DedicatedServerProperties.WorldGenProperties;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;
import java.util.Random;
import java.util.logging.Logger;

@Mixin(WorldGenSettings.class)
public abstract class WorldGenSettingsMixin {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void injectWorldCreation(RegistryAccess ra, WorldGenProperties wgp, CallbackInfoReturnable<WorldGenSettings> cir)
    {
        String levelType = wgp.levelType().trim().toLowerCase();
        //for the otg level type
        if(levelType == Constants.MOD_ID_SHORT) {
            System.out.println("OTG FABRIC TESTING 123");
            //does this work this early?
            LogManager.getLogger(Constants.MOD_ID_SHORT).info(LogCategory.MAIN + " Initializing OTG world...");

            //parse the seed
            OptionalLong parsedInputtedSeed = WorldGenSettings.parseSeed(wgp.levelSeed());
            long seed = 0;

            if(parsedInputtedSeed.isEmpty()) {
                seed = (new Random()).nextLong(); //gen random seed if none given
            }
            else {
                seed = parsedInputtedSeed.getAsLong(); //otherwise use the inputted seed
            }

            Registry<DimensionType> dimensionTypeRegistry = ra.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
            Registry<Biome> biomeRegistry = ra.registryOrThrow(Registry.BIOME_REGISTRY);
            Registry<StructureSet> structureSetRegistry = ra.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
            Registry<LevelStem> levelStemRegistry = ra.registryOrThrow(Registry.LEVEL_STEM_REGISTRY);

            cir.setReturnValue(new WorldGenSettings(
                    seed, //seed
                    wgp.generateStructures(), //generate stuctujres
                    false, //generate bonus chest
                    WorldGenSettings.withOverworld(
                            dimensionTypeRegistry,
                            biomeRegistry,
                            new OTGNoiseChunkGenerator(

                            )

                    )

            ));

        }
    }

}
