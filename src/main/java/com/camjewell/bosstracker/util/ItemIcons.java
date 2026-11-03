package com.camjewell.bosstracker.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Item sprites don't always sit in the middle of their 36x32 canvas: the Rondache, for one, is
 * drawn against its left edge, so it looks off-centre alone in a tile even though the canvas is
 * centred.
 */
public final class ItemIcons
{
	private ItemIcons()
	{
	}

	/**
	 * The same image with its visible pixels moved to the middle of the canvas, or the image
	 * itself if they're already there (or there are none). Only for images without a quantity,
	 * since the number in the corner would count toward what's centred.
	 */
	public static BufferedImage centred(BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		int minX = width;
		int minY = height;
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}

		if (maxX < 0)
		{
			return image;
		}

		int dx = (width - (maxX - minX + 1)) / 2 - minX;
		int dy = (height - (maxY - minY + 1)) / 2 - minY;
		if (dx == 0 && dy == 0)
		{
			return image;
		}

		BufferedImage centred = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = centred.createGraphics();
		graphics.drawImage(image, dx, dy, null);
		graphics.dispose();
		return centred;
	}
}
