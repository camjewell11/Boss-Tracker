package com.camjewell.bosstracker.util;

import java.awt.image.BufferedImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class ItemIconsTest
{
	private static final int OPAQUE = 0xFF808080;

	@Test
	public void anItemAgainstTheLeftEdgeMovesToTheMiddle()
	{
		// 29 wide from x=1 on a 36 wide canvas, like the Rondache: 1 spare on the left, 6 on the right.
		BufferedImage image = canvas();
		fill(image, 1, 4, 29, 25);

		BufferedImage centred = ItemIcons.centred(image);

		// 7 spare columns: 3 on the left, 4 on the right.
		assertEquals(0, centred.getRGB(2, 15) >>> 24);
		assertEquals(OPAQUE, centred.getRGB(3, 15));
		assertEquals(OPAQUE, centred.getRGB(31, 15));
		assertEquals(0, centred.getRGB(32, 15) >>> 24);
	}

	@Test
	public void aCentredItemIsLeftAlone()
	{
		BufferedImage image = canvas();
		fill(image, 4, 3, 28, 26);

		assertSame(image, ItemIcons.centred(image));
	}

	@Test
	public void anEmptyImageIsLeftAlone()
	{
		BufferedImage image = canvas();

		assertSame(image, ItemIcons.centred(image));
	}

	private static BufferedImage canvas()
	{
		return new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
	}

	private static void fill(BufferedImage image, int x, int y, int width, int height)
	{
		for (int i = x; i < x + width; i++)
		{
			for (int j = y; j < y + height; j++)
			{
				image.setRGB(i, j, OPAQUE);
			}
		}
	}
}
