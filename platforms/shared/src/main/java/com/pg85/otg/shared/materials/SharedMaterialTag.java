package com.pg85.otg.shared.materials;

import com.pg85.otg.util.materials.LocalMaterialTag;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

@Getter
public class SharedMaterialTag extends LocalMaterialTag {
    private final TagKey<Block> key;
    public SharedMaterialTag(TagKey<Block> key, String name) {
        super(name);
        this.key = key;
    }

    public static LocalMaterialTag ofString(String tag) {
        if (!tag.contains(":") ||
                (!tag.startsWith("#") && !tag.startsWith("otg"))
        ) {
            return null;
        }
        TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, new ResourceLocation(tag));
        return new SharedMaterialTag(tagKey, tag);
    }

}
