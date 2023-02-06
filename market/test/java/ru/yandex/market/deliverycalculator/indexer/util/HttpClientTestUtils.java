package ru.yandex.market.deliverycalculator.indexer.util;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Утилитный класс для работы с Apache HTTP client'ом в тестах.
 */
public final class HttpClientTestUtils {

    private HttpClientTestUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static CloseableHttpResponse mockResponse(final InputStream responseContent) throws IOException {
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final StatusLine okStatusLine = mock(StatusLine.class);
        final HttpEntity entity = mock(HttpEntity.class);

        when(response.getEntity()).thenReturn(entity);
        when(okStatusLine.getStatusCode()).thenReturn(200);
        when(response.getStatusLine()).thenReturn(okStatusLine);
        when(entity.getContent()).thenReturn(responseContent);
        return response;
    }

}
