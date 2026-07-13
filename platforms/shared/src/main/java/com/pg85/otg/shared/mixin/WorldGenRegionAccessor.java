package com.pg85.otg.shared.mixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(WorldGenRegion.class)
public interface WorldGenRegionAccessor {
    @Accessor("cache")
    List<ChunkAccess> getCache();
}
