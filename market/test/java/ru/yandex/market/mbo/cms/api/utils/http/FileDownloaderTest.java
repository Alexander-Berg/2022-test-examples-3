package ru.yandex.market.mbo.cms.api.utils.http;

import java.io.IOException;
import java.net.URISyntaxException;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.utils.http.FileDownloader;

/**
 * @author ayratgdl
 * @date 15.03.18
 */
@SuppressWarnings("checkstyle:magicnumber")
@Issue("MBO-14811")
@Issue("MBO-14938")
public class FileDownloaderTest {
    private static final String TEST_URL = "http://example.com/file";
    private FileDownloader downloader;
    private MockUrlClient urlClient;

    @Before
    public void setUp() {
        urlClient = new MockUrlClient();
        downloader = new FileDownloader();
        downloader.setUrlClient(urlClient);
    }

    @Test
    public void downloadEmptyFile() throws IOException, URISyntaxException {
        urlClient.setDefaultResponse(new byte[0]);
        Assert.assertArrayEquals(new byte[0], downloader.download(TEST_URL));
        Assert.assertEquals(null, downloader.getMimeType());
    }

    @Test
    public void downloadHtmlFile() throws IOException, URISyntaxException {
        String content = "<!DOCTYPE html><html></html>";
        urlClient.setDefaultResponse(content.getBytes());
        Assert.assertArrayEquals(content.getBytes(), downloader.download(TEST_URL));
        Assert.assertEquals("text/html", downloader.getMimeType());
    }

    @Test(expected = IOException.class)
    public void downloadTextFileWhenAllowedOnlyHtml() throws IOException, URISyntaxException {
        String content = "A very interesting text.";
        urlClient.setDefaultResponse(content.getBytes());
        downloader.addAllowedMimeTypes("text/html");
        downloader.download(TEST_URL);
    }

    @Test
    public void downloadFileThatSmallerLimit() throws IOException, URISyntaxException {
        String content = "A very interesting text.";
        urlClient.setDefaultResponse(content.getBytes());
        downloader.setLimit(content.getBytes().length + 1);
        Assert.assertArrayEquals(content.getBytes(), downloader.download(TEST_URL));
    }

    @Test(expected = IOException.class)
    public void downloadFileThatBiggerLimit() throws IOException, URISyntaxException {
        String content = "A very interesting text.";
        urlClient.setDefaultResponse(content.getBytes());
        downloader.setLimit(content.getBytes().length - 1);
        downloader.download(TEST_URL);
    }

    @Test
    public void downloadFileThatEqualLimit() throws IOException, URISyntaxException {
        String content = "A very interesting text.";
        urlClient.setDefaultResponse(content.getBytes());
        downloader.setLimit(content.getBytes().length);
        Assert.assertArrayEquals(content.getBytes(), downloader.download(TEST_URL));
    }

    @Test
    public void detectJpegFile() throws IOException, URISyntaxException {
        byte[] beginningJpeg = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0};
        urlClient.setDefaultResponse(beginningJpeg);
        downloader.download(TEST_URL);
        Assert.assertEquals("image/jpeg", downloader.getMimeType());
    }

    @Test
    public void detectPngFile() throws IOException, URISyntaxException {
        byte[] beginningPng = new byte[]{
                (byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47, (byte) 0x0d, (byte) 0x0a, (byte) 0x1a, (byte) 0x0a};
        urlClient.setDefaultResponse(beginningPng);
        downloader.download(TEST_URL);
        Assert.assertEquals("image/png", downloader.getMimeType());
    }
}
