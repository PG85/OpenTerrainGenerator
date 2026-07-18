package com.pg85.otg.shared.biome;

import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.interfaces.IBiome;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

@Getter
public class SharedBiome implements IBiome {
    private final BiomeSettings biomeSettings;
    private final Biome biome;
    private final Holder.Reference<Biome> biomeHolder;

    public SharedBiome(BiomeSettings biomeSettings, Biome biome, Holder.Reference<Biome> biomeHolder) {
        this.biomeSettings = biomeSettings;
        this.biome = biome;
        this.biomeHolder = biomeHolder;
    }

    @Override
    public float getTemperatureAt(int x, int y, int z) {
        return biome.getBaseTemperature();
    }
}
