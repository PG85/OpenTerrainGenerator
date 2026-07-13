package com.pg85.otg.customobject;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.customobject.structures.CustomStructureCache;
import com.pg85.otg.interfaces.IWorldGenRegion;
import com.pg85.otg.util.bo3.Rotation;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.LocalMaterials;
import com.pg85.otg.util.materials.MaterialSet;
import com.pg85.otg.util.minecraft.TreeType;

import java.nio.file.Path;
import java.util.Random;

/**
 * A Minecraft tree, viewed as a custom object.
 *
 * <p>For historical reasons, TreeObject implements {@link CustomObject} instead
 * of just {@link SpawnableObject}. We can probably refactor the Tree resource
 * to accept {@link SpawnableObject}s instead of {@link CustomObject}s, so that
 * all the extra methods are no longer needed.
 */
class TreeObject implements CustomObject {
    private final TreeType type;

    TreeObject(TreeType type) {
        this.type = type;
    }

    @Override
    public boolean onEnable(
            String presetFolderName,
            Path otgRootFolder
    ) {
        return true;
    }

    @Override
    public String getName() {
        return type.name();
    }

    @Override
    public boolean canSpawnAsTree() {
        return true;
    }

    // Called during decoration.
    @Override
    public boolean process(
            CustomStructureCache structureCache,
            IWorldGenRegion world,
            Random random
    ) {
        // A tree has no frequency or rarity, so spawn it once in the chunk
        // Make sure we stay within decoration bounds.
        int x = world.getDecorationArea().getChunkBeingDecoratedMinX()
                + random.nextInt(Constants.CHUNK_SIZE);
        int z = world.getDecorationArea().getChunkBeingDecoratedMinZ()
                + random.nextInt(Constants.CHUNK_SIZE);
        int y = world.getHighestBlockAboveYAt(x, z);
        if (y < world.getWorldInfo().minY() || y > world.getWorldInfo().maxY()) {
            return false;
        }
        if (!canSpawnAt(world, x, y, z, null)) {
            return false;
        }
        return spawnForced(
                structureCache,
                world,
                random,
                Rotation.NORTH,
                x,
                y,
                z,
                false
        );
    }

    /**
     * getHighestBlockAboveYAt also returns the first position above liquids,
     * so without this check trees spawn on top of oceans, lakes and ice.
     * When sourceBlocks is given, the block below must be in the set;
     * otherwise anything solid except liquids and ice is accepted.
     */
    private boolean canSpawnAt(IWorldGenRegion world, int x, int y, int z, MaterialSet sourceBlocks) {
        LocalMaterialData blockAtY = world.getMaterial(x, y, z);
        if (blockAtY == null || !blockAtY.isAir()) {
            return false;
        }
        LocalMaterialData blockBelow = world.getMaterial(x, y - 1, z);
        if (blockBelow == null) {
            return false;
        }
        if (sourceBlocks != null) {
            return sourceBlocks.contains(blockBelow);
        }
        return !blockBelow.isLiquid()
                && !blockBelow.isMaterial(LocalMaterials.ICE)
                && !blockBelow.isMaterial(LocalMaterials.PACKED_ICE)
                && !blockBelow.isMaterial(LocalMaterials.BLUE_ICE);
    }

    @Override
    public boolean spawnFromSapling(
            IWorldGenRegion worldGenRegion,
            Random random,
            Rotation rotation,
            int x,
            int y,
            int z
    ) {
        return worldGenRegion.placeTree(type, random, x, y, z);
    }

    @Override
    public boolean spawnForced(
            CustomStructureCache structureCache,
            IWorldGenRegion world,
            Random random,
            Rotation rotation,
            int x,
            int y,
            int z,
            boolean allowReplaceBlocks
    ) {
        return world.placeTree(type, random, x, y, z);
    }

    @Override
    public boolean spawnAsTree(
            CustomStructureCache structureCache,
            IWorldGenRegion world,
            Random random,
            int x,
            int z,
            int minY,
            int maxY,
            MaterialSet sourceBlocks
    ) {
        int y = world.getHighestBlockAboveYAt(x, z);
        Rotation rotation = Rotation.getRandomRotation(random);

        if (!(minY == -1 && maxY == -1)) {
            if (y < minY || y > maxY) {
                return false;
            }
        }

        if (y < world.getWorldInfo().minY() || y > world.getWorldInfo().maxY()) {
            return false;
        }

        if (!canSpawnAt(world, x, y, z, sourceBlocks)) {
            return false;
        }

        return spawnForced(
                structureCache,
                world,
                random,
                rotation,
                x,
                y,
                z,
                true
        );
    }

    @Override
    public boolean canRotateRandomly() {
        // Trees cannot be rotated
        return false;
    }

    @Override
    public boolean loadChecks() {
        return true;
    }

    @Override
    public boolean doReplaceBlocks() {
        return false;
    }
}
