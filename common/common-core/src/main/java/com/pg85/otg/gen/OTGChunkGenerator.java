package com.pg85.otg.gen;

import com.pg85.otg.OTG;
import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.config.settings.biome.BiomeTerrainSettings;
import com.pg85.otg.config.settings.preset.TerrainSettings;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.gen.biome.CachedBiomeProvider;
import com.pg85.otg.gen.carver.Carver;
import com.pg85.otg.gen.carver.CaveCarver;
import com.pg85.otg.gen.carver.RavineCarver;
import com.pg85.otg.gen.noise.BlendedBiomeParams;
import com.pg85.otg.gen.noise.OctavePerlinNoiseSampler;
import com.pg85.otg.gen.noise.TerrainNoisePipeline;
import com.pg85.otg.gen.noise.legacy.NoiseGeneratorPerlinMesaBlocks;
import com.pg85.otg.interfaces.*;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.gen.*;
import com.pg85.otg.util.helpers.MathHelper;
import com.pg85.otg.util.logging.LogCategory;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import lombok.Getter;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Generates the base terrain, sets stone/ground/surface blocks and does SurfaceAndGroundControl, generates caves and canyons.
 */
//@SuppressWarnings("deprecation")
public class OTGChunkGenerator implements ISurfaceGeneratorNoiseProvider {

    private static final float[] BIOME_WEIGHT_TABLE = make(
            new float[65 * 65], (array) -> {
                for (int x = -32; x <= 32; ++x) {
                    for (int z = -32; z <= 32; ++z) {
                        float f = 10.0F / MathHelper.sqrt((float) (x * x + z * z) + 0.2F);
                        array[x + 32 + (z + 32) * 65] = f;
                    }
                }
            }
    );

    private static final float[] NOISE_WEIGHT_TABLE = make(
            new float[24 * 24 * 24], (array) -> {
                for (int z = 0; z < 24; ++z) {
                    for (int x = 0; x < 24; ++x) {
                        for (int y = 0; y < 24; ++y) {
                            array[z * 24 * 24 + x * 24 + y] = (float) calculateNoiseWeight(x - 12, y - 12, z - 12);
                        }
                    }
                }
            }
    );

    // TODO: ThreadLocal is used mostly as a crutch here, ideally these classes wouldn't maintain state.
    // ThreadLocal may have some overhead for the gets/sets, even when used on a single thread.
    // Some of these classes may not be thread-safe (tho testing seems ok), need to check all the internal state.

    private TerrainNoisePipeline noisePipeline;

    private final Preset preset;
    private long seed;
    private final CachedBiomeProvider cachedBiomeProvider;

    private static final int NOISE_SIZE_X = 4;
    @Getter
    private final int noiseSizeY;
    private static final int NOISE_SIZE_Z = 4;

    private final ThreadLocal<NoiseCache> noiseCache;
    private NoiseGeneratorPerlinMesaBlocks biomeBlocksNoiseGen;
    // Carvers
    private final Carver caves;
    private final Carver ravines;
    // Biome blocks noise
    // TODO: Use new noise?
    private final ThreadLocal<double[]> biomeBlocksNoise =
            ThreadLocal.withInitial(() -> new double[Constants.CHUNK_SIZE * Constants.CHUNK_SIZE]);
    private final ThreadLocal<Integer> lastX = ThreadLocal.withInitial(() -> Integer.MAX_VALUE);
    private final ThreadLocal<Integer> lastZ = ThreadLocal.withInitial(() -> Integer.MAX_VALUE);
    private final ThreadLocal<Double> lastNoise = ThreadLocal.withInitial(() -> 0d);

    public OTGChunkGenerator(
            Preset preset,
            ILayerSource biomeProvider,
            IBiome[] biomesById,
            OTGWorldInfo otgWorldInfo
    ) {
        this.preset = preset;
        this.cachedBiomeProvider = new CachedBiomeProvider(biomeProvider, biomesById);

        this.noiseSizeY = otgWorldInfo.getHeight() / Constants.PIECE_Y_SIZE;
        this.noiseCache = ThreadLocal.withInitial(() -> new NoiseCache(128, this.noiseSizeY + 1));

        this.caves = new CaveCarver(preset.getPresetConfig());
        this.ravines = new RavineCarver(preset.getPresetConfig());

    }

