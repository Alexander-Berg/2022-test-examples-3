package ru.yandex.market.mbo.image;

import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.yandex.market.mbo.core.conf.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author sergtru
 * @since 28.11.2017
 */
public class MarketImageScalerTest {

    private MarketImageScaler imageScaler;

    @Before
    public void setUp() throws Exception {
        imageScaler = new MarketImageScaler();
        imageScaler.setEnvironment(Environment.DEVELOPMENT.name());
    }

    /**
     * ImageIO fails to import any jpeg images except RGB.
     * This test checks that we still import com.twelvemonkeys.imageio:imageio-jpeg
     * and cmyk images could be opened
     */
    @Test
    public void testImportYcbcrImage() throws IOException {
        try (InputStream sourceImage = inputStream("/mbo-core/cmyk.jpg")) {
            ImageIO.read(sourceImage); //ImageIO throws exception without plugins
        }
    }

    @Test
    public void testImportSvgImage() throws Exception {
        try (InputStream sourceImage = inputStream("/mbo-core/test-image.svg")) {
            ImageIO.read(sourceImage); //ImageIO throws exception without plugins
        }
    }

    @Test
    public void scaleSmallAlphaPng() throws IOException, MarketImageScaler.ScaleException {
        final int imageWidthAfterScale = 104;
        final int imageHeightAfterScale = 38;
        try (InputStream sourceImage = inputStream("/mbo-core/alpha.png")) {
            byte[] data = imageScaler.doScale(sourceImage, 100, new ImageUploadContext(), "image/png");
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            BufferedImage image = ImageIO.read(bis);
            assertEquals(imageWidthAfterScale, image.getWidth());
            assertEquals(imageHeightAfterScale, image.getHeight());
        }
    }

    @Test
    public void scaleBigJpeg() throws IOException, MarketImageScaler.ScaleException {
        try (InputStream sourceImage = inputStream("/mbo-core/test-image-alcatel.jpg");
             InputStream expectedImage = inputStream("/mbo-core/test-image-alcatel-scaled.jpg")) {

            byte[] data = imageScaler.doScale(sourceImage, 100, new ImageUploadContext(), "image/jpeg");

            assertThat(streamOf(data)).hasSameContentAs(expectedImage);
        }
    }

    private static InputStream inputStream(String path) {
        return MarketImageScalerTest.class.getResourceAsStream(path);
    }

    private static InputStream streamOf(byte[] data) {
        return new ByteArrayInputStream(data);
    }
}
