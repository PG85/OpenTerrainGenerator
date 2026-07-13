package com.pg85.otg;

import com.pg85.otg.config.PluginConfig;
import com.pg85.otg.config.io.FileSettingsReader;
import com.pg85.otg.config.io.FileSettingsWriter;
import com.pg85.otg.config.settings.preset.PresetInfo;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.customobject.CustomObjectManager;
import com.pg85.otg.customobject.config.CustomObjectResourcesManager;
import com.pg85.otg.customobject.structures.CustomStructureCache;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.interfaces.IModLoadedChecker;
import com.pg85.otg.interfaces.IPluginConfig;
import com.pg85.otg.presets.LocalPresetLoader;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Implemented and provided by the platform-specific layer on app start and accessed via OTG.startEngine()/OTG.getEngine(),
 * this class holds any objects and methods used during an app session.
 * 
 * Constructor parameters are platform-specific implementations of wrapper classes, such as a logger, material reader, 
 * preset loader etc. Implement these to provide support for a platform (Forge, Spigot etc).
 *  
 * OTGEngine.onStart() should be called on mod/plugin start, creates all OTG files and folders and registers all presets 
 * and biomes via the platform-specific preset loader provided as a constructor parameter. 
 */
public abstract class OTGEngine
{
	// Classes implemented/provided by the platform-specific layer.
	
	@Getter
    protected final LocalPresetLoader presetLoader;
	@Getter
    protected final ILogger logger;
	@Getter
    private final IModLoadedChecker modLoadedChecker;

	// Common classes
	
	private final Path otgRootFolder;
	@Getter
    private final Path globalObjectsFolder;
	protected PluginConfig pluginConfig;

    // Create manager objects

    @Getter
    private final CustomObjectResourcesManager customObjectResourcesManager = new CustomObjectResourcesManager();
	@Getter
    private CustomObjectManager customObjectManager;
	
	protected OTGEngine(ILogger logger, Path otgRootFolder, IModLoadedChecker modLoadedChecker, LocalPresetLoader presetLoader)
	{
		this.logger = logger;
		this.otgRootFolder = otgRootFolder;
		this.globalObjectsFolder = otgRootFolder.resolve(Constants.GLOBAL_OBJECTS_FOLDER);
		this.presetLoader = presetLoader;
		this.modLoadedChecker = modLoadedChecker;
	}
	
	// Get jar file that's running OTG, where we will find our default preset
	public abstract File getJarFile();
	
	// Startup / shutdown

	public void onStart()
	{
		// Load plugin config

		File pluginConfigFile = Paths.get(getOTGRootFolder().toString(), Constants.PluginConfigFilename).toFile();
		this.pluginConfig = new PluginConfig(
				FileSettingsReader.read(Constants.PluginConfigFilename, pluginConfigFile),
				pluginConfigFile.toPath()
		);
		this.logger.init(
			this.pluginConfig.getLogLevel().getLevel(), 
			this.pluginConfig.logCustomObjects(), 
			this.pluginConfig.logStructurePlotting(), 
			this.pluginConfig.logConfigs(),
			this.pluginConfig.logBiomeRegistry(),
			this.pluginConfig.logPerformance(),
			this.pluginConfig.logDecoration(),
			this.pluginConfig.logMobs(),
			this.pluginConfig.logPresets()
		);
		FileSettingsWriter.writeToFile(this.pluginConfig.getSettingsAsMap(), pluginConfigFile, this.pluginConfig.getSettingsMode());

		// Create OTG folders

		File presetsDir = Paths.get(getOTGRootFolder().toString(), Constants.PRESETS_FOLDER).toFile();
		if(!presetsDir.exists())
		{
			presetsDir.mkdirs();
		}

		File dimensionConfigsDir = Paths.get(getOTGRootFolder().toString(), Constants.DIMENSION_CONFIGS_FOLDER).toFile();
		if(!dimensionConfigsDir.exists())
		{
			dimensionConfigsDir.mkdirs();
		}

		File globalObjectsDir = this.globalObjectsFolder.toFile();
		if(!globalObjectsDir.exists())
		{
			globalObjectsDir.mkdirs();
		}

		unpackDefaultPresetAndExamples(presetsDir);

        this.customObjectManager = new CustomObjectManager(
			getPluginConfig().getDeveloperModeEnabled(),
			this.otgRootFolder, 
			getPresetsDirectory(), 
			this.customObjectResourcesManager
		);

		// Load presets

		this.presetLoader.loadPresetsFromDisk();
	}

	private void unpackDefaultPresetAndExamples(File presetsDir)
	{
		JarFile jarFile = openModJar();
		if (jarFile == null)
		{
			this.logger.warn(LogCategory.MAIN, "Skipping default preset and example dimension configs unpack (copy them manually from the resources folder for development).");
			return;
		}

		try
		{
			if (shouldSkipDefaultPresetUnpack(presetsDir, jarFile))
			{
				this.logger.info(LogCategory.MAIN, "Default preset is up-to-date, skipping unpack.");
				return;
			}
			unpackJarEntries(jarFile);
		}
		finally
		{
			try
			{
				jarFile.close();
			}
			catch (IOException e)
			{
				this.logger.printStackTrace(LogLevel.ERROR, LogCategory.MAIN, e);
			}
		}
	}

