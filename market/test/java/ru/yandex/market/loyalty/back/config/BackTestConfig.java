package ru.yandex.market.loyalty.back.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.inside.passport.tvm2.exceptions.TvmBaseException;
import ru.yandex.market.loyalty.back.controller.CoinsController;
import ru.yandex.market.loyalty.back.controller.PerkController;
import ru.yandex.market.loyalty.back.security.Actions;
import ru.yandex.market.loyalty.back.security.BackClient;
import ru.yandex.market.loyalty.back.security.BackClientSupplier;
import ru.yandex.market.loyalty.back.security.LoggingTvmHandler;
import ru.yandex.market.loyalty.back.security.TvmHandler;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.client.RestMarketLoyaltyClient;
import ru.yandex.market.loyalty.client.TvmTicketProvider;
import ru.yandex.market.loyalty.core.config.CoreTestConfig;
import ru.yandex.market.loyalty.core.config.DatasourceType;
import ru.yandex.market.loyalty.core.config.Default;
import ru.yandex.market.loyalty.db.config.LiquibaseChangelog;
import ru.yandex.market.loyalty.test.TestCoveragePostProcessor;
import ru.yandex.market.loyalty.test.TestCoverageRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static ru.yandex.market.loyalty.test.TestCoverageRule.exclude;

/**
 * Конфиг бека для тестов, внешние зависмости промокированы
 */
@Configuration
@Import({
        BackConfigInternal.class,
        CoreTestConfig.class,
        MarketLoyaltyBackMockConfigurer.class
})
@PropertySource("classpath:/test.properties")
public class BackTestConfig {
    @Bean
    public TestCoverageRule testCoverageRule() {
        return new TestCoverageRule(
                "ru.yandex.market.loyalty.back",
                exclude(CoinsController.class, "onServerStart"), //если что, просто приложение не поднимется
                exclude(PerkController.class, "onServerStart") //если что, просто приложение не поднимется
        );
    }

    @Bean
    public TestCoveragePostProcessor testCoverageAspect(TestCoverageRule testCoverageRule) {
        return new TestCoveragePostProcessor(testCoverageRule);
    }


