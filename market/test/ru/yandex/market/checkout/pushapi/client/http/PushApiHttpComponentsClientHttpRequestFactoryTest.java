package ru.yandex.market.checkout.pushapi.client.http;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Функциональный тест для проверки работы повторных запросов к сервису в случае разных ошибок
 *
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {
        "classpath:ru/yandex/market/checkout/pushapi/client/http/PushApiHttpComponentsClientHttpRequestFactoryTest.xml",
        "classpath:WEB-INF/push-api-client.xml"})
public class PushApiHttpComponentsClientHttpRequestFactoryTest {

    @Autowired
    RestTemplate pushApiRestTemplate;


    @Autowired
    private HttpRequestRetryHandler httpRequestRetryHandler;

    @Test
    public void shouldRetry5TimesWhenConnectionTimeout() {
        Assertions.assertThrows(ResourceAccessException.class, () -> {
            try {
                pushApiRestTemplate.getForEntity("http://127.0.0.1", String.class);
            } finally {
                verify(httpRequestRetryHandler, times(5)).retryRequest(any(IOException.class), anyInt(), any(HttpContext.class));
            }
        });
    }
}
