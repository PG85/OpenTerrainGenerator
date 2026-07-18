package com.pg85.otg.shared.gen.noise;

import com.mojang.serialization.MapCodec;
import com.pg85.otg.gen.OTGChunkGenerator;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Exposes OTG's base terrain noise as a vanilla {@link DensityFunction}, normalized to the
 * value range vanilla expects at the slopedCheese slot of the noise router (~0 at the surface,
 * positive solid below, deep values reaching the cave thresholds 1.5625/2.34).
 *
 * Runtime-only: this function is placed in a hand-built NoiseRouter that never becomes
 * registry data, so its codec is a non-serializable placeholder.
 *
 * Coordinate mapping matches OTGChunkGenerator.populateNoise: noise column index i corresponds
 * to world Y = i * 8, with one column per 4x4 block area. Under vanilla's interpolated()
 * wrapper this is only sampled at cell corners (x, z multiples of 4 and y multiples of 8),
 * where the lookups below are exact; off-grid coordinates (e.g. preliminary surface probes)
 * fall back to nearest column with linear y interpolation.
 */
public final class OTGTerrainDensityFunction implements DensityFunction.SimpleFunction {
    // Matches vanilla's initialDensity clamp; generous bounds, only used for optimization.
    private static final double MAX_DENSITY = 64.0;

    private final OTGChunkGenerator internalGenerator;
    private final int noiseSizeY;
    private final double inverseScale;
    private final KeyDispatchDataCodec<? extends DensityFunction> codec =
            KeyDispatchDataCodec.of(MapCodec.unit(this));

    private final ThreadLocal<ColumnMemo> columnMemo;

    public OTGTerrainDensityFunction(OTGChunkGenerator internalGenerator, double densityScale) {
        this.internalGenerator = internalGenerator;
        this.noiseSizeY = internalGenerator.getNoiseSizeY();
        this.inverseScale = 1.0 / densityScale;
        this.columnMemo = ThreadLocal.withInitial(() -> new ColumnMemo(this.noiseSizeY + 1));
    }

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        int noiseX = Math.floorDiv(context.blockX(), 4);
        int noiseZ = Math.floorDiv(context.blockZ(), 4);
        double[] column = this.columnMemo.get().fetch(this.internalGenerator, noiseX, noiseZ);

        int blockY = context.blockY();
        int cellY = Math.floorDiv(blockY, 8);

        double raw;
        if (cellY < 0) {
            // Below OTG's terrain range (worlds with MinY < 0): hold the bottom value (solid).
            raw = column[0];
        } else if (cellY >= this.noiseSizeY) {
            raw = column[this.noiseSizeY];
        } else {
            double yLerp = (blockY - cellY * 8) / 8.0;
            raw = Mth.lerp(yLerp, column[cellY], column[cellY + 1]);
        }

        return Mth.clamp(raw * this.inverseScale, -MAX_DENSITY, MAX_DENSITY);
    }

    @Override
    public double minValue() {
        return -MAX_DENSITY;
    }

    @Override
    public double maxValue() {
        return MAX_DENSITY;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return this.codec;
    }

    /**
     * Per-thread memo of the last fetched noise column. The underlying NoiseCache is already
     * thread-local, but this avoids a hash lookup and array copy for consecutive samples in
     * the same column (the common access pattern during cell iteration).
     */
    private static final class ColumnMemo {
        private final double[] column;
        private int noiseX = Integer.MAX_VALUE;
        private int noiseZ = Integer.MAX_VALUE;

        private ColumnMemo(int columnSize) {
            this.column = new double[columnSize];
        }

        private double[] fetch(OTGChunkGenerator generator, int noiseX, int noiseZ) {
            if (this.noiseX != noiseX || this.noiseZ != noiseZ) {
                generator.getNoiseColumn(this.column, noiseX, noiseZ);
                this.noiseX = noiseX;
                this.noiseZ = noiseZ;
            }
            return this.column;
        }
    }
}
