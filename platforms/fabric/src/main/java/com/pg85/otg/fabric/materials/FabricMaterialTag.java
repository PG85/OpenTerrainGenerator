package com.pg85.otg.fabric.materials;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.util.materials.LocalMaterialTag;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class FabricMaterialTag extends LocalMaterialTag {
    private static Map<ResourceLocation, TagKey<Block>> VANILLA_TAGS = new HashMap<>();

    static {
        for (Field f : BlockTags.class.getDeclaredFields())
        {
            try
            {
                TagKey<Block> o = (TagKey<Block>) f.get(null);

                VANILLA_TAGS.put(o.location(), o);

            } catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
    }


    public static LocalMaterialTag ofString(String name)
    {
        // If otg: or no domain was supplied, try OTG tags.

        // TODO: This might not be updated correctly, needs another look --auth
        TagKey<Block> optTag;
        if(!name.contains(":") || name.startsWith(Constants.MOD_ID_SHORT + ":"))
        {
            final ResourceLocation otgResourceLocation;
            try
            {
                otgResourceLocation = new ResourceLocation(Constants.MOD_ID_SHORT + ":" + name.trim().toLowerCase().replace(Constants.MOD_ID_SHORT + ":", ""));
                optTag = VANILLA_TAGS.get(otgResourceLocation);
                if(optTag != null)
                {
                    return new FabricMaterialTag(optTag, otgResourceLocation.toString());
                }
            } catch(ResourceLocationException ex) { }
        }

        final ResourceLocation resourceLocation;
        try
        {
            resourceLocation = new ResourceLocation(name.trim().toLowerCase());
            optTag = VANILLA_TAGS.get(resourceLocation);
            if(optTag != null)
            {
                return new FabricMaterialTag(optTag, resourceLocation.toString());
            }
        } catch(ResourceLocationException ex) { }

        return null;
    }

    private final String name;
    private final TagKey<Block> blockTag;

    private FabricMaterialTag(TagKey<Block> blockTag, String name)
    {
        this.blockTag = blockTag;
        this.name = name;
    }

    public TagKey<Block> getTag()
    {
        return this.blockTag;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
