package ru.yandex.market.checkout.checkouter.mocks;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import ru.yandex.market.metrics.micrometer.filters.WebMvcEndpointsLoadMetricsFilter;
import ru.yandex.market.metrics.micrometer.filters.WebMvcMetricsFilter;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public class MockMvcFactory {

    @Autowired
    private WebApplicationContext wac;

    public MockMvc getMockMvc() {
        return MockMvcBuilders.webAppContextSetup(this.wac)
                .alwaysDo(log())
                .addFilter(new RequestContextFilter())
                .addFilter(new WebMvcMetricsFilter(Metrics.globalRegistry))
                .addFilter(new WebMvcEndpointsLoadMetricsFilter())
                .build();
    }

    /**
     * Аналог того, что делает обработчик Jetty - для каждого запроса формирует начальную трассу.
     */
    public static class RequestContextFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            RequestContext savedRequestContext = RequestContextHolder.getContext();
            try {
                RequestContextHolder.createContext(
                        savedRequestContext.getRequestId(),
                        request.getMethod(),
                        request.getRequestURI(),
                        "mockmvc"
                );
                filterChain.doFilter(request, response);
            } finally {
                RequestContextHolder.setContext(savedRequestContext);
            }
        }
    }
}
