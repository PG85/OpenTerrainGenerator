package com.pg85.otg.shared.util;

import com.pg85.otg.OTG;
import com.pg85.otg.shared.gen.SharedWorldGenRegion;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.gen.LocalWorldGenRegion;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import com.pg85.otg.util.nbt.LocalNBTHelper;
import com.pg85.otg.util.nbt.NamedBinaryTag;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.text.MessageFormat;
import java.util.Set;

public class SharedNBTHelper extends LocalNBTHelper {

    /**
     * Converts a net.minecraft.server list NBT tag to a
     * net.minecraftwiki.wiki.NBTClass NBT list tag.
     *
     * @param name	Name of the Minecraft tag.
     * @param nmsListTag The Minecraft tag.
     * @return The converted tag.
     */
    private static NamedBinaryTag getNBTFromNMSTagList(String name, ListTag nmsListTag)
    {
        if (nmsListTag.isEmpty())
        {
            // Nothing to return
            return null;
        }

        NamedBinaryTag.Type listType = NamedBinaryTag.Type.values()[nmsListTag.getElementType()];
        NamedBinaryTag listTag = new NamedBinaryTag(name, listType);

        // Add all child tags
        for (Tag nmsChildTag : nmsListTag) {
            switch (listType) {
                case TAG_End:
                    break;
                case TAG_Byte:
                case TAG_Short:
                case TAG_Int:
                case TAG_Long:
                case TAG_Float:
                case TAG_Double:
                case TAG_Byte_Array:
                case TAG_String:
                case TAG_Int_Array:
                    listTag.addTag(new NamedBinaryTag(listType, null, getValueFromNms(nmsChildTag)));
                    break;
                case TAG_List:
                    NamedBinaryTag listChildTag = getNBTFromNMSTagList(null, (ListTag) nmsChildTag);
                    if (listChildTag != null) {
                        listTag.addTag(listChildTag);
                    }
                    break;
                case TAG_Compound:
                    listTag.addTag(getNBTFromNMSTagCompound(null, (CompoundTag) nmsChildTag));
                    break;
                default:
                    if (OTGLog.getLogger().getLogCategoryEnabled(LogCategory.CUSTOM_OBJECTS)) {
                        OTGLog.log(
                                LogLevel.ERROR,
                                LogCategory.CUSTOM_OBJECTS,
                                MessageFormat.format(
                                        "Cannot convert list subtype {0} from its NMS value",
                                        listType
                                )
                        );
                    }
                    break;
            }
        }
        return listTag;
    }

    /**
     * Converts a net.minecraft.server compound NBT tag to a
     * net.minecraftwiki.wiki.NBTClass NBT compound tag.
     *
     * @param name		Name of the Minecraft tag.
     * @param nmsCompoundTag The Minecraft tag.
     * @return The converted tag.
     */
    public static NamedBinaryTag getNBTFromNMSTagCompound(String name, CompoundTag nmsCompoundTag) {
        NamedBinaryTag compoundTag = new NamedBinaryTag(NamedBinaryTag.Type.TAG_Compound, name,
                new NamedBinaryTag[]{new NamedBinaryTag(NamedBinaryTag.Type.TAG_End, null, null)});

        Set<String> keys = nmsCompoundTag.getAllKeys();

        // Add all child tags to the compound tag
        for (String key : keys)
        {
            Tag nmsChildTag = nmsCompoundTag.get(key);

            if (nmsChildTag == null)
            {
                if(OTG.getEngine().getLogger().getLogCategoryEnabled(LogCategory.CUSTOM_OBJECTS))
                {
                    OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.CUSTOM_OBJECTS,
                            "Failed to read NBT property " + key + " from tag " + nmsCompoundTag.getId());
                }
                continue;
            }

            NamedBinaryTag.Type type = NamedBinaryTag.Type.values()[nmsChildTag.getId()];
            switch (type)
            {
                case TAG_End:
                    break;
                case TAG_Byte:
                case TAG_Short:
                case TAG_Int:
                case TAG_Long:
                case TAG_Float:
                case TAG_Double:
                case TAG_Byte_Array:
                case TAG_String:
                case TAG_Int_Array:
                    compoundTag.addTag(new NamedBinaryTag(type, key, getValueFromNms(nmsChildTag)));
                    break;
                case TAG_List:
                    NamedBinaryTag listChildTag = getNBTFromNMSTagList(key, (ListTag) nmsChildTag);
                    if (listChildTag != null)
                    {
                        compoundTag.addTag(listChildTag);
                    }
                    break;
                case TAG_Compound:
                    compoundTag.addTag(getNBTFromNMSTagCompound(key, (CompoundTag) nmsChildTag));
                    break;
                default:
                    break;
            }
        }

