package com.pg85.otg.shared.gen;

import java.util.*;

import com.pg85.otg.shared.biome.SharedBiome;
import com.pg85.otg.shared.gen.noise.OTGNoiseCaveFiller;
import com.pg85.otg.shared.gen.noise.OTGNoiseRouterFactory;
import com.pg85.otg.shared.materials.SharedMaterialData;
import com.pg85.otg.shared.mixin.WorldGenRegionAccessor;
import com.pg85.otg.interfaces.IBiome;
import com.pg85.otg.interfaces.ICachedBiomeProvider;
import com.pg85.otg.util.BlockPos2D;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.FifoMap;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.gen.JigsawStructureData;
import com.pg85.otg.util.gen.OTGWorldInfo;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.LocalMaterials;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionStructure;
import org.jetbrains.annotations.Nullable;

/**
 * Shadow chunk generation means generating base terrain for chunks
 * without using mc's world generation flow. OTG's chunkgenerator is
 * called internally to generate base terrain for dummy chunks in a
 * thread-safe/non-blocking way. Shadowgenned chunks are stored in a
 * fixed size FIFO cache, data is reused when base terraingen is requested
 * for those chunks via normal worldgen. Shadowgen is used for BO4's,
 * worker threads to speed up world generation and /otg mapterrain.
 * <p>
 * Shadowgen can only be done for chunks that don't contain vanilla structures,
 * since those structures may use density based smoothing applied during noisegen,
 * which unfortunately is done in a non-thread-safe/blocking manner, necessitating
 * the use of a WorldGenRegion.
 */
public class ShadowChunkGenerator {
    // TODO: Add a setting to the worldconfig for the size of these caches?
    private final FifoMap<BlockPos2D, LocalMaterialData[]> unloadedBlockColumnsCache = new FifoMap<BlockPos2D, LocalMaterialData[]>(
            1024);
    private final FifoMap<ChunkCoordinate, ChunkAccess> unloadedChunksCache = new FifoMap<ChunkCoordinate, ChunkAccess>(
            512);
    private final FifoMap<ChunkCoordinate, Integer> hasVanillaStructureChunkCache = new FifoMap<ChunkCoordinate, Integer>(
            2048);
    private final FifoMap<ChunkCoordinate, Integer> hasVanillaNoiseStructureChunkCache = new FifoMap<ChunkCoordinate, Integer>(
            2048);

    private final Object workerLock = new Object();
    private final int maxConcurrent;
    private final Worker[] threads;
    private boolean threadsInitialized = false;
    private final LinkedList<ChunkCoordinate> chunksToLoad = new LinkedList<ChunkCoordinate>();
    private static final int maxQueueSize = 512;
    private final ChunkCoordinate[] chunksBeingLoaded;
    private static final int waitTimeInMS = 25;
    private static final int idleTimeInMS = 50;
    private volatile int cacheHits = 0;
    private volatile int cacheMisses = 0;

    public ShadowChunkGenerator(int maxConcurrentThreads) {
        this.maxConcurrent = maxConcurrentThreads;
        this.threads = new Worker[this.maxConcurrent];
        // chunksBeingLoaded[maxConcurrent] means worldgen thread, not a worker thread.
        this.chunksBeingLoaded = new ChunkCoordinate[this.maxConcurrent + 1];
    }

    // Called on world unload to stop threads and release resources.
    public void stopWorkerThreads() {
        if (this.maxConcurrent > 0) {
            for (int i = 0; i < this.maxConcurrent; i++) {
                if (this.threads[i] != null) {
                    this.threads[i].stop();
                }
            }
        }
    }

