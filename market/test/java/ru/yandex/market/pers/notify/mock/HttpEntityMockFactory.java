package ru.yandex.market.pers.notify.mock;

import org.apache.http.HttpEntity;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author korolyov
 * 21.06.16
 */
public class HttpEntityMockFactory {
    private HttpEntityMockFactory() {
    }

    public static HttpEntity httpEntityMock(String content) throws IOException {
        HttpEntity httpEntity = mock(HttpEntity.class);
        Answer<ByteArrayInputStream> responseStream = x -> new ByteArrayInputStream(content.getBytes("UTF-8"));
        when(httpEntity.getContent()).then(responseStream);
        when(httpEntity.isStreaming()).thenReturn(true);
        return httpEntity;
    }
}