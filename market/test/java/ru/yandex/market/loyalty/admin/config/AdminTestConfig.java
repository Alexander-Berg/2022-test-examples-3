package ru.yandex.market.loyalty.admin.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import ru.yandex.market.loyalty.admin.controller.CoinPromoController;
import ru.yandex.market.loyalty.admin.controller.ExportController;
import ru.yandex.market.loyalty.admin.controller.Promo3pController;
import ru.yandex.market.loyalty.admin.controller.PromoController;
import ru.yandex.market.loyalty.admin.controller.PromocodePromoController;
import ru.yandex.market.loyalty.admin.controller.SecretSalesController;
import ru.yandex.market.loyalty.admin.security.AccessController;
import ru.yandex.market.loyalty.core.config.CoreTestConfig;
import ru.yandex.market.loyalty.core.model.security.AdminRole;
import ru.yandex.market.loyalty.test.TestCoveragePostProcessor;
import ru.yandex.market.loyalty.test.TestCoverageRule;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static ru.yandex.market.loyalty.test.TestCoverageRule.exclude;

/**
 * Конфиг админки для тестов, внешние зависмости промокированы
 */
@Configuration
@Import({
        AdminConfigInternal.class,
        CoreTestConfig.class,
        MarketLoyaltyAdminMockConfigurer.class
})
@PropertySource("classpath:/test.properties")
@ContextConfiguration
public class AdminTestConfig {
    @Bean
    public TestCoverageRule testCoverageRule() {
        return new TestCoverageRule(
                "ru.yandex.market.loyalty.admin",
                exclude(Promo3pController.class, "sampleDownload"),
                exclude(SecretSalesController.class, "sampleDownload"),
                exclude(ExportController.class, "generateCoins", "enqueueDiscountsGenerationRequest",
                        "generateHashes", "getExportedStatus"
                ),
                exclude(CoinPromoController.class, "uploadImage", "getDefaultDescription", "changeStatus"),
                exclude(PromocodePromoController.class, "changeStatus"),
                exclude(PromoController.class, "addReserveBudget", "getReserveBudget"),
                exclude(AccessController.class, "main")
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
                .addFilter(new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true, true))
                .addFilter(new RequestWithSecureParamsFilter())
                .apply(springSecurity())
                .addDispatcherServletCustomizer(
                        dispatcherServlet -> dispatcherServlet.setThrowExceptionIfNoHandlerFound(true))
                .build();
    }

    private static class RequestWithSecureParamsFilter extends OncePerRequestFilter {
        private static final Cookie[] EMPTY_COOKIE = new Cookie[0];

        @Override
        protected void doFilterInternal(
                @NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                @NotNull FilterChain filterChain
        ) throws ServletException, IOException {
            MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest(request);
            Cookie[] cookies = getCookies(request);
            if (!hasSessionId(cookies)) {
                cookies = cookiesWithSessionId(cookies);
            }
            mockHttpServletRequest.setCookies(cookies);

            mockHttpServletRequest.addHeader("X-Real-IP", "127.0.0.1");
            filterChain.doFilter(request, response);
        }

        private static Cookie[] cookiesWithSessionId(Cookie[] cookies) {
            Cookie[] newCookies = Arrays.copyOf(cookies, cookies.length + 1);
            newCookies[newCookies.length - 1] = new Cookie("Session_id", AdminRole.SUPERUSER_ROLE.getCode());
            return newCookies;
        }

        private static Cookie[] getCookies(HttpServletRequest request) {
            return request.getCookies() != null ? request.getCookies() : EMPTY_COOKIE;
        }

        private static boolean hasSessionId(Cookie[] cookies) {
            return Arrays.stream(cookies).map(Cookie::getName).anyMatch("Session_id"::equals);
        }

        private static MockHttpServletRequest getMockHttpServletRequest(ServletRequest request) {
            while (!(request instanceof MockHttpServletRequest)) {
                if (!(request instanceof ServletRequestWrapper)) {
                    throw new AssertionError();
                }
                request = ((ServletRequestWrapper) request).getRequest();
            }
            return (MockHttpServletRequest) request;
        }
    }
}
