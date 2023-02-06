package ru.yandex.market.clab.test.config;

import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.clab.common.config.component.RemoteServicesConfiguration;
import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mboc.http.MboMappingsService;

/**
 * @author anmalysh
 * @since 26.11.2018
 */
@Configuration
@Profile("test")
@SuppressWarnings("checkstyle:magicnumber")
public class TestRemoteServicesConfiguration implements RemoteServicesConfiguration {

    @Override
    public MboMappingsService mboMappingsService() {
        return null;
    }

    @Override
    public MboCategoryService mboCategoryService() {
        return null;
    }

    @Bean
    public DeliveryParams deliveryParamsService() {
        return new DeliveryParams() {
            @Override
            public MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse searchFulfilmentSskuParams(
                MboMappingsForDelivery.SearchFulfilmentSskuParamsRequest searchFulfilmentSskuParamsRequest) {
                return MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                    .addAllFulfilmentInfo(
                        searchFulfilmentSskuParamsRequest.getKeysList().stream()
                        .map(key -> MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                            .setSupplierId(key.getSupplierId())
                            .setShopSku(key.getShopSku())
                            .setAllowInbound(true)
                            .setAllowCargoType(true)
                            .build())
                        .collect(Collectors.toList())
                    )
                    .build();
            }

            @Override
            public MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse searchFulfillmentSskuParamsForInterval(
                MboMappingsForDelivery.SearchFulfillmentSskuParamsForIntervalRequest searchFulfillmentSskuParamsForIntervalRequest) {
                return null;
            }

            @Override
            public MonitoringResult ping() {
                return null;
            }

            @Override
            public MonitoringResult monitoring() {
                return null;
            }
        };
    }
}
