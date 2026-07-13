package com.pg85.otg.shared.mixin;

import com.pg85.otg.shared.biome.SharedBiomeLoader;
import com.pg85.otg.shared.biome.SharedLegacyBiomeLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.biome.BiomeData;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeData.class)
public class BiomeDataMixin {

    // Inject code at the end of the bootstrap method
    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void storeHolderGetters(BootstapContext<Biome> arg, CallbackInfo ci) {
        SharedLegacyBiomeLoader.PLACED_FEATURE_HOLDER = arg.lookup(Registries.PLACED_FEATURE);
        SharedLegacyBiomeLoader.CONFIGURED_CARVER_HOLDER = arg.lookup(Registries.CONFIGURED_CARVER);
        SharedLegacyBiomeLoader.BIOME_DATA_INITIALIZED = true;

        SharedBiomeLoader.PLACED_FEATURE_HOLDER = arg.lookup(Registries.PLACED_FEATURE);
        SharedBiomeLoader.CONFIGURED_CARVER_HOLDER = arg.lookup(Registries.CONFIGURED_CARVER);
        SharedBiomeLoader.BIOME_DATA_INITIALIZED = true;
    }
}