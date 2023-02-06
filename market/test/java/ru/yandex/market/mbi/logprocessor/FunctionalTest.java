package ru.yandex.market.mbi.logprocessor;

import java.time.Clock;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.mbi.logprocessor.config.SpringApplicationConfig;
import ru.yandex.market.mbi.logprocessor.util.HostNameProvider;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;

@ActiveProfiles(profiles = {"functionalTest"})
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        YaMakeTestExecutionListener.class
})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        })
@TestPropertySource(locations = {"classpath:functional-test.properties"})
@Import(FunctionalTest.TestConfig.class)
public class FunctionalTest {
    @Autowired
    protected TestRestTemplate testRestTemplate;

    @Autowired
    protected Clock clock;

    @Autowired
    protected MbiOpenApiClient mbiOpenApiClient;

    @Autowired
    protected MemCachingService memCachingService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        public HostNameProvider hostNameProvider() {
            return () -> "testHostName";
        }

        @Bean
        public Clock timeProvider() {
            return Mockito.mock(Clock.class);
        }

        @Bean
        public Terminal terminal() {
            return Mockito.mock(Terminal.class);
        }

        @Bean
        public YtCleanupApplicationListener ytCleanupApplicationListener(
                @Value("#{ytTablesPathProvider.logDynTablesPath}") String pushApiLogsTargetPath,
                @Value("#{ytTablesPathProvider.logHistoryTablesPath}")
                        String pushApiLogsHistorySourcePath,
                @Value("${market.mbi-log-processor.source.yt.table.push_api_logs_history.cluster}")
                        String pushApiLogsHistoryCluster
        ) {
            return new YtCleanupApplicationListener(pushApiLogsTargetPath, pushApiLogsHistorySourcePath, pushApiLogsHistoryCluster);
        }

        @Bean
        public MbiOpenApiClient mbiOpenApiClient() {
            return Mockito.mock(MbiOpenApiClient.class);
        }

        @Bean
        public MemCachingService memCachingService() {
            return Mockito.mock(MemCachingService.class);
        }
    }
}