        return compoundTag;
    }

    @Override
    public NamedBinaryTag getNBTFromLocation(LocalWorldGenRegion world, int x, int y, int z) {
        BlockEntity blockEntity = ((SharedWorldGenRegion) world).getBlockEntity(new BlockPos(x, y, z));
        
        if (blockEntity == null) {
            return null;
        }
        
        CompoundTag nbt = new CompoundTag();
        
        blockEntity.load(nbt);
        nbt.remove("x");
        nbt.remove("y");
        nbt.remove("z");
        
        return getNBTFromNMSTagCompound(null, nbt);
    }

    // Internal methods below

    /**
     * Gets the value from a nms tag (since that object doesn't have a simple
     * value field)
     *
     * @param inbt The Minecraft tag.
     * @return The value in the tag.
     */
    private static Object getValueFromNms(Tag inbt)
    {
        NamedBinaryTag.Type type = NamedBinaryTag.Type.values()[inbt.getId()];
        // Cannot read this from a tag
        return switch (type) {
            case TAG_Byte -> ((ByteTag) inbt).getAsByte();
            case TAG_Short -> ((ShortTag) inbt).getAsShort();
            case TAG_Int -> ((IntTag) inbt).getAsInt();
            case TAG_Long -> ((LongTag) inbt).getAsLong();
            case TAG_Float -> ((FloatTag) inbt).getAsFloat();
            case TAG_Double -> ((DoubleTag) inbt).getAsDouble();
            case TAG_Byte_Array -> ((ByteArrayTag) inbt).getAsByteArray();
            case TAG_String -> inbt.getAsString();
            case TAG_Int_Array -> ((IntArrayTag) inbt).getAsIntArray();
            default -> throw new IllegalArgumentException(type + "doesn't have a simple value!");
        };
    }

    /**
     * Converts a net.minecraftwiki.wiki.NBTClass NBT compound tag into an
     * net.minecraft.server NBT compound tag.
     *
     * @param compoundTag Our tag.
     * @return The Minecraft tag.
     */
    public static CompoundTag getNMSFromNBTTagCompound(NamedBinaryTag compoundTag)
    {
        CompoundTag nmsTag = new CompoundTag();
        NamedBinaryTag[] childTags = (NamedBinaryTag[]) compoundTag.getValue();
        if (childTags == null)
        {
            return nmsTag;
        }
        for (NamedBinaryTag tag : childTags)
        {
            switch (tag.getType())
            {
                case TAG_End:
                    break;
                case TAG_Byte:
                case TAG_Short:
                case TAG_Int:
                case TAG_Long:
                case TAG_Float:
                case TAG_Double:
                case TAG_Byte_Array:
                case TAG_String:
                case TAG_Int_Array:
                    nmsTag.put(tag.getName(), createTagNms(tag.getType(), tag.getValue()));
                    break;
                case TAG_List:
                    nmsTag.put(tag.getName(), getNMSFromNBTTagList(tag));
                    break;
                case TAG_Compound:
                    nmsTag.put(tag.getName(), getNMSFromNBTTagCompound(tag));
                    break;
                default:
                    break;
            }
        }
        return nmsTag;
    }

    /**
     * Converts a OpenTerrainGenerator NBT list tag into an net.minecraft.server NBT
     * list tag.
     *
     * @param listTag The OpenTerrainGenerator list tag.
     * @return The Minecraft list tag.
     */
    private static ListTag getNMSFromNBTTagList(NamedBinaryTag listTag)
    {
        ListTag nmsTag = new ListTag();
        NamedBinaryTag[] childTags = (NamedBinaryTag[]) listTag.getValue();
        for (NamedBinaryTag tag : childTags)
        {
            switch (tag.getType())
            {
                case TAG_Byte:
                case TAG_Short:
                case TAG_Int:
                case TAG_Long:
                case TAG_Float:
                case TAG_Double:
                case TAG_Byte_Array:
                case TAG_String:
                case TAG_Int_Array:
                    nmsTag.add(createTagNms(tag.getType(), tag.getValue()));
                    break;
                case TAG_List:
                    nmsTag.add(getNMSFromNBTTagList(tag));
                    break;
                case TAG_Compound:
                    nmsTag.add(getNMSFromNBTTagCompound(tag));
                    break;
                case TAG_End:
                default:
                    break;
            }
        }
        return nmsTag;
    }

    /**
     * Creates a Minecraft ITag tag. Doesn't work for ends, lists and
     * compounds.
     *
     * @param type  Type of the tag.
     * @param value Value of the tag.
     * @return The Minecraft NBTBast tag.
     */
    private static Tag createTagNms(NamedBinaryTag.Type type, Object value)
    {
        return switch (type) {
            case TAG_Byte -> ByteTag.valueOf((Byte) value);
            case TAG_Short -> ShortTag.valueOf((Short) value);
            case TAG_Int -> IntTag.valueOf((Integer) value);
            case TAG_Long -> LongTag.valueOf((Long) value);
            case TAG_Float -> FloatTag.valueOf((Float) value);
            case TAG_Double -> DoubleTag.valueOf((Double) value);
            case TAG_Byte_Array -> new ByteArrayTag((byte[]) value);
            case TAG_String -> StringTag.valueOf((String) value);
            case TAG_Int_Array -> new IntArrayTag((int[]) value);
            // Cannot make this into a tag
            default -> throw new IllegalArgumentException(type + "doesn't have a simple value!");
        };
    }
}