    public void setSeed(long seed) {
        this.seed = seed;
        this.cachedBiomeProvider.setSeed(seed);
        // Setup noises
        Random random = new Random(seed);

        // Volatility noise
        OctavePerlinNoiseSampler interpolationNoise = new OctavePerlinNoiseSampler(random, IntStream.rangeClosed(-7, 0));
        // Volatility1 noise
        OctavePerlinNoiseSampler lowerInterpolatedNoise = new OctavePerlinNoiseSampler(random, IntStream.rangeClosed(-15, 0));
        // Volatility2 noise
        OctavePerlinNoiseSampler upperInterpolatedNoise = new OctavePerlinNoiseSampler(random, IntStream.rangeClosed(-15, 0));
        OctavePerlinNoiseSampler depthNoise = new OctavePerlinNoiseSampler(random, IntStream.rangeClosed(-15, 0));
        this.biomeBlocksNoiseGen = new NoiseGeneratorPerlinMesaBlocks(random, 4);

        TerrainSettings terrainSettings = this.preset.getPresetConfig().getTerrainSettings();
        this.noisePipeline = new TerrainNoisePipeline(
                interpolationNoise,
                lowerInterpolatedNoise,
                upperInterpolatedNoise,
                depthNoise,
                this.noiseSizeY,
                terrainSettings.getWorldHeightScale() / Constants.PIECE_Y_SIZE + 1, // surfaceSections
                terrainSettings.getContinentalScale(),
                terrainSettings.getContinentalBias(),
                terrainSettings.getBaseHeightFraction(),
                terrainSettings.getBiomeHeightWeight(),
                terrainSettings.getContinentalHeightWeight(),
                terrainSettings.getFalloffSteepness(),
                terrainSettings.getNoiseAmplitude()
        );
    }

    public ICachedBiomeProvider getCachedBiomeProvider() {
        return this.cachedBiomeProvider;
    }

    private static <T> T make(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }

    private static double getNoiseWeight(int x, int y, int z) {
        int arrayX = x + 12;
        int arrayZ = y + 12;
        int arrayY = z + 12;
        if (arrayX >= 0 && arrayX < 24) {
            if (arrayZ >= 0 && arrayZ < 24) {
                return arrayY >= 0 && arrayY < 24 ?
                        (double) NOISE_WEIGHT_TABLE[arrayY * 24 * 24 + arrayX * 24 + arrayZ] :
                        0.0D;
            } else {
                return 0.0D;
            }
        } else {
            return 0.0D;
        }
    }

    private static double calculateNoiseWeight(int x, int y, int z) {
        // Make a circle cutout
        double sqrXZ = x * x + z * z;

        // Offset the y to prevent 0
        double offsetY = (double) y + 0.5D;

        // Square the y to make a
        double sqrY = offsetY * offsetY;

        // Get the density of the current position
        double density = Math.pow(Math.E, -(sqrY / 16.0D + sqrXZ / 16.0D));

        // Controls the density (bottom is solid, top is air)
        double yOffset = -offsetY * MathHelper.fastInverseSqrt(sqrY / 2.0D + sqrXZ / 2.0D) / 2.0D;

        // Multiply the density by the y offset to get the final density
        return yOffset * density;
    }


    public void getNoiseColumn(double[] buffer, int x, int z) {
        // TODO: check only for edges
        this.noiseCache.get().get(buffer, x, z);
    }

    private void generateNoiseColumn(double[] noiseColumn, int noiseX, int noiseZ) {
        BlendedColumn blended = blendColumnParams(noiseX, noiseZ);
        this.noisePipeline.generateColumn(
                noiseColumn, noiseX, noiseZ, blended.params(), blended.chc(),
                blended.disableBiomeHeight()
        );
    }

    /**
     * Blended terrain center height for a noise column, in blocks. This is the surface anchor
     * of the falloff gradient, before detail noise; the vanilla noise cave depth proxy keys
     * cave depth to it so cave placement is independent of biome volatility.
     */
    public double getColumnCenterHeightInBlocks(int noiseX, int noiseZ) {
        BlendedBiomeParams params = blendColumnParams(noiseX, noiseZ).params();
        float extraHeight = (float) (this.noisePipeline.computeExtraHeight(
                noiseX, noiseZ, params.valleyFactor(), params.peakFactor()
        ) * this.noisePipeline.continentalScale());
        return this.noisePipeline.computeColumnHeight(params.height(), extraHeight, this.noisePipeline.surfaceSections()) * 8.0;
    }

