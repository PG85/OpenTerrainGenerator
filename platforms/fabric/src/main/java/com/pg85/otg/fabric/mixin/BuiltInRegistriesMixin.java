package com.pg85.otg.fabric.mixin;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.shared.biome.SharedOTGBiomeProvider;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltInRegistries.class)
public class BuiltInRegistriesMixin {
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void registerGeneratorAndBiomeSource(CallbackInfo ci) {
        Registry.register(BuiltInRegistries.BIOME_SOURCE, new ResourceLocation(Constants.MOD_ID_SHORT, Constants.MOD_ID_SHORT), SharedOTGBiomeProvider.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, new ResourceLocation(Constants.MOD_ID_SHORT, Constants.MOD_ID_SHORT), SharedOTGChunkGenerator.CODEC);
    }
}