	/**
	 * Opens the jar file that's running OTG.
	 * @return the jar, or null if it couldn't be found or opened
	 */
	private JarFile openModJar()
	{
		File jarFileLocation = getJarFile();
		if (jarFileLocation == null || !jarFileLocation.exists())
		{
			this.logger.warn(LogCategory.MAIN, "Could not find root jar file, location: " + jarFileLocation);
			return null;
		}
		try
		{
			return new JarFile(jarFileLocation);
		}
		catch (IOException e)
		{
			this.logger.warn(LogCategory.MAIN, "Could not open root jar file " + jarFileLocation + ": " + e.getMessage());
			return null;
		}
	}

	/**
	 * Checks whether the default preset on disk is already the same version or
	 * newer than the one bundled in the jar.
	 */
	private boolean shouldSkipDefaultPresetUnpack(File presetsDir, JarFile jarFile)
	{
		File presetDir = new File(presetsDir, Constants.DEFAULT_PRESET_NAME);
		if (!presetDir.exists())
		{
			return false;
		}

		File presetConfigFile = new File(presetDir, Constants.PRESET_CONFIG_FILE);
		if (!presetConfigFile.exists())
		{
			return false;
		}

		try (BufferedReader existingConfigReader = new BufferedReader(new FileReader(presetConfigFile)))
		{
			int existingMajorVer = parseMajorVersion(existingConfigReader);
			int existingMinorVer = parseMinorVersion(existingConfigReader);

			int bundledMajorVer = 0;
			int bundledMinorVer = 0;

			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements())
			{
				JarEntry jarEntry = entries.nextElement();
				if (jarEntry.getName().contains(Constants.DEFAULT_PRESET_NAME + "/" + Constants.PRESET_CONFIG_FILE))
				{
					try (BufferedReader jarConfigReader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(jarEntry))))
					{
						bundledMajorVer = parseMajorVersion(jarConfigReader);
						bundledMinorVer = parseMinorVersion(jarConfigReader);
					}
					break;
				}
			}

			// Skip if the existing version is the same or newer
			return (bundledMajorVer < existingMajorVer) ||
				(bundledMajorVer == existingMajorVer && bundledMinorVer <= existingMinorVer);
		}
		catch (IOException e)
		{
			this.logger.printStackTrace(LogLevel.ERROR, LogCategory.MAIN, e);
			return false;
		}
	}

	/**
	 * Extracts the default preset and the example dimension configs from the
	 * jar into the OTG root directory. Existing files are overwritten.
	 */
	private void unpackJarEntries(JarFile jarFile)
	{
		try
		{
			String rootDir = getOTGRootFolder().toString();
			String defaultPresetPath = "resources/" + Constants.PRESETS_FOLDER + "/" + Constants.DEFAULT_PRESET_NAME + "/";
			String dimensionConfigsPath = "resources/" + Constants.DIMENSION_CONFIGS_FOLDER + "/";
			Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements())
			{
				JarEntry entry = entries.nextElement();
				if (
					entry.getName().startsWith(dimensionConfigsPath) ||
					entry.getName().startsWith(defaultPresetPath)
				)
				{
					// Strip the leading "resources/"
					File file = new File(rootDir + File.separator + (entry.getName().substring(10)));

					if (entry.isDirectory())
					{
						file.mkdirs();
					} else {
						file.createNewFile();
						try (
							java.io.InputStream is = jarFile.getInputStream(entry);
							FileOutputStream fos = new FileOutputStream(file)
						)
						{
							byte[] byteArray = new byte[4096];
							int i;
							while ((i = is.read(byteArray)) > 0)
							{
								fos.write(byteArray, 0, i);
							}
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			this.logger.printStackTrace(LogLevel.ERROR, LogCategory.MAIN, e);
		}
	}
	
	private int parseMajorVersion(BufferedReader reader) throws IOException
	{
		return parseVersion(reader, PresetInfo.MAJOR_VERSION.getName());
	}
	
	private int parseMinorVersion(BufferedReader reader) throws IOException
	{
		return parseVersion(reader, PresetInfo.MINOR_VERSION.getName());
	}
	
	private int parseVersion(BufferedReader reader, String name) throws IOException
	{
		int version = -1;
		String line;
		// Filter out the line with Version in it
		while ((line = reader.readLine()) != null)
		{
			if (line.contains(name))
			{
				break;
			}
		}
		if (line != null)
		{
			String v = line.split(":")[1];
			v = v.trim();
			version = Integer.parseInt(v);
		}
		return version;
	}

	public void onShutdown()
	{
		// Shutdown all loaders
		this.customObjectManager.shutdown();
	}

    // OTG Configs
	
	public IPluginConfig getPluginConfig()
	{
		return this.pluginConfig;
	}

	// OTG dirs

	public Path getOTGRootFolder()
	{
		return this.otgRootFolder;
	}

    public Path getPresetsDirectory()
	{
		return Paths.get(this.getOTGRootFolder().toString(), Constants.PRESETS_FOLDER);
	}

	// Logging

    // Builders/Factories
	
	public CustomStructureCache createCustomStructureCache(String presetFolderName, Path worldSavepath, long worldSeed, boolean isBo4Enabled)
	{
		// TODO: ModLoadedChecker
		return new CustomStructureCache(
			presetFolderName, 
			worldSavepath, 
			worldSeed, 
			isBo4Enabled, 
			getOTGRootFolder()
		);
	}
}
