package ru.yandex.market.api.partner.ping;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.ping.Tvm2Checker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author i-milyaev
 */
class PingControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private List<Tvm2> tvm2List;

    @Autowired
    private Tvm2Checker tvm2Checker;

    private static Stream<Arguments> httpMethods() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, HttpStatus.OK),
                Arguments.of(HttpMethod.POST, HttpStatus.METHOD_NOT_ALLOWED),
                Arguments.of(HttpMethod.HEAD, HttpStatus.OK),
                Arguments.of(HttpMethod.PUT, HttpStatus.METHOD_NOT_ALLOWED),
                Arguments.of(HttpMethod.OPTIONS, HttpStatus.OK),
                Arguments.of(HttpMethod.DELETE, HttpStatus.METHOD_NOT_ALLOWED),
                Arguments.of(HttpMethod.TRACE, HttpStatus.FORBIDDEN)
        );
    }

    private static Stream<Arguments> httpMethodsNegative() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, HttpStatus.INTERNAL_SERVER_ERROR),
                Arguments.of(HttpMethod.POST, HttpStatus.METHOD_NOT_ALLOWED),
                Arguments.of(HttpMethod.HEAD, HttpStatus.INTERNAL_SERVER_ERROR),
                Arguments.of(HttpMethod.PUT, HttpStatus.METHOD_NOT_ALLOWED),
                Arguments.of(HttpMethod.OPTIONS, HttpStatus.OK),
                Arguments.of(HttpMethod.DELETE, HttpStatus.METHOD_NOT_ALLOWED),
                Arguments.of(HttpMethod.TRACE, HttpStatus.FORBIDDEN)
        );
    }

    @BeforeEach
    void setUp() {
        tvm2List.forEach(Mockito::reset);
        tvm2Checker.reset();
    }

    @ParameterizedTest
    @MethodSource("httpMethods")
    void testPingWithAcceptField(HttpMethod method, HttpStatus expectedResponseStatus) {
        testPing(method, expectedResponseStatus, ImmutableList.of(MediaType.ALL));
    }

    @ParameterizedTest
    @MethodSource("httpMethods")
    void testPingWithoutAcceptField(HttpMethod method, HttpStatus expectedResponseStatus) {
        testPing(method, expectedResponseStatus, null);
    }

    /**
     * Проверить, что ручка "/ping" отвечает корректно на разные HTTP-методы, когда результат проверок "ОК".
     */
    void testPing(HttpMethod method, HttpStatus expectedResponseStatus, List<MediaType> accepts) {

        for (Tvm2 tvm : tvm2List) {
            when(tvm.isInitialized()).thenReturn(true);
        }

        if (expectedResponseStatus == HttpStatus.OK) {
            final HttpStatus actualResponse = getResponseEntity(method, accepts).getStatusCode();

            assertThat(actualResponse, equalTo(expectedResponseStatus));
        } else {
            HttpClientErrorException exception = Assertions.assertThrows(
                    HttpClientErrorException.class,
                    () -> getResponseEntity(method, accepts)
            );

            assertThat(
                    exception,
                    HttpClientErrorMatcher.hasErrorCode(expectedResponseStatus)
            );
        }
    }

    @ParameterizedTest
    @MethodSource("httpMethodsNegative")
    void testPingNotOkWithAcceptField(HttpMethod method, HttpStatus expectedResponseStatus) {
        testPingNotOk(method, expectedResponseStatus, ImmutableList.of(MediaType.ALL));
    }

    @ParameterizedTest
    @MethodSource("httpMethodsNegative")
    void testPingNotOkWithoutAcceptField(HttpMethod method, HttpStatus expectedResponseStatus) {
        testPingNotOk(method, expectedResponseStatus, null);
    }

    /**
     * Проверить, что ручка "/ping" отвечает корректно на разные HTTP-методы, когда результат проверок не "ОК".
     */
    void testPingNotOk(HttpMethod method, HttpStatus expectedResponseStatus, List<MediaType> accepts) {

        if (expectedResponseStatus.is5xxServerError()) {
            HttpServerErrorException exception = Assertions.assertThrows(
                    HttpServerErrorException.class,
                    () -> getResponseEntity(method, accepts)
            );

            if (!method.equals(HttpMethod.HEAD)) {
                assertTrue(exception.getResponseBodyAsString().contains(Tvm2Checker.FAIL_STATUS));
            }
            assertEquals(expectedResponseStatus, exception.getStatusCode());

        } else if (expectedResponseStatus.is4xxClientError()) {
            HttpClientErrorException exception = Assertions.assertThrows(
                    HttpClientErrorException.class,
                    () -> getResponseEntity(method, accepts)
            );
            assertThat(
                    exception,
                    HttpClientErrorMatcher.hasErrorCode(expectedResponseStatus)
            );
        } else {
            final HttpStatus actualResponse = getResponseEntity(method, accepts).getStatusCode();
            assertThat(actualResponse, equalTo(expectedResponseStatus));
        }
    }

    @Nonnull
    private ResponseEntity<String> getResponseEntity(HttpMethod method, List<MediaType> accepts) {
        if (accepts != null) {
            HttpHeaders httpHeaders = new HttpHeaders();
            return FunctionalTestHelper.makeRequest(urlBasePrefix + "/ping", method, httpHeaders, String.class);
        }
        return FunctionalTestHelper.makeRequest(urlBasePrefix + "/ping", method, String.class);
    }
}
