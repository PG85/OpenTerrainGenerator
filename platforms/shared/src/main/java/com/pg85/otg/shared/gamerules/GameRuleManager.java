package com.pg85.otg.shared.gamerules;

import com.pg85.otg.util.OTGLog;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds per-dimension GameRules. The mixin on Level.getGameRules()
 * dispatches here for OTG dimensions.
 */
public final class GameRuleManager {
    private static final Map<ResourceKey<Level>, GameRules> dimensionRules = new ConcurrentHashMap<>();

    private GameRuleManager() {}

    public static @Nullable GameRules getGameRules(ResourceKey<Level> dimension) {
        return dimensionRules.get(dimension);
    }

    public static void register(ResourceKey<Level> dimension, GameRules rules) {
        dimensionRules.put(dimension, rules);
        OTGLog.info("Registered custom GameRules for dimension {}", dimension.location());
    }

    public static void unregister(ResourceKey<Level> dimension) {
        if (dimensionRules.remove(dimension) != null) {
            OTGLog.info("Unregistered custom GameRules for dimension {}", dimension.location());
        }
    }

    public static boolean hasCustomRules(ResourceKey<Level> dimension) {
        return dimensionRules.containsKey(dimension);
    }

    public static void clear() {
        int count = dimensionRules.size();
        dimensionRules.clear();
        if (count > 0) {
            OTGLog.info("Cleared {} dimension GameRules entries", count);
        }
    }
}
