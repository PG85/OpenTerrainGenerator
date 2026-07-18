package com.pg85.otg.shared.network;

import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.util.biome.ColorSet;
import com.pg85.otg.util.biome.ColorThreshold;
import com.pg85.otg.util.biome.SimpleColorSet;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SharedBiomeSyncWrapper {
    private final float fogDensity;
    private final ColorSet grassColorControl;
    private final ColorSet foliageColorControl;
    private final ColorSet waterColorControl;

    public SharedBiomeSyncWrapper(BiomeSettings config)
    {
        this.fogDensity = config.getVisualSettings().getFogDensity();
        this.grassColorControl = config.getVisualSettings().getGrassColorControl();
        this.foliageColorControl = config.getVisualSettings().getFoliageColorControl();
        this.waterColorControl = config.getVisualSettings().getWaterColorControl();
    }

    public SharedBiomeSyncWrapper(FriendlyByteBuf buffer)
    {
        this.fogDensity = buffer.readFloat();
        byte size;

        List<ColorThreshold> grassColors = new ArrayList<>();
        size = buffer.readByte();
        for (int i = size; i > 0; i--)
        {
            grassColors.add(new ColorThreshold(buffer.readInt(), buffer.readFloat()));
        }
        this.grassColorControl = new SimpleColorSet(grassColors);

        List<ColorThreshold> foliageColors = new ArrayList<>();
        size = buffer.readByte();
        for (int i = size; i > 0; i--)
        {
            foliageColors.add(new ColorThreshold(buffer.readInt(), buffer.readFloat()));
        }
        this.foliageColorControl = new SimpleColorSet(foliageColors);

        List<ColorThreshold> waterColors = new ArrayList<>();
        size = buffer.readByte();
        for (int i = size; i > 0; i--)
        {
            waterColors.add(new ColorThreshold(buffer.readInt(), buffer.readFloat()));
        }
        this.waterColorControl = new SimpleColorSet(waterColors);
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeFloat(this.fogDensity);

        buffer.writeByte((byte) this.grassColorControl.getLayers().size());
        for (ColorThreshold color : this.grassColorControl.getLayers())
        {
            buffer.writeInt(color.getColor().intValue());
            buffer.writeFloat(color.getMaxNoise());
        }

        buffer.writeByte((byte) this.foliageColorControl.getLayers().size());
        for (ColorThreshold color : this.foliageColorControl.getLayers())
        {
            buffer.writeInt(color.getColor().intValue());
            buffer.writeFloat(color.getMaxNoise());
        }

        buffer.writeByte((byte) this.waterColorControl.getLayers().size());
        for (ColorThreshold color : this.waterColorControl.getLayers())
        {
            buffer.writeInt(color.getColor().intValue());
            buffer.writeFloat(color.getMaxNoise());
        }
    }
}