    private record BlendedColumn(BlendedBiomeParams params, double[] chc, boolean disableBiomeHeight) {
    }

    private BlendedColumn blendColumnParams(int noiseX, int noiseZ) {
        BiomeSettings center = this.cachedBiomeProvider.getNoiseBiomeConfig(noiseX, noiseZ, true);

        float height = 0; // depth
        float biomeVolatility = 0;
        double volatility1 = 0;
        double volatility2 = 0;
        double horizontalFracture = 0;
        double verticalFracture = 0;
        double volatilityWeight1 = 0;
        double volatilityWeight2 = 0;
        double valleyFactor = 0;
        double peakFactor = 0;
        double[] chc = new double[this.noiseSizeY + 1];
        float weight = 0;
        int smoothRadius = center.getTerrainSettings().getSmoothRadius();
        int chcSmoothRadius = center.getTerrainSettings().getCHCSmoothRadius();
        int largestRadius = Math.max(smoothRadius, chcSmoothRadius);
        int areaSize = largestRadius * 2 + 1;
        BiomeSettings[] biomes = this.cachedBiomeProvider.getNoiseBiomeConfigsForRegion(
                noiseX - largestRadius,
                noiseZ - largestRadius,
                areaSize
        );
        BiomeSettings biome;
        BiomeTerrainSettings biomeTerrainSettings;
        TerrainSettings terrainSettings = this.preset.getPresetConfig().getTerrainSettings();
        int worldHeightCap = terrainSettings.getHeight();
        float heightAt;
        float weightAt;
        int cacheX;
        int cacheZ;
        for (int x1 = -smoothRadius; x1 <= smoothRadius; ++x1) {
            cacheX = x1 + largestRadius;
            for (int z1 = -smoothRadius; z1 <= smoothRadius; ++z1) {
                cacheZ = z1 + largestRadius;
                int biomeIndex = cacheX * areaSize + cacheZ;
                if (biomeIndex >= biomes.length || (biome = biomes[biomeIndex]) == null) {
                    // Provider can come up short at region edges; skip instead of crashing
                    continue;
                }
                biomeTerrainSettings = biome.getTerrainSettings();
                heightAt = biomeTerrainSettings.getBiomeHeight();
                // TODO: vanilla reduces the weight by half when the depth here is greater than the center depth, but OTG doesn't do that?
                weightAt = BIOME_WEIGHT_TABLE[x1 + 32 + (z1 + 32) * 65] / (heightAt + 2.0F);
                weightAt = Math.abs(weightAt); // This is required to prevent seams when height goes below -2

                weight += weightAt;

                height += heightAt * weightAt;
                biomeVolatility += biomeTerrainSettings.getBiomeVolatility() * weightAt;
                volatility1 += biomeTerrainSettings.getVolatility1() * weightAt;
                volatility2 += biomeTerrainSettings.getVolatility2() * weightAt;
                horizontalFracture += terrainSettings.getFractureHorizontal() * weightAt;
                verticalFracture += terrainSettings.getFractureVertical() * weightAt;
                volatilityWeight1 += biomeTerrainSettings.getVolatilityWeight1() * weightAt;
                volatilityWeight2 += biomeTerrainSettings.getVolatilityWeight2() * weightAt;
                valleyFactor += biomeTerrainSettings.getValleyFactor() * weightAt;
                peakFactor += biomeTerrainSettings.getPeakFactor() * weightAt;
            }
        }

        // CHC Smoothing
        double chcWeight = 0;
        for (int x1 = -chcSmoothRadius; x1 <= chcSmoothRadius; ++x1) {
            cacheX = x1 + largestRadius;
            for (int z1 = -chcSmoothRadius; z1 <= chcSmoothRadius; ++z1) {
                cacheZ = z1 + largestRadius;
                int chcBiomeIndex = cacheX * areaSize + cacheZ;
                if (chcBiomeIndex >= biomes.length || (biome = biomes[chcBiomeIndex]) == null) {
                    continue;
                }

                heightAt = biome.getTerrainSettings().getBiomeHeight();
                weightAt = BIOME_WEIGHT_TABLE[x1 + 32 + (z1 + 32) * 65] / (heightAt + 2.0F);
                weightAt = Math.abs(weightAt);

                chcWeight += weightAt;

                for (int y = 0; y < this.noiseSizeY + 1; y++) {
                    chc[y] += biome.getCHCData(y) * weightAt;
                }
            }
        }

        // Normalize biome data
        height /= weight;
        biomeVolatility /= weight;
        volatility1 /= weight;
        volatility2 /= weight;
        horizontalFracture /= weight;
        verticalFracture /= weight;
        volatilityWeight1 /= weight;
        volatilityWeight2 /= weight;
        valleyFactor /= weight;
        peakFactor /= weight;

        // Normalize CHC
        for (int y = 0; y < this.noiseSizeY + 1; y++) {
            chc[y] /= chcWeight;
        }

        BlendedBiomeParams params = new BlendedBiomeParams(
                height, biomeVolatility,
                volatility1, volatility2,
                horizontalFracture, verticalFracture,
                volatilityWeight1, volatilityWeight2,
                valleyFactor, peakFactor
        );

        return new BlendedColumn(params, chc, center.getTerrainSettings().isDisableBiomeHeight());
    }

