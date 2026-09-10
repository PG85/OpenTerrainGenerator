package com.pg85.otg.generator.resource;

import com.pg85.otg.common.LocalMaterialData;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.configuration.biome.BiomeConfig;
import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.exception.InvalidConfigException;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.helpers.MathHelper;
import com.pg85.otg.util.helpers.RandomHelper;
import com.pg85.otg.util.materials.MaterialHelper;
import com.pg85.otg.util.materials.MaterialSet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class OreGen extends Resource
{
    private final int maxAltitude;
    private final int maxSize;
    private final int minAltitude;
    private final MaterialSet sourceBlocks;
    // Stack of caches to handle cascading chunk generation (nested spawn() calls on same thread)
    // Each spawn() pushes its own cache, preventing corruption from nested calls
    // Using short[][] allows -1 sentinel for "unset" (more efficient than byte + BitSet)
    private final Deque<short[][]> cacheStack = new ArrayDeque<>();

    public OreGen(BiomeConfig biomeConfig, List<String> args) throws InvalidConfigException
    {
        super(biomeConfig);
        assureSize(7, args);

        material = readMaterial(args.get(0));
        maxSize = readInt(args.get(1), 1, 128);
        frequency = readInt(args.get(2), 1, 100);
        rarity = readRarity(args.get(3));
        minAltitude = readInt(args.get(4), PluginStandardValues.WORLD_DEPTH, PluginStandardValues.WORLD_HEIGHT - 1);
        maxAltitude = readInt(args.get(5), minAltitude, PluginStandardValues.WORLD_HEIGHT - 1);
        sourceBlocks = readMaterials(args, 6);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!super.equals(other))
            return false;
        if (other == null)
            return false;
        if (other == this)
            return true;
        if (getClass() != other.getClass())
            return false;
        final OreGen compare = (OreGen) other;
        return this.maxSize == compare.maxSize
               && this.minAltitude == compare.minAltitude
               && this.maxAltitude == compare.maxAltitude
               && (this.sourceBlocks == null ? this.sourceBlocks == compare.sourceBlocks
                   : this.sourceBlocks.equals(compare.sourceBlocks));
    }

    @Override
    public int getPriority()
    {
        return 10;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 11 * hash + super.hashCode();
        hash = 11 * hash + this.minAltitude;
        hash = 11 * hash + this.maxAltitude;
        hash = 11 * hash + this.maxSize;
        hash = 11 * hash + (this.sourceBlocks != null ? this.sourceBlocks.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString()
    {
        return "Ore(" + material + "," + maxSize + "," + frequency + "," + rarity + "," + minAltitude + "," + maxAltitude + makeMaterials(sourceBlocks) + ")";
    }

    @Override
    protected void createCache()
    {
        short[][] cache = new short[32][32];
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                cache[i][j] = -1; // = unset
            }
        }
        cacheStack.push(cache);
    }

    @Override
    protected void clearCache()
    {
        if (!cacheStack.isEmpty()) {
            cacheStack.pop();
        }
    }

    /**
     * Packs relative x,y,z coordinates into a single int for deduplication.
     * Format: [relX:5 bits][Y:8 bits][relZ:5 bits]
     * Uses chunk-relative coordinates (0-31) for X/Z, absolute Y (0-255)
     */
    private static int packCoords(int relX, int y, int relZ) {
        return (relX << 13) | (y << 5) | relZ;
    }

    @Override
    public void spawn(LocalWorld world, Random rand, boolean villageInChunk, int x, int z, ChunkCoordinate chunkBeingPopulated) {
        //This codes logic is a copy of WorldGenMinable.generate, with some added cfgs and guards

        material = material.parseForWorld(world);
        sourceBlocks.parseForWorld(world);

        if (world.getConfigs().getWorldConfig().disableOreGen && MaterialHelper.isOre(this.material))
            return;

        int y = RandomHelper.numberInRange(rand, this.minAltitude, this.maxAltitude);

        float veinAngle = rand.nextFloat() * (float) Math.PI;

        // Define the path endpoints that the ore vein will follow through 3D space
        // Spheres will be placed along this line to form the complete vein
        double veinStartX = ((float) (x + 8) + MathHelper.sin(veinAngle) * (float) this.maxSize / 8.0F);
        double veinEndX = ((float) (x + 8) - MathHelper.sin(veinAngle) * (float) this.maxSize / 8.0F);
        double veinStartZ = ((float) (z + 8) + MathHelper.cos(veinAngle) * (float) this.maxSize / 8.0F);
        double veinEndZ = ((float) (z + 8) - MathHelper.cos(veinAngle) * (float) this.maxSize / 8.0F);
        double veinStartY = (y + rand.nextInt(3) - 2); // Minimal Y variation (0-3 blocks)
        double veinEndY = (y + rand.nextInt(3) - 2);   // Both use same formula = mostly horizontal veins

        int areaBeingPopulatedSize = 32;

        // Deduplicate block positions across overlapping sphere segments
        IntOpenHashSet processedBlocks = new IntOpenHashSet(this.maxSize * 4);

        // Generate ore vein as a series of spherical segments connected along a path
        // Each segment forms part of a continuous vein with varying thickness (sine wave bulge)
        for (int segmentIndex = 0; segmentIndex < this.maxSize; segmentIndex++) {
            // Calculate interpolation factor (0.0 at start point, 1.0 at end point)
            float interpolFactor = (float) segmentIndex / (float) this.maxSize;

            // Interpolate current position along the vein path
            double currentX = veinStartX + (veinEndX - veinStartX) * interpolFactor;
            double currentY = veinStartY + (veinEndY - veinStartY) * interpolFactor;
            double currentZ = veinStartZ + (veinEndZ - veinStartZ) * interpolFactor;

            // Calculate sphere size at this point (creates bulge in middle of the entire path via sine)
            double sizeVariation = rand.nextDouble() * this.maxSize / 16.0D;
            double diameter = (MathHelper.sin((float) Math.PI * interpolFactor) + 1.0F) * sizeVariation + 1.0D;
            double radius = diameter / 2.0D;
            double radiusSqr = radius * radius;

            // Calculate bounding box for this sphere segment
            int minX = MathHelper.floor(currentX - radius);
            int maxX = MathHelper.floor(currentX + radius);
            int minY = MathHelper.floor(currentY - radius);
            int maxY = MathHelper.floor(currentY + radius);
            int minZ = MathHelper.floor(currentZ - radius);
            int maxZ = MathHelper.floor(currentZ + radius);

            // Skip this sphere segment if it's fully outside the given bounds
            if (minX < chunkBeingPopulated.getBlockX()) continue;
            if (maxX > chunkBeingPopulated.getBlockX() + areaBeingPopulatedSize - 1) continue;
            if (minZ < chunkBeingPopulated.getBlockZ()) continue;
            if (maxZ > chunkBeingPopulated.getBlockZ() + areaBeingPopulatedSize - 1) continue;
            if (minY < PluginStandardValues.WORLD_DEPTH) continue;
            if (minY > PluginStandardValues.WORLD_HEIGHT - 1) continue;

            // Iterate through all blocks in the sphere's bounding box, order x -> z -> y
            for (int blockX = minX; blockX <= maxX; blockX++) {
                // Calculate X distance from sphere center
                double dx = (double) blockX + 0.5D - currentX;
                double dxSqr = dx * dx;
                if (dxSqr >= radiusSqr) continue;  // Skip if X coordinate is outside sphere
                int relX = blockX - chunkBeingPopulated.getBlockX();

                for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                    int clampedMaxY = maxY;

                    // Check terrain height to avoid generating ore in air (only used above vanilla sea level, somewhat incorrect)
                    if (clampedMaxY > 63) {
                        short[][] currentCache = cacheStack.peek();
                        if (currentCache == null) continue; // should never happen

                        int cacheX = blockX - chunkBeingPopulated.getBlockX();
                        int cacheZ = blockZ - chunkBeingPopulated.getBlockZ();

                        // check cached height
                        int highestSolidBlock = currentCache[cacheX][cacheZ];
                        if (highestSolidBlock == -1) { // Cache miss
                            highestSolidBlock = world.getHeightMapHeight(blockX, blockZ, chunkBeingPopulated);
                            if (highestSolidBlock == -1)  // Empty column, no solid blocks
                                continue;  // Skip this entire Z column
                            currentCache[cacheX][cacheZ] = (short) highestSolidBlock;
                        }

                        // Limit Y iteration to terrain surface (don't generate ore above ground)
                        clampedMaxY = Math.min(clampedMaxY, highestSolidBlock);
                    }

                    double dz = (double) blockZ + 0.5D - currentZ;
                    double dxzSqr = dxSqr + dz * dz;
                    if (dxzSqr >= radiusSqr) continue;  // Skip if XZ coordinates are outside sphere
                    int relZ = blockZ - chunkBeingPopulated.getBlockZ();

                    for (int blockY = minY; blockY <= clampedMaxY; blockY++) {
                        double dy = (double) blockY + 0.5D - currentY;

                        // Is point outside sphere? (3D pythagoras)
                        if (dxzSqr + dy * dy >= radiusSqr) continue;

                        // Don't process blocks multiple times
                        if (!processedBlocks.add(packCoords(relX, blockY, relZ))) continue;

                        LocalMaterialData material = world.getMaterial(blockX, blockY, blockZ, chunkBeingPopulated);
                        if (this.sourceBlocks.contains(material)) {
                            world.setBlock(blockX, blockY, blockZ, this.material, null, chunkBeingPopulated, true);
                        }
                    }
                }
            }
        }
    }
}
