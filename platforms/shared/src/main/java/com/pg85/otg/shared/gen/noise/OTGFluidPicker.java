package com.pg85.otg.shared.gen.noise;

import com.pg85.otg.config.settings.biome.SurfaceSettings;
import com.pg85.otg.shared.materials.SharedMaterialData;
import com.pg85.otg.gen.OTGChunkGenerator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;

/**
 * Fluid picker honoring OTG's per-biome water levels, used with aquifers disabled.
 * Reproduces the legacy base terrain rule (water when WaterLevelMin < y < WaterLevelMax),
 * via {@link Aquifer.FluidStatus#at}: water below the status level, air otherwise.
 * No lava floor: legacy base terrain doesn't place one either (carvers handle lava).
 */
public final class OTGFluidPicker implements Aquifer.FluidPicker {
    private static final Aquifer.FluidStatus EMPTY =
            new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState());

    private final OTGChunkGenerator internalGenerator;

    private final ThreadLocal<ColumnMemo> columnMemo = ThreadLocal.withInitial(ColumnMemo::new);

    public OTGFluidPicker(OTGChunkGenerator internalGenerator) {
        this.internalGenerator = internalGenerator;
    }

    @Override
    public Aquifer.FluidStatus computeFluid(int x, int y, int z) {
        ColumnMemo memo = this.columnMemo.get();
        int noiseX = x >> 2;
        int noiseZ = z >> 2;
        if (memo.noiseX != noiseX || memo.noiseZ != noiseZ) {
            memo.surfaceSettings = this.internalGenerator.getCachedBiomeProvider()
                    .getNoiseBiome(noiseX, noiseZ).getBiomeSettings().getSurfaceSettings();
            memo.noiseX = noiseX;
            memo.noiseZ = noiseZ;
        }
        SurfaceSettings surface = memo.surfaceSettings;

        if (y <= surface.getWaterLevelMin()) {
            return EMPTY;
        }
        BlockState water = ((SharedMaterialData) surface.getWaterBlockReplaced(y)).getState();
        return new Aquifer.FluidStatus(surface.getWaterLevelMax(), water);
    }

    private static final class ColumnMemo {
        private int noiseX = Integer.MAX_VALUE;
        private int noiseZ = Integer.MAX_VALUE;
        private SurfaceSettings surfaceSettings;
    }
}
