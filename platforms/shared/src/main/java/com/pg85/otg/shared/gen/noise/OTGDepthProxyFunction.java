package com.pg85.otg.shared.gen.noise;

import com.mojang.serialization.MapCodec;
import com.pg85.otg.gen.OTGChunkGenerator;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Volatility-independent depth measure for vanilla noise caves: a linear gradient below each
 * column's blended terrain center height (the falloff anchor, before detail noise).
 *
 * OTG's raw terrain density cannot serve as vanilla's depth signal, because its falloff
 * gradient is divided by biome volatility: in high-volatility biomes the density never reaches
 * the cave thresholds at any playable depth, and the detail noise (which scales WITH
 * volatility) swamps any rescaling attempt. This function sidesteps both problems by deriving
 * depth purely from the smoothed per-column center height, so cave zones open at the same
 * depth in every biome. The actual terrain shape (overhangs, valleys) is preserved separately
 * by min-combining terrain density with the cave field in the router.
 *
 * Runtime-only, like {@link OTGTerrainDensityFunction}: never serialized.
 */
public final class OTGDepthProxyFunction implements DensityFunction.SimpleFunction {
    private static final double MAX_DENSITY = 64.0;

    private final OTGChunkGenerator internalGenerator;
    private final double depthGradient;
    private final KeyDispatchDataCodec<? extends DensityFunction> codec =
            KeyDispatchDataCodec.of(MapCodec.unit(this));

    private final ThreadLocal<ColumnMemo> columnMemo = ThreadLocal.withInitial(ColumnMemo::new);

    public OTGDepthProxyFunction(OTGChunkGenerator internalGenerator, double depthGradient) {
        this.internalGenerator = internalGenerator;
        this.depthGradient = depthGradient;
    }

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        int noiseX = Math.floorDiv(context.blockX(), 4);
        int noiseZ = Math.floorDiv(context.blockZ(), 4);
        double centerHeight = this.columnMemo.get().fetch(this.internalGenerator, noiseX, noiseZ);
        double proxy = (centerHeight - context.blockY()) * this.depthGradient;
        return Mth.clamp(proxy, -MAX_DENSITY, MAX_DENSITY);
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

    private static final class ColumnMemo {
        private double centerHeight;
        private int noiseX = Integer.MAX_VALUE;
        private int noiseZ = Integer.MAX_VALUE;

        private double fetch(OTGChunkGenerator generator, int noiseX, int noiseZ) {
            if (this.noiseX != noiseX || this.noiseZ != noiseZ) {
                this.centerHeight = generator.getColumnCenterHeightInBlocks(noiseX, noiseZ);
                this.noiseX = noiseX;
                this.noiseZ = noiseZ;
            }
            return this.centerHeight;
        }
    }
}
