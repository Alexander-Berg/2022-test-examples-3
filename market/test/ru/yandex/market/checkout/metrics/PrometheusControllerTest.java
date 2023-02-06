package ru.yandex.market.checkout.metrics;

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.OncePerRequestFilter;

import ru.yandex.market.metrics.micrometer.PrometheusConfiguration;
import ru.yandex.market.metrics.micrometer.PrometheusController;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {
        PrometheusConfiguration.class,
        PrometheusController.class,
        PrometheusControllerTest.MockConfiguration.class
})
class PrometheusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPrometheusEndpoint() throws Exception {
        mockMvc.perform(
                        get("/actuator/prometheus")
                                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("jvm_threads_live_threads{host=")));
    }


    @Configuration
    public static class MockConfiguration {

        @Bean
        public FilterRegistrationBean<RequestContextFilter> mockRequestContextFilter() {
            RequestContextFilter filter = new RequestContextFilter();
            FilterRegistrationBean<RequestContextFilter> registration =
                    new FilterRegistrationBean<>(filter);
            registration.setOrder(Ordered.LOWEST_PRECEDENCE - 1);
            registration.setDispatcherTypes(DispatcherType.REQUEST);
            return registration;
        }
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
