package com.pg85.otg.gen.carver;

import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.config.settings.biome.SurfaceSettings;
import com.pg85.otg.config.settings.preset.CarverSettings;
import com.pg85.otg.config.settings.preset.PresetSettings;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.interfaces.ICachedBiomeProvider;
import com.pg85.otg.interfaces.ISurfaceGeneratorNoiseProvider;
import com.pg85.otg.util.MutableBoolean;
import com.pg85.otg.util.gen.ChunkBuffer;
import com.pg85.otg.util.gen.DecorationArea;
import com.pg85.otg.util.gen.OTGWorldInfo;
import com.pg85.otg.util.helpers.MathHelper;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.LocalMaterials;

import java.util.BitSet;
import java.util.Random;

public abstract class Carver {
    protected final PresetSettings presetConfig;
    protected final CarverSettings carverSettings;

    public Carver(PresetSettings presetConfig) {
        this.presetConfig = presetConfig;
        this.carverSettings = presetConfig.getCarverSettings();
    }

    public int getBranchFactor() {
        return 4;
    }

    protected boolean carveRegion(
            ISurfaceGeneratorNoiseProvider noiseProvider,
            float[] cache,
            ChunkBuffer chunkBuffer,
            long seed,
            int chunkX,
            int chunkZ,
            double localX,
            double localY,
            double localZ,
            double yaw,
            double pitch,
            BitSet carvingMask,
            ICachedBiomeProvider cachedBiomeProvider,
            OTGWorldInfo otgWorldInfo
    ) {
        Random random = new Random(seed + (long) chunkX + (long) chunkZ);
        double chunkStartX =
                chunkX * Constants.CHUNK_SIZE + DecorationArea.CARVER_OFFSET;
        double chunkStartZ =
                chunkZ * Constants.CHUNK_SIZE + DecorationArea.CARVER_OFFSET;
        boolean carved;
        BiomeSettings[] biomeConfigs =
                cachedBiomeProvider.getBiomeConfigsForChunk(chunkBuffer.getChunkCoordinate());
        if (localX < chunkStartX - 16.0D - yaw * 2.0D
            || localZ < chunkStartZ - 16.0D - yaw * 2.0D
            || localX > chunkStartX + 16.0D + yaw * 2.0D
            || localZ > chunkStartZ + 16.0D + yaw * 2.0D) {
            return false;
        }
        int minX = Math.max(
                MathHelper.floor(localX - yaw)
                - chunkX * Constants.CHUNK_SIZE
                - 1, 0
        );
        int maxX = Math.min(
                MathHelper.floor(localX + yaw) - chunkX * Constants.CHUNK_SIZE + 1,
                Constants.CHUNK_SIZE
        );
        int minY =
                Math.max(MathHelper.floor(localY - pitch) - 1, otgWorldInfo.minY() + 8);
        // +8 for consistency, but was originally Y = 1
        int maxY =
                Math.min(MathHelper.floor(localY + pitch) + 1, otgWorldInfo.maxY() - 8);
        int minZ = Math.max(
                MathHelper.floor(localZ - yaw)
                - chunkZ * Constants.CHUNK_SIZE
                - 1, 0
        );
        int maxZ = Math.min(
                MathHelper.floor(localZ + yaw) - chunkZ * Constants.CHUNK_SIZE + 1,
                Constants.CHUNK_SIZE
        );
        int worldX;
        double scaledX;
        int worldZ;
        double scaledZ;
        double scaledY;
        BiomeSettings biomeConfig;
        MutableBoolean foundSurface;
        if (this.isRegionUncarvable(
                chunkBuffer,
                chunkX,
                chunkZ,
                minX,
                maxX,
                minY,
                maxY,
                minZ,
                maxZ
        )) {
            return false;
        }
        carved = false;
        for (int currentX = minX; currentX < maxX; ++currentX) {
            worldX = currentX + chunkX * Constants.CHUNK_SIZE;
            scaledX = ((double) worldX + 0.5D - localX) / yaw;
            for (int currentZ = minZ; currentZ < maxZ; ++currentZ) {
                worldZ = currentZ + chunkZ * Constants.CHUNK_SIZE;
                biomeConfig = biomeConfigs[currentX * Constants.CHUNK_SIZE + currentZ];
                scaledZ = ((double) worldZ + 0.5D - localZ) / yaw;
                if (!(scaledX * scaledX + scaledZ * scaledZ >= 1.0D))
                //if (f * f + g * g < 1.0D)
                {
                    foundSurface = new MutableBoolean(false);
                    for (int currentY = maxY; currentY > minY; --currentY) {
                        scaledY = ((double) currentY - 0.5D - localY) / pitch;
                        double ravineY = cache == null ? 0.0D : cache[currentY - otgWorldInfo.minY() -1];// Zero-indexed, could use cleanup
                        if (!this.isPositionExcluded(
                                scaledX,
                                scaledY,
                                scaledZ,
                                ravineY
                        )) {
                            carved |= this.carveAtPoint(
                                    noiseProvider,
                                    chunkBuffer,
                                    carvingMask,
                                    worldX,
                                    worldZ,
                                    currentX,
                                    currentY,
                                    currentZ,
                                    foundSurface,
                                    biomeConfig,
                                    otgWorldInfo
                            );
                        }
                    }
                }
            }
        }
        return carved;
    }

