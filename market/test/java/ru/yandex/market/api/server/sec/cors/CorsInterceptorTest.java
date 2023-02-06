package ru.yandex.market.api.server.sec.cors;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * @author dimkarp93
 */
@WithMocks
public class CorsInterceptorTest extends UnitTestBase {
    @Mock
    HttpServletResponse response;

    @Mock
    CorsConfig corsConfig;

    @InjectMocks
    CorsInterceptor corsInterceptor;

    @Test
    public void noHeadersIfDisallowed() {
        Mockito.when(corsConfig.isAllowed(Mockito.anyString()))
            .thenReturn(false);

        HttpServletRequest request = MockRequestBuilder.start().build();

        corsInterceptor.preHandle(request, response, null);

        Mockito.verifyZeroInteractions(response);
    }

    @Test
    public void existsHeadersIfAllowed() {
        Mockito.when(corsConfig.isAllowed(Mockito.anyString()))
            .thenReturn(true);

        Mockito.when(corsConfig.getAllowHeaders(Mockito.anyString()))
            .thenReturn("head");
        Mockito.when(corsConfig.getAllowMethods(Mockito.anyString()))
            .thenReturn("meth");
        Mockito.when(corsConfig.isAllowCredentials(Mockito.anyString()))
            .thenReturn(false);
        Mockito.when(corsConfig.getMaxAge(Mockito.anyString()))
            .thenReturn(-1);
        Mockito.when(corsConfig.getPatterns(Mockito.anyString()))
            .thenReturn(Collections.singletonList(
                CorsHelper.compilePattern("yandex\\.(ru|by|kz|ua|com|com\\.tr|com\\.ge|com\\.il|az|kg|lv|lt|md|tj|tm|fr|ee)")
            ));

        HttpServletRequest request = MockRequestBuilder.start()
            .header("Origin", "https://yandex.ru")
            .build();

        corsInterceptor.preHandle(request, response, null);

        Mockito.verify(response)
            .setHeader("Access-Control-Allow-Origin", "https://yandex.ru");
        Mockito.verify(response)
            .setHeader("Access-Control-Allow-Headers", "head");
        Mockito.verify(response)
            .setHeader("Access-Control-Allow-Methods", "meth");
        Mockito.verify(response)
            .setHeader("Access-Control-Allow-Credentials", "false");
        Mockito.verify(response)
            .setHeader("Access-Control-Max-Age", "-1");
    }
}
