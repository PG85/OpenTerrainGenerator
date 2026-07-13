package com.pg85.otg.shared.mixin.util;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class RegistryUtil {
    public static <T> Optional<Holder.Reference<T>> getAsReference(Registry<T> registry, ResourceLocation key) {
        return registry.getOptional(key)
                .flatMap(registry::getResourceKey)
                .flatMap(registry::getHolder);
    }
}
