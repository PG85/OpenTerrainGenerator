package com.pg85.otg.customobjects.structures;

import static com.pg85.otg.configuration.standard.WorldStandardValues.StructureDataBackupFileExtension;
import static com.pg85.otg.configuration.standard.WorldStandardValues.StructureDataFileExtension;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.pg85.otg.OTG;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.configuration.standard.PluginStandardValues;
import com.pg85.otg.configuration.standard.WorldStandardValues;
import com.pg85.otg.customobjects.bo3.bo3function.BO3ModDataFunction;
import com.pg85.otg.customobjects.bo3.bo3function.BO3ParticleFunction;
import com.pg85.otg.customobjects.bo3.bo3function.BO3SpawnerFunction;
import com.pg85.otg.customobjects.bo4.bo4function.BO4ModDataFunction;
import com.pg85.otg.customobjects.bo4.bo4function.BO4ParticleFunction;
import com.pg85.otg.customobjects.bo4.bo4function.BO4SpawnerFunction;
import com.pg85.otg.customobjects.bofunctions.ModDataFunction;
import com.pg85.otg.customobjects.bofunctions.ParticleFunction;
import com.pg85.otg.customobjects.bofunctions.SpawnerFunction;
import com.pg85.otg.customobjects.structures.bo3.BO3CustomStructure;
import com.pg85.otg.customobjects.structures.bo3.BO3CustomStructureCoordinate;
import com.pg85.otg.customobjects.structures.bo4.BO4CustomStructure;
import com.pg85.otg.customobjects.structures.bo4.BO4CustomStructureCoordinate;
import com.pg85.otg.customobjects.structures.bo4.CustomStructurePlaceHolder;
import com.pg85.otg.customobjects.structures.bo4.smoothing.SmoothingAreaLine;
import com.pg85.otg.logging.LogMarker;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.DataUtil;
import com.pg85.otg.util.DataUtil.IOBiConsumer;
import com.pg85.otg.util.DataUtil.IOConsumer;
import com.pg85.otg.util.bo3.Rotation;
import com.pg85.otg.util.helpers.StreamHelper;

public class CustomStructureFileManager
{
    private static Path worldDataDir(LocalWorld world)
    {
        Path dir = world.getWorldSaveDir().toPath().resolve(PluginStandardValues.PLUGIN_NAME);
        return world.getDimensionId() != 0 ? dir.resolve("DIM-" + world.getDimensionId()) : dir;
    }

    private static void write(LocalWorld world, String path, IOConsumer<DataOutputStream> writer)
    {
        Path dir = worldDataDir(world);
        Path file = dir.resolve(path + StructureDataFileExtension);
        Path backup = dir.resolve(path + StructureDataBackupFileExtension);
        DataUtil.writeCompressed(file, backup, writer);
    }

    private static void read(LocalWorld world, String path, IOConsumer<DataInputStream> reader)
    {
        Path dir = worldDataDir(world);
        Path file = dir.resolve(path + StructureDataFileExtension);
        Path backup = dir.resolve(path + StructureDataBackupFileExtension);
        DataUtil.readCompressed(file, backup, reader);
    }

    private static void readAll(LocalWorld world, String path, IOBiConsumer<Path, DataInputStream> reader)
    {
        DataUtil.readCompressed(worldDataDir(world).resolve(path), StructureDataFileExtension, StructureDataBackupFileExtension, reader);
    }

    private static String toFileName(ChunkCoordinate chunkCoordinate)
    {
        return chunkCoordinate.getChunkX() + "_" + chunkCoordinate.getChunkZ();
    }

    private static ChunkCoordinate parseChunkCoordinate(Path file) throws IllegalArgumentException
    {
        try
        {
            String s = file.getFileName().toString();
            s = StringUtils.removeEnd(s, StructureDataBackupFileExtension);
            s = StringUtils.removeEnd(s, StructureDataFileExtension);
            int i = s.indexOf('_');
            if(i < 0)
            {
                throw new IllegalArgumentException("File name \"" + file.getFileName() + "\" cannot be converted to chunk coordinate. Missing underscore (_) delimiter.");
            }

            int x = Integer.parseInt(s.substring(0, i));
            int z = Integer.parseInt(s.substring(i + 1));
            return ChunkCoordinate.fromChunkCoords(x, z);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException("File name \"" + file.getFileName() + "\" cannot be converted to chunk coordinate.", e);
        }
    }

    // Plotted chunks

