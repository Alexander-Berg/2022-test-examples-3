package ru.yandex.market.notification.test.converter;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpInputMessage;

import ru.yandex.market.notification.common.converter.AbstractXmlResponseConverter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Базовый абстрактный класс для тестирования HTTP конвертеров.
 *
 * @param <T> тип результата HTTP-ответа
 *
 * @author avetokhin 15/06/16.
 */
public abstract class AbstractBaseConverterTest<T> {

    protected abstract Class<T> getType();

    protected abstract AbstractXmlResponseConverter<T> getConverter();


    protected final T getResponse(final Class<?> clazz, final String resourceName) throws IOException {
        try (InputStream stream = clazz.getResourceAsStream(resourceName)) {
            return getResponse(stream);
        }
    }


    private T getResponse(final InputStream stream) throws IOException {
        final Class<T> type = getType();
        final AbstractXmlResponseConverter<T> converter = getConverter();
        final HttpInputMessage message = mockHttpMessage(stream);

        return converter.read(type, message);
    }

    private HttpInputMessage mockHttpMessage(final InputStream stream) throws IOException {
        final HttpInputMessage message = mock(HttpInputMessage.class);
        final InputStream inputStream = message.getBody();

        when(inputStream).thenReturn(stream);
        return message;
    }

}
