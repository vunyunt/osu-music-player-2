package com.vunyunt.omp.visualization;

import java.io.File;

import com.vunyunt.omp.persistence.library.Music;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

public abstract class VisualizationEngine
{
	/**
	 * Pane where the visualization content is displayed
	 */
	private Canvas mCanvas;

	/**
	 * Source file for the music
	 */

	public VisualizationEngine(Canvas canvas)
	{
		mCanvas = canvas;
	}

	/**
	 * @return {@link VisualizationEngine#mCanvas}
	 */
	public Canvas getCanvas() { return mCanvas; }
}
