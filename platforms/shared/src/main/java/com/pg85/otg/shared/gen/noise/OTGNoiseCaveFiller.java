package com.pg85.otg.shared.gen.noise;

import com.pg85.otg.shared.mixin.NoiseChunkAccessor;
import com.pg85.otg.util.gen.ChunkBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.ChunkPos;

/**
 * Fills a chunk's base terrain from a NoiseChunk, mirroring NoiseBasedChunkGenerator.doFill
 * (1.20.1). Used for the vanilla-noise-cave path instead of OTGChunkGenerator.populateNoise.
 *
 * Differences from vanilla's doFill:
 * - Runs synchronously (no section acquire/release; the caller doesn't hop executors).
 * - Records the highest placed block per column on the ChunkBuffer, which OTG's surface and
 *   ground control needs.
 * - No debug hooks.
 */
public final class OTGNoiseCaveFiller {
    private OTGNoiseCaveFiller() {
    }

    public static void fill(
            NoiseChunk noiseChunk,
            ChunkAccess chunkAccess,
            ChunkBuffer buffer,
            NoiseGeneratorSettings settings
    ) {
        NoiseSettings noiseSettings = settings.noiseSettings().clampToHeightAccessor(chunkAccess.getHeightAccessorForGeneration());
        int cellHeight = noiseSettings.getCellHeight();
        int cellWidth = noiseSettings.getCellWidth();
        int minCellY = Mth.floorDiv(noiseSettings.minY(), cellHeight);
        int cellCountY = Mth.floorDiv(noiseSettings.height(), cellHeight);
        if (cellCountY <= 0) {
            return;
        }
        int cellCountXZ = 16 / cellWidth;

        Heightmap oceanFloor = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunkAccess.getPos();
        int minBlockX = chunkPos.getMinBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int cellX = 0; cellX < cellCountXZ; cellX++) {
            noiseChunk.advanceCellX(cellX);

            for (int cellZ = 0; cellZ < cellCountXZ; cellZ++) {
                int sectionIndex = chunkAccess.getSectionsCount() - 1;
                LevelChunkSection section = chunkAccess.getSection(sectionIndex);

                for (int cellY = cellCountY - 1; cellY >= 0; cellY--) {
                    noiseChunk.selectCellYZ(cellY, cellZ);

                    for (int inCellY = cellHeight - 1; inCellY >= 0; inCellY--) {
                        int blockY = (minCellY + cellY) * cellHeight + inCellY;
                        int sectionLocalY = blockY & 15;
                        int blockSectionIndex = chunkAccess.getSectionIndex(blockY);
                        if (sectionIndex != blockSectionIndex) {
                            sectionIndex = blockSectionIndex;
                            section = chunkAccess.getSection(blockSectionIndex);
                        }

                        noiseChunk.updateForY(blockY, (double) inCellY / cellHeight);

                        for (int inCellX = 0; inCellX < cellWidth; inCellX++) {
                            int localX = cellX * cellWidth + inCellX;
                            int blockX = minBlockX + localX;
                            noiseChunk.updateForX(blockX, (double) inCellX / cellWidth);

                            for (int inCellZ = 0; inCellZ < cellWidth; inCellZ++) {
                                int localZ = cellZ * cellWidth + inCellZ;
                                int blockZ = minBlockZ + localZ;
                                noiseChunk.updateForZ(blockZ, (double) inCellZ / cellWidth);

                                BlockState state = ((NoiseChunkAccessor) noiseChunk).callGetInterpolatedState();
                                if (state == null) {
                                    state = settings.defaultBlock();
                                }
                                if (state.isAir()) {
                                    continue;
                                }

                                section.setBlockState(localX, sectionLocalY, localZ, state, false);
                                oceanFloor.update(localX, blockY, localZ, state);
                                worldSurface.update(localX, blockY, localZ, state);
                                buffer.setHighestBlockForColumn(localX, localZ, blockY);
                                if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
                                    mutablePos.set(blockX, blockY, blockZ);
                                    chunkAccess.markPosForPostprocessing(mutablePos);
                                }
                            }
                        }
                    }
                }
            }

            noiseChunk.swapSlices();
        }

        noiseChunk.stopInterpolation();
    }
}
