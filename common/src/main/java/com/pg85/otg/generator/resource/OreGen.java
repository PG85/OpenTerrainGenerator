package com.pg85.otg.generator.resource;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.configuration.biome.BiomeConfig;
import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.exception.InvalidConfigException;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.helpers.MathHelper;
import com.pg85.otg.util.helpers.RandomHelper;
import com.pg85.otg.util.materials.MaterialHelper;
import com.pg85.otg.util.materials.MaterialSet;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class OreGen extends Resource
{
    private static class GenerationContext
    {
        private final int[] heightmap = new int[32 * 32];
        private final BitSet visited = new BitSet(32 * 32 * 256);

        public int getHeight(LocalWorld world, int x, int z, ChunkCoordinate chunkCoords)
        {
            int i = (x - chunkCoords.getBlockX()) << 5 | (z - chunkCoords.getBlockZ());
            int height = this.heightmap[i];
            if(height < 0)
            {
                this.heightmap[i] = height = world.getHeightMapHeight(x, z, chunkCoords);
            }
            return height;
        }

        public boolean visit(int x, int y, int z, ChunkCoordinate chunkCoords)
        {
            int i = y << 10 | (x - chunkCoords.getBlockX()) << 5 | (z - chunkCoords.getBlockZ());
            if(this.visited.get(i))
            {
                return false;
            }
            this.visited.set(i);
            return true;
        }

        public void reset()
        {
            Arrays.fill(this.heightmap, -1);
            this.visited.clear();
        }
    }

    private final int maxAltitude;
    private final int maxSize;
    private final int minAltitude;
    private final MaterialSet sourceBlocks;

    //Cache keeps already initialized objects to be used by the stack, which exists for nested generation calls to not interfere with each other
    private static final Queue<GenerationContext> contextCache = new ArrayBlockingQueue<>(2);
    private static final Stack<GenerationContext> contextStack = new ObjectArrayList<>();

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
        GenerationContext context = Optional.ofNullable(contextCache.poll()).orElseGet(GenerationContext::new);
        contextStack.push(context);
    }

    @Override
    protected void clearCache()
    {
        GenerationContext context = contextStack.pop();
        context.reset();
        contextCache.offer(context);
    }

    @Override
    protected void spawnInChunk(LocalWorld world, Random random, boolean villageInChunk, ChunkCoordinate chunkCoord)
    {
        material = material.parseForWorld(world);
        sourceBlocks.parseForWorld(world);

        if(world.getConfigs().getWorldConfig().disableOreGen && MaterialHelper.isOre(this.material))
            return;

        // invoke this.spawn() multiple (frequency) times with the same cache
        super.spawnInChunk(world, random, villageInChunk, chunkCoord);
    }

    @Override
    public void spawn(LocalWorld world, Random rand, boolean villageInChunk, int x, int z, ChunkCoordinate chunkBeingPopulated) {
        // This codes logic is a copy of WorldGenMinable.generate, with some added cfgs and guards
        GenerationContext context = contextStack.top();
        int y = RandomHelper.numberInRange(rand, this.minAltitude, this.maxAltitude);

        float veinAngle = rand.nextFloat() * (float) Math.PI;
        float veinAngleSin = MathHelper.sin(veinAngle) * this.maxSize / 8.0F;
        float veinAngleCos = MathHelper.cos(veinAngle) * this.maxSize / 8.0F;

        // Define the line endpoints that the ore vein will follow through 3D space
        // Spheres of varying size (biggest in center) will be placed along this line to form the complete vein
        float veinStartX = x + veinAngleSin;
        float veinEndX   = x - veinAngleSin;
        float veinStartZ = z + veinAngleCos;
        float veinEndZ   = z - veinAngleCos;
        float veinStartY = y - 2 + rand.nextInt(3);
        float veinEndY   = y - 2 + rand.nextInt(3);

        // Generate ore vein as a series of spherical segments connected along a path
        // Each segment forms part of a continuous vein with varying thickness (sine wave bulge)
        for (int segmentIndex = 0; segmentIndex < this.maxSize; segmentIndex++) {
            // Calculate interpolation factor (0.0 at start point, 1.0 at end point)
            float interpolFactor = (float) segmentIndex / (float) this.maxSize;

            // Interpolate current position along the vein path
            float currentX = veinStartX + (veinEndX - veinStartX) * interpolFactor;
            float currentY = veinStartY + (veinEndY - veinStartY) * interpolFactor;
            float currentZ = veinStartZ + (veinEndZ - veinStartZ) * interpolFactor;

            // Calculate sphere size at this point (creates bulge in middle of the entire path via sine)
            float radius = ((MathHelper.sin((float) Math.PI * interpolFactor) + 1.0F) * rand.nextFloat() * this.maxSize / 16.0F + 1.0F) * 0.5F;

            // Calculate bounding box for this sphere segment
            int minX = MathHelper.ceil(currentX - radius - 0.5F);
            int maxX = MathHelper.floor(currentX + radius - 0.5F);
            int minY = MathHelper.ceil(currentY - radius - 0.5F);
            int maxY = MathHelper.floor(currentY + radius - 0.5F);
            int minZ = MathHelper.ceil(currentZ - radius - 0.5F);
            int maxZ = MathHelper.floor(currentZ + radius - 0.5F);

            // Skip this sphere segment if it's not fully inside the given bounds
            if (minX < chunkBeingPopulated.getBlockX()) continue;
            if (maxX >= chunkBeingPopulated.getBlockX() + 32) continue;
            if (minZ < chunkBeingPopulated.getBlockZ()) continue;
            if (maxZ >= chunkBeingPopulated.getBlockZ() + 32) continue;
            if (minY < PluginStandardValues.WORLD_DEPTH) continue;
            if (maxY >= PluginStandardValues.WORLD_HEIGHT) continue;

            // Iterate through all blocks in the sphere's bounding box
            for (int blockX = minX; blockX <= maxX; blockX++) {
                for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                    for (int y2 = Math.min(maxY, context.getHeight(world, blockX, blockZ, chunkBeingPopulated)); y2 >= minY; y2--) {
                        float dx = blockX + 0.5F - currentX;
                        float dz = blockZ + 0.5F - currentZ;
                        float dy = y2 + 0.5F - currentY;
                        // Is point outside sphere? (3D pythagoras)
                        if (dx * dx + dy * dy + dz * dz > radius * radius) continue;

                        // Don't process blocks multiple times
                        if (!context.visit(blockX, y2, blockZ, chunkBeingPopulated)) continue;

                        // Actually replace the source block with the ore block
                        if (this.sourceBlocks.contains(world.getMaterial(blockX, y2, blockZ, chunkBeingPopulated))) {
                            world.setBlock(blockX, y2, blockZ, this.material, null, chunkBeingPopulated, true);
                        }
                    }
                }
            }
        }
    }
}
