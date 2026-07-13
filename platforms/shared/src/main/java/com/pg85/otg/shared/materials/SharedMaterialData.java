package com.pg85.otg.shared.materials;

import com.pg85.otg.util.OTGDirection;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.LocalMaterialTag;
import com.pg85.otg.util.materials.MaterialProperties;
import com.pg85.otg.util.materials.MaterialProperty;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class SharedMaterialData extends LocalMaterialData {
    @Getter
    static final LocalMaterialData blank = new SharedMaterialData(null, null);
    @Getter
    private final BlockState state;
    @Getter
    private final String registryName;
    private final String name;
    private static final ConcurrentHashMap<BlockState, SharedMaterialData> stateToMaterialDataMap = new ConcurrentHashMap<>();

    // OTG block tags for logs and leaves
    private static final TagKey<Block> LOGS_TAG = TagKey.create(Registries.BLOCK, new ResourceLocation("otg", "log"));
    private static final TagKey<Block> LEAVES_TAG = TagKey.create(Registries.BLOCK, new ResourceLocation("otg", "leaves"));

    public SharedMaterialData(BlockState state, String raw) {
        super(raw);
        this.state = state;
        if (state == null && raw == null) {
            this.isBlank = true;
        }
        this.registryName = state == null ? null : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        this.name = getName();
    }

    public static LocalMaterialData ofBlockState(@NonNull BlockState blockState) {
        return ofBlockState(blockState, null);
    }

    public static LocalMaterialData ofBlockState(BlockState blockState, String input) {
        if (stateToMaterialDataMap.containsKey(blockState)) {
            return stateToMaterialDataMap.get(blockState);
        }
        if (input == null) {
            // avoid calling toString for literally every ofBlockState lookup
            input = blockState.toString();
        }
        SharedMaterialData materialData = new SharedMaterialData(blockState, input);
        SharedMaterialData previous = stateToMaterialDataMap.putIfAbsent(blockState, materialData);
        return previous == null ? materialData : previous;
    }

    public static LocalMaterialData ofBlock(Block block, String input) {
        return ofBlockState(block.defaultBlockState(), input);
    }

    @Override
    public <T extends Comparable<T>> LocalMaterialData withProperty(MaterialProperty<T> materialProperty, T value) {
        Property<T> property;

        // TODO: This is really bad. We need a way to append properties onto the MaterialProperty
        if (materialProperty == MaterialProperties.AGE_0_25)
        {
            property = (Property<T>) BlockStateProperties.AGE_25;
        }
        else if (materialProperty == MaterialProperties.AGE_0_3)
        {
            property = (Property<T>) BlockStateProperties.AGE_3;
        }
        else if (materialProperty == MaterialProperties.PICKLES_1_4)
        {
            property = (Property<T>) BlockStateProperties.PICKLES;
        }
        else if (materialProperty == MaterialProperties.SNOWY)
        {
            property = (Property<T>) BlockStateProperties.SNOWY;
        }
        else if (materialProperty == MaterialProperties.HORIZONTAL_DIRECTION)
        {
            // Extremely ugly hack for directions
            DirectionProperty directionProperty = BlockStateProperties.HORIZONTAL_FACING;
            Direction direction = Direction.values()[((OTGDirection)value).ordinal()];
            return SharedMaterialData.ofBlockState(this.state.setValue(directionProperty, direction));
        } else {
            throw new IllegalArgumentException("Unknown property: " + materialProperty);
        }

        return SharedMaterialData.ofBlockState(this.state.setValue(property, value));
    }

    public <T extends Comparable<T>> LocalMaterialData withProperty(String property, T value) {
        // Special case for OTGDirection because it's OTG specific, unlike Integer and Boolean
        if (value instanceof OTGDirection) {
            DirectionProperty directionProperty = BlockStateProperties.HORIZONTAL_FACING;
            Direction direction = Direction.values()[((OTGDirection)value).ordinal()];
            return SharedMaterialData.ofBlockState(this.state.setValue(directionProperty, direction));
        }
        for (Property<?> prop : this.state.getProperties()) {
            if (prop.getName().equals(property)) {
                Property<T> matchedProp = (Property<T>) prop;
                Collection<T> allowedValues = matchedProp.getPossibleValues();
                if (!allowedValues.contains(value)) {
                    throw new IllegalArgumentException("Value " + value + " is not allowed for property " + property + " for block "+this.name);
                }
                return SharedMaterialData.ofBlockState(this.state.setValue(matchedProp, value));
            }
        }
        throw new IllegalArgumentException("Unknown property: " + property + " for block "+this.name);
    }

    @Override
    public String getName() {
        if (this.name != null) {
            return this.name;
        }
        if(this.isBlank) {
            return "BLANK";
        }
        else if(this.state == null) {
            return this.rawEntry != null ? this.rawEntry : "Unknown";
        } else {
            if(this.state != this.state.getBlock().defaultBlockState() && (
                    // We set distance 1 when parsing minecraft:xxx_leaves, so check for default blocksate + distance 1
                    !(this.state.getBlock() instanceof LeavesBlock) || this.state != this.state.getBlock().defaultBlockState().setValue(LeavesBlock.DISTANCE, 1)
                )
            ) {
                return this.state.toString()
                        .replace("Block{", "")
                        .replace("}", "");
            } else {
                return registryName;
            }
        }
    }

    @Override
    public boolean canSnowFallOn() {
        return this.state != null && this.state.is(BlockTags.SNOW_LAYER_CAN_SURVIVE_ON);
    }

    @Override
    public boolean canFall() {
        return state.getBlock() instanceof FallingBlock;
    }

    @Override
    public boolean isMaterial(LocalMaterialData material) {
        if (material == null) {
            return false;
        }
        return this.isBlank && material.isBlank() ||
               this.state == ((SharedMaterialData) material).state;
    }

    @Override
    public boolean isBlockTag(LocalMaterialTag tag) {
        return this.state.is(((SharedMaterialTag) tag).getKey());
    }

    @Override
    public boolean isLiquid() {
        // So far, no replacement exists
        return this.state != null && this.state.liquid();
    }

    @Override
    public boolean isSolid() {
        // isSolid is deprecated, but is used plenty. When it disappears, compare to 1.20 and see how they change it
        // So far, no replacement exists
        return this.state != null && this.state.isSolid();
    }

    @Override
    public boolean isEmptyOrAir() {
        return this.state == null || this.state.isAir();
    }

    @Override
    public boolean isNonCaveAir() {
        return this.state != null && this.state.getBlock() == Blocks.AIR;
    }

    @Override
    public boolean isAir() {
        return this.state != null && this.state.isAir();
    }

    @Override
    public boolean isEmpty() {
        return this.state == null || this.isBlank;
    }

    @Override
    public LocalMaterialData rotate(int rotateTimes) {
        if(this.isBlank) {
            return this;
        }

        // Get the rotation if we haven't stored the rotation yet
        if (rotated == null) {
            this.rotated = SharedMaterialData.ofBlockState(state.rotate(Rotation.CLOCKWISE_90));
        }

        if (rotateTimes > 1) {
            return rotated.rotate(rotateTimes-1);
        }

        return this.rotated;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SharedMaterialData otherData) {
            if (this.isBlank) {
                return otherData.isBlank;
            }
            if (otherData.isBlank) {
                return false;
            }
            return this.state.getBlock().equals(otherData.state.getBlock());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.state == null ? -1 : this.state.hashCode();
    }

    @Override
    public LocalMaterialData legalOrPersistentLeaves(boolean leaveIllegalLeaves) {
        if (!this.isLeaves())
        {
            return this;
        }
        int i = state.getValue(LeavesBlock.DISTANCE);
        if (i > 6)
        {
            if (leaveIllegalLeaves)
                return SharedMaterialData.ofBlockState(
                        state.setValue(LeavesBlock.DISTANCE, 1)
                                .setValue(LeavesBlock.PERSISTENT, false));
            return SharedMaterialData.ofBlockState(
                    state.setValue(LeavesBlock.PERSISTENT, true));
        } else {
            return SharedMaterialData.ofBlockState(
                    state.setValue(LeavesBlock.PERSISTENT, false));
        }
    }

    @Override
    protected boolean checkIsLog() {
        // Use tag-based check for logs (includes vanilla + modded logs)
        return this.state != null && this.state.is(LOGS_TAG);
    }

    @Override
    protected boolean checkIsLeaves() {
        // Use tag-based check for leaves (includes vanilla + modded leaves)
        return this.state != null && this.state.is(LEAVES_TAG);
    }
}
