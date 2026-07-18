package com.pg85.otg.shared.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.config.settings.biome.BiomeTerrainSettings;
import com.pg85.otg.config.settings.biome.MobSettings;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import com.pg85.otg.interfaces.ICachedBiomeProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

final class BiomeCommand {

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("biome")
            .executes(BiomeCommand::executeSummary)
            .then(Commands.literal("info").executes(BiomeCommand::executeInfo))
            .then(Commands.literal("spawns").executes(BiomeCommand::executeSpawns));
    }

    /** /otg biome — show current MC + OTG biome name and preset. */
    private static int executeSummary(CommandContext<CommandSourceStack> ctx) {
        if (!CommandHelper.requireOTGWorld(ctx)) return 0;
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        SharedOTGChunkGenerator gen = CommandHelper.getGenerator(level);

        BlockPos pos = BlockPos.containing(src.getPosition());

        String mcBiomeName = level.getBiome(pos).unwrapKey()
            .map(k -> k.location().toString())
            .orElse("unknown");

        ICachedBiomeProvider provider = gen.getInternalGenerator().getCachedBiomeProvider();
        BiomeSettings config = provider.getBiomeConfig(pos.getX(), pos.getZ());
        String otgName = config != null ? config.getConfigName() : "none";

        src.sendSuccess(() -> Component.literal(
            "Biome: " + mcBiomeName +
            "  |  OTG: " + otgName +
            "  |  Preset: " + gen.getPreset().getFolderName()
        ), false);
        return 1;
    }

    /** /otg biome info — show terrain, placement, and visual settings. */
    private static int executeInfo(CommandContext<CommandSourceStack> ctx) {
        if (!CommandHelper.requireOTGWorld(ctx)) return 0;
        CommandSourceStack src = ctx.getSource();
        SharedOTGChunkGenerator gen = CommandHelper.getGenerator(src.getLevel());

        if (gen == null) {
            // Not dealing with SharedOTGChunkGenerator - abort? Do we need it? - yes, probably.
            // Maybe we can support other chunk generators in the future, but not now
            src.sendFailure(Component.literal("Failed to find OTG Chunk Generator - are you in an OTG dimension?"));
            return 0;
        }

        BlockPos pos = BlockPos.containing(src.getPosition());
        BiomeSettings config = gen.getInternalGenerator().getCachedBiomeProvider().getBiomeConfig(pos.getX(), pos.getZ());

        if (config == null) {
            src.sendFailure(Component.literal("No OTG biome at this position."));
            return 0;
        }

        src.sendSuccess(() -> Component.literal("=== " + config.getConfigName() + " ==="), false);

        BiomeTerrainSettings terrain = config.getTerrainSettings();
        if (terrain != null) {
            src.sendSuccess(() -> Component.literal(String.format(
                "Height: %.3f  BiomeVolatility: %.3f  SmoothRadius: %d  CHCSmoothRadius: %d",
                terrain.getBiomeHeight(), terrain.getBiomeVolatility(),
                terrain.getSmoothRadius(), terrain.getCHCSmoothRadius()
            )), false);
            src.sendSuccess(() -> Component.literal(String.format(
                "Volatility1: %.3f (weight %.3f)  Volatility2: %.3f (weight %.3f)  DisableBiomeHeight: %b",
                terrain.getVolatility1(), terrain.getVolatilityWeight1(),
                terrain.getVolatility2(), terrain.getVolatilityWeight2(),
                terrain.isDisableBiomeHeight()
            )), false);
        }

        if (config.getGenerationSettings() != null) {
            src.sendSuccess(() -> Component.literal(String.format(
                "Size: %d  Rarity: %d",
                config.getGenerationSettings().getBiomeSize(),
                config.getGenerationSettings().getBiomeRarity()
            )), false);
        }

        if (config.getVisualSettings() != null) {
            src.sendSuccess(() -> Component.literal(String.format(
                "Temperature: %.2f  Wetness: %.2f",
                config.getVisualSettings().getBiomeTemperature(),
                config.getVisualSettings().getBiomeWetness()
            )), false);
        }

        return 1;
    }

    /** /otg biome spawns — show mob spawn lists. */
    private static int executeSpawns(CommandContext<CommandSourceStack> ctx) {
        if (!CommandHelper.requireOTGWorld(ctx)) return 0;
        CommandSourceStack src = ctx.getSource();
        SharedOTGChunkGenerator gen = CommandHelper.getGenerator(src.getLevel());

        BlockPos pos = BlockPos.containing(src.getPosition());
        BiomeSettings config = gen.getInternalGenerator().getCachedBiomeProvider().getBiomeConfig(pos.getX(), pos.getZ());

        if (config == null) {
            src.sendFailure(Component.literal("No OTG biome at this position."));
            return 0;
        }

        MobSettings mobs = config.getMobSettings();
        if (mobs == null) {
            src.sendSuccess(() -> Component.literal("No mob data for " + config.getConfigName()), false);
            return 1;
        }

        src.sendSuccess(() -> Component.literal("=== Spawns: " + config.getConfigName() + " ==="), false);
        if (!mobs.getInheritMobsBiomeName().isEmpty()) {
            src.sendSuccess(() -> Component.literal("Inherits from: " + mobs.getInheritMobsBiomeName()), false);
        }
        sendCategory(src, "Monsters",        mobs.getMonsters());
        sendCategory(src, "Creatures",       mobs.getCreatures());
        sendCategory(src, "WaterCreatures",  mobs.getWaterCreatures());
        sendCategory(src, "Ambient",         mobs.getAmbientCreatures());
        sendCategory(src, "WaterAmbient",    mobs.getWaterAmbientCreatures());
        sendCategory(src, "Misc",            mobs.getMiscCreatures());
        return 1;
    }

    private static void sendCategory(CommandSourceStack src, String label, List<?> entries) {
        if (entries != null && !entries.isEmpty()) {
            src.sendSuccess(() -> Component.literal(label + ": " + entries), false);
        }
    }
}
