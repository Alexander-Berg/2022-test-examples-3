package ru.yandex.market.core.config;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import ru.yandex.market.core.delivery.AsyncTarifficatorService;
import ru.yandex.market.core.delivery.RetryableTarifficatorClient;
import ru.yandex.market.core.delivery.TarificatorThrowingExceptionResponseHandler;
import ru.yandex.market.core.delivery.service.ShopMetaDataForTarifficatorCreator;
import ru.yandex.market.core.delivery.tariff.service.DeliveryTariffService;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.supplier.dao.PartnerFulfillmentLinkDao;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupDeliveryServiceApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupPaymentApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupStatusApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupTariffApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.ShopDeliveryStateApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.ShopMetaApi;
import ru.yandex.market.logistics.util.client.ClientUtilsFactory;

import static org.mockito.Mockito.mock;

@Configuration
public class TarifficatorClientFunctionalTestConfig {

    @Bean
    WireMockConfiguration tarificatorWireMockConfiguration() {
        return WireMockConfiguration.wireMockConfig().dynamicPort();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    WireMockServer tarifficatorWireMockServer(WireMockConfiguration tarificatorWireMockConfiguration) {
        return new WireMockServer(tarificatorWireMockConfiguration);
    }

    @Bean
    public Retrofit tarifficatorRetrofit(WireMockServer tarifficatorWireMockServer) {
        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        var retrofitBuilder = new Retrofit.Builder()
                .client(client)
                .baseUrl(tarifficatorWireMockServer.baseUrl())
                .validateEagerly(true)
                .addConverterFactory(JacksonConverterFactory.create(ClientUtilsFactory.getObjectMapper()));

        return retrofitBuilder.build();
    }

    @Bean
    public RegionGroupApi tarifficatorRegionGroupApi(Retrofit tarifficatorRetrofit) {
        return tarifficatorRetrofit.create(RegionGroupApi.class);
    }

    @Bean
    public RegionGroupDeliveryServiceApi tarifficatorRegionGroupDeliveryServiceApi(Retrofit tarifficatorRetrofit) {
        return tarifficatorRetrofit.create(RegionGroupDeliveryServiceApi.class);
    }

    @Bean
    public RegionGroupPaymentApi tarifficatorRegionGroupPaymentApi(Retrofit tarifficatorRetrofit) {
        return tarifficatorRetrofit.create(RegionGroupPaymentApi.class);
    }

    @Bean
    public RegionGroupStatusApi tarifficatorRegionGroupStatusApi(Retrofit tarifficatorRetrofit) {
        return tarifficatorRetrofit.create(RegionGroupStatusApi.class);
    }

    @Bean
    public RegionGroupTariffApi tarifficatorRegionGroupTariffApi(Retrofit tarifficatorRetrofit) {
        return tarifficatorRetrofit.create(RegionGroupTariffApi.class);
    }

    @Bean
    public ShopMetaApi tarifficatorShopMetaApi(Retrofit tarifficatorRetrofit) {
        return tarifficatorRetrofit.create(ShopMetaApi.class);
    }

    @Bean
    public ShopDeliveryStateApi tarifficatorDeliveryStateApi(Retrofit tarifficatorRetrofit) {
        return tarifficatorRetrofit.create(ShopDeliveryStateApi.class);
    }

    @Bean
    public RetryableTarifficatorClient retryableTarifficatorClient(RegionGroupApi regionGroupApi,
                                                                   RegionGroupDeliveryServiceApi dsApi,
                                                                   RegionGroupPaymentApi paymentApi,
                                                                   RegionGroupStatusApi statusApi,
                                                                   RegionGroupTariffApi tariffApi,
                                                                   ShopMetaApi shopMetaApi,
                                                                   ShopDeliveryStateApi tarifficatorDeliveryStateApi) {
        return new RetryableTarifficatorClient(
                retryTemplate(),
                regionGroupApi,
                dsApi,
                paymentApi,
                statusApi,
                tariffApi,
                shopMetaApi,
                tarifficatorDeliveryStateApi
        );
    }

    @Bean
    public ShopMetaDataForTarifficatorCreator shopMetaDataForTarifficatorCreator(
            DeliveryTariffService deliveryTariffService,
            DatasourceService datasourceService,
            PartnerFulfillmentLinkDao fulfillmentLinkDao,
            PartnerPlacementProgramService partnerPlacementProgramService
    ) {
        return new ShopMetaDataForTarifficatorCreator(deliveryTariffService,
                datasourceService,
                fulfillmentLinkDao,
                partnerPlacementProgramService);
    }

    private RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());

        ExceptionClassifierRetryPolicy retryPolicy = new ExceptionClassifierRetryPolicy();
        retryPolicy.setPolicyMap(Map.of(
                TarificatorThrowingExceptionResponseHandler.TarificatorException.class, new NeverRetryPolicy(),
                Throwable.class, new SimpleRetryPolicy(1)));
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    @SuppressWarnings("checkstyle:parameterNumber")
    public AsyncTarifficatorService asyncTarifficatorService() {
        return mock(AsyncTarifficatorService.class);
    }
}
