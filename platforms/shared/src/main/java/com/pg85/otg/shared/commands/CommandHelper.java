package com.pg85.otg.shared.commands;

import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

final class CommandHelper {

    @Nullable
    static SharedOTGChunkGenerator getGenerator(ServerLevel level) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        return gen instanceof SharedOTGChunkGenerator otg ? otg : null;
    }

    /** Sends a failure message and returns false if the current world is not an OTG world. */
    static boolean requireOTGWorld(CommandContext<CommandSourceStack> ctx) {
        if (getGenerator(ctx.getSource().getLevel()) == null) {
            ctx.getSource().sendFailure(Component.literal("This command requires an OTG world."));
            return false;
        }
        return true;
    }

    private CommandHelper() {}
}
