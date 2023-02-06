package ru.yandex.market.fintech.banksint.config;

import java.time.Clock;
import java.time.Instant;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.fintech.banksint.job.TestJobImpl;
import ru.yandex.market.fintech.banksint.service.mds.MdsS3Service;
import ru.yandex.market.fintech.banksint.yt.ScoringDataAsyncMonitoringService;

import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {
    @Bean
    public Clock timeProvider() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public Terminal terminal() {
        return Mockito.mock(Terminal.class);
    }

    @Bean
    public MdsS3Service mdsS3Service() {
        return Mockito.mock(MdsS3Service.class);
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer configurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setOrder(-1);
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

    @Bean
    public ScoringDataAsyncMonitoringService scoringDataAsyncMonitoringService() {
        var result = Mockito.mock(ScoringDataAsyncMonitoringService.class);
        when(result.getCurrentMonitoringState())
                .thenReturn(new ScoringDataAsyncMonitoringService.ScoringDataMonitoringState(
                        Instant.now(), MonitoringStatus.OK, ""
                ));

        return result;
    }

    @Bean
    public TestJobImpl testJob() {
        return new TestJobImpl();
    }
}
