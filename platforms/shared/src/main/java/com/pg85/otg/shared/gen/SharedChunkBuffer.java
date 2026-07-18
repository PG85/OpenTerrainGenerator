package com.pg85.otg.shared.gen;

import com.pg85.otg.shared.materials.SharedMaterialData;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.gen.ChunkBuffer;
import com.pg85.otg.util.materials.LocalMaterialData;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

@Getter
public class SharedChunkBuffer extends ChunkBuffer {
    private final ChunkAccess chunkAccess;

    public SharedChunkBuffer(ChunkAccess chunkAccess) {
        this.chunkAccess = chunkAccess;
    }

    @Override
    public ChunkCoordinate getChunkCoordinate() {
        return ChunkCoordinate.fromChunkCoords(chunkAccess.getPos().x, chunkAccess.getPos().z);
    }

    @Override
    public void setBlock(int blockX, int blockY, int blockZ, LocalMaterialData material) {
        SharedMaterialData fabricMaterialData = (SharedMaterialData) material;
        chunkAccess.setBlockState(new BlockPos(blockX, blockY, blockZ), fabricMaterialData.getState(), false);
    }

    @Override
    public LocalMaterialData getBlock(int blockX, int blockY, int blockZ) {
        return SharedMaterialData.ofBlockState(chunkAccess.getBlockState(new BlockPos(blockX, blockY, blockZ)));
    }
}
