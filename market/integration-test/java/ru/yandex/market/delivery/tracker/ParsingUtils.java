package ru.yandex.market.delivery.tracker;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.getField;

@SuppressWarnings("WeakerAccess")
public final class ParsingUtils {
    private ParsingUtils() {
    }

    public static void decorateWithBuffering(RestTemplate actualTemplate) {
        ClientHttpRequestFactory currentFactory = actualTemplate.getRequestFactory();
        ClientHttpRequestFactory coreRequestFactory = isInterceptedRequestFactory(currentFactory)
            ? extractUnderlyingFactory(currentFactory)
            : currentFactory;
        actualTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(coreRequestFactory));
    }

    public static String extractFileContent(String relativePath) throws IOException {
        return IOUtils.toString(
            getSystemResourceAsStream(relativePath),
            "UTF-8"
        );
    }

    public static Date parseDate(String source) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(source);
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
