package com.pg85.otg.shared.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.OTG;
import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import com.pg85.otg.interfaces.ICachedBiomeProvider;
import com.pg85.otg.util.Color;
import com.pg85.otg.util.helpers.MathHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class MapCommand {

    private static final int DEFAULT_SIZE = 512;
    private static final int MAX_SIZE = 4096;
    private static final int COLOR_BLACK = 0x000000;
    private static final int COLOR_GRAY  = 0x808080;

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("map")
            .then(Commands.literal("biomes")
                .executes(ctx -> executeMap(ctx, true, DEFAULT_SIZE, DEFAULT_SIZE))
                .then(Commands.argument("width", IntegerArgumentType.integer(64, MAX_SIZE))
                    .then(Commands.argument("height", IntegerArgumentType.integer(64, MAX_SIZE))
                        .executes(ctx -> executeMap(ctx, true,
                            IntegerArgumentType.getInteger(ctx, "width"),
                            IntegerArgumentType.getInteger(ctx, "height"))))))
            .then(Commands.literal("terrain")
                .executes(ctx -> executeMap(ctx, false, DEFAULT_SIZE, DEFAULT_SIZE))
                .then(Commands.argument("width", IntegerArgumentType.integer(64, MAX_SIZE))
                    .then(Commands.argument("height", IntegerArgumentType.integer(64, MAX_SIZE))
                        .executes(ctx -> executeMap(ctx, false,
                            IntegerArgumentType.getInteger(ctx, "width"),
                            IntegerArgumentType.getInteger(ctx, "height"))))));
    }

    private static int executeMap(CommandContext<CommandSourceStack> ctx, boolean biomeMode, int width, int height) {
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

        BlockPos center = BlockPos.containing(src.getPosition());
        String mode = biomeMode ? "biomes" : "terrain";
        src.sendSuccess(() -> Component.literal(
            "Generating " + width + "x" + height + " " + mode + " map centered at " +
            center.getX() + ", " + center.getZ() + "..."
        ), false);

        // TODO Java 21: Thread.ofVirtual().start(() -> {
        new Thread(() -> {
            try {
                Path outputDir = OTG.getEngine().getOTGRootFolder().resolve("output");
                Files.createDirectories(outputDir);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                String filename = gen.getPreset().getFolderName() + "-" + mode + "-" + timestamp + ".png";
                File outFile = outputDir.resolve(filename).toFile();

                BufferedImage image = biomeMode
                    ? renderBiomeMap(gen, center, width, height)
                    : renderTerrainMap(gen, level, center, width, height);

                ImageIO.write(image, "PNG", outFile);

                level.getServer().execute(() -> src.sendSuccess(() ->
                    Component.literal("Map saved to: " + outFile.getAbsolutePath()), false));
            } catch (IOException e) {
                level.getServer().execute(() ->
                    src.sendFailure(Component.literal("Failed to save map: " + e.getMessage())));
            }
        }).start(); // TODO Java 21: remove .start(), ofVirtual handles it

        return 1;
    }

    private static BufferedImage renderBiomeMap(SharedOTGChunkGenerator gen,
                                                BlockPos center, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ICachedBiomeProvider provider = gen.getInternalGenerator().getCachedBiomeProvider();
        int halfW = width / 2;
        int halfH = height / 2;

        // Each pixel maps to 4 blocks (noise biome scale)
        for (int px = 0; px < width; px++) {
            for (int pz = 0; pz < height; pz++) {
                int worldX = center.getX() + (px - halfW) * 4;
                int worldZ = center.getZ() + (pz - halfH) * 4;
                BiomeSettings config = provider.getNoiseBiomeConfig(worldX >> 2, worldZ >> 2, false);
                Color color = (config != null && config.getGenerationSettings() != null)
                    ? config.getGenerationSettings().getBiomeMapColor()
                    : null;
                image.setRGB(px, pz, color != null ? color.intValue() : COLOR_BLACK);
            }
        }
        return image;
    }

    private static BufferedImage renderTerrainMap(SharedOTGChunkGenerator gen, ServerLevel level,
                                                  BlockPos center, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ICachedBiomeProvider provider = gen.getInternalGenerator().getCachedBiomeProvider();
        int halfW = width / 2;
        int halfH = height / 2;
        int seaLevel = gen.getSeaLevel();
        int worldHeight = gen.getGenDepth();

        for (int px = 0; px < width; px++) {
            for (int pz = 0; pz < height; pz++) {
                int worldX = center.getX() + (px - halfW) * 4;
                int worldZ = center.getZ() + (pz - halfH) * 4;

                int surfaceY = gen.getBaseHeight(worldX, worldZ, Heightmap.Types.MOTION_BLOCKING,
                    level, level.getChunkSource().randomState());

                BiomeSettings config = provider.getNoiseBiomeConfig(worldX >> 2, worldZ >> 2, false);
                Color base = (config != null && config.getGenerationSettings() != null)
                    ? config.getGenerationSettings().getBiomeMapColor()
                    : null;

                // Shade by altitude: above sea = lighten, below = darken
                float shade = (surfaceY - seaLevel) / (float) worldHeight;
                int rgb = base != null ? base.intValue() : COLOR_GRAY;
                image.setRGB(px, pz, shadeColor(rgb, shade));
            }
        }
        return image;
    }

    /** Lighten or darken a packed RGB int by a shade factor (positive = lighter, negative = darker). */
    private static int shadeColor(int rgb, float shade) {
        float factor = 1.0f + shade * 0.8f;
        // TODO Java 21: Math.clamp((int)(...), 0, 255)
        int r = MathHelper.clamp((int)(((rgb >> 16) & 0xFF) * factor), 0, 255);
        int g = MathHelper.clamp((int)(((rgb >>  8) & 0xFF) * factor), 0, 255);
        int b = MathHelper.clamp((int)(( rgb        & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }
}
