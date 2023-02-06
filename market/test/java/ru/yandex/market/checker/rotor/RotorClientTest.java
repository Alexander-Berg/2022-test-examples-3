package ru.yandex.market.checker.rotor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checker.core.CoreCheckerTask;
import ru.yandex.market.checker.zora.util.Platform;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author valeriashanti
 * @date 27/07/2020
 */
class RotorClientTest {

    private final byte[] originalContent = "some response".getBytes();
    private final ByteArrayInputStream encodedContent = new ByteArrayInputStream(Base64.encodeBase64(originalContent));

    private RotorClient rotorClient;

    @BeforeEach
    void init() throws IOException {
        var httpClient = Mockito.mock(CloseableHttpClient.class);
        var mockSupplier = mock(Supplier.class);
        var mockHttpEntity = Mockito.mock(HttpEntity.class);
        var mockHttpResponse = mock(CloseableHttpResponse.class);
        var statusLine = mock(StatusLine.class);
        rotorClient = new RotorClient("",0, httpClient, mockSupplier);

        when(mockSupplier.get()).thenReturn(Optional.empty());
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getEntity()).thenReturn(mockHttpEntity);
        when(mockHttpEntity.getContent()).thenReturn(encodedContent);
        when(mockHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HTTP_OK);
    }

    @Test
    void loadPage() {
        var task = new CoreCheckerTask("yandex.ru", 0, 0, 0,Platform.ANY);
        byte[] bytes = rotorClient.loadPage(task.getUrl(), task);

        assertArrayEquals(originalContent, bytes);
        assertEquals(HTTP_OK, task.getJsHttpStatus());
        assertEquals(new String(originalContent), new String(bytes));
    }
}
