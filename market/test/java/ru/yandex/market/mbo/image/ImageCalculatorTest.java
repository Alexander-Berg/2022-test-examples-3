package ru.yandex.market.mbo.image;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mbo.image.ImageCalculator.isBackgroundWhite;

/**
 * @author dergachevfv
 * @since 10/10/19
 */
public class ImageCalculatorTest {

    @Test
    public void testRepublish() throws IOException {
            assertTrue(isBackgroundWhite(getBufferedImage("alpha.png")));
    }

    private static BufferedImage getBufferedImage(String picFile) throws IOException {
        try (InputStream img = ImageCalculatorTest.class.getClassLoader().getResourceAsStream("mbo-core/" + picFile)) {
            assert img != null;
            return ImageIO.read(img);
        }
    }

    @Test
    public void testIsBackgroundWhite() throws IOException {

        String[] correctImages = {"32bit_alfa.png", "32bit_white_alfa.png", "24bit_white.png", "8bit_transparent.png",
            "8bit_white.png", "8bit_tr.png", "8bit_tr1.png", "8bit_tr2.png", "8bit_tr3.png", "8bit_tr4.png"};
        String[] incorrectImages = {"32bit_black_alfa.png", "24bit_black.png", "8bit_black.png"};

        for (String image : correctImages) {
            assertTrue("Background isn't white/transparent for " + image, isBackgroundWhite(getBufferedImage(image)));
        }
        for (String image : incorrectImages) {
            assertFalse("Background isn't black for " + image, isBackgroundWhite(getBufferedImage(image)));
        }
    }
}
