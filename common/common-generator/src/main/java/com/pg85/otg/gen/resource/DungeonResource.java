package com.pg85.otg.gen.resource;

import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.interfaces.IWorldGenRegion;
import com.pg85.otg.util.Pair;
import com.pg85.otg.util.helpers.RandomHelper;

import java.util.List;
import java.util.Random;

public class DungeonResource extends FrequencyResourceBase
{
    private final int maxAltitude;
    private final int minAltitude;

    public DungeonResource(BiomeSettings biomeConfig, List<String> args) throws InvalidConfigException
    {
        super(biomeConfig, args);
        assureSize(3, args);

        // Legacy presets use Dungeon(Frequency,Rarity,MinAltitude,MaxAltitude);
        // the current format drops Frequency (always 1). Accept both.
        int offset = args.size() >= 4 ? 1 : 0;
        this.frequency = 1;
        this.rarity = readRarity(args.get(offset));
        Pair<Integer, Integer> elevations = readElevations(args.get(offset + 1), args.get(offset + 2));

        this.minAltitude = elevations.getFirst();
        this.maxAltitude = elevations.getSecond();
    }

    @Override
    public String toString()
    {
        return "Dungeon(" + this.rarity + "," + this.minAltitude + "," + this.maxAltitude + ")";
    }

    @Override
    public void spawn(IWorldGenRegion world, Random random, int x, int z)
    {
        int y = RandomHelper.numberInRange(random, this.minAltitude, this.maxAltitude);
        world.placeDungeon(random, x, y, z);
    }
}
