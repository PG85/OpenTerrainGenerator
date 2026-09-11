package com.pg85.otg.generator.resource;

import com.pg85.otg.common.LocalMaterialData;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.helpers.MathHelper;
import com.pg85.otg.util.helpers.RandomHelper;
import com.pg85.otg.util.materials.MaterialSet;

import java.util.Random;

/**
 * Represents a single ore vein.
 *
 */
class Vein
{
    private int x, y, z, size;

    Vein(int blockX, int blockY, int blockZ, int size)
    {
        this.x = blockX;
        this.y = blockY;
        this.z = blockZ;
        this.size = size;
    }

    public int getChunkSize()
    {
        return (size + 15) / 16;
    }

    public boolean reachesChunk(int otherChunkX, int otherChunkZ)
    {
        // Calculate the nearest chunk x and z
        int chunkX = (x + 8) / 16;
        int chunkZ = (z + 8) / 16;
        // Calculate the ceiled chunk size
        int chunkSize = getChunkSize();

        if (MathHelper.abs(otherChunkX - chunkX) > chunkSize || MathHelper.abs(otherChunkZ - chunkZ) > chunkSize)
        {
            return false;
        }

        return true;
    }

    public void spawn(LocalWorld world, Random random, ChunkCoordinate chunkBeingPopulated, VeinGen gen)
    {
        int sizeSquared = size * size;

        for (int i = 0; i < gen.oreFrequency; i++)
        {
            if (random.nextInt(100) < gen.oreRarity)
            {
                int oreX = chunkBeingPopulated.getBlockXCenter() + random.nextInt(ChunkCoordinate.CHUNK_SIZE);
                int oreY = RandomHelper.numberInRange(random, gen.minAltitude, gen.maxAltitude);
                int oreZ = chunkBeingPopulated.getBlockZCenter() + random.nextInt(ChunkCoordinate.CHUNK_SIZE);

                if ((oreX - x) * (oreX - x) + (oreY - y) * (oreY - y) + (oreZ - z) * (oreZ - z) < sizeSquared)
                {
                    spawnOre(world, random, oreX, oreY, oreZ, gen, chunkBeingPopulated);
                }
            }
        }
    }

    private static void spawnOre(LocalWorld world, Random rand, int x, int y, int z, VeinGen gen, ChunkCoordinate chunkBeingPopulated)
    {
        int maxSize = gen.oreSize;
        LocalMaterialData material = gen.material;
        MaterialSet sourceBlocks = gen.sourceBlocks;

        float f = rand.nextFloat() * (float) Math.PI;
        float sinf = MathHelper.sin(f) * maxSize / 8.0F;
        float cosf = MathHelper.cos(f) * maxSize / 8.0F;

        float maxX = x + sinf;
        float minX = x - sinf;
        float maxZ = z + cosf;
        float minZ = z - cosf;

        float maxY = y - 2 + rand.nextInt(3);
        float minY = y - 2 + rand.nextInt(3);

        for(int i = 0; i < maxSize; i++)
        {
            float iFactor = (float) i / (float) maxSize;
            float x1 = maxX + (minX - maxX) * iFactor;
            float y1 = maxY + (minY - maxY) * iFactor;
            float z1 = maxZ + (minZ - maxZ) * iFactor;

            float r = ((MathHelper.sin((float) Math.PI * iFactor) + 1.0F) * rand.nextFloat() * maxSize / 16.0F + 1.0F) * 0.5F;

            int minX1 = MathHelper.ceil(x1 - r - 0.5F);
            int minY1 = MathHelper.ceil(y1 - r - 0.5F);
            int minZ1 = MathHelper.ceil(z1 - r - 0.5F);

            int maxX1 = MathHelper.floor(x1 + r - 0.5F);
            int maxY1 = MathHelper.floor(y1 + r - 0.5F);
            int maxZ1 = MathHelper.floor(z1 + r - 0.5F);

            for(int x2 = minX1; x2 <= maxX1; x2++)
            {
                for(int y2 = minY1; y2 <= maxY1; y2++)
                {
                    for(int z2 = minZ1; z2 <= maxZ1; z2++)
                    {
                        float dx = x2 + 0.5F - x1;
                        float dy = y2 + 0.5F - y1;
                        float dz = z2 + 0.5F - z1;
                        if(dx * dx + dy * dy + dz * dz > r * r)
                        {
                            continue;
                        }
                        if(!sourceBlocks.contains(world.getMaterial(x2, y2, z2, chunkBeingPopulated)))
                        {
                            continue;
                        }
                        world.setBlock(x2, y2, z2, material, null, chunkBeingPopulated, true);
                    }
                }
            }
        }
    }
}
