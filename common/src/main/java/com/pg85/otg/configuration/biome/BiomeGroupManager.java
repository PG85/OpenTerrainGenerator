package com.pg85.otg.configuration.biome;

import com.pg85.otg.OTG;
import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.logging.LogMarker;
import com.pg85.otg.util.WeightedList;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;

/**
 * Manages a collection of biome groups that are accesible by their name and
 * id.
 *
 */
public final class BiomeGroupManager
{
    static final int MAX_BIOME_GROUP_COUNT = 127;
    private final Map<String, BiomeGroup> nameToGroup = new LinkedHashMap<String, BiomeGroup>(4);
    private final BiomeGroup[] idToGroup = new BiomeGroup[MAX_BIOME_GROUP_COUNT + 1];
    public BiomeGroupManager()
    {

    }

    /**
     * Registers a new group. If the group could not be added (for example
     * because the maximum amount of biomes has been reached) a message is
     * logged and the group is not registered.
     * @param newGroup The group to register.
     */
    public void registerGroup(BiomeGroup newGroup)
    {
        if (isRoomForMoreGroups())
        {
            BiomeGroup existingWithSameName = nameToGroup.get(newGroup.getName());
            if (existingWithSameName != null)
            {
                OTG.log(LogMarker.WARN, "Two biome groups have the same name \"{}\". Removing the second one.",
                        newGroup.getName());
                OTG.printStackTrace(LogMarker.WARN, new Exception());
            } else {
                int newGroupId = getNextGroupId();
                newGroup.setGroupId(newGroupId);

                nameToGroup.put(newGroup.getName(), newGroup);
                idToGroup[newGroupId] = newGroup;
            }
        } else {
            OTG.log(LogMarker.WARN, "Biome group \"{}\" could not be added. Max biome group count reached.", newGroup.getName());
        }
    }

    /**
     * Gets the next group id. This group id is based on which group ids are
     * currently in used.
     * @return The next group id.
     */
    private int getNextGroupId()
    {
        // Adding +1 ensures that the id 0 will never be in use. The id 0
        // seems to be used as a null value by the biome generator
        return getGroupCount() + 1;
    }

    /**
     * Checks if the next group id will still fit in the group limit.
     * @return True if the next group id will fit, false otherwise.
     */
    private boolean isRoomForMoreGroups()
    {
        return getNextGroupId() < MAX_BIOME_GROUP_COUNT;
    }

    /**
     * Gets the group with the given group id.
     * @param groupId Id of the group.
     * @return The group, or null if no such group exists.
     */
    public BiomeGroup getGroupById(int groupId)
    {
        return idToGroup[groupId];
    }

    /**
     * Gets the group with the given name.
     * @param name Name of the group, case sensitive.
     * @return The group.
     */
    public BiomeGroup getGroupByName(String name)
    {
        return nameToGroup.get(name);
    }

    /**
     * Gets all groups.
     * @return All groups.
     */
    public Collection<BiomeGroup> getGroups()
    {
        return nameToGroup.values();
    }

    /**
     * Gets the amount of groups currently registered. Calling this method is
     * equivalent to calling {@code getGroups().size()}.
     * @return The amount of groups.
     */
    public int getGroupCount()
    {
        return nameToGroup.size();
    }

    private final Int2ObjectMap<WeightedList<BiomeGroup>> cachedGroupDepthMaps = new Int2ObjectOpenHashMap<>();

    public WeightedList<BiomeGroup> getGroupDepthMap(int depth)
    {
        WeightedList<BiomeGroup> map = this.cachedGroupDepthMaps.get(depth);

        if(map == null)
        {
            map = new WeightedList<>();
            for(BiomeGroup biomeGroup : this.getGroups())
            {
                if(biomeGroup.getGenerationDepth() == depth)
                {
                    map.add(biomeGroup, biomeGroup.getGroupRarity());
                }
            }
            if(map.totalWeight() < map.size() * 100)
            {
                // Pad up to size * 100 total, add() takes a weight, not a cumulative key
                map.add(null, map.size() * 100 - map.totalWeight());
            }
            this.cachedGroupDepthMaps.put(depth, map);
        }

        return map;
    }

    public boolean isGroupDepthMapEmpty(int depth)
    {
        for (BiomeGroup group : getGroups())
        {
            if (group.getGenerationDepth() == depth)
            {
                return false;
            }
        }
        return true;
    }

    public boolean isBiomeDepthMapEmpty(int depth)
    {
        for (BiomeGroup group : getGroups())
        {
            if (!group.isBiomeDepthMapEmpty(depth))
                return false;
        }
        return true;
    }

    public void processBiomeData(LocalWorld world)
    {
        for (BiomeGroup entry : nameToGroup.values())
        {
            entry.processBiomeData(world);
        }
    }

    /**
     * Filters all biome names in the groups. Invalid biomes names will be
     * removed.
     * @param customBiomeNames Set of all custom biomes in the world.
     */
    public void filterBiomes(ArrayList<String> customBiomeNames, boolean logWarnings)
    {
        for (Iterator<BiomeGroup> it = nameToGroup.values().iterator(); it.hasNext();)
        {
            BiomeGroup group = it.next();
            group.filterBiomes(customBiomeNames, logWarnings);
            if (group.hasNoBiomes())
            {
                it.remove();
            }
        }
    }
}
