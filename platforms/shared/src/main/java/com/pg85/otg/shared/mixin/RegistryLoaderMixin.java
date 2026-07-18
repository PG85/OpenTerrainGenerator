package com.pg85.otg.shared.mixin;

import com.mojang.datafixers.util.Pair;
import com.pg85.otg.shared.registry.OTGRegistryHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Map;

// Keep this class logic-free: it lives in the Mixin-policed package, where any
// javac synthetic (enum-switch $SwitchMap, anonymous class) becomes unloadable
// after the Forgix merge suffix-renames it past Mixin's Outer$Inner companion
// detection. All work happens in OTGRegistryHelper.
@Mixin(RegistryDataLoader.class)
@SuppressWarnings("unused") // Mixins are by nature unused
public class RegistryLoaderMixin {
    // Explainer: The injector below needs to match:
    // 1. The method signature of the target method
    // 2. The CallbackInfoReturnable parameter (not normal CallbackInfo)
    // 3. The local variables in the target method, in order
    // Only then do we get access to the list of registries
    @Inject(
            method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;" +
                    "Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
            at = @At(
                value = "INVOKE",
                target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
                ordinal = 1
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    @SuppressWarnings("rawtypes") // Raw types required for this to work, not used in the code
    private static void loadOTGPresets(ResourceManager resourceManager, RegistryAccess registryAccess, List<RegistryDataLoader.RegistryData<?>> list,
                                       CallbackInfoReturnable ci, Map errorMap, List<Pair<WritableRegistry<?>, Object>> registries) {
        OTGRegistryHelper.loadOTGPresets(registries, registryAccess);
    }
}
