package com.pg85.otg.fabric;

import com.pg85.otg.OTG;
import com.pg85.otg.shared.commands.OTGCommand;
import com.pg85.otg.fabric.events.WorldSaveCallback;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import com.pg85.otg.shared.materials.SharedMaterialReader;
import com.pg85.otg.shared.util.SharedLogger;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.world.level.chunk.ChunkGenerator;

@SuppressWarnings("unused")
public class OTGPlugin implements ModInitializer {
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		OTGLog.setLogger(new SharedLogger());
		OTGLog.getLogger().log(LogLevel.INFO, LogCategory.MAIN, "OTG Engine starting");
		OTGMaterialReader.set(new SharedMaterialReader());
		OTG.startEngine(new FabricEngine());

		registerWorldSave();
		registerCommands();

		OTG.log("OTG Engine started, presets loaded");
	}

	void registerCommands() {
		CommandRegistrationCallback.EVENT.register(OTGCommand::register);
	}

	void registerWorldSave() {
		WorldSaveCallback.EVENT.register((serverLevel) -> {
			ChunkGenerator chunkGenerator = serverLevel.getChunkSource().getGenerator();
			if (chunkGenerator instanceof SharedOTGChunkGenerator fabricChunkGenerator) {
				OTGLog.info(LogCategory.STRUCTURE_PLOTTING, "Saving structure cache for world " + fabricChunkGenerator.getPreset().getFolderName());
				fabricChunkGenerator.saveStructureCache();
			}
		});
	}
}
