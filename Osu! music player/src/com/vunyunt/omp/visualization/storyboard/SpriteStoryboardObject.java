package com.vunyunt.omp.visualization.storyboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class SpriteStoryboardObject extends StoryboardObject
{
	private Image mImage;

	public SpriteStoryboardObject(StoryboardVE storyboardVE, File spriteFile, int timeStart, int timeEnd) throws FileNotFoundException
	{
		super(storyboardVE, timeStart, timeEnd);
		mImage = new Image(new FileInputStream(spriteFile));
	}

	@Override
	public void draw(GraphicsContext gc, int timeMillis)
	{
		super.fitAtCenter(gc, mImage);
	}
}
