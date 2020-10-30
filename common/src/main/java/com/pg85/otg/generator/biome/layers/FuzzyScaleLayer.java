package com.pg85.otg.generator.biome.layers;

import com.pg85.otg.generator.biome.layers.util.LayerSampleContext;

public class FuzzyScaleLayer extends ScaleLayer
{
	@Override
	protected int sample(LayerSampleContext<?> context, int i, int j, int k, int l)
	{
		return context.choose(i, j, k, l);
	}
}