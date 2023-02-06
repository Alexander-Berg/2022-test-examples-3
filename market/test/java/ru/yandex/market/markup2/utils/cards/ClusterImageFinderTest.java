package ru.yandex.market.markup2.utils.cards;

import com.google.common.util.concurrent.MoreExecutors;
import junit.framework.TestCase;
import ru.yandex.market.markup2.utils.image.ImageDownloaderService;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ClusterImageFinderTest extends TestCase {
    private class ImageDownloaderServiceMock extends ImageDownloaderService {
        private Map<String, BufferedImage> urlToImage;

        ImageDownloaderServiceMock(Map<String, BufferedImage> urlToImage) {
            super(0, 0, MoreExecutors.directExecutor());

            this.urlToImage = urlToImage;
        }

        @Override
        public BufferedImage download(URL url) throws IOException {
            BufferedImage bufferedImage = urlToImage.get(url.toString());
            if (bufferedImage == null) {
                throw new IOException();
            }

            return bufferedImage;
        }
    }

    public void testClusterImageFinder() throws MalformedURLException {
        ArrayList<String> urls = new ArrayList<>();
        urls.add("http://bad.com");
        urls.add("http://bad1.com");
        urls.add("bad3.com");
        urls.add("http://good.com");
        urls.add("http://bad4.com");
        urls.add("http://bad5.com");
        urls.add("");


        ArrayList<BufferedImage> images = new ArrayList<>();
        images.add(new BufferedImage(100, 100, 1));
        images.add(new BufferedImage(100, 100, 1));
        images.add(null);
        images.add(new BufferedImage(300, 400, 1));
        images.add(new BufferedImage(100, 100, 1));

        HashMap<String, BufferedImage> urlToImage = new HashMap<>();
        for (int i = 0; i < images.size(); i++) {
            urlToImage.put(urls.get(i), images.get(i));
        }
        ImageDownloaderServiceMock imageDownloaderMock = new ImageDownloaderServiceMock(urlToImage);

        ClusterImageFinder clusterImageFinder = new ClusterImageFinder(imageDownloaderMock);

        Optional<String> suitableImageForCluster = clusterImageFinder.findSuitableImageForCluster(urls);
        assertEquals("http://good.com", suitableImageForCluster.get());
    }

    public void testClusterImageFinderEmtpyInput() throws MalformedURLException {
        ClusterImageFinder clusterImageFinder = new ClusterImageFinder(null);

        Optional<String> suitableImageForCluster = clusterImageFinder.findSuitableImageForCluster(new LinkedList<>());
        assertFalse(suitableImageForCluster.isPresent());
    }

    public void testClusterImageFinderAllUrlsAreBroken() throws MalformedURLException {
        ArrayList<String> urls = new ArrayList<>();
        urls.add("http://bad.com");
        urls.add("http://bad1.com");


        ArrayList<BufferedImage> images = new ArrayList<>();
        images.add(null);
        images.add(null);

        HashMap<String, BufferedImage> urlToImage = new HashMap<>();
        for (int i = 0; i < images.size(); i++) {
            urlToImage.put(urls.get(i), images.get(i));
        }
        ImageDownloaderServiceMock imageDownloaderMock = new ImageDownloaderServiceMock(urlToImage);

        ClusterImageFinder clusterImageFinder = new ClusterImageFinder(imageDownloaderMock);

        Optional<String> suitableImageForCluster = clusterImageFinder.findSuitableImageForCluster(new LinkedList<>());
        assertFalse(suitableImageForCluster.isPresent());
    }
}
