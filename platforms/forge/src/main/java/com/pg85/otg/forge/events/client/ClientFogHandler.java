package com.pg85.otg.forge.events.client;

import java.util.Arrays;

import org.lwjgl.opengl.GL11;

import com.pg85.otg.OTG;
import com.pg85.otg.common.LocalBiome;
import com.pg85.otg.configuration.biome.BiomeConfig;
import com.pg85.otg.forge.ForgeEngine;
import com.pg85.otg.forge.dimensions.OTGWorldProvider;
import com.pg85.otg.forge.world.ForgeWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Adapted from Minecraft and Biomes O' Plenty.
 * 
 * @see net.minecraft.client.renderer.RenderGlobal
 * @see <a href=
 *      "https://github.com/Glitchfiend/BiomesOPlenty/blob/BOP-1.12.x-7.0.x/src/main/java/biomesoplenty/common/handler/FogEventHandler.java">https://github.com/Glitchfiend/BiomesOPlenty/blob/BOP-1.12.x-7.0.x/src/main/java/biomesoplenty/common/handler/FogEventHandler.java</a>
 */
public class ClientFogHandler
{
	// Max blend distance in ForgeModContainer.blendRanges
	private final int MAX_BLEND_DISTANCE = 34;
	private short[][] biomeCache = new short[(MAX_BLEND_DISTANCE * 2) + 1][(MAX_BLEND_DISTANCE * 2) + 1];
	private double lastX, lastZ;
	private ForgeWorld fogBlendWorld;
	private World fogBlendMinecraftWorld;
	private BiomeConfig[] fogBlendBiomeConfigs;
	private int fogBlendBiomeConfigRevision;
	private long fogBlendX;
	private long fogBlendZ;
	private int fogBlendBlockX;
	private int fogBlendBlockZ;
	private int fogBlendDistance;
	private float cachedBiomeFogDistance;
	private float cachedWeightBiomeFog;
	private double cachedBiomeFogRed;
	private double cachedBiomeFogGreen;
	private double cachedBiomeFogBlue;
	private double cachedBiomeFogWeight;
	private BiomeConfig cachedCenterBiomeConfig;
	private boolean cachedFogFound;
	private boolean fogBlendCacheValid;

