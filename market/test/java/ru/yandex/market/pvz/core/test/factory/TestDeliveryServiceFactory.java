package ru.yandex.market.pvz.core.test.factory;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.approve.delivery_service.SetupStage;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryServiceCommandService;

public class TestDeliveryServiceFactory extends TestObjectFactory {

    @Autowired
    private DeliveryServiceCommandService deliveryServiceCommandService;

    public DeliveryService createDeliveryService() {
        return createDeliveryService(DeliveryServiceParams.builder().build());
    }

    public DeliveryService createDeliveryService(DeliveryServiceParams params) {
        var deliveryService = createNotSetupDeliveryService(params);
        return deliveryServiceCommandService.updateSetupStage(deliveryService, SetupStage.PARTNER_ACTIVATE);
    }

    public DeliveryService createNotSetupDeliveryService() {
        return createNotSetupDeliveryService(DeliveryServiceParams.builder().build());
    }

    public DeliveryService createNotSetupDeliveryService(DeliveryServiceParams params) {
        return deliveryServiceCommandService.getOrCreateDeliveryService(
                params.getId(),
                params.getName(),
                params.getToken(),
                params.getSortingCenterAddress()
        );
    }

    public DeliveryService updateSetupStage(long deliveryServiceId, SetupStage setupStage) {
        return deliveryServiceCommandService.updateSetupStage(deliveryServiceId, setupStage);
    }

    @Data
    @Builder
    public static class DeliveryServiceParams {

        public static final String DEFAULT_NAME = "ASHOT DELIVERY SOLUTIONS UNLIMITED";
        public static final String DEFAULT_SORTING_CENTER_ADDRESS = "г. Москва, Дмитровское шоссе, 157с12";

        @Builder.Default
        private Long id = RandomUtils.nextLong();

        @Builder.Default
        private String name = DEFAULT_NAME;

        @Builder.Default
        private String token = randomString(16);

        @Builder.Default
        private String sortingCenterAddress = DEFAULT_SORTING_CENTER_ADDRESS;
    }
}
