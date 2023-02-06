package ru.yandex.market.pvz.core.test.factory;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.ds.PickupPointCourierMapping;
import ru.yandex.market.pvz.core.domain.pickup_point.ds.PickupPointCourierMappingCommandService;

@Transactional
public class TestPickupPointCourierMappingFactory extends TestObjectFactory {

    @Autowired
    private TestPickupPointFactory pickupPointFactory;

    @Autowired
    private PickupPointCourierMappingCommandService pickupPointCourierMappingCommandService;

    public PickupPointCourierMapping create(PickupPointCourierMappingTestParamsBuilder builder) {
        if (builder.getPickupPoint() == null) {
            builder.setPickupPoint(pickupPointFactory.createPickupPoint());
        }
        return pickupPointCourierMappingCommandService.createOrUpdate(
                builder.getPickupPoint().getId(),
                builder.getParams().getCourierDeliveryServiceId());
    }

    @Data
    @Builder
    public static class PickupPointCourierMappingTestParamsBuilder {

        @Builder.Default
        private PickupPointCourierMappingTestParams params = PickupPointCourierMappingTestParams.builder().build();

        @Builder.Default
        private PickupPoint pickupPoint;
    }

    @Data
    @Builder
    public static class PickupPointCourierMappingTestParams {

        public static final long DEFAULT_COURIER_DELIVERY_SERVICE_ID = 1005477L;

        @Builder.Default
        private long courierDeliveryServiceId = DEFAULT_COURIER_DELIVERY_SERVICE_ID;
    }
}