	public ClientFogHandler()
	{
		for (short[] row : biomeCache)
		{
			Arrays.fill(row, (short) -1);
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event)
	{
		if (event.getWorld().isRemote)
		{
			clearFogBlendCache();
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event)
	{
		if (event.getWorld().isRemote)
		{
			clearFogBlendCache();
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onChunkLoad(ChunkEvent.Load event)
	{
		invalidateFogBlendForChunk(event);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onChunkUnload(ChunkEvent.Unload event)
	{
		invalidateFogBlendForChunk(event);
	}

	private void invalidateFogBlendForChunk(ChunkEvent event)
	{
		if (!fogBlendCacheValid || event.getWorld() != fogBlendMinecraftWorld || !event.getWorld().isRemote)
		{
			return;
		}

		int chunkMinX = event.getChunk().getPos().x << 4;
		int chunkMinZ = event.getChunk().getPos().z << 4;
		int chunkMaxX = chunkMinX + 15;
		int chunkMaxZ = chunkMinZ + 15;

		if (
			chunkMaxX >= fogBlendBlockX - fogBlendDistance
			&& chunkMinX <= fogBlendBlockX + fogBlendDistance
			&& chunkMaxZ >= fogBlendBlockZ - fogBlendDistance
			&& chunkMinZ <= fogBlendBlockZ + fogBlendDistance
		)
		{
			clearFogBlendCache();
		}
	}

	// Handle the fog color
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onGetFogColor(EntityViewRenderEvent.FogColors event)
	{
		if (!(event.getEntity() instanceof EntityPlayer) || !(event.getEntity().getEntityWorld().provider instanceof OTGWorldProvider))
		{
			// Not a player or OTG world
			return;
		}
		
		ForgeWorld forgeWorld = ((ForgeEngine) OTG.getEngine()).getWorld(event.getEntity().world);
		if (forgeWorld == null)
		{
			return;
		}
		
		BiomeConfig[] biomeConfigs = OTG.getEngine().getOTGBiomeIds(forgeWorld.getName());
		int biomeConfigRevision = OTG.getEngine().getBiomeConfigRevision(forgeWorld.getName());
		Vec3d fogColor = blendFogColors(forgeWorld, (EntityLivingBase) event.getEntity(), event.getRed(), event.getGreen(), event.getBlue(), event.getRenderPartialTicks(), biomeConfigs, biomeConfigRevision);

		if(fogColor != null)
		{
			event.setRed((float) fogColor.x);
			event.setGreen((float) fogColor.y);
			event.setBlue((float) fogColor.z);
		}
		
		lastX = event.getEntity().posX;
		lastZ = event.getEntity().posZ;
	}

	@SideOnly(Side.CLIENT)
	private void resetFogDistance(Minecraft mc, int fogMode)
	{
		if(otgDidLastFogRender)
		{
			// Non-OTG dims and OTG dims without fog settings don't properly reset 
			// the fog start and end when players teleport between dimensions.
			// Reset the fog distance here.
			otgDidLastFogRender = false;
			float farPlaneDistance = (float)(mc.gameSettings.renderDistanceChunks * 16);
			
            if (fogMode < 0)
            {
    			GL11.glFogf(GL11.GL_FOG_START, 0.0F);
    			GL11.glFogf(GL11.GL_FOG_END, farPlaneDistance);
            } else {
    			GL11.glFogf(GL11.GL_FOG_START, farPlaneDistance * 0.75F);
    			GL11.glFogf(GL11.GL_FOG_END, farPlaneDistance);
            }
            
    		for (short[] row : biomeCache)
    		{
    			Arrays.fill(row, (short) -1);
    		}
		}
	}
	
	@SideOnly(Side.CLIENT)
	private void clearBiomeCacheOnWorldChanged(Minecraft mc, int fogMode, ForgeWorld forgeWorld)
	{
		// If the player switched worlds, clear the biome id cache
		if(!lastWorldName.equals(forgeWorld.getName()))
		{
			lastWorldName = forgeWorld.getName();
			clearFogBlendCache();
			for (short[] row : biomeCache)
			{
				Arrays.fill(row, (short) -1);
			}
			resetFogDistance(mc, fogMode);
		}
	}
	
	boolean otgDidLastFogRender = false;
	// Handle the fog distance blending
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event)
	{
		if (!(event.getEntity().getEntityWorld().provider instanceof OTGWorldProvider))
		{
			clearFogBlendCache();
			resetFogDistance(event.getRenderer().mc, event.getFogMode());
			return;
		}
		
		GameSettings settings = Minecraft.getMinecraft().gameSettings;
		int[] ranges = ForgeModContainer.blendRanges;
		int blendDistance = 6;

		if (
			settings.fancyGraphics && settings.renderDistanceChunks >= 0
			&& settings.renderDistanceChunks < ranges.length
		)
		{
			blendDistance = ranges[settings.renderDistanceChunks];
		}

		Entity entity = event.getEntity();

		int blockX = MathHelper.floor(entity.posX);
		int blockZ = MathHelper.floor(entity.posZ);

		ForgeWorld forgeWorld = ((ForgeEngine) OTG.getEngine()).getWorld(entity.getEntityWorld());

		if (forgeWorld == null)
		{
			// Not an OTG world
			clearFogBlendCache();
			resetFogDistance(event.getRenderer().mc, event.getFogMode());		
			return;
		}

		clearBiomeCacheOnWorldChanged(event.getRenderer().mc, event.getFogMode(), forgeWorld);
		BiomeConfig[] biomeConfigs = OTG.getEngine().getOTGBiomeIds(forgeWorld.getName());
		int biomeConfigRevision = OTG.getEngine().getBiomeConfigRevision(forgeWorld.getName());

		if (!updateFogBlendCache(forgeWorld, biomeConfigs, biomeConfigRevision, entity, blendDistance, blockX, blockZ))
		{
			return;
		}

		float biomeFogDistance = cachedBiomeFogDistance;
		float weightBiomeFog = cachedWeightBiomeFog;
		boolean bFound = cachedFogFound;
		
		if(!bFound)
		{
			// OTG world with default fog settings.
			resetFogDistance(event.getRenderer().mc, event.getFogMode());		
			return;
		}

		float weightMixed = (blendDistance * 2) * (blendDistance * 2);
		float weightDefault = weightMixed - weightBiomeFog;

		if(weightDefault < 0.0f)
		{
			weightDefault = 0.0f;
		}
		
		float fogDistanceAvg = weightBiomeFog == 0.0f ? 0.0f : biomeFogDistance / weightBiomeFog;

		float fogDistance = (biomeFogDistance * 240.0f + event.getFarPlaneDistance() * weightDefault) / weightMixed;
		float fogDistanceScaleBiome = (0.1f * (1.0f - fogDistanceAvg) + 0.75f * fogDistanceAvg);
		float fogDistanceScale = (fogDistanceScaleBiome * weightBiomeFog + 0.75f * weightDefault) / weightMixed;
	
		float finalFogDistance = Math.min(fogDistance, event.getFarPlaneDistance());
		
		lastX = entity.posX;
		lastZ = entity.posZ;

		otgDidLastFogRender = true;
		// Render the fog
		if (event.getFogMode() < 0)
		{
			GL11.glFogf(GL11.GL_FOG_START, 0.0F);
			GL11.glFogf(GL11.GL_FOG_END, finalFogDistance);
		} else {
			GL11.glFogf(GL11.GL_FOG_START, finalFogDistance * fogDistanceScale);
			GL11.glFogf(GL11.GL_FOG_END, finalFogDistance);
		}
	}

	// Get the difference between the raw coordinate and block coordinate
	private double getDifference(double rawCoord, int blockCoord, int pos, int distance)
	{
		if (pos == -distance)
		{
			return 1.0f - (rawCoord - blockCoord);
		}
		else if (pos == distance)
		{
			return (rawCoord - blockCoord);
		}
		return -1.0f;
	}

	private void clearFogBlendCache()
	{
		fogBlendCacheValid = false;
		fogBlendWorld = null;
		fogBlendMinecraftWorld = null;
		fogBlendBiomeConfigs = null;
		cachedCenterBiomeConfig = null;
	}

	private boolean updateFogBlendCache(ForgeWorld forgeWorld, BiomeConfig[] biomeConfigs, int biomeConfigRevision, Entity entity, int blendDistance, int blockX, int blockZ)
	{
		long fogX = Double.doubleToLongBits(entity.posX);
		long fogZ = Double.doubleToLongBits(entity.posZ);
		World minecraftWorld = entity.getEntityWorld();
		if (
			fogBlendCacheValid
			&& fogBlendWorld == forgeWorld
			&& fogBlendMinecraftWorld == minecraftWorld
			&& fogBlendBiomeConfigs == biomeConfigs
			&& fogBlendBiomeConfigRevision == biomeConfigRevision
			&& fogBlendX == fogX
			&& fogBlendZ == fogZ
			&& fogBlendDistance == blendDistance
		)
		{
			return true;
		}

		float biomeFogDistance = 0.0F;
		float weightBiomeFog = 0.0F;
		double biomeFogRed = 0.0D;
		double biomeFogGreen = 0.0D;
		double biomeFogBlue = 0.0D;
		double biomeFogWeight = 0.0D;
		BiomeConfig centerBiomeConfig = null;
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(0, 0, 0);
		boolean hasMoved = !fogBlendCacheValid || entity.posX != lastX || entity.posZ != lastZ
			|| fogBlendWorld != forgeWorld || fogBlendMinecraftWorld != minecraftWorld
			|| fogBlendBiomeConfigs != biomeConfigs || fogBlendDistance != blendDistance;
		boolean fogFound = false;

		for (int x = -blendDistance; x <= blendDistance; ++x)
		{
			for (int z = -blendDistance; z <= blendDistance; ++z)
			{
				blockPos.setPos(blockX + x, 0, blockZ + z);
				BiomeConfig config = getBiomeConfig(forgeWorld, x + blendDistance, z + blendDistance, blockPos, hasMoved, biomeConfigs);

				if (config == null)
				{
					fogBlendCacheValid = false;
					return false;
				}

				if (x == 0 && z == 0)
				{
					centerBiomeConfig = config;
				}

				if (config.fogColor != 0x000000)
				{
					fogFound = true;
					float fogDensity = 1.0F - config.fogDensity;
					float densityWeight = 1.0F;
					double fogRed = (config.fogColor & 0xFF0000) >> 16;
					double fogGreen = (config.fogColor & 0x00FF00) >> 8;
					double fogBlue = config.fogColor & 0x0000FF;
					float fogWeight = 1.0F;
					double differenceX = getDifference(entity.posX, blockX, x, blendDistance);
					double differenceZ = getDifference(entity.posZ, blockZ, z, blendDistance);

					if (differenceX >= 0.0F)
					{
						fogDensity *= differenceX;
						densityWeight *= differenceX;
						fogRed *= differenceX;
						fogGreen *= differenceX;
						fogBlue *= differenceX;
						fogWeight *= differenceX;
					}

					if (differenceZ >= 0.0F)
					{
						fogDensity *= differenceZ;
						densityWeight *= differenceZ;
						fogRed *= differenceZ;
						fogGreen *= differenceZ;
						fogBlue *= differenceZ;
						fogWeight *= differenceZ;
					}

					biomeFogDistance += fogDensity;
					weightBiomeFog += densityWeight;
					biomeFogRed += fogRed;
					biomeFogGreen += fogGreen;
					biomeFogBlue += fogBlue;
					biomeFogWeight += fogWeight;
				}
			}
		}

		fogBlendWorld = forgeWorld;
		fogBlendMinecraftWorld = minecraftWorld;
		fogBlendBiomeConfigs = biomeConfigs;
		fogBlendBiomeConfigRevision = biomeConfigRevision;
		fogBlendX = fogX;
		fogBlendZ = fogZ;
		fogBlendBlockX = blockX;
		fogBlendBlockZ = blockZ;
		fogBlendDistance = blendDistance;
		cachedBiomeFogDistance = biomeFogDistance;
		cachedWeightBiomeFog = weightBiomeFog;
		cachedBiomeFogRed = biomeFogRed;
		cachedBiomeFogGreen = biomeFogGreen;
		cachedBiomeFogBlue = biomeFogBlue;
		cachedBiomeFogWeight = biomeFogWeight;
		cachedCenterBiomeConfig = centerBiomeConfig;
		cachedFogFound = fogFound;
		fogBlendCacheValid = true;

		return true;
	}

	// Blend the fog color
	@SideOnly(Side.CLIENT)
	private Vec3d blendFogColors(ForgeWorld forgeWorld, EntityLivingBase entity, float red, float green, float blue, double renderPartialTicks, BiomeConfig[] biomeConfigs, int biomeConfigRevision)
	{
		GameSettings settings = Minecraft.getMinecraft().gameSettings;
		int[] ranges = ForgeModContainer.blendRanges;
		int blendDistance = 6;

		if (settings.fancyGraphics && settings.renderDistanceChunks >= 0
				&& settings.renderDistanceChunks < ranges.length)
		{
			blendDistance = ranges[settings.renderDistanceChunks];
		}

		int blockX = (int) Math.floor(entity.posX);
		int blockZ = (int) Math.floor(entity.posZ);

		if (!updateFogBlendCache(forgeWorld, biomeConfigs, biomeConfigRevision, entity, blendDistance, blockX, blockZ))
		{
			return null;
		}

		double biomeFogRed = cachedBiomeFogRed;
		double biomeFogGreen = cachedBiomeFogGreen;
		double biomeFogBlue = cachedBiomeFogBlue;
		double biomeFogWeight = cachedBiomeFogWeight;
		BiomeConfig biomeConfig = cachedCenterBiomeConfig;

		if (biomeConfig == null || biomeFogWeight <= 0.0f || blendDistance <= 0.0f)
		{
			return new Vec3d(red, green, blue);
		}

		// Convert integer to float from 0-1
		biomeFogRed /= 255.0f;
		biomeFogGreen /= 255.0f;
		biomeFogBlue /= 255.0f;

		// Scale color based on world time
		float baseScale = 1.0f;

		float time = MathHelper.clamp(
			MathHelper.cos(forgeWorld.getWorld().getCelestialAngle((float) renderPartialTicks) * (float) Math.PI * 2.0F) * 2.0F + 0.5F, 0.0f, 1.0f
		);

		baseScale *= 1.0f - (1.0f - time) * biomeConfig.fogTimeWeight;

		// Adjust based on weather
		float rainStrength = forgeWorld.getWorld().getRainStrength((float) renderPartialTicks);
		float thunderStrength = forgeWorld.getWorld().getThunderStrength((float) renderPartialTicks);

		if (thunderStrength >= 0.0f)
		{
			baseScale *= Math.min(1.0f - thunderStrength * biomeConfig.fogThunderWeight, 1.0f - rainStrength * biomeConfig.fogRainWeight);
		}
		else if (rainStrength >= 0.0f)
		{
			baseScale *= 1.0f - rainStrength * biomeConfig.fogRainWeight;
		}

		biomeFogRed *= baseScale / biomeFogWeight;
		biomeFogGreen *= baseScale / biomeFogWeight;
		biomeFogBlue *= baseScale / biomeFogWeight;

		// Mix default fog and our fog
		double weightMixed = (blendDistance * 2) * (blendDistance * 2);
		double weightDefault = weightMixed - biomeFogWeight;

		double fogRed = (biomeFogRed * biomeFogWeight + red * weightDefault) / weightMixed;
		double fogGreen = (biomeFogGreen * biomeFogWeight + green * weightDefault) / weightMixed;
		double fogBlue = (biomeFogBlue * biomeFogWeight + blue * weightDefault) / weightMixed;

		return new Vec3d(fogRed, fogGreen, fogBlue);
	}

	String lastWorldName = "";
	// Get the biome config from the cache or freshly from the world if needed
	private BiomeConfig getBiomeConfig(ForgeWorld world, int x, int z, MutableBlockPos blockPos, boolean hasMoved, BiomeConfig[] biomeConfigs)
	{		
		short cachedId = biomeCache[x][z];
		if (cachedId != -1 && !hasMoved)
		{
			return biomeConfigs[cachedId];
		} else {
			Biome biome = world.getBiomeFromChunk(blockPos.getX(), blockPos.getZ());
			LocalBiome localBiome = biome != null ? world.getBiomeByNameOrNull(biome.getBiomeName()) : null;
            if (localBiome == null || localBiome.getBiomeConfig() == null)
            {
            	biomeCache[x][z] = (short) -1;
                return null;
            }

			biomeCache[x][z] = (short) localBiome.getIds().getOTGBiomeId();
			return localBiome.getBiomeConfig();
		}
	}
}
