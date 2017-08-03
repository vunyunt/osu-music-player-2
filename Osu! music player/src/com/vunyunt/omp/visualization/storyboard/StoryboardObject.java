package com.vunyunt.omp.visualization.storyboard;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public abstract class StoryboardObject
{
	/**
	 * Time at which the object start to be drawn
	 */
	private int mTimeStart;

	/**
	 * Time at which the object is stopped to be drawn
	 */
	private int mTimeEnd;

	/**
	 * The storyboard visualization engine that hosts this storyboard object
	 */
	private StoryboardVE mStoryboardVE;

	public StoryboardObject(StoryboardVE storyboardVE, int timeStart, int timeEnd)
	{
		mTimeStart = timeStart;
		mTimeEnd = timeEnd;
		mStoryboardVE = storyboardVE;
	}

	/**
	 * Draws the object
	 *
	 * @param gc		GraphicsContext for this object to draw on
	 * @param timeMilli	Time since (music) played, in milliseconds
	 */
	public abstract void draw(GraphicsContext gc, int timeMilli);

	/**
	 * @see StoryboardObject#mTimeStart
	 */
	public int getTimeStart()
	{
		return mTimeStart;
	}

	/**
	 * @see StoryboardObject#mTimeStart
	 */
	public void setTimeStart(int mTimeStart)
	{
		this.mTimeStart = mTimeStart;
	}

	/**
	 * @see StoryboardObject#mTimeEnd
	 */
	public int getTimeEnd()
	{
		return mTimeEnd;
	}

	/**
	 * @see StoryboardObject#mTimeEnd
	 */
	public void setTimeEnd(int mTimeEnd)
	{
		this.mTimeEnd = mTimeEnd;
	}

	/**
	 * Fit an image at center on the given graphics context
	 * Internally it calls {@link StoryboardObject#drawAtCenter(GraphicsContext, Image, double, double)}
	 */
	public void fitAtCenter(GraphicsContext gc, Image image)
	{
		double w = image.getWidth();
		double h = image.getHeight();

		double scale = mStoryboardVE.getFittingScale(w, h, mStoryboardVE.getPlayAreaWidth(), mStoryboardVE.getPlayAreaHeight());

		drawAtCenter(gc, image, w * scale, h * scale);
	}

	/**
	 * Draws an image on the given graphics context
	 * The graphics context is assumed to be already transformed by
	 * {@link StoryboardVE#playAreaTransform(Canvas)}
	 */
	public void drawAtCenter(GraphicsContext gc, Image image, double width, double height)
	{
		double left = (mStoryboardVE.getPlayAreaWidth() - width) / 2;
		double top = (mStoryboardVE.getPlayAreaHeight() - height) / 2;
		gc.drawImage(image, left, top, width, height);
	}
}