    // Whenever MC requests noisegen/base terrain gen for a chunk, it also exposes a cache of chunks currently loaded/queued.
    // These chunks are highly likely to be requested next, so we can filter out any that need noisegen/base terrain gen and
    // pre-emptively generate and cache them asynchronously. When MC requests those chunks a moment later as part of worldgen,
    // we return the async generated chunk data.
    public void queueChunksForWorkerThreads(
            WorldGenRegion worldGenRegion, ChunkAccess chunk,
            SharedOTGChunkGenerator otgChunkGenerator,
            OTGWorldInfo otgWorldInfo
    ) {
        if (this.maxConcurrent > 0) {
            if (!this.threadsInitialized) {
                for (int i = 0; i < this.maxConcurrent; i++) {
                    @SuppressWarnings("deprecation")
                    Worker thread = this.new Worker(i, this.unloadedChunksCache, this.chunksToLoad,
                                                    this.chunksBeingLoaded,
                                                    worldGenRegion.getLevel(),
                                                    otgChunkGenerator,
                                                    otgWorldInfo
                    );
                    this.threads[i] = thread;
                    thread.start();
                }
                this.threadsInitialized = true;
            }
            synchronized (this.workerLock) {
                //OTG.log(LogMarker.INFO, "Fetching chunks for async chunkgen");
                for (ChunkAccess wgrChunk : ((WorldGenRegionAccessor) worldGenRegion).getCache()) {
                    ChunkCoordinate wgrChunkCoord = ChunkCoordinate.fromChunkCoords(wgrChunk.getPos().x,
                                                                                    wgrChunk.getPos().z
                    );
                    if (wgrChunk != chunk && !wgrChunk.getStatus().isOrAfter(ChunkStatus.NOISE)) {
                        if (!this.unloadedChunksCache.containsKey(wgrChunkCoord) && !this.chunksToLoad.contains(wgrChunkCoord)) {
                            boolean bFound = false;
                            for (ChunkCoordinate chunkCoordinate : this.chunksBeingLoaded) {
                                if (wgrChunkCoord.equals(chunkCoordinate)) {
                                    bFound = true;
                                    break;
                                }
                            }
                            if (!bFound) {
                                // TODO: Queue order shouldn't really matter bc
                                // of the way maxQueueSize is enforced here.
                                // Might affect cache hits/misses and waits tho, test?
                                this.chunksToLoad.addFirst(wgrChunkCoord);
                                if (this.chunksToLoad.size() == maxQueueSize) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private SharedChunkBuffer getUnloadedChunk(
            ServerLevel serverLevel,
            SharedOTGChunkGenerator otgChunkGenerator, OTGWorldInfo otgWorldInfo,
            ChunkCoordinate chunkCoordinate
    ) {

        Registry<Biome> biomeRegistry = getRegistry(serverLevel.registryAccess(), Registries.BIOME);
        if (biomeRegistry == null) {
            throw new RuntimeException("Could not get biome registry when loading chunk");
        }

        ProtoChunk chunk = new ProtoChunk(
                new ChunkPos(chunkCoordinate.getChunkX(), chunkCoordinate.getChunkZ()),
                UpgradeData.EMPTY,
                serverLevel,
                biomeRegistry,
                null
        );

        SharedChunkBuffer buffer = new SharedChunkBuffer(chunk);

        // This is where vanilla processes any noise affecting structures like villages, in order to spawn smoothing areas.
        // Doing this for unloaded chunks causes a hang on load since getChunk is called by StructureManager.
        // BO4's/shadowgen avoid villages, so this method should never be called to fetch unloaded chunks that contain villages,
        // so we can skip noisegen affecting structures here.

        // Fill biomes before noise, matching MC's BIOMES -> NOISE phase order.
        // Climate.Sampler is a record and cannot be trivially instantiated, but SharedOTGBiomeProvider
        // ignores the sampler argument entirely, so null is safe here.
        chunk.fillBiomesFromNoise(otgChunkGenerator.getBiomeSource(), null);

        Random random = otgChunkGenerator.getRandomFromChunkCoord(chunkCoordinate);
        if (otgChunkGenerator.isVanillaCavesEnabled()) {
            // Vanilla noise cave path: shadow chunks must match what fillFromNoise produces, or
            // BO4 height/material checks would see cave-less terrain. Structure terrain
            // adaptation is skipped (empty Beardifier) — shadowgen already avoids chunks near
            // noise-affecting structures, same guarantee as the legacy path.
            OTGNoiseRouterFactory.OTGNoiseCaveContext ctx =
                    otgChunkGenerator.ensureNoiseCaveContext(serverLevel.registryAccess());
            NoiseChunk noiseChunk = NoiseChunk.forChunk(
                    chunk,
                    ctx.randomState(),
                    new Beardifier(
                            ObjectLists.<Beardifier.Rigid>emptyList().listIterator(),
                            ObjectLists.<JigsawJunction>emptyList().listIterator()
                    ),
                    ctx.runtimeSettings(),
                    ctx.fluidPicker(),
                    Blender.empty()
            );
            OTGNoiseCaveFiller.fill(noiseChunk, chunk, buffer, ctx.runtimeSettings());
            otgChunkGenerator.getInternalGenerator().doSurfaceAndGroundControlForChunk(otgWorldInfo, buffer, random);
        } else {
            ObjectList<JigsawStructureData> structures = new ObjectArrayList<>(10);
            otgChunkGenerator.getInternalGenerator().populateNoise(otgWorldInfo, buffer, buffer.getChunkCoordinate(), structures, random);
        }
        return buffer;
    }

    public ChunkAccess getChunkWithWait(ChunkCoordinate chunkCoord) {
        // Fetch the chunk if it is cached, otherwise check if no other thread
        // is generating the chunk. If not, claim the chunk and generate it.
        // If so, wait for the other thread to finish.
        synchronized (this.workerLock) {
            ChunkAccess cachedChunk = this.unloadedChunksCache.get(chunkCoord);
            if (cachedChunk != null) {
                return cachedChunk;
            } else {
                // If a chunk is in unloadedChunksCache but is null, it's in a chunk that
                // shouldn't be generated async due to a vanilla structure start nearby.
                if (this.unloadedChunksCache.containsKey(chunkCoord)) {
                    this.unloadedChunksCache.remove(chunkCoord);
                    this.chunksToLoad.remove(chunkCoord);
                    // MaxConcurrent means worldgen thread, not a worker thread.
                    this.chunksBeingLoaded[this.maxConcurrent] = chunkCoord;
                    return null;
                } else {
                    boolean bFound = false;
                    for (ChunkCoordinate chunkCoordinate : this.chunksBeingLoaded) {
                        if (chunkCoord.equals(chunkCoordinate)) {
                            bFound = true;
                            break;
                        }
                    }
                    if (!bFound) {
                        this.chunksToLoad.remove(chunkCoord);
                        // MaxConcurrent means worldgen thread, not a worker thread.
                        this.chunksBeingLoaded[this.maxConcurrent] = chunkCoord;
                        return null;
                    }
                }
            }
        }

        // A worker thread is generating the chunk, wait.

        while (true) {
            try {
                //OTG.log(LogMarker.INFO, "Waiting for chunk");
                // TODO: If a worker thread is stuck or crashed, this may wait indefinitely.
                Thread.sleep(waitTimeInMS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (this.workerLock) {
                ChunkAccess cachedChunk = this.unloadedChunksCache.get(chunkCoord);
                if (cachedChunk != null) {
                    return cachedChunk;
                } else {
                    // If a chunk is in unloadedChunksCache but is null, it's in a chunk that
                    // shouldn't be generated async due to a vanilla structure start nearby.
                    if (this.unloadedChunksCache.containsKey(chunkCoord)) {
                        this.unloadedChunksCache.remove(chunkCoord);
                        this.chunksToLoad.remove(chunkCoord);
                        // MaxConcurrent means worldgen thread, not a worker thread.
                        this.chunksBeingLoaded[this.maxConcurrent] = chunkCoord;
                        return null;
                    }
                }
            }
        }
    }

    public void setChunkGenerated(ChunkCoordinate chunkCoord) {
        //OTG.log(LogMarker.INFO, "Cache miss " + + this.cacheMisses);
        synchronized (workerLock) {
            this.cacheMisses++;
            // Zero index, so MaxConcurrent means worldgen thread, not a worker thread.
            this.chunksBeingLoaded[this.maxConcurrent] = null;
            this.chunksToLoad.remove(chunkCoord);
        }
    }

    // Vanilla structure detection (avoidance)
    // Some vanilla structures use density based smoothing of terrain underneath, which is factored into noisegen.
    // Unfortunately this requires fetching structure data in a non-thread-safe manner, so we can't do async
    // chunkgen (base terrain) for these chunks and have to avoid them.

    public boolean checkHasVanillaStructureWithoutLoading(
            ServerLevel serverWorld, ChunkCoordinate chunkCoordinate,
            ICachedBiomeProvider cachedBiomeProvider, boolean noiseAffectingOnly
    ) {
        StructureManager manager = serverWorld.structureManager();

        RegistryAccess registryAccess = manager.registryAccess();

        Registry<Structure> structureRegistry = getRegistry(registryAccess, Registries.STRUCTURE);
        if (structureRegistry == null) {
            OTGLog.error("Failed to get structure registry during shadow chunk gen");
            return false;
        }

        Registry<Biome> biomeRegistry = getRegistry(registryAccess, Registries.BIOME);
        if (biomeRegistry == null) {
            OTGLog.error("Failed to get structure registry during shadow chunk gen");
            return false;
        }

        // Since we can't check for structure components/references, only structure starts,
        // we'll keep a safe distance away from any vanilla structure start points.
        int radiusInChunks = 5;
        ProtoChunk chunk;
        ChunkPos chunkpos;
        IBiome biome;
        if (!serverWorld.getServer().getWorldData().worldGenOptions().generateStructures()) {
            return false;
        }

        List<ChunkCoordinate> chunksToHandle = new ArrayList<>();
        Map<ChunkCoordinate, Integer> chunksHandled = new HashMap<>();
        if (noiseAffectingOnly) {
            synchronized (this.hasVanillaNoiseStructureChunkCache) {
                if (checkHasVanillaStructureWithoutLoadingCache(this.hasVanillaNoiseStructureChunkCache,
                                                                chunkCoordinate, radiusInChunks, chunksToHandle
                )) {
                    return true;
                }
            }
        } else {
            synchronized (this.hasVanillaStructureChunkCache) {
                if (checkHasVanillaStructureWithoutLoadingCache(this.hasVanillaStructureChunkCache, chunkCoordinate,
                                                                radiusInChunks, chunksToHandle
                )) {
                    return true;
                }
            }
        }

        Set<ResourceLocation> allStructures = structureRegistry.keySet();

        for (ChunkCoordinate chunkToHandle : chunksToHandle) {
            chunk = new ProtoChunk(
                    new ChunkPos(chunkToHandle.getChunkX(), chunkToHandle.getChunkZ()),
                    UpgradeData.EMPTY,
                    serverWorld,
                    biomeRegistry,
                    null
            );
            chunkpos = chunk.getPos();

            int distanceFromQuery = (int) Math.floor(Math.sqrt(
                    Math.pow(chunkToHandle.getChunkX() - chunkCoordinate.getChunkX(), 2) + Math.pow(
                            chunkToHandle.getChunkZ() - chunkCoordinate.getChunkZ(), 2)));

            // Borrowed from STRUCTURE_STARTS phase of chunkgen, only determines structure start point
            // based on biome and resource settings (distance etc). Does not plot any structure components.

            // TODO: Optimise this for biome lookups, fetch a whole region of noise biome info at once?
            biome = cachedBiomeProvider.getNoiseBiome((chunkpos.x << 2) + 2, (chunkpos.z << 2) + 2);

            Biome regBiome = ((SharedBiome) biome).getBiome();

            Holder<Biome> biomeHolder = biomeRegistry.wrapAsHolder(regBiome);

            for (ResourceLocation resourceLocation : allStructures) {
                Structure structure = structureRegistry.get(resourceLocation);

                if (structure == null) {
                    continue;
                }

                if (!structure.biomes().contains(biomeHolder)) {
                    continue;
                }

                int radius = 1;

                if (structure instanceof JigsawStructure ||
                        structure instanceof EndCityStructure ||
                        structure instanceof OceanMonumentStructure ||
                        structure instanceof WoodlandMansionStructure
                ) {
                    radius = 4;
                }

                if (hasStructureStart(structure, manager, chunkpos, noiseAffectingOnly)) {
                    chunksHandled.put(chunkToHandle, radius);
                    if (radius >= distanceFromQuery) {
                        if (noiseAffectingOnly) {
                            synchronized (this.hasVanillaNoiseStructureChunkCache) {
                                this.hasVanillaNoiseStructureChunkCache.putAll(chunksHandled);
                            }
                        } else {
                            synchronized (this.hasVanillaStructureChunkCache) {
                                this.hasVanillaStructureChunkCache.putAll(chunksHandled);
                            }
                        }
                        return true;
                    }
                }
            }
            chunksHandled.putIfAbsent(chunkToHandle, 0);
        }
        if (noiseAffectingOnly) {
            synchronized (this.hasVanillaNoiseStructureChunkCache) {
                this.hasVanillaNoiseStructureChunkCache.putAll(chunksHandled);
            }
        } else {
            synchronized (this.hasVanillaStructureChunkCache) {
                this.hasVanillaStructureChunkCache.putAll(chunksHandled);
            }
        }
        return false;
    }

    private static @Nullable <T> Registry<T> getRegistry(RegistryAccess registryAccess, ResourceKey<Registry<T>> resourceKey) {
        Optional<Registry<T>> ops = registryAccess.registry(resourceKey);

        if (ops.isEmpty()) {
            OTGLog.error("Could not get structure registry during shadow chunk gen");
            return null;
        }

        return ops.get();
    }

    private boolean checkHasVanillaStructureWithoutLoadingCache(
            FifoMap<ChunkCoordinate, Integer> cache, ChunkCoordinate chunkCoordinate, int radiusInChunks,
            List<ChunkCoordinate> chunksToHandle
    ) {
        for (int cycle = 0; cycle < radiusInChunks; ++cycle) {
            for (int xOffset = -cycle; xOffset <= cycle; ++xOffset) {
                for (int zOffset = -cycle; zOffset <= cycle; ++zOffset) {
                    int distance = (int) Math.floor(Math.sqrt(Math.pow(xOffset, 2) + Math.pow(zOffset, 2)));
                    if (distance == cycle) {
                        ChunkCoordinate searchChunk = ChunkCoordinate.fromChunkCoords(
                                chunkCoordinate.getChunkX() + xOffset, chunkCoordinate.getChunkZ() + zOffset);
                        Integer result = cache.get(searchChunk);
                        if (result != null) {
                            if (result > 0 && result >= distance) {
                                return true;
                            }
                        } else {
                            chunksToHandle.add(searchChunk);
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasStructureStart(
            Structure structure, StructureManager manager, ChunkPos chunkPos, boolean noiseAffectingOnly
    ) {
        TerrainAdjustment terrainAdjustment = structure.terrainAdaptation();

        if (noiseAffectingOnly && (terrainAdjustment == TerrainAdjustment.NONE || terrainAdjustment == TerrainAdjustment.BURY)) {
            return false;
        }

        if (structure.step() != GenerationStep.Decoration.SURFACE_STRUCTURES) {
            return false;
        }

        return switch (manager.checkStructurePresence(chunkPos, structure, false)) {
            case START_PRESENT -> true;
            case START_NOT_PRESENT -> false;
            case CHUNK_LOAD_NEEDED -> {
                // Chunk data isn't loaded yet (common during pregeneration). Proceed with shadow
                // gen rather than blocking BO4 lookahead — the occasional minor terrain mismatch
                // near a vanilla noise-affecting structure is the acceptable tradeoff.
                yield false;
            }
        };
    }

    public void fillWorldGenChunkFromShadowChunk(ChunkAccess chunk, ChunkAccess cachedChunk) {
        ChunkCoordinate chunkCoord = ChunkCoordinate.fromChunkCoords(chunk.getPos().x, chunk.getPos().z);

        System.arraycopy(cachedChunk.getSections(), 0, chunk.getSections(), 0, chunk.getSections().length);

        for (Map.Entry<Heightmap.Types, Heightmap> entry : cachedChunk.getHeightmaps()) {
            Heightmap.Types type = entry.getKey();
            Heightmap heightmap = entry.getValue();
            chunk.setHeightmap(type, heightmap.getRawData());
        }
        //OTG.log(LogMarker.INFO, "Cache hit " + this.cacheHits);
        synchronized(this.workerLock)
        {
            this.cacheHits++;
            this.unloadedChunksCache.remove(chunkCoord);
        }
    }

    // /otg mapterrain

    // /otg mapterrain fetches chunks in order to create a map of base terrain, without touching any of the caches or
    // resources used for worldgen or bo4 shadowgen, since the chunks aren't actually supposed to generate in the world.
    // We won't get any density based smoothing applied to noisegen for vanilla structures, but that's ok for /otg mapterrain.

    public SharedChunkBuffer getChunkWithoutLoadingOrCaching(
            ServerLevel serverLevel,
            SharedOTGChunkGenerator otgChunkGenerator, OTGWorldInfo otgWorldInfo, Random random,
            ChunkCoordinate chunkCoordinate
    ) {
        return getUnloadedChunk(serverLevel, otgChunkGenerator, otgWorldInfo, chunkCoordinate);
    }

    // BO4's / Smoothing Areas

    // BO4's and smoothing areas may do material and height checks in unloaded chunks during decoration.
    // Shadowgen is used to do this without causing cascades. Shadowgenned chunks are requested on-demand for the worldgen thread (BO4's).
    // Async worker threads may also pre-emptively shadowgen and cache unloaded chunks, which speeds up base terrain generation but also BO4's.
    // Note: BO4's are always processed on the worldgen thread, never on a worker thread, since they are not a part of base terrain generation.

    private LocalMaterialData[] getBlockColumnInUnloadedChunk(
            ServerLevel serverLevel,
            SharedOTGChunkGenerator otgChunkGenerator, OTGWorldInfo otgWorldInfo, int x, int z
    ) {
        BlockPos2D blockPos = new BlockPos2D(x, z);
        ChunkCoordinate chunkCoord = ChunkCoordinate.fromBlockCoords(x, z);

        LocalMaterialData[] cachedColumn = this.unloadedBlockColumnsCache.get(blockPos);

        if (cachedColumn != null) {
            return cachedColumn;
        }

        ChunkAccess chunk = this.getChunkWithWait(chunkCoord);
        if (chunk == null) {
            // Generate a chunk without loading/decorating it
            chunk = getUnloadedChunk(serverLevel, otgChunkGenerator, otgWorldInfo, chunkCoord).getChunkAccess();
            synchronized (this.workerLock) {
                this.unloadedChunksCache.put(chunkCoord, chunk);
                // Zero index, so MaxConcurrent means worldgen thread, not a worker thread.
                this.chunksBeingLoaded[this.maxConcurrent] = null;
            }
        }

        // Get internal coordinates for block in chunk
        byte blockX = (byte) (x & 0xF);
        byte blockZ = (byte) (z & 0xF);

        LocalMaterialData[] blocksInColumn = new LocalMaterialData[otgWorldInfo.getHeight()];
        BlockState blockInChunk;

        for (short y = 0; y < otgWorldInfo.getHeight(); y++) {
            blockInChunk = chunk.getBlockState(new BlockPos(blockX, y, blockZ));
            blocksInColumn[y] = SharedMaterialData.ofBlockState(blockInChunk);
        }

        this.unloadedBlockColumnsCache.put(blockPos, blocksInColumn);

        return blocksInColumn;
    }

    public LocalMaterialData getMaterialInUnloadedChunk(
            ServerLevel serverLevel,
            SharedOTGChunkGenerator otgChunkGenerator, OTGWorldInfo otgWorldInfo, int x, int y, int z
    ) {
        LocalMaterialData[] blockColumn = getBlockColumnInUnloadedChunk(
                serverLevel, otgChunkGenerator, otgWorldInfo, x, z
        );
        return blockColumn[y];
    }

    public int getHighestBlockYInUnloadedChunk(
            ServerLevel serverLevel,
            SharedOTGChunkGenerator otgChunkGenerator, OTGWorldInfo otgWorldInfo, int x, int z,
            boolean findSolid, boolean findLiquid, boolean ignoreLiquid, boolean ignoreSnow
    ) {
        int height = otgWorldInfo.minY() - 1;

        LocalMaterialData[] blockColumn = getBlockColumnInUnloadedChunk(serverLevel, otgChunkGenerator, otgWorldInfo, x,
                                                                        z
        );
        SharedMaterialData material;
        boolean isLiquid;
        boolean isSolid;

        for (int y = otgWorldInfo.maxY(); y >= otgWorldInfo.minY(); y--) {
            material = (SharedMaterialData) blockColumn[y];
            isLiquid = material.isLiquid();
            isSolid = material.isSolid() || (!ignoreSnow && material.isMaterial(LocalMaterials.SNOW));
            if (!(isLiquid && ignoreLiquid)) {
                if ((findSolid && isSolid) || (findLiquid && isLiquid)) {
                    return y;
                }
                if ((findSolid && isLiquid) || (findLiquid && isSolid)) {
                    return -1;
                }
            }
        }
        return height;
    }

    // Async Worker for generating chunks up to ChunkStatus.NOISE.
    // This is only used for chunks that don't require density based
    // smoothing for vanilla structures, since that cannot be done
    // in a thread-safe/non-blocking manner.

    private class Worker implements Runnable {
        private Thread runner;
        private boolean stop = false;

        private final int index;
        private final FifoMap<ChunkCoordinate, ChunkAccess> unloadedChunksCache;
        private final List<ChunkCoordinate> chunksToLoad;
        private final ChunkCoordinate[] chunksBeingLoaded;
        private final ServerLevel serverWorld;
        private final SharedOTGChunkGenerator otgChunkGenerator;
        private final OTGWorldInfo otgWorldInfo;

        Worker(
                int index, FifoMap<ChunkCoordinate, ChunkAccess> unloadedChunksCache,
                List<ChunkCoordinate> chunksToLoad, ChunkCoordinate[] chunksBeingLoaded, ServerLevel serverWorld,
                SharedOTGChunkGenerator otgChunkGenerator, OTGWorldInfo otgWorldInfo
        ) {
            this.index = index;
            this.unloadedChunksCache = unloadedChunksCache;
            this.chunksToLoad = chunksToLoad;
            this.chunksBeingLoaded = chunksBeingLoaded;
            this.serverWorld = serverWorld;
            this.otgChunkGenerator = otgChunkGenerator;
            this.otgWorldInfo = otgWorldInfo;
        }

        public void start() {
            this.runner = new Thread(this);
            this.runner.start();
        }

        public void stop() {
            this.stop = true;
        }

        @Override
        public void run() {
            // Process chunks if any are in the queue,
            // otherwise wait for the queue to be filled.
            while (true) {
                if (this.stop) {
                    this.stop = false;
                    return;
                }

                ChunkCoordinate coords = null;
                int sizeLeft;
                synchronized (workerLock) {
                    sizeLeft = this.chunksToLoad.size();
                    if (sizeLeft > 0) {
                        coords = this.chunksToLoad.remove(sizeLeft - 1);
                        this.chunksBeingLoaded[this.index] = coords;
                    }
                }
                if (coords != null) {
                    if (!checkHasVanillaStructureWithoutLoading(
                            this.serverWorld, coords, this.otgChunkGenerator.getInternalGenerator().getCachedBiomeProvider(), true
                    )) {
                        // Generate a chunk without loading/decorating it.
                        ChunkAccess cachedChunk = getUnloadedChunk(this.serverWorld, this.otgChunkGenerator, this.otgWorldInfo, coords
                        ).getChunkAccess();
                        synchronized (workerLock) {
                            this.unloadedChunksCache.put(coords, cachedChunk);
                            this.chunksBeingLoaded[this.index] = null;
                        }
                    } else {
                        synchronized (workerLock) {
                            // This chunk should not be shadowgenned, add it
                            // to the unloadedChunksCache as null so workers
                            // avoid it and the worldgen thread takes care
                            // of it in getChunkWithWait().
                            this.unloadedChunksCache.put(coords, null);
                            this.chunksBeingLoaded[this.index] = null;
                        }
                    }
                } else {
                    try {
                        //OTG.log(LogMarker.INFO, "Worker " + this.index + " idle");
                        Thread.sleep(idleTimeInMS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

