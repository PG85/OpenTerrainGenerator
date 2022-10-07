package com.pg85.otg.fabric.gen;

import com.pg85.otg.fabric.materials.FabricMaterialData;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.gen.ChunkBuffer;
import com.pg85.otg.util.materials.LocalMaterialData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;

public class FabricChunkBuffer extends ChunkBuffer {
    private final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    private final ProtoChunk chunk;

    FabricChunkBuffer(ProtoChunk chunk)
    {
        this.chunk = chunk;
    }

    @Override
    public ChunkCoordinate getChunkCoordinate()
    {
        ChunkPos pos = this.chunk.getPos();
        return ChunkCoordinate.fromChunkCoords(pos.x, pos.z);
    }

    @Override
    public void setBlock(int internalX, int blockY, int internalZ, LocalMaterialData material)
    {
        this.chunk.setBlockState(this.mutable.set(internalX, blockY, internalZ), ((FabricMaterialData) material).internalBlock(), false);
    }

    @Override
    public LocalMaterialData getBlock(int internalX, int blockY, int internalZ)
    {
        BlockState blockState = this.chunk.getBlockState(this.mutable.set(internalX, blockY, internalZ));
        return blockState == null ? null : FabricMaterialData.ofBlockState(blockState);
    }

    public ChunkAccess getChunk()
    {
        return this.chunk;
    }
}
