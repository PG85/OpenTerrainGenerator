package com.pg85.otg.shared.gamerules;

import com.mojang.serialization.Dynamic;
import com.pg85.otg.config.settings.preset.GameRuleSettings;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import com.pg85.otg.util.OTGLog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates MC GameRules from an OTG preset config.
 */
public final class GameRuleApplier {

    private GameRuleApplier() {}

    /**
     * Creates GameRules with 3-layer override hierarchy:
     * 1. DimensionPresetConfig.ini GameRules (base)
     * 2. WorldPreset YAML world-level GameRules (override)
     * 3. WorldPreset YAML per-dimension GameRules (override)
     *
     * Each layer only overrides non-null fields.
     */
    /**
     * Applies the overworld preset's GameRules when it opts in with
     * OverrideGameRules: true. The preset config is the source of truth,
     * so this runs on every server start.
     */
    public static void applyToOverworldIfConfigured(MinecraftServer server) {
        if (!(server.overworld().getChunkSource().getGenerator() instanceof SharedOTGChunkGenerator otgGen)) {
            return;
        }
        Preset preset = otgGen.getPreset();
        if (preset == null || preset.getPresetConfig() == null) {
            return;
        }
        GameRuleSettings gameRuleSettings = preset.getPresetConfig().getGameRuleSettings();
        if (gameRuleSettings == null || !gameRuleSettings.isOverrideGameRules()) {
            return;
        }
        GameRuleManager.register(net.minecraft.world.level.Level.OVERWORLD, createGameRules(gameRuleSettings, server));
        OTGLog.info("Applied GameRules for the OTG overworld (preset: %s)", preset.getFolderName());
    }

    public static GameRules createGameRules(
            GameRuleSettings presetRules,
            MinecraftServer server
    ) {
        GameRules rules = new GameRules();

        // Layer 1: base from DimensionPresetConfig.ini (only if preset opts in)
        if (presetRules.isOverrideGameRules()) {
            applyFromPreset(rules, presetRules, server);
        }

        // Layer 2: world-level overrides from WorldPreset YAML

        // Layer 3: per-dimension overrides from WorldPreset YAML

        return rules;
    }

