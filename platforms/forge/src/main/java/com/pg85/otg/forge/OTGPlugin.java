package com.pg85.otg.forge;

import com.mojang.serialization.Codec;
import com.pg85.otg.OTG;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.shared.biome.SharedOTGBiomeProvider;
import com.pg85.otg.shared.commands.OTGCommand;
import com.pg85.otg.shared.gen.SharedOTGChunkGenerator;
import com.pg85.otg.shared.materials.SharedMaterialReader;
import com.pg85.otg.shared.util.SharedLogger;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

@SuppressWarnings("unused")
@Mod(Constants.MOD_ID_SHORT)
public class OTGPlugin {
    // Same registry ids as fabric ("otg:otg") so worlds are portable between loaders
    private static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Constants.MOD_ID_SHORT);
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Constants.MOD_ID_SHORT);

    static {
        BIOME_SOURCES.register(Constants.MOD_ID_SHORT, () -> SharedOTGBiomeProvider.CODEC);
        CHUNK_GENERATORS.register(Constants.MOD_ID_SHORT, () -> SharedOTGChunkGenerator.CODEC);
    }

    public OTGPlugin() {
        OTGLog.setLogger(new SharedLogger());
        OTGLog.getLogger().log(LogLevel.INFO, LogCategory.MAIN, "OTG Engine starting");
        OTGMaterialReader.set(new SharedMaterialReader());
        OTG.startEngine(new ForgeEngine());

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BIOME_SOURCES.register(modEventBus);
        CHUNK_GENERATORS.register(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelSave);

        OTG.log("OTG Engine started, presets loaded");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        OTGCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    private void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && serverLevel.getChunkSource().getGenerator() instanceof SharedOTGChunkGenerator chunkGenerator) {
            OTGLog.info(LogCategory.STRUCTURE_PLOTTING, "Saving structure cache for world " + chunkGenerator.getPreset().getFolderName());
            chunkGenerator.saveStructureCache();
        }
    }
}
