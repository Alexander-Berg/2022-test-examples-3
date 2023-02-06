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

import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;
import ru.yandex.market.mbi.logprocessor.config.SpringApplicationConfig;
import ru.yandex.market.mbi.logprocessor.util.HostNameProvider;



@ActiveProfiles(profiles = {"largeTest"})
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        })
@TestPropertySource(locations = {"classpath:large-test.properties"})
@Import(LargeTest.TestConfig.class)
public class LargeTest {
    @Autowired
    protected TestRestTemplate testRestTemplate;

    @Autowired
    protected Clock clock;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public HostNameProvider hostNameProvider() {
            return () -> "testHostName";
        }

        @Bean
        public Clock clock() {
            return Mockito.mock(Clock.class);
        }

        @Bean
        public YtCleanupApplicationListener ytCleanupApplicationListener(
                @Value("#{ytTablesPathProvider.logDynTablesPath}") String commonPath,
                @Value("#{ytTablesPathProvider.logHistoryTablesPath}") String pushApiLogsHistorySourcePath,
                @Value("${market.mbi-log-processor.source.yt.table.push_api_logs_history.cluster}")
                        String pushApiLogsHistoryCluster
                ) {
            return new YtCleanupApplicationListener(commonPath, pushApiLogsHistorySourcePath, pushApiLogsHistoryCluster);
        }
    }
}