    protected boolean carveAtPoint(
            ISurfaceGeneratorNoiseProvider noiseProvider,
            ChunkBuffer chunkBuffer,
            BitSet carvingMask,
            int worldX,
            int worldZ,
            int relativeX,
            int y,
            int relativeZ,
            MutableBoolean foundSurface,
            BiomeSettings biomeConfig,
            OTGWorldInfo otgWorldInfo
    ) {
        SurfaceSettings surface = biomeConfig.getSurfaceSettings();
        // Offset Y by minY to ensure a non-negative BitSet index (1.18+ worlds can have negative Y)
        int offsetY = y - otgWorldInfo.minY();
        int i = relativeX | relativeZ << 4 | offsetY << 8;
        if (carvingMask.get(i)) {
            return false;
        }
        carvingMask.set(i);

        LocalMaterialData material = chunkBuffer.getBlock(worldX, y, worldZ);
        if (material.isNonCaveAir() || material.isMaterial(LocalMaterials.WATER)) {
            return false;
        }

        LocalMaterialData blockAbove = chunkBuffer.getBlock(worldX, y + 1, worldZ);

        // Check to see if we've found the surface, if so place surfaceblocks.
        // TODO: Search a larger height up instead of just the current carving sphere?
        // Vanilla logic
        // Normally doesn't see sand as surface?
        if (material.isMaterial(surface.getSurfaceBlockAtHeight(
                noiseProvider,
                worldX,
                y - 1,
                worldZ
        ))) {
            foundSurface.setValue(true);
        }
        if (!material.isSolid() || blockAbove.isMaterial(LocalMaterials.WATER)) {
            return false;
        }
        if (y <= this.presetConfig.getTerrainSettings().getCarverLavaBlockHeight()) {
            // Not sure Why world coords are passed to chunkbuffer, it just does >> 4.
            chunkBuffer.setBlock(
                    worldX,
                    y,
                    worldZ,
                    this.presetConfig.getBlockSettings().getCarverLavaBlock()
            );
        } else {
            chunkBuffer.setBlock(worldX, y, worldZ, LocalMaterials.CAVE_AIR);
            if (foundSurface.isValue()) {
                LocalMaterialData blockBelow =
                        chunkBuffer.getBlock(worldX, y - 1, worldZ);
                if (blockBelow.isMaterial(surface.getGroundBlockAtHeight(
                        noiseProvider,
                        worldX,
                        y - 1,
                        worldZ
                ))) {
                    chunkBuffer.setBlock(
                            worldX,
                            y - 1,
                            worldZ,
                            surface.getSurfaceBlockAtHeight(
                                    noiseProvider,
                                    worldX,
                                    y - 1,
                                    worldZ
                            )
                    );
                }
            }
        }

        return true;
    }

    protected boolean isRegionUncarvable(
            ChunkBuffer chunk,
            int mainChunkX,
            int mainChunkZ,
            int relMinX,
            int relMaxX,
            int minY,
            int maxY,
            int relMinZ,
            int relMaxZ
    ) {
        for (int i = relMinX; i < relMaxX; ++i) {
            for (int j = relMinZ; j < relMaxZ; ++j) {
                for (int k = minY - 1; k <= maxY + 1; ++k) {
                    if (chunk.getBlock(
                            i + mainChunkX * Constants.CHUNK_SIZE,
                            k,
                            j + mainChunkZ * Constants.CHUNK_SIZE
                    ).isMaterial(LocalMaterials.WATER)) {
                        return true;
                    }

                    if (k != maxY + 1 && !this.isOnBoundary(
                            relMinX,
                            relMaxX,
                            relMinZ,
                            relMaxZ,
                            i,
                            j
                    )) {
                        k = maxY;
                    }
                }
            }
        }

        return false;
    }

    private boolean isOnBoundary(int minX, int maxX, int minZ, int maxZ, int x, int z) {
        return x == minX || x == maxX - 1 || z == minZ || z == maxZ - 1;
    }

    protected boolean canCarveBranch(
            int mainChunkX,
            int mainChunkZ,
            double x,
            double z,
            int branch,
            int branchCount,
            float baseWidth
    ) {
        double d = mainChunkX * Constants.CHUNK_SIZE + DecorationArea.CARVER_OFFSET;
        double e = mainChunkZ * Constants.CHUNK_SIZE + DecorationArea.CARVER_OFFSET;
        double f = x - d;
        double g = z - e;
        double h = branchCount - branch;
        double i = baseWidth + 2.0F + 16.0F;
        return f * f + g * g - h * h <= i * i;
    }

    public abstract boolean carve(
            ISurfaceGeneratorNoiseProvider noiseProvider,
            ChunkBuffer chunk,
            Random random,
            int chunkX,
            int chunkZ,
            int mainChunkX,
            int mainChunkZ,
            BitSet carvingMask,
            ICachedBiomeProvider cachedBiomeProvider,
            OTGWorldInfo otgWorldInfo
    );

    public abstract boolean isStartChunk(Random random, int chunkX, int chunkZ);

    protected abstract boolean isPositionExcluded(
            double scaledRelativeX,
            double scaledRelativeY,
            double scaledRelativeZ,
            double y
    );
}
