package ru.yandex.market.ff4shops.config;

import javax.annotation.Nonnull;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.common.idx.IndexerApiClient;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.ff4shops.delivery.courier.DeliveryClient;
import ru.yandex.market.ff4shops.delivery.stocks.DatacampMessageLogbrokerEvent;
import ru.yandex.market.ff4shops.lgw.LogisticApiRequestsClientService;
import ru.yandex.market.ff4shops.offer.datacamp.DataCampOfferService;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.logistic.gateway.client.LogisticApiRequestsClient;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientWrapper;
import ru.yandex.market.logistics4shops.client.api.OutboundApi;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.RestMbiApiClient;
import ru.yandex.market.mbi.api.client.config.MbiApiClientConfig;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.mock;

/**
 * @author fbokovikov
 */
@Configuration
public class MockConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        final var configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setOrder(-1);
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }

    @Bean
    public DataCampOfferService dataCampOfferService() {
        return Mockito.mock(DataCampOfferService.class);
    }

    @Bean
    public IndexerApiClient indexerApiClient() {
        return Mockito.mock(IndexerApiClient.class);
    }

    @Bean
    public IndexerApiClient planeshiftApiClient() {
        return Mockito.mock(IndexerApiClient.class);
    }

    @Bean
    public LMSClient lmsClient() {
        return Mockito.mock(LMSClient.class);
    }

    @Bean
    public MdsS3Client mdsS3Client() {
        return Mockito.mock(MdsS3Client.class);
    }

    @Bean
    public CheckouterAPI checkouterAPI() {
        return Mockito.mock(CheckouterAPI.class);
    }

    @Bean
    public LomClient lomClietn() {
        return Mockito.mock(LomClient.class);
    }

    @Bean
    public DeliveryClient deliveryClient() {
        return Mockito.mock(DeliveryClient.class);
    }

    @Bean
    public JdbcTemplate yqlJdbcTemplate() {
        return Mockito.mock(JdbcTemplate.class);
    }

    @Bean
    public TvmClient ticketParserTvmClient() {
        return Mockito.mock(TvmClient.class);
    }

    @Bean
    public DataCampClient dataCampShopClient() {
        return Mockito.mock(DataCampClient.class);
    }

    @Bean
    public Tvm2 tvm2() {
        return Mockito.mock(Tvm2.class);
    }

    @Bean
    public LogisticApiRequestsClient logisticApiRequestsClient() {
        return Mockito.mock(LogisticApiRequestsClient.class);
    }

    @Bean
    @Primary
    public Terminal terminal() {
        return mock(Terminal.class);
    }

    @Bean
    @Qualifier("logbrokerPartnerApiStockEventPublisher")
    public LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerPartnerApiStockEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public LogbrokerCluster logbrokerCluster() {
        return mock(LogbrokerCluster.class);
    }

    @Bean
    public OutboundApi outboundApi() {
        return Mockito.mock(OutboundApi.class);
    }

    @Bean
    public TvmClientApi tvmClientApi() {
        return Mockito.mock(TvmClientWrapper.class);
    }

    @Configuration
    static class TestMbiApiClientConfig extends MbiApiClientConfig {
        @Value("${mbi.api.url}")
        private String mbiApiUrl;

        @Bean
        @Nonnull
        @Override
        public RestTemplate mbiApiRestTemplate() {
            return super.mbiApiRestTemplate();
        }

        @Bean
        public MbiApiClient mbiApiClient(RestTemplate mbiApiRestTemplate) {
            return new RestMbiApiClient(mbiApiRestTemplate, mbiApiUrl);
        }
    }

    @Bean
    public PartnerNotificationClient partnerNotificationClient() {
        return Mockito.mock(PartnerNotificationClient.class);
    }

    @Bean
    public NesuClient nesuClient() {
        return Mockito.mock(NesuClient.class);
    }

    @Bean
    public LogisticApiRequestsClientService logisticApiRequestsClientService() {
        return mock(LogisticApiRequestsClientService.class);
    }

    @Bean
    public MbiOpenApiClient mbiOpenApiClient() {
        return mock(MbiOpenApiClient.class);
    }
}
