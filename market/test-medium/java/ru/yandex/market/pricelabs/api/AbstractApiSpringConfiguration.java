package ru.yandex.market.pricelabs.api;

import java.util.Optional;
import java.util.function.Supplier;

import okhttp3.mockwebserver.MockWebServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import ru.yandex.market.pricelabs.AbstractSpringConfiguration;
import ru.yandex.market.pricelabs.CoreConfigurationForTests.Basic;
import ru.yandex.market.pricelabs.api.api.PublicApi;
import ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApi;
import ru.yandex.market.pricelabs.api.api.PublicLogsApi;
import ru.yandex.market.pricelabs.api.api.PublicMainApi;
import ru.yandex.market.pricelabs.api.api.PublicModelbidsApi;
import ru.yandex.market.pricelabs.api.api.PublicNotifyApi;
import ru.yandex.market.pricelabs.api.api.PublicPartnerApi;
import ru.yandex.market.pricelabs.api.api.PublicProgramApi;
import ru.yandex.market.pricelabs.api.api.PublicRecommendationsApi;
import ru.yandex.market.pricelabs.api.api.PublicScheduleApi;
import ru.yandex.market.pricelabs.api.api.PublicUsersApi;
import ru.yandex.market.pricelabs.api.api.QueryHelper;
import ru.yandex.market.pricelabs.api.program.AdvProgramApiConfig;
import ru.yandex.market.pricelabs.api.search.SearchConfig;
import ru.yandex.market.pricelabs.api.services.balance.BalanceService;
import ru.yandex.market.pricelabs.timing.TimingConfig;

@ContextHierarchy(
        @ContextConfiguration(name = "api", classes = {
                AbstractApiSpringConfiguration.ApiConfiguration.class
        })
)
public class AbstractApiSpringConfiguration extends AbstractSpringConfiguration {

    @Configuration
    @PropertySource({
            "classpath:api.properties",
            "classpath:unittest/api.properties"
    })
    @Import({SearchConfig.class,
            QueryHelper.class,
            PublicApi.class,
            PublicAutostrategiesApi.class,
            PublicLogsApi.class,
            PublicMainApi.class,
            PublicModelbidsApi.class,
            PublicNotifyApi.class,
            PublicPartnerApi.class,
            PublicRecommendationsApi.class,
            PublicScheduleApi.class,
            PublicUsersApi.class,
            PublicProgramApi.class,
            TimingConfig.class,
            AdvProgramApiConfig.class
    })
    public static class ApiConfiguration {

        @Bean(initMethod = "start", destroyMethod = "close")
        public MockWebServer mockWebServerTms() {
            var webServer = Basic.mockWebServer();
            webServer.setDispatcher(Basic.getAgnosticDispatcher());
            return webServer;
        }

        @Bean
        public String tmsUrl(@Qualifier("mockWebServerTms") MockWebServer mockWebServerTms) {
            return mockWebServerTms.url("shared_api/").toString();
        }

        @Bean
        public Supplier<String> tvmTicketSource() {
            return () -> "ticket-" + timeSource().getMillis();
        }

        @Bean
        public BalanceService balanceService() {
            return uid -> Optional.empty();
        }
    }
}
