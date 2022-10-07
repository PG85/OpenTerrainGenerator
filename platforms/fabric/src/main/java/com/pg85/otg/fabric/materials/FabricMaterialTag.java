package com.pg85.otg.fabric.materials;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.util.materials.LocalMaterialTag;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;

public class FabricMaterialTag extends LocalMaterialTag {

    public static LocalMaterialTag ofString(String name) {
        // If otg: or no domain was supplied, try OTG tags.

        // TODO: This might not be updated correctly, needs another look --auth
        Tag<Block> optTag;
        if(!name.contains(":") || name.startsWith(Constants.MOD_ID_SHORT + ":"))
        {
            final ResourceLocation otgResourceLocation;
            try
            {
                otgResourceLocation = new ResourceLocation(Constants.MOD_ID_SHORT + ":" + name.trim().toLowerCase().replace(Constants.MOD_ID_SHORT + ":", ""));
                optTag = BlockTags.getAllTags().getTag(otgResourceLocation);
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
            optTag = BlockTags.getAllTags().getTag(resourceLocation);
            if(optTag != null)
            {
                return new FabricMaterialTag(optTag, resourceLocation.toString());
            }
        } catch(ResourceLocationException ex) { }

        return null;
    }

    private final String name;
    private final Tag<Block> blockTag;

    private FabricMaterialTag(Tag<Block> blockTag, String name)
    {
        this.blockTag = blockTag;
        this.name = name;
    }

    public Tag<Block> getTag()
    {
        return this.blockTag;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
