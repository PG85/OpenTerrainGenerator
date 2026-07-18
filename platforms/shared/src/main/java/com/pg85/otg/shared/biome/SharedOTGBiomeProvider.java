package com.pg85.otg.shared.biome;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pg85.otg.OTG;
import com.pg85.otg.gen.biome.layers.BiomeLayers;
import com.pg85.otg.gen.biome.layers.util.CachingLayerSampler;
import com.pg85.otg.interfaces.ILayerSampler;
import com.pg85.otg.interfaces.ILayerSource;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Getter
public class
SharedOTGBiomeProvider extends BiomeSource implements ILayerSource, BiomeManager.NoiseBiomeSource {
    public static final Codec<SharedOTGBiomeProvider> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.fieldOf("preset_name").stable().forGetter(SharedOTGBiomeProvider::getPresetFolderName),
                    Codec.LONG.fieldOf("seed").stable().forGetter(SharedOTGBiomeProvider::getSeed)
            ).apply(instance, instance.stable(SharedOTGBiomeProvider::new)));
    private final String presetFolderName;
    //this.layer = ThreadLocal.withInitial(() -> BiomeLayers.create(seed, OTG.getEngine().getPresetLoader().getPresetGenerationData().get(presetFolderName), OTG.getEngine().getLogger()));
    private final CountDownLatch latch = new CountDownLatch(1);
    private long seed;
    private ThreadLocal<CachingLayerSampler> layer;
    private final Int2ObjectOpenHashMap<Holder<Biome>> keyLookup = new Int2ObjectOpenHashMap<>();

    public SharedOTGBiomeProvider(String presetFolderName, long seed) {
        this.presetFolderName = presetFolderName;
        this.seed = seed;
    }

    @Override
    public ILayerSampler getSampler() {
        return layer.get();
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull Stream<Holder<Biome>> collectPossibleBiomes() {
        var iBiomes = OTG.getEngine().getPresetLoader().getGlobalIdMapping(presetFolderName);
        if (iBiomes == null) {
            OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.BIOME_REGISTRY,
                    "Biome mapping for preset " + presetFolderName + " is null.");
            return Stream.empty();
        }
        for (int otgBiomeID = 0; otgBiomeID < iBiomes.length; otgBiomeID++) {
            keyLookup.put(otgBiomeID, ((SharedBiome) iBiomes[otgBiomeID]).getBiomeHolder());
        }
        return Stream.of(iBiomes).map(iBiome -> ((SharedBiome) iBiome).getBiomeHolder());
    }

    @Override
    public @Nullable Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(int i, int j, int k, int l, int m, Predicate<Holder<Biome>> predicate, RandomSource randomSource, boolean bl, Climate.Sampler sampler) {
        if (this.getLayer() == null) {
            throw new IllegalStateException("Layer is null. Seed was not set properly.");
        }
        return super.findBiomeHorizontal(i, j, k, l, m, predicate, randomSource, bl, sampler);
    }

    @Override
    public @Nullable Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(int i, int j, int k, int l, Predicate<Holder<Biome>> predicate, RandomSource randomSource, Climate.Sampler sampler) {
        if (this.getLayer() == null) {
            throw new IllegalStateException("Layer is null. Seed was not set properly.");
        }
        return super.findBiomeHorizontal(i, j, k, l, predicate, randomSource, sampler);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int i, int j, int k, Climate.Sampler sampler) {
        return keyLookup.get(this.getLayer().get().sample(i, k));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int i, int j, int k) {
        return keyLookup.get(this.getLayer().get().sample(i, k));
    }

    public void setSeed(long seed) {
        synchronized (this) {
            if (this.seed == seed && this.layer != null) {
                return;
            }
            if (this.seed != seed && this.layer != null) {
                OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.MAIN,
                        "Biome provider seed changed from " + this.seed + " to " + seed +
                                ". This is unexpected and may lead to inconsistent biome generation.");
            }
            this.seed = seed;
            layer = ThreadLocal.withInitial(() -> BiomeLayers.create(seed, OTG.getEngine().getPresetLoader().getPresetGenerationData().get(presetFolderName), OTG.getEngine().getLogger()));
            latch.countDown();
        }
    }
}
