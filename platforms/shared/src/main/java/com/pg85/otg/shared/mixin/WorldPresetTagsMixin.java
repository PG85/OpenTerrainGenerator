package com.pg85.otg.shared.mixin;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.shared.mixin.util.RegistryUtil;
import com.pg85.otg.util.OTGLog;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Mixin(ReloadableServerResources.class)
public class WorldPresetTagsMixin {
    @Inject(method = "updateRegistryTags", at = @At("RETURN"))
    private void addOurOwnTags(RegistryAccess registryAccess, CallbackInfo ci) {
        OTGLog.getLogger().info("Adding OTG presets to the world preset tags");
        var presets = registryAccess.registryOrThrow(Registries.WORLD_PRESET);
        // get all the tags from the presets registry
        var tags = presets.getTags();
        HashMap<TagKey<WorldPreset>, List<Holder<WorldPreset>>> collected = tags.collect(
                HashMap::new,
                (tagMap, pair) -> tagMap.put(pair.getFirst(), new ArrayList<>(pair.getSecond().stream().toList())),
                HashMap::putAll);
        // add our own presets to the tags
        presets.keySet().forEach(id -> {
            // check it's in the OTG name space
            if (!id.getNamespace().equalsIgnoreCase(Constants.MOD_ID_SHORT)) {
                //OTGLog.getLogger().info("Skipping preset %s as it's not in the OTG namespace", id.toString());
                return;
            }
            // can't use direct holders, need reference
            Holder.Reference<WorldPreset> holder = getAsReference(presets, id).orElse(null);
            if (holder == null) {
                OTGLog.getLogger().error("Preset %s does not exist!", id.toString());
                return;
            }
            OTGLog.getLogger().info("Adding preset %s to the tags", id.toString());
            collected.computeIfAbsent(WorldPresetTags.NORMAL, tag -> new ArrayList<>())
                    .add(holder);
        });

        // print tags

        presets.bindTags(collected);
    }
    private static <T> Optional<Holder.Reference<T>> getAsReference(Registry<T> registry, ResourceLocation key) {
        return registry.getOptional(key)
                .flatMap(registry::getResourceKey)
                .flatMap(registry::getHolder);
    }
}
