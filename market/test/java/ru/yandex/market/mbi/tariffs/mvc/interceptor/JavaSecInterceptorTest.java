package ru.yandex.market.mbi.tariffs.mvc.interceptor;

import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.security.interceptor.InterceptorRequestSkip;
import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.security.SecManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link JavaSecInterceptor}
 */
@ParametersAreNonnullByDefault
public class JavaSecInterceptorTest extends FunctionalTest {
    private JavaSecInterceptor javaSecInterceptor;

    @Autowired
    private SecManager secManager;

    @Autowired
    private InterceptorRequestSkip interceptorRequestSkip;

    @BeforeEach
    void setUp() {
        javaSecInterceptor = spy(new JavaSecInterceptor(secManager, interceptorRequestSkip));
    }

    @DisplayName("Тест на пропуск проверки javasec'a")
    @ParameterizedTest(name = "[{index}] for {0} should be {1}")
    @MethodSource("skipCheckData")
    void testSkipCheck(
            HttpServletRequest request,
            boolean skipCheck
    ) {
        assertEquals(skipCheck, javaSecInterceptor.skipCheck(request));
    }

    private static Stream<Arguments> skipCheckData() {
        return Stream.of(
                Arguments.of(mockRequest(null, null, null), false),
                Arguments.of(mockRequest("/ping", null, null), true),
                Arguments.of(mockRequest("/monitoring", null, null), true),
                Arguments.of(mockRequest("/drafts", null, null), false),
                Arguments.of(mockRequest("/drafts", "UseMock123", "true"), false)
                //тест не проходит при сборке, т.к. при сборке используется Production енв
//                Arguments.of(mockRequest("/drafts", "UseMock", "true"), true)
        );
    }

    private static HttpServletRequest mockRequest(
            @Nullable String path,
            @Nullable String useMockHeaderName,
            @Nullable String useMockHeaderValue
    ) {
        HttpServletRequest mock = mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn(path);
        when(mock.getHeader(useMockHeaderName)).thenReturn(useMockHeaderValue);
        when(mock.toString()).thenReturn(String.format(
                "Request [%s], Headers: [%s:%s]", path, useMockHeaderName, useMockHeaderValue
        ));
        return mock;
    }
}
