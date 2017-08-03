package com.vunyunt.omp.visualization.storyboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import com.vunyunt.omp.persistence.PersistenceManager;
import com.vunyunt.omp.persistence.library.Music;
import com.vunyunt.omp.visualization.VisualizationEngine;

import javafx.animation.AnimationTimer;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Affine;
import javafx.util.Pair;

public class StoryboardVE extends VisualizationEngine
{
	public static final int PLAYAREA_WIDTH = 640;
	public static final int PLAYAREA_HEIGHT = 480;

	private int mPlayAreaWidth = PLAYAREA_WIDTH;
	private int mPlayAreaHeight = PLAYAREA_HEIGHT;

	private PersistenceManager mPersistenceManager = PersistenceManager.getInstance();
	private Music mMusic;
	private ConcurrentSkipListSet<StoryboardObject> mObjects;

	public StoryboardVE(Canvas canvas)
	{
		super(canvas);
		mObjects = new ConcurrentSkipListSet<>(new Comparator<StoryboardObject>()
		{
			@Override
			public int compare(StoryboardObject o1, StoryboardObject o2)
			{
				return o1.getTimeStart() - o2.getTimeStart();
			}
		});
	}

	/**
	 * Loads the storyboard associated with a music file to this storyboard visualization engine
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void loadStoryboard(Music music) throws FileNotFoundException, IOException
	{
		mObjects.clear();
		File beatmapFile = music.getBeatmapFile(mPersistenceManager);
		mMusic = music;

		// Process beatmap file line by line
		LineIterator it = FileUtils.lineIterator(beatmapFile);
		String line;

		// Find the [Events] section
		while(it.hasNext() && !(line = it.next().toLowerCase()).startsWith("[events]"));

		// Process the storyboard data, converting them into StoryboardObject
		// End the loop when another '[' occurs (which marks the next section)
		while(it.hasNext() && !(line = it.next().toLowerCase().trim()).startsWith("["))
		{
			// Skip the line if it is either empty or a comment
			if(line == null || line.isEmpty() || line.startsWith("//"))
			{
				continue;
			}

			String tokens[] = line.split(",");

			// Determine what is the line for
			switch(tokens[0].trim().toLowerCase())
			{
			case "0": // Background image
				mObjects.add(new SpriteStoryboardObject(this, getFileInFolder(tokens[2].replaceAll("\"", "")), 0, Integer.MAX_VALUE));
				break;
			}
		}

		it.close();
	}

	private File getFileInFolder(String fileName)
	{
		return mMusic.getFile(mPersistenceManager, fileName);
	}

	private AnimationTimer mAnimationTimer;

	/**
	 * Starts playback of storyboard
	 *
	 * @param timesMillis Time of the music playback in milliseconds
	 */
	public void play(ObservableDoubleValue timeMillis)
	{
		Canvas canvas = getCanvas();
		GraphicsContext gc = canvas.getGraphicsContext2D();

		if(mAnimationTimer == null)
		{
			AnimationTimer mAnimationTimer = new AnimationTimer()
			{
				@Override
				public void handle(long now)
				{
					gc.save();
					gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

					playAreaTransform(canvas);
					for (StoryboardObject storyboardObject : mObjects)
					{
						storyboardObject.draw(gc, (int) timeMillis.get());
					}

					gc.restore();
				}
			};
			mAnimationTimer.start();
		}
	}

	public int getPlayAreaWidth() { return mPlayAreaWidth; }
	public int getPlayAreaHeight() { return mPlayAreaHeight; }

	/**
	 * Apply a transformation to the canvas such that the playarea of 640x480 fits at the center
	 */
	private void playAreaTransform(Canvas c)
	{
		GraphicsContext gc = c.getGraphicsContext2D();

		double cWidth = c.getWidth();
		double cHeight = c.getHeight();

		double scale = getFittingScale(mPlayAreaWidth, mPlayAreaHeight, cWidth, cHeight);

		double translateX = (cWidth - (mPlayAreaWidth * scale)) / 2;
		double translateY = (cHeight - (mPlayAreaHeight * scale)) / 2;

		gc.translate(translateX, translateY);
		gc.scale(scale, scale);
	}

	/**
	 * Returns a scale that fits an "object" (x1, y1) to a "container" (x2, y2)
	 */
	public double getFittingScale(double x1, double y1, double x2, double y2)
	{
		double ratioX = x2 / x1;
		double ratioY = y2 / y1;
		return Math.min(ratioX, ratioY);
	}
}
