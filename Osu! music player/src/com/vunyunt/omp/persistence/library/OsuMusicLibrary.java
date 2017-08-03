package com.vunyunt.omp.persistence.library;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

import com.vunyunt.omp.persistence.AppConfig;
import com.vunyunt.omp.persistence.PersistenceManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class OsuMusicLibrary
{
	private static final Logger LOGGER = Logger.getLogger(OsuMusicLibrary.class);

	private String mOsuPath;
	private String mSongsPath;

	private File mSongsFolder;
	private MusicIndex mIndex;

	private ObservableList<Music> mMusics;

	private AppConfig mAppConfig = PersistenceManager.getInstance().getAppConfig();

	/**
	 * Constructs a music library from the given Osu! path (Installation folder of Osu!)
	 *
	 * @param osuPath Installation folder of Osu!
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public OsuMusicLibrary(String osuPath) throws IllegalArgumentException, IOException
	{
		mMusics = FXCollections.observableArrayList();

		mOsuPath = osuPath;
		StringBuilder songsPathBuilder = new StringBuilder(mOsuPath);
		if(!(mOsuPath.endsWith("/") || mOsuPath.endsWith("\\")))
		{
			songsPathBuilder.append("/");
		}
		songsPathBuilder.append("Songs/");
		mSongsPath = songsPathBuilder.toString();

		// Checks if the Songs folder exists
		// The directory is considered not to be a valid Osu! path otherwise.
		mSongsFolder = new File(mSongsPath);
		if(!(mSongsFolder.exists() && mSongsFolder.isDirectory()))
		{
			throw new IllegalArgumentException("Unable to find Songs folder in the given Osu! path.");
		}

		LOGGER.info("Initializing music index...");
		mIndex = new MusicIndex("./musicindex/");

		// Synchronizes available musics and the music index in a background thread
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				LOGGER.info("Importing all music in Songs folder...");
				loadMusics(mIndex);
			}
		});
		t.start();
	}

	/**
	 * Load and synchronize musics between the index and the Osu! path
	 */
	private void loadMusics(MusicIndex index)
	{
		if(mAppConfig.clearLucene)
		{
			mAppConfig.clearLucene = false;
			try
			{
				index.clear();
			}
			catch (IOException e)
			{
				LOGGER.error("Unable to clear index");
				LOGGER.debug(e.getMessage());
			}
			PersistenceManager.getInstance().saveConfig();
		}
		loadIndex(index);
		importMusics(index);
	}

	/**
	 * Loads all music from the given index
	 * Also removes deleted musics
	 */
	private void loadIndex(MusicIndex index)
	{
		try
		{
			LOGGER.info("Loading all musics from index...");
			List<Music> musics = index.getAllMusics();
			PersistenceManager pm = PersistenceManager.getInstance();
			for (Music music : musics)
			{
				if(music.getAudioFile(pm).exists())
				{
					Platform.runLater(new Runnable()
					{
						@Override
						public void run()
						{
							mMusics.add(music);
						}
					});
				}
				else
				{
					mIndex.remove(music);
				}
			}
		}
		catch (IOException e)
		{
			LOGGER.error("Unable to load musics from the index");
			LOGGER.debug(e.getClass().getName() + " - " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Imports all music from the given Osu! path into the index and music list
	 *
	 * @throws IOException
	 */
	private void importMusics(MusicIndex index)
	{
		// List all directories in the songs folder
		String directories[] = mSongsFolder.list(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return new File(dir, name).isDirectory();
			}
		});

		for (String dir : directories)
		{
			// List all beatmaps in a directories
			// Each difficulty is stored in a separate beatmap
			// Since each difficulty can specify its own audio file,
			// All difficulty needs to be considered.
			File dirFile = new File(mSongsFolder, dir);
			String beatmaps[] = dirFile.list(new FilenameFilter()
			{
				@Override
				public boolean accept(File dir, String name)
				{
					return name.endsWith(".osu");
				}
			});

			// Get MapSet ID and name
			String dirnameData[] = dir.split(" ", 2);
			String mapsetId = dirnameData[0];
			//String mapsetName = dirnameData.length == 2? dirnameData[1] : "";

			// For each beatmap, get the audio file path from the AudioFilename field
			for (String beatmap : beatmaps)
			{
				File beatmapFile = new File(dirFile, beatmap);

				try // (BufferedReader br = new BufferedReader(new FileReader(beatmapFile)))
				{
					LineIterator it = FileUtils.lineIterator(beatmapFile);
					String line;

					// Iterate over the lines to read and process necessary informations
					// Once the amount of section is reached (i.e. all necessary informations
					// are read), the loop is broke.
					int sectionToProcess = 2;
					int processedSection = 0;

					Map<String, String> metadata = new HashMap<String, String>();

					while(it.hasNext() && (line = it.next()) != null && processedSection < sectionToProcess)
					{
						if(line.startsWith("["))
						{
							++processedSection;
							switch(line.toLowerCase())
							{
							case "[general]":
							case "[metadata]":
								metadata.putAll(readSection(it));
								break;
							default:
								--processedSection;
								break;
							}
						}
					}

					// Adds the music into index if not already indexed
					Music m = new Music(mapsetId + metadata.get("AudioFilename"), dir, beatmap, metadata);
					if(index.addNewMusic(m))
					{
						Platform.runLater(new Runnable()
						{
							@Override
							public void run()
							{
								mMusics.add(m);
							}
						});

					}
				}
				catch (IOException e)
				{
					LOGGER.error("Unable to load beatmap " + beatmap);
					LOGGER.debug(e.getMessage());
				}
			}
		}

		try
		{
			mIndex.commit();
		}
		catch(IOException e)
		{
			LOGGER.error("Unable to commit to index");
			LOGGER.debug(e.getClass() + " " + e.getMessage());
		}
	}

	/**
	 * Reads the section LineIterator is currently at into a key value map
	 */
	private Map<String, String> readSection(LineIterator it)
	{
		Map<String, String> kvMap = new HashMap<String, String>();

		String line;
		while(it.hasNext() && !(line = it.next().trim()).startsWith("["))
		{
			if(line != null && line.length() > 0)
			{
				String tokens[] = line.split(":");
				if(tokens.length == 2)
				{
					kvMap.put(tokens[0].trim(), tokens[1].trim());
				}
			}
		}

		return kvMap;
	}

	/**
	 * Gets a observable list of the musics in this library
	 *
	 * @return The list is updated dynamically.
	 */
	public ObservableList<Music> getMusicsObservable()
	{
		return mMusics;
	}

	public String getSongsFolder()
	{
		return mSongsFolder.getAbsolutePath();
	}

	public ObservableList<Music> search(String query)
	{
		ObservableList<Music> results = FXCollections.observableArrayList();
		try
		{
			List<Music> hits = mIndex.search(query, 50);
			results.addAll(hits);
		}
		catch (IOException e)
		{
			LOGGER.error("Unable to search index.");
			LOGGER.debug(e.getClass().getName() + " - " + e.getMessage());
		}

		return results;
	}

	public void close()
	{
		mIndex.close();
	}
}
