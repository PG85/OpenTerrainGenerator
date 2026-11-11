package com.pg85.otg.generator.biome.layers;

import com.pg85.otg.common.LocalBiome;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.configuration.biome.BiomeGroup;
import com.pg85.otg.configuration.biome.BiomeGroupManager;
import com.pg85.otg.generator.biome.ArraysCache;
import com.pg85.otg.util.WeightedList;

public class LayerBiome extends Layer
{
    private BiomeGroupManager manager;
    private int depth;
    private double freezeTemp;

    LayerBiome(long seed, LocalWorld world, Layer childLayer, BiomeGroupManager groupManager, int depth)
    {
        super(seed, world);
        this.child = childLayer;
        this.manager = groupManager;
        this.depth = depth;
        this.freezeTemp = world.getConfigs().getWorldConfig().frozenOceanTemperature;
    }

    @Override
    public int[] getInts(LocalWorld world, ArraysCache cache, int x, int z, int xSize, int zSize)
    {
        int[] childInts = this.child.getInts(world, cache, x, z, xSize, zSize);
        int[] thisInts = cache.getArray(xSize * zSize);

        int currentPiece;
        
        for (int i = 0; i < zSize; i++)
        {
            for (int j = 0; j < xSize; j++)
            {
                initChunkSeed(j + x, i + z);
                currentPiece = childInts[(j + i * xSize)];

                if ((currentPiece & BiomeGroupBits) != 0 && ((currentPiece & BiomeBitsAreSetBit) == 0 || (currentPiece & BiomeBits) == this.defaultOceanId))    // has biomegroup bits but not biome bits
                {
                    BiomeGroup group = manager.getGroupById((currentPiece & BiomeGroupBits) >> BiomeGroupShift);
                    WeightedList<LocalBiome> weightedBiomes = group.getDepthMapOrHigher(depth);
                    if(!weightedBiomes.isEmpty())
                    {
                        LocalBiome biome = weightedBiomes.getRandom(this::nextInt);
                        if (biome != null && biome.getBiomeConfig().biomeSize == this.depth) {
                            // Clear any stale BiomeBits/IceBit/BiomeBitsAreSetBit (e.g. from a
                            // previous depth having set this cell to defaultOceanId) before
                            // writing the newly picked biome's id, otherwise the old bits get
                            // OR'd into the new id instead of being replaced by it.
                            currentPiece = (currentPiece & ~(BiomeBits | IceBit | BiomeBitsAreSetBit)) |
                                biome.getIds().getOTGBiomeId() |
                                // Set IceBit based on Biome Temperature
                                (biome.getBiomeConfig().biomeTemperature <= freezeTemp ? IceBit : 0) |
                                // Set BiomeBitsAreSetBit
                                BiomeBitsAreSetBit
                                ;
                        }
                    }
                }
                thisInts[(j + i * xSize)] = currentPiece;
            }
        }
        return thisInts;
    }
}
