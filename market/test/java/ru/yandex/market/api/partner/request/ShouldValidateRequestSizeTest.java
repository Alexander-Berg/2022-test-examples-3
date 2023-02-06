package ru.yandex.market.api.partner.request;

import java.util.stream.Stream;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

/**
 * Тесты на метод {@link MultipartRequestHelper#shouldValidateRequestSize(ServletRequest)}.
 *
 * @author fbokovikov
 */
public class ShouldValidateRequestSizeTest {

    public static Stream<Arguments> params() {
        return Stream.of(
                Arguments.of("api.partner.yandex.ru/campaigns/21008055/feeds/upload.json?oauth_token=qwerty&oauth_client_id=clientId", false),
                Arguments.of("api.partner.yandex.ru/campaigns/21008055/feeds/upload.xml", false),
                Arguments.of("/campaigns/21008055/feeds/upload.xml", false),
                Arguments.of("/campaigns/21008055/feeds/upload", false),
                Arguments.of("api.partner.yandex.ru", true)
        );
    }

    private static HttpServletRequest prepareMockRequest(String url) {
        Request request = Mockito.mock(Request.class);
        when(request.getHttpURI()).thenReturn(new HttpURI(url));
        return request;
    }

    @ParameterizedTest
    @MethodSource("params")
    public void shouldValidateTest(String url, boolean expected) {
        HttpServletRequest request = prepareMockRequest(url);
        boolean actual = MultipartRequestHelper.shouldValidateRequestSize(request);
        Assertions.assertEquals(expected, actual);
    }

}
