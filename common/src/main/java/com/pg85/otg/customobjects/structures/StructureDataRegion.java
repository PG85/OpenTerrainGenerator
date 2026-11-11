package com.pg85.otg.customobjects.structures;

import static com.pg85.otg.customobjects.structures.CustomStructureCache.REGION_SIZE;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.pg85.otg.util.ChunkCoordinate;

public class StructureDataRegion
{
    private boolean requiresSave = false;
    private CustomStructure[] structures = new CustomStructure[REGION_SIZE * REGION_SIZE];

    public boolean requiresSave()
    {
        return this.requiresSave;
    }

    public void markSaved()
    {
        this.requiresSave = false;
    }

    public void markSaveRequired()
    {
        this.requiresSave = true;
    }

    public CustomStructure getStructure(int internalX, int internalZ)
    {
        return this.structures[internalX * REGION_SIZE + internalZ];
    }

    public void setStructure(int internalX, int internalZ, CustomStructure structure, boolean requiresSave)
    {
        this.structures[internalX * REGION_SIZE + internalZ] = structure;
        this.requiresSave = this.requiresSave || requiresSave;
    }

    // Returns world chunk coordinates (not region-internal coordinates) for each structure in this region.
    public Stream<Map.Entry<ChunkCoordinate, CustomStructure>> getStructures(ChunkCoordinate regionCoord)
    {
        int baseX = regionCoord.getChunkX() * REGION_SIZE;
        int baseZ = regionCoord.getChunkZ() * REGION_SIZE;
        return IntStream.range(0, REGION_SIZE * REGION_SIZE).filter(i -> structures[i] != null).mapToObj(i -> new SimpleEntry<>(ChunkCoordinate.fromChunkCoords(baseX + i / REGION_SIZE, baseZ + i % REGION_SIZE), structures[i]));
    }
}
