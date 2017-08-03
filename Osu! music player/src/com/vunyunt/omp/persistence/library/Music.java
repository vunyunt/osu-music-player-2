package com.vunyunt.omp.persistence.library;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.vunyunt.omp.persistence.PersistenceManager;

/**
 * Represents a music track.
 *
 * @author vun
 *
 */
public class Music
{
	public static final String FIELD_ID = "Id";
	public static final String FIELD_FOLDER = "Folder";
	public static final String FIELD_BEATMAP_FILENAME = "BeatmapFilename";

	/**
	 * ID of music is composed of the beatmap ID (digits in the beginning of the
	 * name of a beatmap folder) + audio file name.
	 * Altough this can be constructed from Name and Filepath, it might change
	 * in the future.
	 */
	private String mId;

	/**
	 * Folder of the beatmap (of this Music), using the Songs folder as base
	 */
	private String mFolder;

	/**
	 *File path to the beatmap file, using the beatmap folder as base
	 */
	private String mBeatmapFileName;

	/**
	 * All the metadata of the beatmap
	 */
	private Map<String, String> mMetadata;

	/**
	 * Constructs a new music object
	 *
	 * @param id				{@link Music#mId}
	 * @param folder			{@link Music#mFolder}
	 * @param beatmapFileName	{@link Music#mBeatmapFileName}
	 * @param metadata			{@link Music#mMetadata}
	 */
	public Music(String id, String folder, String beatmapFileName, Map<String, String> metadata)
	{
		this.construct(id, folder, beatmapFileName, metadata);
	}

	/**
	 * Constructs a new music object
	 * The deserialization operation for the map created by {@link Music#serializeToMap()}
	 *
	 * @param data	Data used to construct this music object, containing all the parameter in {@link Music#Music(String, String, String, Map)}
	 * 				and all metadata.
	 * 				Field name defined as constants of this class as FIELD_*
	 */
	public Music(Map<String, String> data)
	{
		String id = popFromMap(data, FIELD_ID);
		String folder = popFromMap(data, FIELD_FOLDER);
		String beatmapFileName = popFromMap(data, FIELD_BEATMAP_FILENAME);

		this.construct(id, folder, beatmapFileName, data);
	}

	/**
	 * Serialize this music to a map
	 * This music object can be reconstructed using the output map via {@link Music#Music(Map)}
	 */
	public Map<String, String> serializeToMap()
	{
		Map<String, String> serialized = new HashMap<String, String>(mMetadata);
		serialized.put(FIELD_ID, getId());
		serialized.put(FIELD_FOLDER, getFolder());
		serialized.put(FIELD_BEATMAP_FILENAME, getBeatmapFileName());

		return serialized;
	}

	private void construct(String id, String folder, String beatmapFileName, Map<String, String> metadata)
	{
		mId = id;
		mFolder = folder;
		mBeatmapFileName = beatmapFileName;
		mMetadata = metadata;
	}

	public static String popFromMap(Map<String, String> map, String key)
	{
		String item = map.get(key);
		map.remove(key);
		return item;
	}

	/**
	 * @see Music#mId
	 */
	public String getId()
	{
		return mId;
	}

	/**
	 * Name of the music, to be displayed to the user
	 */
	public String getName()
	{
		return getArtist() + " - " + mMetadata.get("Title") + " (" + getAudioFileName() + ")";
	}

	/**
	 * @see Artist of the music
	 */
	public String getArtist()
	{
		return mMetadata.get("Artist");
	}

	/**
	 * Audio file name of the music
	 */
	public String getAudioFileName()
	{
		return mMetadata.get("AudioFilename");
	}

	/**
	 * Root folder of the beatmap
	 */
	public String getFolder()
	{
		return mFolder;
	}

	/**
	 * File name of the beatmap
	 */
	public String getBeatmapFileName()
	{
		return mBeatmapFileName;
	}

	/**
	 * Gets the audio file this Music represents
	 */
	public File getAudioFile(PersistenceManager context)
	{
		return getFile(context, getAudioFileName());
	}

	/**
	 * Gets the beatmap file for this music
	 */
	public File getBeatmapFile(PersistenceManager context)
	{
		return getFile(context, mBeatmapFileName);
	}

	/**
	 * Gets a file in the beatmap folder
	 */
	public File getFile(PersistenceManager context, String fileName)
	{
		if(fileName == null)
		{
			fileName = "";
		}

		return new File(new File(context.getMusicsBasePath(), mFolder), fileName);
	}
}
