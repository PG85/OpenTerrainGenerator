package com.pg85.otg.shared.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.OTG;
import com.pg85.otg.config.settings.preset.PresetInfo;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import com.pg85.otg.presets.Preset;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

final class PresetCommand {

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("preset")
            .executes(PresetCommand::executeInfo)
            .then(Commands.literal("list").executes(PresetCommand::executeList));
    }

    /** /otg preset — show current world's preset info. */
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

        Preset preset = gen.getPreset();
        PresetInfo info = preset.getPresetConfig().getPresetInfo();

        src.sendSuccess(() -> Component.literal("=== Preset: " + info.getDisplayName() + " ==="), false);
        src.sendSuccess(() -> Component.literal("Registry name: " + info.getRegistryName()), false);

        String author = info.getAuthor();
        if (author != null && !author.isEmpty()) {
            src.sendSuccess(() -> Component.literal("Author: " + author), false);
        }

        String desc = info.getDescription();
        if (desc != null && !desc.isEmpty()) {
            src.sendSuccess(() -> Component.literal("Description: " + desc), false);
        }

        src.sendSuccess(() -> Component.literal(
            "Version: " + info.getMajorVersion() +
            "." + info.getMinorVersion()
        ), false);

        src.sendSuccess(() -> Component.literal(
            "Biomes: " + preset.getAllBiomeNames().size() +
            "  |  BiomeMode: " + preset.getPresetConfig().getGenerationSettings().getBiomeMode()
        ), false);

        return 1;
    }

    /** /otg preset list — list all loaded presets. */
    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<Preset> presets = OTG.getEngine().getPresetLoader().getAllPresets();

        if (presets.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No presets loaded."), false);
            return 1;
        }

        src.sendSuccess(() -> Component.literal("Loaded presets (" + presets.size() + "):"), false);
        for (Preset p : presets) {
            src.sendSuccess(() -> Component.literal(
                "  " + p.getFolderName() +
                " (" + p.getPresetConfig().getPresetInfo().getRegistryName() + ")" +
                " — " + p.getAllBiomeNames().size() + " biomes"
            ), false);
        }
        return 1;
    }
}
