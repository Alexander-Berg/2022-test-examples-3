package ru.yandex.market.tpl.tms.config;

import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.tpl.common.taxi.driver.trackstory.client.api.DefaultTaxiDriverTrackStoryTrackApi;
import ru.yandex.market.tpl.core.domain.pickup.yt.YtPickupPointHolidayMerger;
import ru.yandex.market.tpl.core.domain.pickup.yt.YtPickupPointScheduleMerger;
import ru.yandex.market.tpl.core.external.cms.MboCmsApiClient;
import ru.yandex.market.tpl.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.tpl.core.service.delivery.YtDsService;
import ru.yandex.market.tpl.tms.config.props.SurchargeTrackerProperties;
import ru.yandex.market.tpl.tms.service.external.TplCheckouterExternalService;
import ru.yandex.market.tpl.tms.service.surcharge.SurchargeTrackerSessionFactory;

import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableConfigurationProperties({
        SurchargeTrackerProperties.class,
})
@ComponentScan(basePackages = {
        "ru.yandex.market.tpl.tms.executor",
        "ru.yandex.market.tpl.tms.service",
        "ru.yandex.market.tpl.tms.logbroker",
        "ru.yandex.market.tpl.tms.clientreturn"
})
public class TestTplTmsConfiguration {
    @Bean
    public static SqsQueueProperties sqsQueueProperties() {
        SqsQueueProperties properties = mock(SqsQueueProperties.class);

        Mockito.when(properties.getOutQueue()).thenReturn("courier_out");
        Mockito.when(properties.getSource()).thenReturn("courier");

        return properties;
    }

    @Primary
    @Bean
    public TplCheckouterExternalService mockedCheckouterExternalService() {
        return mock(TplCheckouterExternalService.class);
    }

    @Bean
    public MboCmsApiClient mockedMboCmsApiClient() {
        return mock(MboCmsApiClient.class);
    }

    @Bean
    public YtDsService mockedYtDsService() {
        return mock(YtDsService.class);
    }

    @Bean
    public YtPickupPointScheduleMerger mockedYtPickupPointScheduleMerger() {
        return mock(YtPickupPointScheduleMerger.class);
    }

    @Bean
    public YtPickupPointHolidayMerger mockedYtPickupPointHolidayMerger() {
        return mock(YtPickupPointHolidayMerger.class);
    }

    @Bean
    public CheckouterClient mockedCheckouterClient() {
        return mock(CheckouterClient.class);
    }

    @Bean
    public MbiOpenApiClient mockedMbiOpenApiClient() {
        return mock(MbiOpenApiClient.class);
    }

    @Bean
    public MdbClient mockedMdbClient() {
        return mock(MdbClient.class);
    }

    @Bean
    public DefaultTaxiDriverTrackStoryTrackApi mockedDefaultTaxiDriverTrackStoryTrackApi() {
        return mock(DefaultTaxiDriverTrackStoryTrackApi.class);
    }

    @Primary
    @Bean
    public SurchargeTrackerSessionFactory mockedSurchargeTrackerSessionFactory() {
        return mock(SurchargeTrackerSessionFactory.class);
    }

}
