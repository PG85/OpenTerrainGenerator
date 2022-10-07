package com.pg85.otg.fabric.util;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;

public class FabricDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator fdg) {
        fdg.addProvider(FabricBlockTags::new);
        fdg.addProvider(FabricBiomeTags::new);
    }
}