    // Surface / ground / stone blocks / SAGC

    public void populateNoise(
            OTGWorldInfo worldHeight,
            ChunkBuffer buffer,
            ChunkCoordinate chunkCoord,
            ObjectList<JigsawStructureData> structures,
            Random random
    ) {
        ILogger logger = OTG.getEngine().getLogger();

        ObjectListIterator<JigsawStructureData> structureIterator = structures.iterator();

        long startTime = System.currentTimeMillis();

        // Fill waterLevel array, used when placing stone/ground/surface blocks.
        // This 256 is a combined x/z size, not y.
        int[] waterLevel = new int[Constants.CHUNK_SIZE * Constants.CHUNK_SIZE];

        int blockX = chunkCoord.getBlockX();
        int blockZ = chunkCoord.getBlockZ();

        IBiome[] biomes = this.cachedBiomeProvider.getBiomesForChunk(chunkCoord);
        for (int x = 0; x < Constants.CHUNK_SIZE; x++) {
            for (int z = 0; z < Constants.CHUNK_SIZE; z++) {
                // TODO: water levels used to be interpolated via bilinear interpolation. Do we still need to do that?
                waterLevel[x * Constants.CHUNK_SIZE + z] =
                        biomes[x * Constants.CHUNK_SIZE + z].getBiomeSettings().getSurfaceSettings().getWaterLevelMax();
            }
        }

        // TODO: this double[][][] is probably really bad for performance
        double[][][] noiseData = new double[2][NOISE_SIZE_Z + 1][this.noiseSizeY + 1];
        // Max smoothing radius is 32, so area covered is 32+5+32=69 (noise/biome coords, so *4)

        // Initialize noise data on the x0 column.
        for (int noiseZ = 0; noiseZ < NOISE_SIZE_Z + 1; ++noiseZ) {
            noiseData[0][noiseZ] = new double[this.noiseSizeY + 1];
            this.getNoiseColumn(
                    noiseData[0][noiseZ],
                    chunkCoord.getChunkX() * NOISE_SIZE_X,
                    chunkCoord.getChunkZ() * NOISE_SIZE_Z + noiseZ
            );
            noiseData[1][noiseZ] = new double[this.noiseSizeY + 1];
        }

        BiomeSettings biomeConfig;
        // [0, 4] -> x noise chunks
        int noiseZ;
        double x0z0y0;
        double x0z1y0;
        double x1z0y0;
        double x1z1y0;
        double x0z0y1;
        double x0z1y1;
        double x1z0y1;
        double x1z1y1;
        int realY;
        double yLerp;
        double x0z0;
        double x1z0;
        double x0z1;
        double x1z1;
        int realX;
        int localX;
        double xLerp;
        double z0;
        double z1;
        int realZ;
        int localZ;
        double zLerp;
        double rawNoise;
        double density;
        int structureX;
        int structureY;
        int structureZ;
        JigsawStructureData structure;
        double[][] xColumn;
        for (int noiseX = 0; noiseX < NOISE_SIZE_X; ++noiseX) {
            // Initialize noise data on the x1 column
            for (noiseZ = 0; noiseZ < NOISE_SIZE_Z + 1; ++noiseZ) {
                this.getNoiseColumn(
                        noiseData[1][noiseZ],
                        chunkCoord.getChunkX() * NOISE_SIZE_X + noiseX + 1,
                        chunkCoord.getChunkZ() * NOISE_SIZE_Z + noiseZ
                );
            }

            // [0, 4] -> z noise chunks
            for (noiseZ = 0; noiseZ < NOISE_SIZE_Z; ++noiseZ) {
                // [0, 32] -> y noise chunks
                for (int noiseY = this.noiseSizeY - 1; noiseY >= 0; --noiseY) {
                    // Lower samples
                    x0z0y0 = noiseData[0][noiseZ][noiseY];
                    x0z1y0 = noiseData[0][noiseZ + 1][noiseY];
                    x1z0y0 = noiseData[1][noiseZ][noiseY];
                    x1z1y0 = noiseData[1][noiseZ + 1][noiseY];
                    // Upper samples
                    x0z0y1 = noiseData[0][noiseZ][noiseY + 1];
                    x0z1y1 = noiseData[0][noiseZ + 1][noiseY + 1];
                    x1z0y1 = noiseData[1][noiseZ][noiseY + 1];
                    x1z1y1 = noiseData[1][noiseZ + 1][noiseY + 1];

                    // [0, 8] -> y noise pieces
                    for (int pieceY = 8 - 1; pieceY >= 0; --pieceY) {
                        realY = noiseY * 8 + pieceY;

                        // progress within loop
                        yLerp = (double) pieceY / 8.0;

                        // Interpolate noise data based on y progress
                        x0z0 = MathHelper.lerp(yLerp, x0z0y0, x0z0y1);
                        x1z0 = MathHelper.lerp(yLerp, x1z0y0, x1z0y1);
                        x0z1 = MathHelper.lerp(yLerp, x0z1y0, x0z1y1);
                        x1z1 = MathHelper.lerp(yLerp, x1z1y0, x1z1y1);

                        // [0, 4] -> x noise pieces
                        for (int pieceX = 0; pieceX < 4; ++pieceX) {
                            realX = blockX + noiseX * 4 + pieceX;
                            localX = realX & 15;
                            xLerp = (double) pieceX / 4.0;
                            // Interpolate noise based on x progress
                            z0 = MathHelper.lerp(xLerp, x0z0, x1z0);
                            z1 = MathHelper.lerp(xLerp, x0z1, x1z1);

                            // [0, 4) -> z noise pieces
                            for (int pieceZ = 0; pieceZ < 4; ++pieceZ) {
                                realZ = blockZ + noiseZ * 4 + pieceZ;
                                localZ = realZ & 15;
                                zLerp = (double) pieceZ / 4.0;
                                // Get the real noise here by interpolating the last 2 noises together
                                rawNoise = MathHelper.lerp(zLerp, z0, z1);
                                // Normalize the noise from (-256, 256) to [-1, 1]
                                density = MathHelper.clamp(rawNoise / 200.0D, -1.0D, 1.0D);

                                biomeConfig = biomes[localX * 16 + localZ].getBiomeSettings();

                                // TODO: make this bigger and look better
                                // Iterate through structures to add density
                                for (
                                        density = density / 2.0D - density * density * density / 24.0D;
                                        structureIterator.hasNext();
                                        density += getNoiseWeight(structureX, structureY, structureZ) * 0.8D
                                ) {
                                    structure = structureIterator.next();
                                    structureX = Math.max(0, Math.max(structure.minX - realX, realX - structure.maxX));
                                    structureY = realY - (structure.minY + (structure.useDelta ? structure.delta : 0));
                                    structureZ = Math.max(0, Math.max(structure.minZ - realZ, realZ - structure.maxZ));
                                }
                                structureIterator.back(structures.size());
                                if (density > 0.0) {
                                    buffer.setBlock(
                                            localX,
                                            realY,
                                            localZ,
                                            biomeConfig.getSurfaceSettings().getStoneBlockReplaced(realY)
                                    );
                                    buffer.setHighestBlockForColumn(pieceX + noiseX * 4, noiseZ * 4 + pieceZ, realY);
                                } else if (realY < waterLevel[localX * Constants.CHUNK_SIZE + localZ]
                                           && realY > biomeConfig.getSurfaceSettings().getWaterLevelMin()) {
                                    buffer.setBlock(
                                            localX,
                                            realY,
                                            localZ,
                                            biomeConfig.getSurfaceSettings().getWaterBlockReplaced(realY)
                                    );
                                    buffer.setHighestBlockForColumn(pieceX + noiseX * 4, noiseZ * 4 + pieceZ, realY);
                                }
                            }
                        }
                    }
                }
            }

            // Reuse noise data from the previous column for speed
            xColumn = noiseData[0];
            noiseData[0] = noiseData[1];
            noiseData[1] = xColumn;
        }

        doSurfaceAndGroundControl(biomes, random, worldHeight, this.seed, buffer, waterLevel);

        if (logger.getLogCategoryEnabled(LogCategory.PERFORMANCE) && (System.currentTimeMillis() - startTime) > 50) {
            logger.warn(
                    LogCategory.PERFORMANCE,
                    "Warning: Terrain generation for chunk at %s ~ %s took %s ms.",
                    chunkCoord.getBlockX() + DecorationArea.DECORATION_OFFSET,
                    chunkCoord.getBlockZ() + DecorationArea.DECORATION_OFFSET,
                    System.currentTimeMillis() - startTime
            );
        }
    }

