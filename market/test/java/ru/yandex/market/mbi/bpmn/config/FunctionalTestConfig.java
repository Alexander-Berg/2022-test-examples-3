package ru.yandex.market.mbi.bpmn.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageWarehouseGroupClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.bpmn.client.Ff4shopsClient;
import ru.yandex.market.mbi.bpmn.client.PartnerStatusServiceClient;
import ru.yandex.market.mbi.bpmn.client.RetryableTarifficatorClient;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class FunctionalTestConfig {

    @MockBean
    private Ff4shopsClient ff4shopsClient;

    @MockBean
    private MbiApiClient mbiApiClient;

    @MockBean
    private MbiOpenApiClient mbiOpenApiClient;

    @MockBean
    private StockStorageWarehouseGroupClient stockStorageWarehouseGroupClient;

    @MockBean
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @MockBean(name = "saasService")
    private SaasService saasService;

    @MockBean
    private RetryableTarifficatorClient tarifficatorClient;

    @MockBean
    private PartnerNotificationClient partnerNotificationClient;

    @MockBean
    private PartnerStatusServiceClient partnerStatusServiceClient;

    @Bean
    public CloseableHttpClient httpClient() {
        return mock(CloseableHttpClient.class);
    }

    @Bean
    public Tvm2 tvm() {
        return mock(Tvm2.class);
    }

    @Bean
    @Qualifier("ticketParserTvmClient")
    public TvmClient iTvmClient() {
        return mock(TvmClient.class);
    }

    @Bean
    @Primary
    public static PropertyPlaceholderConfigurer propertyConfigurerMock(
            PropertySourcesPlaceholderConfigurer propertyConfigurer
    ) {
        propertyConfigurer.setOrder(0);
        propertyConfigurer.setIgnoreUnresolvablePlaceholders(true);
        PropertyPlaceholderConfigurer configurer = mock(PropertyPlaceholderConfigurer.class);
        doReturn(Integer.MAX_VALUE).
                when(configurer).getOrder();
        return configurer;
    }

    @Bean
    public MbiOpenApiClient mbiOpenApiClient() {
        return mock(MbiOpenApiClient.class);
    }
}