    private static void applyFromPreset(GameRules rules, GameRuleSettings s, MinecraftServer server) {
        // Boolean rules
        rules.getRule(GameRules.RULE_DOFIRETICK).set(s.isDoFireTick(), server);
        rules.getRule(GameRules.RULE_MOBGRIEFING).set(s.isMobGriefing(), server);
        rules.getRule(GameRules.RULE_KEEPINVENTORY).set(s.isKeepInventory(), server);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(s.isDoMobSpawning(), server);
        rules.getRule(GameRules.RULE_DOMOBLOOT).set(s.isDoMobLoot(), server);
        rules.getRule(GameRules.RULE_DOBLOCKDROPS).set(s.isDoTileDrops(), server);
        rules.getRule(GameRules.RULE_DOENTITYDROPS).set(s.isDoEntityDrops(), server);
        rules.getRule(GameRules.RULE_COMMANDBLOCKOUTPUT).set(s.isCommandBlockOutput(), server);
        rules.getRule(GameRules.RULE_NATURAL_REGENERATION).set(s.isNaturalRegeneration(), server);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(s.isDoDaylightCycle(), server);
        rules.getRule(GameRules.RULE_LOGADMINCOMMANDS).set(s.isLogAdminCommands(), server);
        rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(s.isShowDeathMessages(), server);
        rules.getRule(GameRules.RULE_SENDCOMMANDFEEDBACK).set(s.isSendCommandFeedback(), server);
        rules.getRule(GameRules.RULE_SPECTATORSGENERATECHUNKS).set(s.isSpectatorsGenerateChunks(), server);
        rules.getRule(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK).set(s.isDisableElytraMovementCheck(), server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(s.isDoWeatherCycle(), server);
        rules.getRule(GameRules.RULE_LIMITED_CRAFTING).set(s.isDoLimitedCrafting(), server);
        rules.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(s.isAnnounceAdvancements(), server);
        rules.getRule(GameRules.RULE_DISABLE_RAIDS).set(s.isDisableRaids(), server);
        rules.getRule(GameRules.RULE_DOINSOMNIA).set(s.isDoInsomnia(), server);
        rules.getRule(GameRules.RULE_DROWNING_DAMAGE).set(s.isDrowningDamage(), server);
        rules.getRule(GameRules.RULE_FALL_DAMAGE).set(s.isFallDamage(), server);
        rules.getRule(GameRules.RULE_FIRE_DAMAGE).set(s.isFireDamage(), server);
        rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(s.isDoPatrolSpawning(), server);
        rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(s.isDoTraderSpawning(), server);
        rules.getRule(GameRules.RULE_FORGIVE_DEAD_PLAYERS).set(s.isForgiveDeadPlayers(), server);
        rules.getRule(GameRules.RULE_UNIVERSAL_ANGER).set(s.isUniversalAnger(), server);
        // New 1.21.1 boolean rules
        rules.getRule(GameRules.RULE_REDUCEDDEBUGINFO).set(s.isReducedDebugInfo(), server);
        rules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(s.isDoImmediateRespawn(), server);
        rules.getRule(GameRules.RULE_FREEZE_DAMAGE).set(s.isFreezeDamage(), server);
        rules.getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(s.isDoWardenSpawning(), server);
        rules.getRule(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY).set(s.isBlockExplosionDropDecay(), server);
        rules.getRule(GameRules.RULE_MOB_EXPLOSION_DROP_DECAY).set(s.isMobExplosionDropDecay(), server);
        rules.getRule(GameRules.RULE_TNT_EXPLOSION_DROP_DECAY).set(s.isTntExplosionDropDecay(), server);
        rules.getRule(GameRules.RULE_WATER_SOURCE_CONVERSION).set(s.isWaterSourceConversion(), server);
        rules.getRule(GameRules.RULE_LAVA_SOURCE_CONVERSION).set(s.isLavaSourceConversion(), server);
        rules.getRule(GameRules.RULE_GLOBAL_SOUND_EVENTS).set(s.isGlobalSoundEvents(), server);
        rules.getRule(GameRules.RULE_DO_VINES_SPREAD).set(s.isDoVinesSpread(), server);

        // Integer rules
        rules.getRule(GameRules.RULE_RANDOMTICKING).set(s.getRandomTickSpeed(), server);
        rules.getRule(GameRules.RULE_SPAWN_RADIUS).set(s.getSpawnRadius(), server);
        rules.getRule(GameRules.RULE_MAX_ENTITY_CRAMMING).set(s.getMaxEntityCramming(), server);
        rules.getRule(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH).set(s.getMaxCommandChainLength(), server);
        // New 1.21.1 integer rules
        rules.getRule(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT).set(s.getCommandModificationBlockLimit(), server);
        rules.getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE).set(s.getPlayersSleepingPercentage(), server);
        rules.getRule(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT).set(s.getSnowAccumulationHeight(), server);
    }

    // Only applies fields explicitly set in YAML (non-null). Unset fields keep preset values.
    public static Map<String, Object> toMap(GameRules rules) {
        Map<String, Object> map = new LinkedHashMap<>();
        var tag = rules.createTag();
        for (String key : tag.getAllKeys()) {
            String value = tag.getString(key);
            try {
                map.put(key, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                map.put(key, Boolean.parseBoolean(value));
            }
        }
        return map;
    }

    /**
     * Deserializes a GameRules instance from a persisted map.
     * Returns vanilla defaults if data is corrupt.
     */
    public static GameRules fromMap(Map<String, Object> map) {
        try {
            var tag = new CompoundTag();
            for (var entry : map.entrySet()) {
                tag.putString(entry.getKey(), String.valueOf(entry.getValue()));
            }
            return new GameRules(new Dynamic<>(NbtOps.INSTANCE, tag));
        } catch (Exception e) {
            OTGLog.error("Failed to deserialize GameRules from storage, using vanilla defaults: {}", e.getMessage());
            return new GameRules();
        }
    }
}