    public void carve(ChunkBuffer chunk, long seed, BitSet carvingMask, boolean cavesEnabled, boolean ravinesEnabled, OTGWorldInfo otgWorldInfo) {
        // TODO: it should be possible to cache these carver graphs to make larger carvers more efficient and easier to use
        if (cavesEnabled || ravinesEnabled) {
            Random random = new Random();
            ChunkCoordinate chunkCoordinate = chunk.getChunkCoordinate();
            int chunkX = chunkCoordinate.getChunkX();
            int chunkZ = chunkCoordinate.getChunkZ();
            for (int localChunkX = chunkX - 8; localChunkX <= chunkX + 8; ++localChunkX) {
                for (int localChunkZ = chunkZ - 8; localChunkZ <= chunkZ + 8; ++localChunkZ) {
                    setCarverSeed(random, seed, localChunkX, localChunkZ);

                    if (cavesEnabled && this.caves.isStartChunk(random, localChunkX, localChunkZ)) {
                        this.caves.carve(
                                this,
                                chunk,
                                random,
                                localChunkX,
                                localChunkZ,
                                chunkX,
                                chunkZ,
                                carvingMask,
                                this.cachedBiomeProvider,
                                otgWorldInfo
                        );
                    }

                    setCarverSeed(random, seed, localChunkX, localChunkZ);

                    if (ravinesEnabled && this.ravines.isStartChunk(random, localChunkX, localChunkZ)) {
                        this.ravines.carve(
                                this,
                                chunk,
                                random,
                                localChunkX,
                                localChunkZ,
                                chunkX,
                                chunkZ,
                                carvingMask,
                                this.cachedBiomeProvider,
                                otgWorldInfo
                        );
                    }
                }
            }
        }
    }

