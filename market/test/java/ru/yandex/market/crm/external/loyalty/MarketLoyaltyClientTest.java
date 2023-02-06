package ru.yandex.market.crm.external.loyalty;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpClient;
import ru.yandex.market.mcrm.http.HttpClientFactory;
import ru.yandex.market.mcrm.http.HttpResponse;
import ru.yandex.market.mcrm.http.ResponseMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MarketLoyaltyClientTest {
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpClientFactory httpClientFactory;

    private MarketLoyaltyClient marketLoyaltyClient;

    @BeforeEach
    public void setUp() {
        when(httpClientFactory.create(anyString())).thenReturn(httpClient);
        marketLoyaltyClient = new MarketLoyaltyClientImpl(
                httpClientFactory,
                mock(JsonSerializer.class),
                mock(JsonDeserializer.class)
        );
    }

    /**
     * Если при получении статуса выдачи лоялти возвращает ответ с ошибкой,
     * то пытаемся повторно запросить статус (3 раза)
     */
    @Test
    public void testCheckCoinBunchRequestStatus_retryIfErrorResponse() {
        AtomicInteger requestCont = new AtomicInteger();

        doAnswer(inv -> {
            requestCont.incrementAndGet();
            return new HttpResponse(new ResponseMock(503, new byte[0]));
        }).when(httpClient).execute(any());

        try {
            marketLoyaltyClient.checkCoinBunchRequestStatus("123");
        } catch (Exception ex) {
            Assertions.assertTrue(ex.getMessage().contains("503"));
        }

        Assertions.assertEquals(3, requestCont.get());
    }
}
