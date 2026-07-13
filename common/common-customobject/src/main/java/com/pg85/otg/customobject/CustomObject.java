package com.pg85.otg.customobject;

import com.pg85.otg.customobject.structures.CustomStructureCache;
import com.pg85.otg.interfaces.ICustomObject;
import com.pg85.otg.interfaces.IWorldGenRegion;
import com.pg85.otg.util.bo3.Rotation;
import com.pg85.otg.util.materials.MaterialSet;

import java.nio.file.Path;
import java.util.Random;

/**
 * A custom object.
 *
 * <p>A custom object is a user-provided object that can be spawned in the
 * world. Unlike a plain {@link SpawnableObject}, it can have spawn conditions.
 * Also unlike a plain {@link SpawnableObject}, it can have other objects
 * attached to it using a {@link #getBranches(Rotation) branch system}.
 */
public interface CustomObject extends SpawnableObject, ICustomObject {
    /**
	 * Called after all objects are loaded. The settings should be loaded
	 * inside this method.
	 *
	 * @param otherObjectsInDirectory A map of all other objects in the
	 *                                directory. Keys are lowercase.
	 */
    boolean onEnable(
            String presetFolderName,
            Path otgRootFolder
    );

    /**
     * Returns the name of this object.
     *
     * @return The name, without the extension.
     */
    String getName();

    /**
     * Returns whether this object can spawn as a tree. UseWorld and UseBiome
     * should return true.
     *
     * @return Whether this object can spawn as a tree.
     */
    boolean canSpawnAsTree();

    /**
     * Returns whether this object can be placed with a random rotation. If
     * not, the rotation should always be NORTH.
     *
     * @return Whether this object can be placed with a random rotation.
     */
    boolean canRotateRandomly();

    /**
	 * Spawns the object at the given position. It should search a suitable y
	 * location by itself. If the object isn't a tree, it shouldn't spawn and it
	 * should return false.
	 *
	 * @param world
	 * @param x
	 * @param z
	 * @param sourceBlocks Blocks the tree may spawn on, or null for the default rules.
	 * @return Whether the attempt was successful.
	 */
    boolean spawnAsTree(
			CustomStructureCache structureCache,
			IWorldGenRegion world,
			Random random,
			int x,
			int z,
			int minY,
			int maxY,
			MaterialSet sourceBlocks
    );

    /**
	 * Spawns the object one or more times in a chunk. The object can search a good y position by
	 * itself.
	 *
	 * @param chunkCoord The chunk to spawn the objects in.
	 * @param world      The world to spawn in.
	 * @param random     Random number generator based on the world seed.
	 * @return Whether at least one object spawned successfully.
	 */
    boolean process(
			CustomStructureCache structureCache,
			IWorldGenRegion world,
			Random random
    );

    boolean spawnFromSapling(
			IWorldGenRegion worldGenRegion,
			Random random,
			Rotation rotation,
			int x,
			int y,
			int z
    );

    boolean loadChecks();

    // Disables biomeconfig replaceBlocks to save performance.
    boolean doReplaceBlocks();
}
