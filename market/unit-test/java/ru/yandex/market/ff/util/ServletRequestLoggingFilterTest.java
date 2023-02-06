package ru.yandex.market.ff.util;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.ff.config.RestTemplateLoggingConfig.EXCLUDED_PATHS;
import static ru.yandex.market.ff.config.RestTemplateLoggingConfig.EXCLUDED_PREFIXES;

@ParametersAreNonnullByDefault
class ServletRequestLoggingFilterTest {

    private ServletRequestLoggingFilter servletRequestLoggingFilter;

    @BeforeEach
    void setUp() {
        servletRequestLoggingFilter = new ServletRequestLoggingFilter()
                .setExcludedPaths(EXCLUDED_PATHS)
                .setExcludedPrefixes(EXCLUDED_PREFIXES);
    }

    @Test
    void shouldLog() throws URISyntaxException {

        assertFalse(servletRequestLoggingFilter.shouldLog(getRequest("/ping")));
        assertTrue(servletRequestLoggingFilter.shouldLog(getRequest("/pingSomething")));
        assertTrue(servletRequestLoggingFilter.shouldLog(getRequest("/ping/something")));

        assertFalse(servletRequestLoggingFilter.shouldLog(getRequest("/pagematch")));
        assertTrue(servletRequestLoggingFilter.shouldLog(getRequest("/pagematchSomething")));
        assertTrue(servletRequestLoggingFilter.shouldLog(getRequest("/pagematch/something")));

        assertFalse(servletRequestLoggingFilter.shouldLog(getRequest("/health")));
        assertTrue(servletRequestLoggingFilter.shouldLog(getRequest("/healthSomething")));
        assertFalse(servletRequestLoggingFilter.shouldLog(getRequest("/health/")));
        assertFalse(servletRequestLoggingFilter.shouldLog(getRequest("/health/something")));

    }

    @NotNull
    private MockHttpServletRequest getRequest(String uri) throws URISyntaxException {
        return MockMvcRequestBuilders.request("GET", new URI(uri)).buildRequest(null);
    }
}
