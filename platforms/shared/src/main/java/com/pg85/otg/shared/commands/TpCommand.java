package com.pg85.otg.shared.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import com.pg85.otg.presets.Preset;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

final class TpCommand {

    private static final int DEFAULT_RANGE = 10000;

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("tp")
            .then(Commands.argument("biome", StringArgumentType.string())
                .suggests(biomeSuggestions())
                .executes(ctx -> execute(ctx, DEFAULT_RANGE))
                .then(Commands.argument("range", IntegerArgumentType.integer(1, 100000))
                    .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "range")))));
    }

    private static SuggestionProvider<CommandSourceStack> biomeSuggestions() {
        return (ctx, builder) -> {
            SharedOTGChunkGenerator gen = CommandHelper.getGenerator(ctx.getSource().getLevel());
            if (gen != null) {
                return SharedSuggestionProvider.suggest(gen.getPreset().getAllBiomeNames(), builder);
            }
            return builder.buildFuture();
        };
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, int range) {
        if (!CommandHelper.requireOTGWorld(ctx)) return 0;
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        SharedOTGChunkGenerator gen = CommandHelper.getGenerator(level);
        if (gen == null) {
            // Not dealing with SharedOTGChunkGenerator - abort? Do we need it? - yes, probably.
            // Maybe we can support other chunk generators in the future, but not now
            src.sendFailure(Component.literal("Failed to find OTG Chunk Generator - are you in an OTG dimension?"));
            return 0;
        }
        Preset preset = gen.getPreset();

        String biomeName = StringArgumentType.getString(ctx, "biome");

        // Resolve OTG biome name → registry ResourceKey
        ResourceKey<Biome> biomeKey = resolveKey(preset, biomeName);
        if (biomeKey == null) {
            src.sendFailure(Component.literal("Unknown OTG biome: \"" + biomeName + "\". Use tab-complete to see available biomes."));
            return 0;
        }

        BlockPos origin = BlockPos.containing(src.getPosition());
        src.sendSuccess(() -> Component.literal("Searching for \"" + biomeName + "\" within " + range + " blocks..."), false);

        // Run biome search async so we don't freeze the server tick
        final ResourceKey<Biome> key = biomeKey;
        // TODO Java 21: Thread.ofVirtual().start(() -> {
        new Thread(() -> {
            Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(
                holder -> holder.is(key),
                origin, range, 32, 64
            );

            level.getServer().execute(() -> {
                if (result == null) {
                    src.sendFailure(Component.literal("Could not find \"" + biomeName + "\" within " + range + " blocks."));
                    return;
                }

                BlockPos dest = result.getFirst();
                int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, dest.getX(), dest.getZ());
                BlockPos safeDest = new BlockPos(dest.getX(), surfaceY, dest.getZ());

                ServerPlayer player = src.getPlayer();
                if (player != null) {
                    player.teleportTo(level, safeDest.getX() + 0.5, safeDest.getY(), safeDest.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
                    int dist = Mth.floor(Math.sqrt(origin.distSqr(safeDest)));
                    src.sendSuccess(() -> Component.literal(
                        "Teleported to \"" + biomeName + "\" at " +
                        safeDest.getX() + ", " + safeDest.getY() + ", " + safeDest.getZ() +
                        " (" + dist + " blocks away)"
                    ), false);
                } else {
                    src.sendSuccess(() -> Component.literal(
                        "Found \"" + biomeName + "\" at " +
                        safeDest.getX() + ", " + safeDest.getY() + ", " + safeDest.getZ()
                    ), false);
                }
            });
        }).start(); // TODO Java 21: remove .start(), ofVirtual handles it

        return 1;
    }

    @Nullable
    private static ResourceKey<Biome> resolveKey(Preset preset, String biomeName) {
        BiomeSettings settings = preset.getBiomeConfig(biomeName);
        if (settings == null) return null;
        if (!(settings instanceof BiomeConfig bc)) return null;
        if (bc.getRegistryKey() == null) return null;
        try {
            ResourceLocation rl = new ResourceLocation(bc.getRegistryKey().toResourceLocationString());
            return ResourceKey.create(Registries.BIOME, rl);
        } catch (Exception e) {
            return null;
        }
    }
}
