package com.vunyunt.omp.persistence.library;

import java.io.File;

import com.vunyunt.omp.persistence.PersistenceManager;

/**
 * Represents a music track.
 *
 * @author vun
 *
 */
public class Music
{
	/**
	 * ID of music is composed of the beatmap ID (digits in the beginning of the
	 * name of a beatmap folder) + audio file name.
	 * Altough this can be constructed from Name and Filepath, it might change
	 * in the future.
	 */
	private String mId;

	/**
	 * Name of the beatmap, to be displayed on the music list view to the user.
	 */
	private String mName;

	/**
	 * Relative path to the audio file, using the Songs folder as the base.
	 */
	private String mFilePath;

	/**
	 * Instantiate a new Music object.
	 *
	 * @param id		{@link Music#mId}
	 * @param name		{@link Music#mName}
	 * @param filePath	{@link Music#mFilePath}
	 */
	public Music(String id, String name, String filePath)
	{
		mId = id;
		mName = name;
		mFilePath = filePath;
	}

	/**
	 * {@link Music#mId}
	 */
	public String getId()
	{
		return mId;
	}

	/**
	 * {@link Music#mName}
	 */
	public String getName()
	{
		return mName;
	}

	/**
	 * {@link Music#mFilePath}
	 */
	public String getFilePath()
	{
		return mFilePath;
	}
}
