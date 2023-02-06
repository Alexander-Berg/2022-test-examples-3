package ru.yandex.market.mbi.tariffs.mvc.interceptor;

import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.security.interceptor.InterceptorRequestSkip;
import ru.yandex.market.mbi.tariffs.FunctionalTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link InterceptorRequestSkip}
 */
@ParametersAreNonnullByDefault
class InterceptorRequestSkipTest extends FunctionalTest {
    @Autowired
    private InterceptorRequestSkip interceptorRequestSkip;

    @ParameterizedTest
    @MethodSource("shouldSkipTestData")
    void testShouldSkip(String path, boolean shouldSkip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);

        boolean actualSkip = interceptorRequestSkip.shouldSkip(request);
        assertEquals(shouldSkip, actualSkip);
    }

    private static Stream<Arguments> shouldSkipTestData() {
        return Stream.of(
                Arguments.of("/ping", true),
                Arguments.of("/monitoring", true),
                Arguments.of("/drafts", false),
                Arguments.of(null, false)
        );
    }
}