    public static void savePlottedChunksData(LocalWorld world, Map<ChunkCoordinate, PlottedChunksRegion> populatedChunks)
    {
        AtomicInteger regionsSaved = new AtomicInteger();

        populatedChunks.forEach((k, v) -> {
            if(!v.requiresSave())
                return;

            write(world, WorldStandardValues.PlottedChunksDataFolderName + File.separator + toFileName(k), v::write);

            v.markSaved();
            regionsSaved.getAndIncrement();
        });

        OTG.log(LogMarker.INFO, regionsSaved + " plotted chunk regions saved.");
    }

    public static void loadPlottedChunksData(LocalWorld world, Map<ChunkCoordinate, PlottedChunksRegion> populatedChunks)
    {
        populatedChunks.clear();

        readAll(world, WorldStandardValues.PlottedChunksDataFolderName, (p, in) -> {
            ChunkCoordinate key;
            try
            {
                key = parseChunkCoordinate(p);
            }
            catch(IllegalArgumentException e)
            {
                return;
            }

            populatedChunks.put(key, PlottedChunksRegion.read(in));
        });
    }

    // Structure cache

    // TODO: Since we're using regions, use short/byte for (internal) coords?
    public static void saveStructureData(Map<ChunkCoordinate, StructureDataRegion> worldInfoChunks, LocalWorld world)
    {
        // Collect all structure start points (and chunks that have bo3's with spawners/moddata/particles in them)
        // and group them by BO name (or "NULL" for bo3's with spawners/moddata/particles).
        // Structure starts are saved per region, if a BO4 structure has chunk data in multiple regions, each region gets 
        // its own BO4CustomStructure containing only the chunk data for that region. When loading, structures that have 
        // their structure start in a different region are loaded as CustomStructurePlaceHolder instead of BO4CustomStructure.
        // When loading regions, we'll reconstitute/update worldInfoChunks by replacing any CustomStructurePlaceHolders with 
        // BO4CustomStructures as soon as they're loaded from disk. Fully spawned chunks that are part of structures are saved 
        // to disk inside their structure start/placeholder, but are only cached/kept in memory in worldInfoChunks.
        // (BO4CustomStructures only cache data for unspawned structure parts and spawners/moddata/particles, worldInfoChunks 
        // caches data about fully spawned structure chunks, plottedChunks caches/persists info about plotted chunks etc).
        AtomicInteger regionsSaved = new AtomicInteger();
        worldInfoChunks.forEach((k, v) -> {
            if(!v.requiresSave())
                return;

            saveStructuresRegionFile(world, k, v.getStructures().collect(Collectors.groupingBy(e -> e.getValue().start != null ? e.getValue().start.bo3Name : "NULL", Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())))));

            v.markSaved();
            regionsSaved.getAndIncrement();
        });

        OTG.log(LogMarker.INFO, regionsSaved + " structure data regions saved.");
    }

    private static void saveStructuresRegionFile(LocalWorld world, ChunkCoordinate regionCoord, Map<String, Map<CustomStructure, List<ChunkCoordinate>>> structuresPerRegion)
    {
        write(world, WorldStandardValues.StructureDataFolderName + File.separator + toFileName(regionCoord), out -> {
            out.writeInt(1); // version

            // Structures have been de-duplicated, should be only one entry per structure start
            DataUtil.writeMap(structuresPerRegion, out, StreamHelper::writeStringToStream, DataUtil.mapWriter((out1, k, v) -> {
                // No need to write to file whether this is a CustomStructurePlaceHolder or not.
                // If the structure start is outside the current region, it's a placeholder.

                // Write structure start data (if any)
                // If name is "NULL", we'll know not to look for these when reading.
                if(k.start != null)
                {
                    out1.writeInt(k.start.rotation.getRotationId());
                    out1.writeInt(k.start.getX());
                    out1.writeInt(k.start.getY());
                    out1.writeInt(k.start.getZ());
                }

                // Write all chunks used for structure
                // TODO: Use internal coords so we can use byte/short 
                DataUtil.writeCollection(v, out1, ChunkCoordinate::write);

                DataUtil.writeOptional(k, BO4CustomStructure.class, BO4CustomStructure::getObjectsToSpawn, DataUtil::nonEmpty, out1, DataUtil.mappingWriter(out1, DataUtil.filterByKey(regionCoord::regionContainsChunk), DataUtil.mapWriter(ChunkCoordinate::write, DataUtil.collectionWriter(CustomStructureCoordinate::write))));

                DataUtil.writeOptional(k, BO4CustomStructure.class, BO4CustomStructure::getSmoothingAreasToSpawn, DataUtil::nonEmpty, out1, DataUtil.mappingWriter(out1, DataUtil.filterByKey(regionCoord::regionContainsChunk), DataUtil.mapWriter(ChunkCoordinate::write, DataUtil.collectionWriter(SmoothingAreaLine::write))));

                // Save moddata/particles/spawner data
                // Bo3 objects/structures have start == null
                // For Bo4's, only save for the start bo4, data will be reconstituted when the file is loaded.

                DataUtil.writeOptional(k, CustomStructure::getModData, DataUtil::nonEmpty, out1, DataUtil.mappingWriter(out1, DataUtil.filter(regionCoord::regionContains), DataUtil.collectionWriter(ModDataFunction::write)));

                DataUtil.writeOptional(k, CustomStructure::getSpawnerData, DataUtil::nonEmpty, out1, DataUtil.mappingWriter(out1, DataUtil.filter(regionCoord::regionContains), DataUtil.collectionWriter(SpawnerFunction::write)));

                DataUtil.writeOptional(k, CustomStructure::getParticleData, DataUtil::nonEmpty, out1, DataUtil.mappingWriter(out1, DataUtil.filter(regionCoord::regionContains), DataUtil.collectionWriter(ParticleFunction::write)));
            }));
        });
    }

    // TODO: Load one region file at a time, on-demand, rather than loading all region files at once.
    // Almost everything should be set up for it, auto-replacing CustomStructurePlaceHolders take care of most things?
    public static Map<CustomStructure, List<ChunkCoordinate>> loadStructureData(LocalWorld world)
    {
        Map<CustomStructure, Entry<CustomStructure, List<ChunkCoordinate>>> output = new HashMap<>();

        readAll(world, WorldStandardValues.StructureDataFolderName, (p, in) -> {
            ChunkCoordinate key;
            try
            {
                key = parseChunkCoordinate(p);
            }
            catch(IllegalArgumentException e)
            {
                return;
            }

            // When parsing structures per region, merge all placeholder structures 
            // into their real structure starts as soon as their regions are loaded.
            parseStructuresFileFromStream(in, key, world).values().stream().map(Map::entrySet).flatMap(Set::stream).forEach(e -> {
                output.merge(e.getKey(), e, (oldEntry, newEntry) -> {
                    if(newEntry.getKey() instanceof CustomStructurePlaceHolder)
                    {
                        ((CustomStructurePlaceHolder) newEntry.getKey()).mergeWithCustomStructure(world, (BO4CustomStructure) oldEntry.getKey());
                        oldEntry.getValue().addAll(newEntry.getValue());
                        return oldEntry;
                    }
                    if(oldEntry.getKey() instanceof CustomStructurePlaceHolder)
                    {
                        ((CustomStructurePlaceHolder) oldEntry.getKey()).mergeWithCustomStructure(world, (BO4CustomStructure) newEntry.getKey());
                        newEntry.getValue().addAll(oldEntry.getValue());
                        return new SimpleEntry<>(newEntry);
                    }
                    return oldEntry;
                });
            });
        });

        if(output.isEmpty())
        {
            return null;
        }
        return output.values().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    // TODO: Since we're using regions now, can use byte/short for internal coords instead of int.
    // TODO: Dev versions of v9 used region size 100, not 250, this may cause problems.
    private static Map<String, Map<CustomStructure, List<ChunkCoordinate>>> parseStructuresFileFromStream(DataInputStream in, ChunkCoordinate regionCoord, LocalWorld world) throws IOException
    {
        in.readInt(); // version

        return DataUtil.readMap(new HashMap<>(), in, in1 -> {
            String structureName = StreamHelper.readStringFromStream(in1);
            return new SimpleEntry<>(structureName, DataUtil.readMap(new HashMap<>(), in1, in2 -> {

                CustomStructureCoordinate structureStart = null;

                // Check if this is a structure start
                if(!structureName.equals("NULL"))
                {
                    Rotation startRotationId = Rotation.getRotation(in2.readInt());
                    int startX = in2.readInt();
                    int startY = in2.readInt();
                    int startZ = in2.readInt();

                    if(world.isBo4Enabled())
                    {
                        structureStart = new BO4CustomStructureCoordinate(world, null, structureName, startRotationId, startX, (short) startY, startZ, 0, false, false, null);
                    }
                    else
                    {
                        structureStart = new BO3CustomStructureCoordinate(world, null, structureName, startRotationId, startX, (short) startY, startZ);
                    }
                }

                List<ChunkCoordinate> chunkCoords = DataUtil.readCollection(new ArrayList<>(), in, ChunkCoordinate::read);

                Map<ChunkCoordinate, Stack<BO4CustomStructureCoordinate>> objectsToSpawn = DataUtil.readOptional(new HashMap<>(), in2, DataUtil.mapReader(HashMap::new, ChunkCoordinate::read, DataUtil.collectionReader(Stack::new, BO4CustomStructureCoordinate.reader(world))));

                Map<ChunkCoordinate, ArrayList<SmoothingAreaLine>> smoothingAreasToSpawn = DataUtil.readOptional(new HashMap<>(), in2, DataUtil.mapReader(HashMap::new, ChunkCoordinate::read, DataUtil.collectionReader(ArrayList::new, SmoothingAreaLine::read)));

                HashSet<ModDataFunction<?>> modData = DataUtil.readOptional(new HashSet<>(), in2, DataUtil.collectionReader(HashSet::new, world.isBo4Enabled() ? BO4ModDataFunction::read : BO3ModDataFunction::read));
                HashSet<SpawnerFunction<?>> spawnerData = DataUtil.readOptional(new HashSet<>(), in2, DataUtil.collectionReader(HashSet::new, world.isBo4Enabled() ? BO4SpawnerFunction::read : BO3SpawnerFunction::read));
                HashSet<ParticleFunction<?>> particleData = DataUtil.readOptional(new HashSet<>(), in2, DataUtil.collectionReader(HashSet::new, world.isBo4Enabled() ? BO4ParticleFunction::read : BO3ParticleFunction::read));

                CustomStructure structure;
                if(world.isBo4Enabled())
                {
                    // If the structure start is outside the current region, it's a placeholder.
                    // We'll replace the placeholder in worldInfoChunks as soon as the region data
                    // containing the "real" structure start is loaded.
                    ChunkCoordinate startChunkCoord = ChunkCoordinate.fromChunkCoords(structureStart.getChunkX(), structureStart.getChunkZ());
                    if(!startChunkCoord.toRegionCoord().equals(regionCoord))
                    {
                        structure = new CustomStructurePlaceHolder(world, (BO4CustomStructureCoordinate) structureStart, objectsToSpawn, smoothingAreasToSpawn, 0);
                    }
                    else
                    {
                        structure = new BO4CustomStructure(world, (BO4CustomStructureCoordinate) structureStart, objectsToSpawn, smoothingAreasToSpawn, 0);
                    }
                    ((BO4CustomStructure) structure).startChunkBlockChecksDone = true;
                }
                else
                {
                    structure = new BO3CustomStructure((BO3CustomStructureCoordinate) structureStart);
                }
                structure.modDataManager.modData = modData;
                structure.spawnerManager.spawnerData = spawnerData;
                structure.particlesManager.particleData = particleData;
                return new SimpleEntry<>(structure, chunkCoords);
            }));
        });
    }

    public static void saveChunksMapFile(LocalWorld world, Map<String, List<ChunkCoordinate>> spawnedStructuresByName, Map<String, Map<ChunkCoordinate, Integer>> spawnedStructuresByGroup)
    {
        write(world, WorldStandardValues.SpawnedStructuresFileName, out -> {
            out.writeInt(1); // version

            DataUtil.writeMap(spawnedStructuresByName, out, StreamHelper::writeStringToStream, DataUtil.collectionWriter(ChunkCoordinate::write));
            DataUtil.writeMap(spawnedStructuresByGroup, out, StreamHelper::writeStringToStream, DataUtil.mapWriter(ChunkCoordinate::write, DataOutput::writeInt));
        });
    }

    public static void loadChunksMapFile(LocalWorld world, Map<String, List<ChunkCoordinate>> spawnedStructuresByName, Map<String, Map<ChunkCoordinate, Integer>> spawnedStructuresByGroup)
    {
        spawnedStructuresByName.clear();
        spawnedStructuresByGroup.clear();

        read(world, WorldStandardValues.SpawnedStructuresFileName, in -> {
            Map<String, List<ChunkCoordinate>> chunksByName = new HashMap<>();
            Map<String, Map<ChunkCoordinate, Integer>> chunksByGroup = new HashMap<>();

            in.readInt(); // version

            DataUtil.readMap(chunksByName, in, StreamHelper::readStringFromStream, DataUtil.collectionReader(ArrayList::new, ChunkCoordinate::read));
            DataUtil.readMap(chunksByGroup, in, StreamHelper::readStringFromStream, DataUtil.mapReader(HashMap::new, ChunkCoordinate::read, DataInput::readInt));

            spawnedStructuresByName.putAll(chunksByName);
            spawnedStructuresByGroup.putAll(chunksByGroup);
        });
    }
}
