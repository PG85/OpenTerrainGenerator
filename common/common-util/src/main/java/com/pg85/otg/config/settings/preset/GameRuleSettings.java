package com.pg85.otg.config.settings.preset;

import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.settingtype.Setting;
import com.pg85.otg.config.settingtype.Settings;
import com.pg85.otg.config.settings.ConfigSection;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GameRuleSettings extends ConfigSection {
    private final boolean overrideGameRules;
    private final boolean doFireTick;
    private final boolean mobGriefing;
    private final boolean keepInventory;
    private final boolean doMobSpawning;
    private final boolean doMobLoot;
    private final boolean doTileDrops;
    private final boolean doEntityDrops;
    private final boolean commandBlockOutput;
    private final boolean naturalRegeneration;
    private final boolean doDaylightCycle;
    private final boolean logAdminCommands;
    private final boolean showDeathMessages;
    private final int randomTickSpeed;
    private final boolean sendCommandFeedback;
    private final boolean spectatorsGenerateChunks;
    private final int spawnRadius;
    private final boolean disableElytraMovementCheck;
    private final int maxEntityCramming;
    private final boolean doWeatherCycle;
    private final boolean doLimitedCrafting;
    private final int maxCommandChainLength;
    private final boolean announceAdvancements;
    private final boolean disableRaids;
    private final boolean doInsomnia;
    private final boolean drowningDamage;
    private final boolean fallDamage;
    private final boolean fireDamage;
    private final boolean doPatrolSpawning;
    private final boolean doTraderSpawning;
    private final boolean forgiveDeadPlayers;
    private final boolean universalAnger;
    // New 1.21.1 rules
    private final boolean reducedDebugInfo;
    private final boolean doImmediateRespawn;
    private final boolean freezeDamage;
    private final boolean doWardenSpawning;
    private final boolean blockExplosionDropDecay;
    private final boolean mobExplosionDropDecay;
    private final boolean tntExplosionDropDecay;
    private final boolean waterSourceConversion;
    private final boolean lavaSourceConversion;
    private final boolean globalSoundEvents;
    private final boolean doVinesSpread;
    private final int commandModificationBlockLimit;
    private final int playersSleepingPercentage;
    private final int snowAccumulationHeight;

    public static final Setting<Boolean> OVERRIDE_GAME_RULES = Settings.booleanSetting(
            "OverrideGameRules", false,
            t -> ((GameRuleSettings) t).isOverrideGameRules()
    );
    public static final Setting<Boolean> DO_FIRE_TICK = Settings.booleanSetting(
            "DoFireTick", true,
            t -> ((GameRuleSettings) t).isDoFireTick()
    );
    public static final Setting<Boolean> MOB_GRIEFING = Settings.booleanSetting(
            "MobGriefing", true,
            t -> ((GameRuleSettings) t).isMobGriefing()
    );
    public static final Setting<Boolean> KEEP_INVENTORY = Settings.booleanSetting(
            "KeepInventory", false,
            t -> ((GameRuleSettings) t).isKeepInventory()
    );
    public static final Setting<Boolean> DO_MOB_SPAWNING = Settings.booleanSetting(
            "DoMobSpawning", true,
            t -> ((GameRuleSettings) t).isDoMobSpawning()
    );
    public static final Setting<Boolean> DO_MOB_LOOT = Settings.booleanSetting(
            "DoMobLoot", true,
            t -> ((GameRuleSettings) t).isDoMobLoot()
    );
    public static final Setting<Boolean> DO_TILE_DROPS = Settings.booleanSetting(
            "DoTileDrops", true,
            t -> ((GameRuleSettings) t).isDoTileDrops()
    );
    public static final Setting<Boolean> DO_ENTITY_DROPS = Settings.booleanSetting(
            "DoEntityDrops", true,
            t -> ((GameRuleSettings) t).isDoEntityDrops()
    );
    public static final Setting<Boolean> COMMAND_BLOCK_OUTPUT = Settings.booleanSetting(
            "CommandBlockOutput", true,
            t -> ((GameRuleSettings) t).isCommandBlockOutput()
    );
    public static final Setting<Boolean> NATURAL_REGENERATION = Settings.booleanSetting(
            "NaturalRegeneration", true,
            t -> ((GameRuleSettings) t).isNaturalRegeneration()
    );
    public static final Setting<Boolean> DO_DAY_LIGHT_CYCLE = Settings.booleanSetting(
            "DoDaylightCycle", true,
            t -> ((GameRuleSettings) t).isDoDaylightCycle()
    );
    public static final Setting<Boolean> LOG_ADMIN_COMMANDS = Settings.booleanSetting(
            "LogAdminCommands", true,
            t -> ((GameRuleSettings) t).isLogAdminCommands()
    );
    public static final Setting<Boolean> SHOW_DEATH_MESSAGES = Settings.booleanSetting(
            "ShowDeathMessages", true,
            t -> ((GameRuleSettings) t).isShowDeathMessages()
    );
    public static final Setting<Boolean> SEND_COMMAND_FEEDBACK = Settings.booleanSetting(
            "SendCommandFeedback", true,
            t -> ((GameRuleSettings) t).isSendCommandFeedback()
    );
    public static final Setting<Boolean> SPECTATORS_GENERATE_CHUNKS = Settings.booleanSetting(
            "SpectatorsGenerateChunks", true,
            t -> ((GameRuleSettings) t).isSpectatorsGenerateChunks()
    );
    public static final Setting<Boolean> DISABLE_ELYTRA_MOVEMENT_CHECK = Settings.booleanSetting(
            "DisableElytraMovementCheck", false,
            t -> ((GameRuleSettings) t).isDisableElytraMovementCheck()
    );
    public static final Setting<Boolean> DO_WEATHER_CYCLE = Settings.booleanSetting(
            "DoWeatherCycle", true,
            t -> ((GameRuleSettings) t).isDoWeatherCycle()
    );
    public static final Setting<Boolean> DO_LIMITED_CRAFTING = Settings.booleanSetting(
            "DoLimitedCrafting", false,
            t -> ((GameRuleSettings) t).isDoLimitedCrafting()
    );
    public static final Setting<Boolean> ANNOUNCE_ADVANCEMENTS = Settings.booleanSetting(
            "AnnounceAdvancements", true,
            t -> ((GameRuleSettings) t).isAnnounceAdvancements()
    );
    public static final Setting<Boolean> DISABLE_RAIDS = Settings.booleanSetting(
            "DisableRaids", false,
            t -> ((GameRuleSettings) t).isDisableRaids()
    );
    public static final Setting<Boolean> DO_INSOMNIA = Settings.booleanSetting(
            "DoInsomnia", true,
            t -> ((GameRuleSettings) t).isDoInsomnia()
    );
    public static final Setting<Boolean> DROWNING_DAMAGE = Settings.booleanSetting(
            "DrowningDamage", true,
            t -> ((GameRuleSettings) t).isDrowningDamage()
    );
    public static final Setting<Boolean> FALL_DAMAGE = Settings.booleanSetting(
            "FallDamage", true,
            t -> ((GameRuleSettings) t).isFallDamage()
    );
    public static final Setting<Boolean> FIRE_DAMAGE = Settings.booleanSetting(
            "FireDamage", true,
            t -> ((GameRuleSettings) t).isFireDamage()
    );
    public static final Setting<Boolean> DO_PATROL_SPAWNING = Settings.booleanSetting(
            "DoPatrolSpawning", true,
            t -> ((GameRuleSettings) t).isDoPatrolSpawning()
    );
    public static final Setting<Boolean> DO_TRADER_SPAWNING = Settings.booleanSetting(
            "DoTraderSpawning", true,
            t -> ((GameRuleSettings) t).isDoTraderSpawning()
    );
    public static final Setting<Boolean> FORGIVE_DEAD_PLAYERS = Settings.booleanSetting(
            "ForgiveDeadPlayers", true,
            t -> ((GameRuleSettings) t).isForgiveDeadPlayers()
    );
    public static final Setting<Boolean> UNIVERSAL_ANGER = Settings.booleanSetting(
            "UniversalAnger", false,
            t -> ((GameRuleSettings) t).isUniversalAnger()
    );
    public static final Setting<Integer> RANDOM_TICK_SPEED = Settings.intSetting(
            "RandomTickSpeed", 3, 0, Integer.MAX_VALUE,
            t -> ((GameRuleSettings) t).getRandomTickSpeed()
    );
    public static final Setting<Integer> SPAWN_RADIUS = Settings.intSetting(
            "SpawnRadius", 10, 0, Integer.MAX_VALUE,
            t -> ((GameRuleSettings) t).getSpawnRadius()
    );
    public static final Setting<Integer> MAX_ENTITY_CRAMMING = Settings.intSetting(
            "MaxEntityCramming", 24, 0, Integer.MAX_VALUE,
            t -> ((GameRuleSettings) t).getMaxEntityCramming()
    );
    public static final Setting<Integer> MAX_COMMAND_CHAIN_LENGTH = Settings.intSetting(
            "MaxCommandChainLength", 65536, 0, Integer.MAX_VALUE,
            t -> ((GameRuleSettings) t).getMaxCommandChainLength()
    );
    // New 1.21.1 rules
    public static final Setting<Boolean> REDUCED_DEBUG_INFO = Settings.booleanSetting(
            "ReducedDebugInfo", false,
            t -> ((GameRuleSettings) t).isReducedDebugInfo()
    );
    public static final Setting<Boolean> DO_IMMEDIATE_RESPAWN = Settings.booleanSetting(
            "DoImmediateRespawn", false,
            t -> ((GameRuleSettings) t).isDoImmediateRespawn()
    );
    public static final Setting<Boolean> FREEZE_DAMAGE = Settings.booleanSetting(
            "FreezeDamage", true,
            t -> ((GameRuleSettings) t).isFreezeDamage()
    );
    public static final Setting<Boolean> DO_WARDEN_SPAWNING = Settings.booleanSetting(
            "DoWardenSpawning", true,
            t -> ((GameRuleSettings) t).isDoWardenSpawning()
    );
    public static final Setting<Boolean> BLOCK_EXPLOSION_DROP_DECAY = Settings.booleanSetting(
            "BlockExplosionDropDecay", true,
            t -> ((GameRuleSettings) t).isBlockExplosionDropDecay()
    );
    public static final Setting<Boolean> MOB_EXPLOSION_DROP_DECAY = Settings.booleanSetting(
            "MobExplosionDropDecay", true,
            t -> ((GameRuleSettings) t).isMobExplosionDropDecay()
    );
    public static final Setting<Boolean> TNT_EXPLOSION_DROP_DECAY = Settings.booleanSetting(
            "TntExplosionDropDecay", false,
            t -> ((GameRuleSettings) t).isTntExplosionDropDecay()
    );
    public static final Setting<Boolean> WATER_SOURCE_CONVERSION = Settings.booleanSetting(
            "WaterSourceConversion", true,
            t -> ((GameRuleSettings) t).isWaterSourceConversion()
    );
    public static final Setting<Boolean> LAVA_SOURCE_CONVERSION = Settings.booleanSetting(
            "LavaSourceConversion", false,
            t -> ((GameRuleSettings) t).isLavaSourceConversion()
    );
    public static final Setting<Boolean> GLOBAL_SOUND_EVENTS = Settings.booleanSetting(
            "GlobalSoundEvents", true,
            t -> ((GameRuleSettings) t).isGlobalSoundEvents()
    );
    public static final Setting<Boolean> DO_VINES_SPREAD = Settings.booleanSetting(
            "DoVinesSpread", true,
            t -> ((GameRuleSettings) t).isDoVinesSpread()
    );
    public static final Setting<Integer> COMMAND_MODIFICATION_BLOCK_LIMIT = Settings.intSetting(
            "CommandModificationBlockLimit", 32768, 0, Integer.MAX_VALUE,
            t -> ((GameRuleSettings) t).getCommandModificationBlockLimit()
    );
    public static final Setting<Integer> PLAYERS_SLEEPING_PERCENTAGE = Settings.intSetting(
            "PlayersSleepingPercentage", 100, 0, 100,
            t -> ((GameRuleSettings) t).getPlayersSleepingPercentage()
    );
    public static final Setting<Integer> SNOW_ACCUMULATION_HEIGHT = Settings.intSetting(
            "SnowAccumulationHeight", 1, 0, Integer.MAX_VALUE,
            t -> ((GameRuleSettings) t).getSnowAccumulationHeight()
    );

    public static GameRuleSettings getGameRuleSettings(SettingsMap reader) {
        var gameRuleSettingsBuilder = builder();

        gameRuleSettingsBuilder.overrideGameRules(reader.getSetting(OVERRIDE_GAME_RULES));
        gameRuleSettingsBuilder.doFireTick(reader.getSetting(DO_FIRE_TICK));
        gameRuleSettingsBuilder.mobGriefing(reader.getSetting(MOB_GRIEFING));
        gameRuleSettingsBuilder.keepInventory(reader.getSetting(KEEP_INVENTORY));
        gameRuleSettingsBuilder.doMobSpawning(reader.getSetting(DO_MOB_SPAWNING));
        gameRuleSettingsBuilder.doMobLoot(reader.getSetting(DO_MOB_LOOT));
        gameRuleSettingsBuilder.doTileDrops(reader.getSetting(DO_TILE_DROPS));
        gameRuleSettingsBuilder.doEntityDrops(reader.getSetting(DO_ENTITY_DROPS));
        gameRuleSettingsBuilder.commandBlockOutput(reader.getSetting(COMMAND_BLOCK_OUTPUT));
        gameRuleSettingsBuilder.naturalRegeneration(reader.getSetting(NATURAL_REGENERATION));
        gameRuleSettingsBuilder.doDaylightCycle(reader.getSetting(DO_DAY_LIGHT_CYCLE));
        gameRuleSettingsBuilder.logAdminCommands(reader.getSetting(LOG_ADMIN_COMMANDS));
        gameRuleSettingsBuilder.showDeathMessages(reader.getSetting(SHOW_DEATH_MESSAGES));
        gameRuleSettingsBuilder.randomTickSpeed(reader.getSetting(RANDOM_TICK_SPEED));
        gameRuleSettingsBuilder.sendCommandFeedback(reader.getSetting(SEND_COMMAND_FEEDBACK));
        gameRuleSettingsBuilder.spectatorsGenerateChunks(reader.getSetting(SPECTATORS_GENERATE_CHUNKS));
        gameRuleSettingsBuilder.spawnRadius(reader.getSetting(SPAWN_RADIUS));
        gameRuleSettingsBuilder.disableElytraMovementCheck(reader.getSetting(DISABLE_ELYTRA_MOVEMENT_CHECK));
        gameRuleSettingsBuilder.maxEntityCramming(reader.getSetting(MAX_ENTITY_CRAMMING));
        gameRuleSettingsBuilder.doWeatherCycle(reader.getSetting(DO_WEATHER_CYCLE));
        gameRuleSettingsBuilder.doLimitedCrafting(reader.getSetting(DO_LIMITED_CRAFTING));
        gameRuleSettingsBuilder.maxCommandChainLength(reader.getSetting(MAX_COMMAND_CHAIN_LENGTH));
        gameRuleSettingsBuilder.announceAdvancements(reader.getSetting(ANNOUNCE_ADVANCEMENTS));
        gameRuleSettingsBuilder.disableRaids(reader.getSetting(DISABLE_RAIDS));
        gameRuleSettingsBuilder.doInsomnia(reader.getSetting(DO_INSOMNIA));
        gameRuleSettingsBuilder.drowningDamage(reader.getSetting(DROWNING_DAMAGE));
        gameRuleSettingsBuilder.fallDamage(reader.getSetting(FALL_DAMAGE));
        gameRuleSettingsBuilder.fireDamage(reader.getSetting(FIRE_DAMAGE));
        gameRuleSettingsBuilder.doPatrolSpawning(reader.getSetting(DO_PATROL_SPAWNING));
        gameRuleSettingsBuilder.doTraderSpawning(reader.getSetting(DO_TRADER_SPAWNING));
        gameRuleSettingsBuilder.forgiveDeadPlayers(reader.getSetting(FORGIVE_DEAD_PLAYERS));
        gameRuleSettingsBuilder.universalAnger(reader.getSetting(UNIVERSAL_ANGER));
        gameRuleSettingsBuilder.reducedDebugInfo(reader.getSetting(REDUCED_DEBUG_INFO));
        gameRuleSettingsBuilder.doImmediateRespawn(reader.getSetting(DO_IMMEDIATE_RESPAWN));
        gameRuleSettingsBuilder.freezeDamage(reader.getSetting(FREEZE_DAMAGE));
        gameRuleSettingsBuilder.doWardenSpawning(reader.getSetting(DO_WARDEN_SPAWNING));
        gameRuleSettingsBuilder.blockExplosionDropDecay(reader.getSetting(BLOCK_EXPLOSION_DROP_DECAY));
        gameRuleSettingsBuilder.mobExplosionDropDecay(reader.getSetting(MOB_EXPLOSION_DROP_DECAY));
        gameRuleSettingsBuilder.tntExplosionDropDecay(reader.getSetting(TNT_EXPLOSION_DROP_DECAY));
        gameRuleSettingsBuilder.waterSourceConversion(reader.getSetting(WATER_SOURCE_CONVERSION));
        gameRuleSettingsBuilder.lavaSourceConversion(reader.getSetting(LAVA_SOURCE_CONVERSION));
        gameRuleSettingsBuilder.globalSoundEvents(reader.getSetting(GLOBAL_SOUND_EVENTS));
        gameRuleSettingsBuilder.doVinesSpread(reader.getSetting(DO_VINES_SPREAD));
        gameRuleSettingsBuilder.commandModificationBlockLimit(reader.getSetting(COMMAND_MODIFICATION_BLOCK_LIMIT));
        gameRuleSettingsBuilder.playersSleepingPercentage(reader.getSetting(PLAYERS_SLEEPING_PERCENTAGE));
        gameRuleSettingsBuilder.snowAccumulationHeight(reader.getSetting(SNOW_ACCUMULATION_HEIGHT));
        return gameRuleSettingsBuilder.build();
    }

    @Override
    public String getSectionName() {
        return "Game Rule Settings";
    }
}