package ru.yandex.market.mbi.partner_stat.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.spring.RestTemplateFactory;

/**
 * Утилитарный класс для тестирование ручек, которые возвращают отчеты.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
public final class ReportTestUtil {

    private static final List<HttpMessageConverter<?>> MESSAGE_CONVERTERS = getMessageConverters();

    private ReportTestUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Отправить запрос на получение отчета.
     *
     * @param url             полный url до ручки
     * @param clazz           класс, через который будет загружен файл с запросом
     * @param requestFilePath путь по файла запроса относительно clazz
     * @param responseType    тип ответа
     * @param uriVariables    переменные uri
     * @param <T>             тип, который вернется
     */
    public static <T> ResponseEntity<T> post(final String url,
                                             final Class<?> clazz,
                                             final String requestFilePath,
                                             final Class<T> responseType,
                                             final Object... uriVariables) {
        final InputStream requestStream = clazz.getResourceAsStream(requestFilePath);
        return FunctionalTestHelper.execute(
                url,
                HttpMethod.POST,
                e -> {
                    e.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    final OutputStream body = e.getBody();
                    IOUtils.copy(requestStream, body);
                },
                e -> {
                    final T body = parseResponse(e, responseType);
                    return ResponseEntity.status(e.getStatusCode()).body(body);
                },
                uriVariables
        );
    }

    private static <T> T parseResponse(final ClientHttpResponse response,
                                       final Class<T> responseType) throws IOException {
        return new HttpMessageConverterExtractor<>(responseType, MESSAGE_CONVERTERS).extractData(response);
    }

    private static List<HttpMessageConverter<?>> getMessageConverters() {
        return RestTemplateFactory
                .createRestTemplate()
                .getMessageConverters();
    }
}
