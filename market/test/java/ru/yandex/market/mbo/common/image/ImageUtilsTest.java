package ru.yandex.market.mbo.common.image;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author commince
 * @date 19.10.2018
 */
public class ImageUtilsTest {

    private static final int MAX_ALPHA_VALUE = 0xff;

    @Test
    public void guessImageFormat() throws Exception {
        assertEquals("gif", ImageUtils.guessImageFormat(picAsBytes("gif_pic.gif")));
        assertEquals("gif", ImageUtils.guessImageFormat(picAsBytes("gif_pic.jpg")));
        assertEquals("gif", ImageUtils.guessImageFormat(picAsBytes("gif_pic.png")));
        assertEquals("jpeg", ImageUtils.guessImageFormat(picAsBytes("jpg_pic.gif")));
        assertEquals("jpeg", ImageUtils.guessImageFormat(picAsBytes("jpg_pic.jpg")));
        assertEquals("jpeg", ImageUtils.guessImageFormat(picAsBytes("jpg_pic.png")));
        assertEquals("png", ImageUtils.guessImageFormat(picAsBytes("png_pic.gif")));
        assertEquals("png", ImageUtils.guessImageFormat(picAsBytes("png_pic.jpg")));
        assertEquals("png", ImageUtils.guessImageFormat(picAsBytes("png_pic.png")));
    }

    @Test
    public void imageColorPickerGetColor() throws IOException {
        assertFalse(hasTransparent(ImageUtils.getColorPicker(getBufferedImage("8bit.png"))));
        assertTrue(hasTransparent(ImageUtils.getColorPicker(getBufferedImage("8bit_transparent.png"))));
        assertFalse(hasTransparent(ImageUtils.getColorPicker(getBufferedImage("24bit.png"))));
        assertTrue(hasTransparent(ImageUtils.getColorPicker(getBufferedImage("32bit.png"))));
    }

    private boolean hasTransparent(ImageUtils.ColorPicker picker) {
        return picker.getColor(0, 0).getAlpha() != MAX_ALPHA_VALUE;
    }

    private static byte[] picAsBytes(String picFile) throws IOException {
        return IOUtils.toByteArray(ImageUtilsTest.class.getClassLoader().getResourceAsStream("images/" + picFile));
    }

    private static BufferedImage getBufferedImage(String picFile) throws IOException {
        try (InputStream sourceImage = ImageUtils.class.getClassLoader().getResourceAsStream("images/" + picFile)) {
            assert sourceImage != null;
            return ImageIO.read(sourceImage);
        }
    }
}