    private void setCarverSeed(Random random, long seed, int x, int z) {
        random.setSeed(seed);
        long i = random.nextLong();
        long j = random.nextLong();
        long k = (long) x * i ^ (long) z * j ^ seed;
        random.setSeed(k);
    }

    /**
     * Runs surface and ground control for a chunk whose base terrain was filled outside
     * populateNoise (e.g. the vanilla noise cave fill path). Computes the per-column water
     * levels and biomes itself; requires the buffer's highest-block-per-column data to be
     * populated by the caller.
     */
    public void doSurfaceAndGroundControlForChunk(OTGWorldInfo worldInfo, ChunkBuffer buffer, Random random) {
        ChunkCoordinate chunkCoord = buffer.getChunkCoordinate();
        IBiome[] biomes = this.cachedBiomeProvider.getBiomesForChunk(chunkCoord);

        int[] waterLevel = new int[Constants.CHUNK_SIZE * Constants.CHUNK_SIZE];
        for (int x = 0; x < Constants.CHUNK_SIZE; x++) {
            for (int z = 0; z < Constants.CHUNK_SIZE; z++) {
                waterLevel[x * Constants.CHUNK_SIZE + z] =
                        biomes[x * Constants.CHUNK_SIZE + z].getBiomeSettings().getSurfaceSettings().getWaterLevelMax();
            }
        }

        doSurfaceAndGroundControl(biomes, random, worldInfo, this.seed, buffer, waterLevel);
    }

