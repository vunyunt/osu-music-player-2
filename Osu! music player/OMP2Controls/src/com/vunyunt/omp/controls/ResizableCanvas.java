package com.vunyunt.omp.controls;

import javafx.scene.canvas.Canvas;

public class ResizableCanvas extends Canvas
{
	@Override
	public boolean isResizable()
	{
		return true;
	}

	@Override
	public void resize(double width, double height)
	{
		super.setWidth(width);
		super.setHeight(height);
	}
}
