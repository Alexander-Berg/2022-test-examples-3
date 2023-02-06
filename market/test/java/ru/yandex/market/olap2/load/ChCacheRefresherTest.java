package ru.yandex.market.olap2.load;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ChCacheRefresher.class, URL.class})
public class ChCacheRefresherTest {

    public static final String URL = "https://mstat-ch-cache.vs.market.yandex.net/api/v1/cache_reset?table=table";
    private final ChCacheRefresher cache = new ChCacheRefresher();
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    private HttpURLConnection chConnection;

    @Before
    public void setup() throws Exception {
        URL url = PowerMockito.mock(URL.class);
        chConnection = PowerMockito.mock(HttpURLConnection.class);
        PowerMockito.whenNew(URL.class).withArguments(URL).thenReturn(url);
        PowerMockito.when(url.openConnection()).thenReturn(chConnection);
    }

    @Test
    public void testConnectError() throws Exception {
        PowerMockito.when(chConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
        PowerMockito.when(chConnection.getResponseMessage()).thenReturn("Smth bad happened");
        PowerMockito.when(chConnection.getErrorStream()).thenReturn(IOUtils.toInputStream("400 Bad Request", Charset.defaultCharset()));
        exceptionRule.expect(ChCacheRefresher.ChCacheResetException.class);
        exceptionRule.expectMessage("Smth bad happened Error details:\n 400 Bad Request");
        cache.connectToChCacheAndGetResponse("table", URL);
    }


    @Test
    public void testConnect404() throws Exception {
        PowerMockito.when(chConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        PowerMockito.when(chConnection.getResponseMessage()).thenReturn("Smth bad happened");
        PowerMockito.when(chConnection.getErrorStream()).thenReturn(IOUtils.toInputStream("Table not found", Charset.defaultCharset()));
        exceptionRule.expect(ChCacheRefresher.ChCacheResetException.class);
        exceptionRule.expectMessage("Smth bad happened Error details:\n Table not found");
        cache.connectToChCacheAndGetResponse("table", URL);
    }


    @Test
    public void testConnectOk() throws Exception {
        PowerMockito.when(chConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        PowerMockito.when(chConnection.getResponseMessage()).thenReturn("OK");
        PowerMockito.when(chConnection.getInputStream())
                .thenReturn(IOUtils.toInputStream("OK", Charset.defaultCharset()));
        cache.connectToChCacheAndGetResponse("table", URL);
    }
}
