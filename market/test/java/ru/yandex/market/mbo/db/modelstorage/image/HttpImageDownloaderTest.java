package ru.yandex.market.mbo.db.modelstorage.image;

import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class HttpImageDownloaderTest {

    private static final byte[] JPEG_PREFIX = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] PNG_PREFIX = new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    private static final byte[] UNKNOWN_PREFIX = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB};
    private static final String URL = "http://url";
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String PNG_MIME_TYPE = "image/png";

    private HttpImageDownloader downloader;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private HttpEntity entity;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        downloader = new DirectImageDownloader(null) {
            @Override
            protected CloseableHttpClient initClient(String userAgent) {
                return httpClient;
            }
        };
    }

    @Test
    public void testErrorStatus() throws IOException {
        when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(HttpVersion.HTTP_1_1, 500, "Internal error"));

        exception.expect(RuntimeException.class);
        downloader.downloadImage(URL);
    }

    @Test
    public void testJpegContent() throws IOException {
        mockSuccessResponse(JPEG_PREFIX);

        ImageData data = downloader.downloadImage(URL);
        assertEquals(JPEG_MIME_TYPE, data.getContentType());
        assertArrayEquals(JPEG_PREFIX, data.getBytes());
    }


    @Test
    public void testPngContent() throws IOException {
        mockSuccessResponse(PNG_PREFIX);

        ImageData data = downloader.downloadImage(URL);
        assertEquals(PNG_MIME_TYPE, data.getContentType());
        assertArrayEquals(PNG_PREFIX, data.getBytes());
    }

    @Test
    public void testUnknownContent() throws IOException {
        mockSuccessResponse(UNKNOWN_PREFIX);

        exception.expect(RuntimeException.class);
        downloader.downloadImage(URL);
    }

    @Test
    public void testUnknownContentCTPresent() throws IOException {
        mockSuccessResponse(UNKNOWN_PREFIX);
        when(entity.getContentType()).thenReturn(new BasicHeader("Content-type", JPEG_MIME_TYPE));

        ImageData data = downloader.downloadImage(URL);

        assertEquals(JPEG_MIME_TYPE, data.getContentType());
        assertArrayEquals(UNKNOWN_PREFIX, data.getBytes());
    }


    private void mockSuccessResponse(byte[] content) throws IOException {
        when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));

        when(httpResponse.getEntity()).thenReturn(entity);

        when(entity.getContent()).thenReturn(new ByteArrayInputStream(content));
    }
}
