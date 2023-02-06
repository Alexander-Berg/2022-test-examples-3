package ru.yandex.market.checkout.checkouter.trace;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HackedInternalHttpClient;
import org.apache.http.impl.execchain.ProtocolExec;
import org.apache.http.impl.execchain.RedirectExec;
import org.apache.http.impl.execchain.RetryExec;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.config.external.LoyaltyConfig;
import ru.yandex.market.checkout.checkouter.config.web.RestTemplateConfig;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.client.RestMarketLoyaltyClient;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(
        classes = {LoyaltyConfig.class, RestTemplateConfig.class}
)
@ExtendWith(SpringExtension.class)
public class MarketLoyaltyClientTest {

    @Autowired
    protected MarketLoyaltyClient loyaltyClient;

    /**
     * Это супер-грязный тест, который лезет во внутренние кишки HttpClient, чтобы проверить есть ли
     * там трассировщики запросов
     */
    @Test
    void shouldHaveTraceInterceptors() throws IllegalAccessException {
        RestTemplate restTemplate = (RestTemplate) FieldUtils.getField(RestMarketLoyaltyClient.class,
                "restTemplate", true).get(loyaltyClient);

        assertThat(restTemplate, Matchers.notNullValue());

        // если есть интерсепторы, то getRequestFactory возвращается не тот, что есть на самом деле
        restTemplate.setInterceptors(List.of());

        HttpComponentsClientHttpRequestFactory requestFactory = (HttpComponentsClientHttpRequestFactory) FieldUtils
                .getField(AbstractClientHttpRequestFactoryWrapper.class,
                        "requestFactory", true).get(restTemplate.getRequestFactory());

        HttpClient httpClient = requestFactory.getHttpClient();

        RedirectExec chain = (RedirectExec) FieldUtils.getField(HackedInternalHttpClient.class,
                "execChain", true).get(httpClient);

        RetryExec requestExecutor = (RetryExec) FieldUtils.getField(RedirectExec.class,
                "requestExecutor", true).get(chain);

        ProtocolExec protocolExec = (ProtocolExec) FieldUtils.getField(RetryExec.class,
                "requestExecutor", true).get(requestExecutor);

        ImmutableHttpProcessor httpProcessor = (ImmutableHttpProcessor) FieldUtils.getField(ProtocolExec.class,
                "httpProcessor", true).get(protocolExec);

        HttpRequestInterceptor[] requestInterceptors =
                (HttpRequestInterceptor[]) FieldUtils.getField(ImmutableHttpProcessor.class,
                        "requestInterceptors", true).get(httpProcessor);
        HttpResponseInterceptor[] responseInterceptors =
                (HttpResponseInterceptor[]) FieldUtils.getField(ImmutableHttpProcessor.class,
                        "responseInterceptors", true).get(httpProcessor);

        assertTrue(Arrays.stream(requestInterceptors)
                .anyMatch(i -> TraceHttpRequestInterceptor.class.isAssignableFrom(i.getClass())));
        assertTrue(Arrays.stream(responseInterceptors)
                .anyMatch(i -> TraceHttpResponseInterceptor.class.isAssignableFrom(i.getClass())));
    }

}
