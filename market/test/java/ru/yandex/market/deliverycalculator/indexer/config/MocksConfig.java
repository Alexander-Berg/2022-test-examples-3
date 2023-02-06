package ru.yandex.market.deliverycalculator.indexer.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.common.util.region.ExtendedRegionTreePlainTextBuilder;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTreeBuilder;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.deliverycalculator.indexer.service.datacamp.DataCampTechCommand;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.storage.util.PooledIdGenerator;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonService;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.mock;

/**
 * Конфигурация, используемая в тестах, которая содержит все зависимости, для которых требуется создавать моки.
 */
@Configuration
public class MocksConfig {

    @Bean
    @Primary
    public Clock clock() {
        return Clock.fixed(
                LocalDateTime.of(2019, 12, 1, 15, 35).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );
    }

    @Bean
    public MdsS3Client mdsS3Client() {
        return mock(MdsS3Client.class);
    }

    @Bean
    public ResourceLocationFactory resourceLocationFactory() {
        return mock(ResourceLocationFactory.class);
    }

    @Bean
    public CloseableHttpClient httpClient() {
        return mock(CloseableHttpClient.class);
    }

    @Bean
    public LMSClient lmsExportClient() {
        return mock(LMSClient.class);
    }

    @Bean
    public LogbrokerEventPublisher<DataCampTechCommand> datacampTechCommandsLogbrokerService() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public RegionTreeBuilder<Region> regionRegionTreeBuilder() {
        return mock(ExtendedRegionTreePlainTextBuilder.class);
    }

    @Bean
    @Primary
    public PooledIdGenerator modifiersIdGenerator() {
        return mock(PooledIdGenerator.class);
    }

    @Bean
    public TariffInfoProvider tariffInfoProvider() {
        return mock(TariffInfoProvider.class);
    }

    @Bean
    @Primary
    public BoilingSolomonService boilingSolomonServiceSpy(final BoilingSolomonService boilingSolomonService) {
        return Mockito.spy(boilingSolomonService);
    }

    @Bean
    @Primary
    public TvmClient tvmClient() {
        return mock(TvmClient.class);
    }

    @Bean
    @Primary
    public Terminal terminal() {
        return mock(Terminal.class);
    }
}
