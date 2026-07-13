package com.pg85.otg.fabric.gen.noise;

import com.pg85.otg.gen.OTGChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Builds the runtime noise router that grafts vanilla 1.18+ noise caves onto OTG's terrain.
 *
 * OTG terrain (via {@link OTGTerrainDensityFunction}) takes the slopedCheese slot in vanilla's
 * overworld cave composition (mirroring NoiseRouterData.overworld/underground for 1.20.1).
 * Vanilla's cave component functions are fetched from the loaded density function registry and
 * wrapped in HolderHolder, exactly as vanilla's own router references them; RandomState's noise
 * wiring then binds their noises with this world's seed.
 *
 * Everything here is runtime-only per world: the registered NoiseGeneratorSettings (with its
 * zero router) stays untouched, because it is global static data while the OTG generator and
 * seed are per-world.
 *
 * Deliberate divergences from vanilla's overworld router:
 * - Cave depth is keyed to {@link OTGDepthProxyFunction} (a uniform gradient below each
 *   column's blended center height) rather than to terrain density itself. OTG's falloff
 *   gradient is divided by biome volatility, so using terrain density as the depth signal
 *   (vanilla's approach) would leave high-volatility biomes without caves at any depth.
 * - In the underground zone the cave field is min-combined with terrain density instead of
 *   replacing it, so OTG terrain shapes (overhangs, deep valleys, CHC air) survive below the
 *   cave threshold depth; caves are carved out of solid terrain only.
 * - No slideOverworld: OTG's pipeline already fades the top of the world, and vanilla's slide
 *   anchors are hardcoded to -64..320, which mis-anchors on configurable world heights.
 * - Ore veins disabled. Aquifers run with vanilla noises on top of {@link OTGFluidPicker}
 *   as the global picker: open terrain floods to the per-biome water level, while caves
 *   below it stay dry apart from vanilla-style aquifer pockets.
 * - Climate/biome slots are zero; OTG does its own biome placement.
 */
public final class OTGNoiseRouterFactory {
    // Thresholds/constants from NoiseRouterData (1.20.1). See underground() and overworld().
    private static final double ENTRANCES_THRESHOLD = 1.5625;

    private OTGNoiseRouterFactory() {
    }

    public record OTGNoiseCaveContext(
            NoiseGeneratorSettings runtimeSettings,
            RandomState randomState,
            Aquifer.FluidPicker fluidPicker
    ) {
    }

    public static OTGNoiseCaveContext create(
            OTGChunkGenerator internalGenerator,
            double densityScale,
            double depthGradient,
            NoiseGeneratorSettings registeredSettings,
            RegistryAccess registryAccess,
            long seed
    ) {
        HolderGetter<DensityFunction> functions =
                registryAccess.registryOrThrow(Registries.DENSITY_FUNCTION).asLookup();
        HolderGetter<NormalNoise.NoiseParameters> noises =
                registryAccess.registryOrThrow(Registries.NOISE).asLookup();

        DensityFunction otgTerrain = new OTGTerrainDensityFunction(internalGenerator, densityScale);
        DensityFunction depthProxy = new OTGDepthProxyFunction(internalGenerator, depthGradient);
        DensityFunction finalDensity = finalDensity(otgTerrain, depthProxy, functions, noises);

        // Vanilla aquifer noises (NoiseRouterData.overworld, 1.20.1). With aquifers enabled,
        // the aquifer keeps caves below the water level dry apart from local pockets;
        // OTGFluidPicker stays the global picker, so per-biome water levels still apply
        // to open terrain (oceans, lakes).
        NoiseRouter router = new NoiseRouter(
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_BARRIER), 0.5), // barrierNoise
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67), // fluidLevelFloodednessNoise
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143), // fluidLevelSpreadNoise
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_LAVA)), // lavaNoise
                DensityFunctions.zero(), // temperature
                DensityFunctions.zero(), // vegetation
                DensityFunctions.zero(), // continents
                DensityFunctions.zero(), // erosion
                DensityFunctions.zero(), // depth
                DensityFunctions.zero(), // ridges
                otgTerrain,              // initialDensityWithoutJaggedness (preliminary surface)
                finalDensity,
                DensityFunctions.zero(), // veinToggle
                DensityFunctions.zero(), // veinRidged
                DensityFunctions.zero()  // veinGap
        );

        NoiseGeneratorSettings runtimeSettings = new NoiseGeneratorSettings(
                registeredSettings.noiseSettings(),
                registeredSettings.defaultBlock(),
                registeredSettings.defaultFluid(),
                router,
                registeredSettings.surfaceRule(),
                registeredSettings.spawnTarget(),
                registeredSettings.seaLevel(),
                registeredSettings.disableMobGeneration(),
                true,  // aquifersEnabled: without them every cave below the water level floods
                false, // oreVeinsEnabled: vein router slots are zero
                registeredSettings.useLegacyRandomSource()
        );

        return new OTGNoiseCaveContext(
                runtimeSettings,
                RandomState.create(runtimeSettings, noises, seed),
                new OTGFluidPicker(internalGenerator)
        );
    }

    // Mirrors NoiseRouterData.overworld()'s finalDensity assembly with OTG terrain as
    // slopedCheese, minus slideOverworld (see class doc). Two OTG-specific changes:
    // the depth proxy (not terrain density) selects the cave zone and fades cheese caves,
    // and the underground cave field is min-combined with terrain so OTG terrain air
    // (overhangs, valleys, CHC) is never filled in.
    private static DensityFunction finalDensity(
            DensityFunction slopedCheese,
            DensityFunction depthProxy,
            HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises
    ) {
        DensityFunction entrances = getFunction(functions, "overworld/caves/entrances");
        DensityFunction noodle = getFunction(functions, "overworld/caves/noodle");

        DensityFunction entrancesOnly = DensityFunctions.min(
                slopedCheese,
                DensityFunctions.mul(DensityFunctions.constant(5.0), entrances)
        );
        DensityFunction caved = DensityFunctions.rangeChoice(
                depthProxy, -1000000.0, ENTRANCES_THRESHOLD,
                entrancesOnly,
                DensityFunctions.min(slopedCheese, underground(depthProxy, entrances, functions, noises))
        );
        return DensityFunctions.min(postProcess(caved), noodle);
    }

    // Mirrors NoiseRouterData.underground() (1.20.1), with the depth proxy in place of
    // slopedCheese for the near-surface cheese fade term.
    private static DensityFunction underground(
            DensityFunction depthProxy,
            DensityFunction entrances,
            HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises
    ) {
        DensityFunction spaghetti2d = getFunction(functions, "overworld/caves/spaghetti_2d");
        DensityFunction spaghettiRoughness = getFunction(functions, "overworld/caves/spaghetti_roughness_function");
        DensityFunction caveLayer = DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_LAYER), 8.0);
        DensityFunction layerBulge = DensityFunctions.mul(DensityFunctions.constant(4.0), caveLayer.square());
        DensityFunction caveCheese = DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666);
        DensityFunction cheese = DensityFunctions.add(
                DensityFunctions.add(DensityFunctions.constant(0.27), caveCheese).clamp(-1.0, 1.0),
                DensityFunctions.add(
                        DensityFunctions.constant(1.5),
                        DensityFunctions.mul(DensityFunctions.constant(-0.64), depthProxy)
                ).clamp(0.0, 0.5)
        );
        DensityFunction caves = DensityFunctions.min(
                DensityFunctions.min(DensityFunctions.add(layerBulge, cheese), entrances),
                DensityFunctions.add(spaghetti2d, spaghettiRoughness)
        );
        DensityFunction pillars = getFunction(functions, "overworld/caves/pillars");
        DensityFunction activePillars = DensityFunctions.rangeChoice(
                pillars, -1000000.0, 0.03, DensityFunctions.constant(-1000000.0), pillars
        );
        return DensityFunctions.max(caves, activePillars);
    }

    // Mirrors NoiseRouterData.postProcess().
    private static DensityFunction postProcess(DensityFunction density) {
        DensityFunction blended = DensityFunctions.blendDensity(density);
        return DensityFunctions.mul(DensityFunctions.interpolated(blended), DensityFunctions.constant(0.64)).squeeze();
    }

    // Equivalent of NoiseRouterData.getFunction: a HolderHolder around the registered function,
    // so RandomState's noise wiring and NoiseChunk's wrapping treat it exactly like vanilla's
    // own router references.
    private static DensityFunction getFunction(HolderGetter<DensityFunction> functions, String name) {
        Holder<DensityFunction> holder = functions.getOrThrow(
                ResourceKey.create(Registries.DENSITY_FUNCTION, new ResourceLocation(name))
        );
        return new DensityFunctions.HolderHolder(holder);
    }
}
