package ru.yandex.market.api.partner.request;

import java.io.IOException;

import javax.servlet.ServletInputStream;

import com.google.common.collect.ImmutableList;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import ru.yandex.market.mbi.http.CachingServletInputStream;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author fbokovikov
 */
@RunWith(Parameterized.class)
public class PartnerServletRequestTest {

    private static final int MAX_INPUT_STREAM_CACHE_SIZE = 1;

    private final String path;
    private final Class expectedClass;

    public PartnerServletRequestTest(String path, Class expectedClass) {
        this.path = path;
        this.expectedClass = expectedClass;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> params() {
        return ImmutableList.of(
                new Object[]{"http://mbi10et.haze.yandex.net:34864/campaigns/21008055/feeds/upload.json", CachingServletInputStream.class},
                new Object[]{"http://mbi10et.haze.yandex.net:34864/campaigns/21008055/feeds/upload.xml", CachingServletInputStream.class},
                new Object[]{"http://mbi10et.haze.yandex.net:34864/ping", LimitedCachingServletInputStream.class}
        );
    }

    /**
     * чекаем, что только для ручки /campaigns/\\d+/feeds/upload возвращается инстанс ServletInputStream,
     * не лимитирующий размер переданных данных.
     */
    @Test
    public void newCachingInputStreamTest() throws IOException {
        Request jettyRequest = Mockito.mock(Request.class);
        when(jettyRequest.getHttpURI()).thenReturn(new HttpURI(path));
        when(jettyRequest.getInputStream()).thenReturn(Mockito.mock(ServletInputStream.class, Mockito.CALLS_REAL_METHODS));
        PartnerServletRequest request = new PartnerServletRequest(jettyRequest, MAX_INPUT_STREAM_CACHE_SIZE);
        assertTrue(request.newCachingServletInputStream().getClass() == expectedClass);
    }

}
