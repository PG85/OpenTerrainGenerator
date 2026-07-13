package com.pg85.otg.shared.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ChunkAccess.class)
public interface ChunkAccessAccessor {
    @Accessor("sections")
    void setSections(LevelChunkSection[] sections);

    @Accessor("heightmaps")
    void setHeightmaps (Map<Heightmap.Types, Heightmap> heightmaps);
}
