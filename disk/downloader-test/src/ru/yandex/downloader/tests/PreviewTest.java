package ru.yandex.downloader.tests;

import org.junit.Test;
import ru.yandex.downloader.TestData;
import ru.yandex.downloader.url.BaseUrlParams;
import ru.yandex.downloader.url.MulcaTargetId;
import ru.yandex.downloader.url.UrlCreator;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * @author akirakozov
 */
public class PreviewTest extends DownloaderTestBase {
    private static final int MAX_SIZE = 600;

    @Test
    public void getPreview1() throws Exception {
        String url = UrlUtils.addParameter(createBaseUrl(), "size", "100x200", "crop", "1");
        BufferedImage img = ImageIO.read(new URL(url));

        Assert.equals(100, img.getWidth());
        Assert.equals(200, img.getHeight());
    }

    @Test
    public void getPreview2() throws Exception {
        String url = UrlUtils.addParameter(createBaseUrl(), "size", "200x200", "crop", "0");
        BufferedImage img = ImageIO.read(new URL(url));

        Assert.equals(200, img.getWidth());
        Assert.equals(150, img.getHeight());
    }

    @Test
    public void getAlbumPreview() throws Exception {
        String url = UrlUtils.addParameter(createBaseUrl(), "user_name", "user", "album_name", "First album");
        BufferedImage img = ImageIO.read(new URL(url));

        Assert.equals(1200, img.getWidth());
        Assert.equals(630, img.getHeight());
    }

    // Random size of preview is used to avoid caching on downloader host
    @Test
    public void getRandomSizePreview() throws Exception {
        int width = Random2.R.nextInt(MAX_SIZE);
        int height = Random2.R.nextInt(MAX_SIZE);
        String size = width + "x" + height;
        String url = UrlUtils.addParameter(createBaseUrl(), "size", size, "crop", "1");
        BufferedImage img = ImageIO.read(new URL(url));

        Assert.equals(width, img.getWidth());
        Assert.equals(height, img.getHeight());
    }

    private String createBaseUrl() {
        BaseUrlParams params = new BaseUrlParams();
        params.fileName = "result.png";
        params.contentType = "image/png";
        params.targetRef = new MulcaTargetId(TestData.IMAGE_PNG_STID);

        return UrlCreator.createPreviewUrl(params);
    }
}
