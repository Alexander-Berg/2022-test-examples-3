package ru.yandex.market.fulfillment.wrap.core.scenario.util;

import java.lang.reflect.Field;

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.getField;

public final class RequestFactoryWrapper {

    private RequestFactoryWrapper() {
        throw new UnsupportedOperationException();
    }

    /**
     * Врапает подлежащую RequestFactory в BufferedRequestFactory, чтобы обеспечить возможность
     * полноценной работы существующим Interceptor'ам запросов.
     *
     * @param restTemplate Темплейт, чей RequestFactory необходимо обернуть в BufferingClientHttpRequestFactory.
     */
    public static void wrapInBufferedRequestFactory(RestTemplate restTemplate) {
        ClientHttpRequestFactory currentFactory = restTemplate.getRequestFactory();

        ClientHttpRequestFactory coreRequestFactory = isInterceptedRequestFactory(currentFactory)
                ? extractUnderlyingFactory(currentFactory)
                : currentFactory;


        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(coreRequestFactory));
    }

    private static ClientHttpRequestFactory extractUnderlyingFactory(ClientHttpRequestFactory currentFactory) {
        Field field = findField(InterceptingClientHttpRequestFactory.class, "requestFactory");
        field.setAccessible(true);

        return (ClientHttpRequestFactory) getField(field, currentFactory);
    }

    private static boolean isInterceptedRequestFactory(ClientHttpRequestFactory currentFactory) {
        return currentFactory instanceof InterceptingClientHttpRequestFactory;
    }
}
