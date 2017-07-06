package com.vunyunt.omp.persistence;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.store.SleepingLockWrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.vunyunt.omp.persistence.library.OsuMusicLibrary;

import javafx.stage.DirectoryChooser;

public class PersistenceManager
{
	private static PersistenceManager mSingleton = new PersistenceManager();
	private static final Logger LOGGER = Logger.getLogger(PersistenceManager.class);

	public static PersistenceManager getInstance() { return mSingleton; }

	private File mConfigFile;
	private Gson mGson;

	private AppConfig mAppConfig;

	/**
	 * Music library in the given Osu! path
	 */
	private OsuMusicLibrary mMusicLibrary;

	private PersistenceManager()
	{
	}

	/**
	 * Initializes the configuration, including reading from or creating a default config
	 * file.
	 *
	 * @param osuPathSupplier Called when the osu path in configuration file is invalid
	 *
	 * @return True if initialization is successful. False otherwise.
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public boolean initialize(Supplier<File> osuPathSupplier) throws IllegalArgumentException, IOException
	{
		LOGGER.info("Initializing gson...");
		mGson = new Gson();

		LOGGER.info("Loading config file...");
		if(!initConfig()) { return false; }

		LOGGER.info("Initializing music library...");
		initMusicLibrary(osuPathSupplier);

		return true;
	}

	/**
	 * Initializes application configuration.
	 */
	private boolean initConfig()
	{
		mConfigFile = new File(AppConfig.APP_CONFIG_FILE_PATH);

		try
		{
			if(mConfigFile.exists())
			{
				LOGGER.info("Config file found. Loading config file...");
				mAppConfig = AppConfig.fromJson(mGson, FileUtils.readFileToString(mConfigFile, AppConfig.DEFAULT_CHARSET));
			}
			else
			{
				LOGGER.info("Config file not found. Creating a new config file...");
				mAppConfig = new AppConfig();
				String json = mGson.toJson(this);
				FileUtils.touch(mConfigFile);
				FileUtils.write(mConfigFile, json, mAppConfig.defaultCharset);
			}

			LOGGER.info("Config file loaded.");
			return true;
		}
		catch(IOException | JsonSyntaxException ex)
		{
			LOGGER.error(ex.getMessage());
			LOGGER.info("Unable to load config file.");
			return false;
		}
	}

	/**
	 * Initializes the music library
	 *
	 * @param osuPathSupplier Called when the osu path in configuration file is invalid
	 */
	private void initMusicLibrary(Supplier<File> osuPathSupplier) throws IllegalArgumentException, IOException
	{
		while(true)
		{
			try
			{
				mMusicLibrary = new OsuMusicLibrary(mAppConfig.osuPath);
				return;
			}
			catch (IllegalArgumentException e)
			{
				LOGGER.error("Unable to open the configured Osu! path.");
				LOGGER.debug(e.getMessage());

				LOGGER.info("User selecting a new path...");
				File f = osuPathSupplier.get();

				if(f == null)
				{
					throw new IllegalArgumentException("Supplied path is null.");
				}
				else
				{
					mAppConfig.osuPath = f.getAbsolutePath();
				}
			}
			catch(IOException e)
			{
				LOGGER.error("Unable to initialize the music index");
				LOGGER.debug(e.getMessage());
				e.printStackTrace();
				throw e;
			}
		}
	}

	/**
	 * {@link PersistenceManager#mMusicLibrary}
	 */
	public OsuMusicLibrary getMusicLibrary()
	{
		return mMusicLibrary;
	}

	/**
	 * Saves the configuration file.
	 *
	 * @return True if config file is saved successfully. False otherwise.
	 */
	public boolean saveConfig()
	{
		LOGGER.info("Saving config file...");
		try
		{
			String configJson = mAppConfig.toJson(mGson);
			FileUtils.write(mConfigFile, configJson, mAppConfig.defaultCharset);
			LOGGER.debug("Config JSON: " + configJson);
			LOGGER.info("Config file saved");
			return true;
		}
		catch(IOException ex)
		{
			LOGGER.error(ex.getMessage());
			LOGGER.info("Config file not saved");
			return false;
		}
	}

	public String getMusicsBasePath()
	{
		return mMusicLibrary.getSongsFolder();
	}

	public AppConfig getAppConfig()
	{
		return mAppConfig;
	}
}