    private void doSurfaceAndGroundControl(
            IBiome[] biomes,
            Random random,
            OTGWorldInfo OTGWorldInfo,
            long worldSeed,
            ChunkBuffer chunkBuffer,
            int[] waterLevel
    ) {
        // Process surface and ground blocks for each column in the chunk
        ChunkCoordinate chunkCoord = chunkBuffer.getChunkCoordinate();
        double d1 = 0.03125D;
        this.biomeBlocksNoise.set(this.biomeBlocksNoiseGen.getRegion(
                this.biomeBlocksNoise.get(),
                chunkCoord.getBlockX(),
                chunkCoord.getBlockZ(),
                Constants.CHUNK_SIZE,
                Constants.CHUNK_SIZE,
                d1 * 2.0D,
                d1 * 2.0D,
                1.0D
        ));
        GeneratingChunk generatingChunk =
                new GeneratingChunk(random, waterLevel, this.biomeBlocksNoise.get(), OTGWorldInfo);
        IBiome biome;
        for (int x = 0; x < Constants.CHUNK_SIZE; x++) {
            for (int z = 0; z < Constants.CHUNK_SIZE; z++) {
                // Get the current biome config and some properties
                biome = biomes[x * Constants.CHUNK_SIZE + z];
                biome.getBiomeSettings().getSurfaceSettings().doSurfaceAndGroundControl(
                        worldSeed,
                        generatingChunk,
                        chunkBuffer,
                        chunkCoord.getBlockX() + x,
                        chunkCoord.getBlockZ() + z,
                        biome
                )
                ;
            }
        }
    }

    // Used by sagc for generating surface/ground block patterns
    public double getBiomeBlocksNoiseValue(int blockX, int blockZ) {
        double noise = this.lastNoise.get();
        if (this.lastX.get() != blockX || this.lastZ.get() != blockZ) {
            double d1 = 0.03125D;
            noise = this.biomeBlocksNoiseGen.getRegion(
                    new double[1],
                    blockX,
                    blockZ,
                    1,
                    1,
                    d1 * 2.0D,
                    d1 * 2.0D,
                    1.0D
            )[0];
            this.lastX.set(blockX);
            this.lastZ.set(blockZ);
            this.lastNoise.set(noise);
        }
        return noise;
    }

    private class NoiseCache {
        private final long[] keys;
        private final double[] values;
        private final int mask;

        private NoiseCache(int size, int noiseSize) {
            size = MathHelper.smallestEncompassingPowerOfTwo(size);
            this.mask = size - 1;

            this.keys = new long[size];
            Arrays.fill(this.keys, Long.MIN_VALUE);
            this.values = new double[size * noiseSize];
        }

        public void get(double[] buffer, int noiseX, int noiseZ) {
            long key = key(noiseX, noiseZ);
            int idx = hash(key) & this.mask;

            // if the entry here has a key that matches ours, we have a cache hit
            if (this.keys[idx] == key) {
                // Copy values into buffer
                System.arraycopy(this.values, idx * buffer.length, buffer, 0, buffer.length);
            } else {
                // cache miss: sample and put the result into our cache entry

                // Sample the noise column to store the new values
                generateNoiseColumn(buffer, noiseX, noiseZ);

                // Create copy of the array
                System.arraycopy(buffer, 0, this.values, idx * buffer.length, buffer.length);

                this.keys[idx] = key;
            }

        }

        private int hash(long key) {
            return (int) HashCommon.mix(key);
        }

        private long key(int x, int z) {
            return MathHelper.toLong(x, z);
        }
    }
}