    @Bean
    public MockMvc createMockMvc(WebApplicationContext wac) {
        return MockMvcBuilders
                .webAppContextSetup(wac)
                .addDispatcherServletCustomizer(
                        dispatcherServlet -> dispatcherServlet.setThrowExceptionIfNoHandlerFound(true))
                .alwaysDo(MockMvcResultHandlers.log())
                .addFilter(new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true, true))
                .addFilter(new CookieAdapterRequest())
                .apply(springSecurity())
                .build();
    }

    @Bean
    @MarketLoyaltyBack
    public RestTemplate createRestTemplate(MockMvc mockMvc) {
        MockMvcClientHttpRequestFactory requestFactory = new MockMvcClientHttpRequestFactory(mockMvc);
        return new RestTemplate(requestFactory);
    }

    @Bean
    public MarketLoyaltyClient createMarketLoyaltyClient(
            @MarketLoyaltyBack RestTemplate restTemplate, @MarketLoyaltyBack ObjectMapper objectMapper,
            TvmTicketProvider tvmTicketProvider
    ) {
        return new RestMarketLoyaltyClient(
                restTemplate, objectMapper, "http://localhost:123456", tvmTicketProvider);
    }

    @Bean
    public TestTvmTicketProvider tvmTicketProvider(@Default Tvm2 tvm2, BackClientSupplier backClientSupplier) {
        return new TestTvmTicketProvider(tvm2, backClientSupplier);
    }

    /**
     * для тестов из ru.yandex.market.loyalty.back.usecase
     */
    @Qualifier("liquibasePopulateDb")
    @Bean
    public SpringLiquibase liquibasePopulateDb(DataSource dataSource) {
        SpringLiquibase liquibase = new TestingSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(LiquibaseChangelog.SCHEMA_AND_DATA.getChangelog());
        liquibase.setDropFirst(true);
        liquibase.setShouldRun(false);
        return liquibase;
    }

    /**
     * для тестов с загрузкой бд тестового окружения
     */
    @Qualifier("liquibasePopulateTestingDb")
    @Bean
    public SpringLiquibase liquibasePopulateTestingDb(DataSource dataSource) {
        SpringLiquibase liquibase = new TestingSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(LiquibaseChangelog.SCHEMA_AND_TESTING_DATA.getChangelog());
        liquibase.setDropFirst(true);
        liquibase.setShouldRun(false);
        return liquibase;
    }

    private static class CookieAdapterRequest extends OncePerRequestFilter {
        private static final Pattern COOKIE_SEPARATOR = Pattern.compile("; ");
        private static final Cookie[] EMPTY_COOKIES_ARRAY = new Cookie[0];

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain
        ) throws ServletException, IOException {
            MockHttpServletRequest mockHttpServletRequest = (MockHttpServletRequest) request;
            Enumeration<String> headers = request.getHeaders(HttpHeaders.COOKIE);
            ImmutableList.Builder<Cookie> cookies = ImmutableList.builder();
            if (request.getCookies() != null) {
                cookies.add(request.getCookies());
            }
            while (headers.hasMoreElements()) {
                String cookie = headers.nextElement();
                for (String value : COOKIE_SEPARATOR.split(cookie)) {
                    String[] parts = value.split("=");
                    if (parts.length != 2) {
                        throw new AssertionError("format of cookie is incorrect");
                    }
                    cookies.add(new Cookie(parts[0], parts[1]));
                }
            }
            mockHttpServletRequest.setCookies(cookies.build().toArray(EMPTY_COOKIES_ARRAY));

            filterChain.doFilter(request, response);
        }
    }

    private static class TestingSpringLiquibase extends SpringLiquibase {
        @Override
        public void afterPropertiesSet() throws LiquibaseException {
            DatasourceType.READ_WRITE.within(super::afterPropertiesSet);
        }
    }

    public static class TestTvmTicketProvider implements TvmTicketProvider, TvmHandler {
        private static final BackClient TEST_CLIENT = new BackClient() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public Set<String> getRoleSet() {
                return Actions.values;
            }
        };
        private final Tvm2 tvm2;
        private final BackClientSupplier backClientSupplier;

        public enum TestMode {
            FAIR_CHECK,
            DEFAULT
        }

        private volatile TestMode testMode = TestMode.DEFAULT;
        private final TvmHandler defaultTvmHandler;

        public TestTvmTicketProvider(@Default Tvm2 tvm2, BackClientSupplier backClientSupplier) {
            this.tvm2 = tvm2;
            this.backClientSupplier = backClientSupplier;
            this.defaultTvmHandler = new LoggingTvmHandler(tvm2, backClientSupplier);
        }

        public void setTestMode(TestMode testMode) {
            this.testMode = testMode;
        }

        @Override
        public int handleUnallowedClient(String clientHandle) {
            if (testMode == TestMode.DEFAULT) {
                return defaultTvmHandler.handleUnallowedClient(clientHandle);
            } else {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
        }

        @Override
        public int handleUnallowedMethod(String clientHandle) {
            if (testMode == TestMode.DEFAULT) {
                return defaultTvmHandler.handleUnallowedMethod(clientHandle);
            } else {
                return AccessDecisionVoter.ACCESS_DENIED;
            }
        }

        @Override
        public Optional<String> getServiceTicket() {
            return Optional.of("test");
        }

        @Override
        public int extractSourceClientHandleFromServiceTicket(String ticket) {
            if (testMode == TestMode.DEFAULT) {
                when(tvm2.checkServiceTicketBySrc(eq(ticket))).thenReturn(0);
                when(tvm2.checkServiceTicketBySrc(any())).thenThrow(new TvmBaseException());
                return defaultTvmHandler.extractSourceClientHandleFromServiceTicket(ticket);
            } else {
                if (Objects.equals(ticket, "test")) {
                    return 0;
                } else {
                    throw new TvmBaseException();
                }
            }
        }

        @Override
        public Optional<BackClient> getClientByHandle(long clientHandle) {
            if (testMode == TestMode.DEFAULT) {
                when(backClientSupplier.findClientByHandle(eq(0))).thenReturn(Optional.of(TEST_CLIENT));
                return defaultTvmHandler.getClientByHandle(clientHandle);
            }
            if (clientHandle == 0) {
                return Optional.of(TEST_CLIENT);
            }
            return Optional.empty();
        }
    }
}
