package com.pg85.otg.shared.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.OTG;
import com.pg85.otg.customobject.BOPackExporter;
import com.pg85.otg.customobject.structures.CustomStructureCache;
import com.pg85.otg.shared.gen.SharedWorldGenRegion;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.gen.OTGWorldInfo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

final class PackCommand
{
	private static final AtomicBoolean isRunning = new AtomicBoolean(false);
	private static final AtomicBoolean isDone = new AtomicBoolean(false);
	private static final AtomicBoolean isCancelled = new AtomicBoolean(false);
	private static final AtomicInteger current = new AtomicInteger(0);
	private static final AtomicInteger total = new AtomicInteger(0);
	private static final AtomicReference<String> currentName = new AtomicReference<>("");
	private static final AtomicReference<Thread> packThread = new AtomicReference<>(null);

	static LiteralArgumentBuilder<CommandSourceStack> register()
	{
		return Commands.literal("pack")
			.then(Commands.literal("start").executes(PackCommand::executeStart))
			.then(Commands.literal("stop").executes(PackCommand::executeStop))
			.then(Commands.literal("status").executes(PackCommand::executeStatus));
	}

	private static int executeStart(CommandContext<CommandSourceStack> ctx)
	{
		CommandSourceStack src = ctx.getSource();

		if(!CommandHelper.requireOTGWorld(ctx))
		{
			return 0;
		}

		if(isRunning.get())
		{
			src.sendFailure(Component.literal("OTG pack is already running. Use '/otg pack status' to check progress or '/otg pack stop' to cancel."));
			return 0;
		}

		SharedOTGChunkGenerator gen = CommandHelper.getGenerator(src.getLevel());
		if(gen == null)
		{
			return 0;
		}

		Preset preset = gen.getPreset();

		isRunning.set(true);
		isDone.set(false);
		isCancelled.set(false);
		current.set(0);
		total.set(0);
		currentName.set("");

		src.sendSuccess(() -> Component.literal("Exporting .bopack files — use '/otg pack status' to check progress."), false);

		ServerLevel level = src.getLevel();
		Path worldSaveFolder = level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).getParent();

		ChunkAccess chunk = level.getChunk(0, 0);
		OTGWorldInfo otgWorldInfo = new OTGWorldInfo(level.getMinBuildHeight(), level.getMaxBuildHeight());
		SharedWorldGenRegion worldGenRegion = new SharedWorldGenRegion(
			preset.getFolderName(),
			OTG.getEngine().getPluginConfig(),
			preset.getPresetConfig(),
			otgWorldInfo,
			level,
			chunk,
			gen
		);
		CustomStructureCache structureCache = gen.getStructureCache(worldSaveFolder);

		Thread thread = new Thread(() ->
		{
			try
			{
				BOPackExporter.exportPreset(
					preset,
					structureCache,
					worldGenRegion,
					OTG.getEngine().getOTGRootFolder(),
					(c, t, name) ->
					{
						if(isCancelled.get())
						{
							throw new RuntimeException("OTG pack export cancelled.");
						}
						current.set(c);
						total.set(t);
						currentName.set(name);
					}
				);
			}
			catch(RuntimeException e)
			{
				if(!isCancelled.get())
				{
					throw e;
				}
				// cancelled — exit cleanly
				return;
			}
			isDone.set(true);
		}, "OTGPack");
		packThread.set(thread);
		thread.start();

		return 1;
	}

	private static int executeStop(CommandContext<CommandSourceStack> ctx)
	{
		CommandSourceStack src = ctx.getSource();

		if(!isRunning.get())
		{
			src.sendFailure(Component.literal("No OTG pack export is currently running."));
			return 0;
		}

		isCancelled.set(true);
		isRunning.set(false);
		Thread t = packThread.get();
		if(t != null)
		{
			t.interrupt();
		}
		src.sendSuccess(() -> Component.literal("OTG pack export cancelled."), false);
		return 1;
	}

	private static int executeStatus(CommandContext<CommandSourceStack> ctx)
	{
		CommandSourceStack src = ctx.getSource();

		if(!isRunning.get())
		{
			src.sendSuccess(() -> Component.literal("No OTG pack export is currently running."), false);
			return 1;
		}

		if(isDone.get())
		{
			isRunning.set(false);
			isDone.set(false);
			src.sendSuccess(() -> Component.literal("OTG pack export completed."), false);
			return 1;
		}

		int c = current.get();
		int t = total.get();
		String name = currentName.get();
		src.sendSuccess(
			() -> Component.literal("OTG pack is running: "
				+ (c == 0 ? "initializing structure starts (" + name + ")" : c + "/" + t + " — " + name)),
			false);
		return 1;
	}
}
