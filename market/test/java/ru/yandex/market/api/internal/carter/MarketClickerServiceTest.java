package ru.yandex.market.api.internal.carter;

import java.net.URI;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.util.Urls;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.HttpClientFactory;
import ru.yandex.market.http.HttpClient;
import ru.yandex.market.http.HttpClientBuilder;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class MarketClickerServiceTest {

    HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);

    HttpClientBuilder httpClientBuilder = mock(HttpClientBuilder.class);

    HttpClient httpClient = mock(HttpClient.class);

    MarketClickerService service;

    @Before
    public void setUp() throws Exception {
        when(httpClientFactory.create(anyString())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient);
        when(httpClient.doGet(any(URI.class), any(Function.class))).thenReturn(Futures.newSucceededFuture(null));

        service = new MarketClickerService(httpClientFactory);
    }

    @Test
    public void testAddItemWithoutDomain() {
        String cpaUrl = "/safeclick/data=338FT8NBgRvqwGz9uOZxgZnKqINkPHHY9DupcUn1FELKSLnpOhvVTbN6XUOrkBAcZ9";

        service.click(cpaUrl);

        String internalCpaUrl = "https://market-click2-internal.vs.market.yandex.net/safeclick/data=338FT8NBgRvqwGz9uOZxgZnKqINkPHHY9DupcUn1FELKSLnpOhvVTbN6XUOrkBAcZ9";

        verify(httpClient).doGet(eq(Urls.toUri(internalCpaUrl)), any(Function.class));
    }

    @Test
    public void testAddItemWithUrlOfForeignDomain() {
        String cpaUrl = "https://malicious.domain.ru/steal/your/money";

        service.click(cpaUrl);

        String internalCpaUrl = "https://market-click2-internal.vs.market.yandex.net/steal/your/money";

        verify(httpClient).doGet(eq(Urls.toUri(internalCpaUrl)), any(Function.class));
    }

    @Test
    public void testAddItem() {
        String cpaUrl = "https://market-click2.yandex.ru/redir/338FT8NBg56v_gbYff0piSt7Lyx_vLSnO-6kkBDFUcw03VVZ" +
            "KpNnKtitg2T2kSZ7u-rsIRZDa6w,,&b64e=1&sign=79180574400ef9ccca2f5feb398ad732&keyno=1";

        service.click(cpaUrl);

        String internalCpaUrl = "https://market-click2-internal.vs.market.yandex.net/redir/338FT8NBg56v_gbYff0piSt7Lyx_vLSnO-6kkBDFUcw03VVZ" +
                "KpNnKtitg2T2kSZ7u-rsIRZDa6w,,&b64e=1&sign=79180574400ef9ccca2f5feb398ad732&keyno=1";

        verify(httpClient).doGet(eq(Urls.toUri(internalCpaUrl)), any(Function.class));
    }
}