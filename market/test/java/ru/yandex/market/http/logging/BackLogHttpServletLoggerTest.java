package ru.yandex.market.http.logging;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

@DisplayName("Тесты на inbound логирующий фильтр")
public class BackLogHttpServletLoggerTest extends AbstractTest {
    private static final String SERVLET_NAME = "TEST";
    private static final String REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd";

    @RegisterExtension
    final BackLogCaptor backLogCaptor = new BackLogCaptor();

    @BeforeEach
    void setupRequestId() {
        RequestContextHolder.setContext(new RequestContext(REQUEST_ID));
    }

    private final BackLogHttpServletLogger servletLogger = new BackLogHttpServletLogger();

    @Test
    @DisplayName("Перед обработкой ничего не логируется")
    public void preHandleIsEmpty() {
        servletLogger.logPreHandle(SERVLET_NAME, REQUEST_ID, new MockHttpServletRequest());

        softly.assertThat(backLogCaptor.getResults())
            .isEmpty();
    }

    @Test
    @DisplayName("После обработки пишется два лога")
    void postHandleNotEmpty() {
        servletLogger.logPostHandle(
            SERVLET_NAME,
            REQUEST_ID,
            new MockHttpServletRequest(),
            "Request body",
            new MockHttpServletResponse(),
            "Response body",
            1
        );

        softly.assertThat(backLogCaptor.getResults())
            .hasSize(2)
            .anyMatch(s -> s.contains(
                "level=INFO"
                    + "\tformat=plain"
                    + "\tpayload=Request body"
                    + "\trequest_id=" + REQUEST_ID
                    + "\ttags=INBOUND_REQUEST"
                    + "\textra_keys=Method,Url,Content-Type"
                    + "\textra_values=,,null"
            ))
            .anyMatch(s -> s.contains(
                "level=INFO"
                    + "\tformat=plain"
                    + "\tpayload=Response body"
                    + "\trequest_id=" + REQUEST_ID
                    + "\ttags=INBOUND_RESPONSE"
                    + "\textra_keys=Status,Duration,Content-Type"
                    + "\textra_values=200,1,null"
            ));
    }

    @Test
    @DisplayName("Путь и метод запроса логируются")
    void methodAndPathLogged() {
        servletLogger.logPostHandle(
            SERVLET_NAME,
            REQUEST_ID,
            new MockHttpServletRequest(HttpMethod.GET.name(), "path/to/controller"),
            "Request body",
            new MockHttpServletResponse(),
            "Response body",
            1
        );

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(s -> s.contains(
                "\textra_keys=Method,Url,Content-Type\textra_values=GET,path/to/controller,null"
            ));
    }

    @Test
    @DisplayName("Параметры запроса адекватно логируются")
    void queryParamsLogged() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.PUT.name(), "path/to/method");
        request.setQueryString("key1=value2&key2=value2");

        servletLogger.logPostHandle(
            SERVLET_NAME,
            REQUEST_ID,
            request,
            "Request body",
            new MockHttpServletResponse(),
            "Response body",
            1
        );

        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(s -> s.contains(
                "\textra_keys=Method,Url,Content-Type\textra_values=PUT,path/to/method?key1=value2&key2=value2,null"
            ));
    }

    @ParameterizedTest
    @MethodSource("contentTypeSource")
    @DisplayName("Принятие решения о логировании тела запроса")
    void contentTypeFiltering(
        String contentType,
        String expectedBodyLog
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(contentType);

        servletLogger.logPostHandle(
            SERVLET_NAME,
            REQUEST_ID,
            request,
            "Request body",
            new MockHttpServletResponse(),
            "Response body",
            1
        );

        // Обработка тела ответа работает аналогично, так что проверяем только запрос
        softly.assertThat(backLogCaptor.getResults())
            .anyMatch(s -> s.contains(
                "\tpayload=" + expectedBodyLog
                    + "\trequest_id=" + REQUEST_ID
                    + "\ttags=INBOUND_REQUEST"
            ));
    }

    private static Stream<Arguments> contentTypeSource() {
        return Stream.concat(

            // Passed values
            Stream.of(
                null,
                "image/jpg",
                "application/octet-stream",
                "application/json",
                "application/json;charset=UTF-8",
                "application/json; charset=UTF-8",
                "application/json;charset=utf-8",
                "application/json; charset=utf-8",
                "application/xhtml+xml",
                "application/x-www-form-urlencoded",
                "text/plain",
                "text/plain; charset=UTF-8",
                "text/xml",
                "application/xml",
                "multipart/form-data",
                "TEXT/HTML"
            )
                .map(ct -> Arguments.of(ct, "Request body")),

            // Filtered values
            Stream.of(
                "application/pdf"
            )
                .map(ct -> Arguments.of(ct, "[BODY HAS CONTENT-TYPE " + ct + ", OMITTED]"))
        );
    }
}
