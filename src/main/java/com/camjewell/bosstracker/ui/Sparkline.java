package com.camjewell.bosstracker.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * A small trend line, oldest value on the left, with the latest value marked. Scaled to the
 * values' own range so session-to-session changes are visible; the labels around it give the
 * actual numbers.
 */
class Sparkline extends JComponent
{
	private static final int HEIGHT = 28;
	private static final int PAD = 3;

	private final double[] values;
	private final Color lineColor;

	Sparkline(double[] values, Color lineColor)
	{
		this.values = values.clone();
		this.lineColor = lineColor;
		setOpaque(false);
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(100, HEIGHT);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, HEIGHT);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		if (values.length < 2)
		{
			return;
		}

		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (double value : values)
		{
			min = Math.min(min, value);
			max = Math.max(max, value);
		}
		double range = max - min;

		int width = getWidth() - 2 * PAD;
		int height = getHeight() - 2 * PAD;
		int[] xs = new int[values.length];
		int[] ys = new int[values.length];
		for (int i = 0; i < values.length; i++)
		{
			xs[i] = PAD + (int) Math.round((double) i * width / (values.length - 1));
			// A flat series sits mid-height rather than on the bottom edge.
			double t = range > 0 ? (values[i] - min) / range : 0.5;
			ys[i] = PAD + (int) Math.round((1 - t) * height);
		}

		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(lineColor);
		g.setStroke(new BasicStroke(1.5f));
		g.drawPolyline(xs, ys, values.length);
		int last = values.length - 1;
		g.fillOval(xs[last] - 2, ys[last] - 2, 5, 5);
		g.dispose();
	}
}
