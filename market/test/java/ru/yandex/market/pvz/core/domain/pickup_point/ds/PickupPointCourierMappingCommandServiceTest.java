package ru.yandex.market.pvz.core.domain.pickup_point.ds;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointCourierMappingCommandServiceTest {

    private static final long COURIER_DELIVERY_SERVICE_ID = 9985540L;

    private final TestPickupPointFactory pickupPointFactory;

    private final PickupPointCourierMappingRepository pickupPointCourierMappingRepository;
    private final PickupPointCourierMappingCommandService courierMappingCommandService;

    @Test
    void createMappings() {
        var pickupPoint1 = pickupPointFactory.createPickupPoint();

        var mapping = courierMappingCommandService.createOrUpdate(pickupPoint1.getId(), COURIER_DELIVERY_SERVICE_ID);
        PickupPointCourierMappingParams actual = new PickupPointCourierMappingParams(
                mapping.getPickupPoint().getPvzMarketId(), mapping.getCourierDeliveryServiceId());

        PickupPointCourierMappingParams expected =
                new PickupPointCourierMappingParams(pickupPoint1.getPvzMarketId(), COURIER_DELIVERY_SERVICE_ID);

        assertThat(actual).isEqualTo(expected);
        assertThat(pickupPointCourierMappingRepository.findAll()).hasSize(1);
        assertThat(pickupPointCourierMappingRepository.findById(pickupPoint1.getId())).isNotEmpty();
        assertThat(pickupPointCourierMappingRepository.findByIdOrThrow(pickupPoint1.getId())
                .getCourierDeliveryServiceId()).isEqualTo(COURIER_DELIVERY_SERVICE_ID);
    }

    @Test
    void updateMappings() {
        var pickupPoint1 = pickupPointFactory.createPickupPoint();

        courierMappingCommandService.createOrUpdate(pickupPoint1.getId(), COURIER_DELIVERY_SERVICE_ID);

        var mapping = courierMappingCommandService.createOrUpdate(pickupPoint1.getId(),
                COURIER_DELIVERY_SERVICE_ID + 1);
        PickupPointCourierMappingParams actual = new PickupPointCourierMappingParams(
                mapping.getPickupPoint().getPvzMarketId(), mapping.getCourierDeliveryServiceId());

        PickupPointCourierMappingParams expected =
                new PickupPointCourierMappingParams(pickupPoint1.getPvzMarketId(), COURIER_DELIVERY_SERVICE_ID + 1);

        assertThat(actual).isEqualTo(expected);
        assertThat(pickupPointCourierMappingRepository.findAll()).hasSize(1);
        assertThat(pickupPointCourierMappingRepository.findById(pickupPoint1.getId())).isNotEmpty();
        assertThat(pickupPointCourierMappingRepository.findByIdOrThrow(pickupPoint1.getId())
                .getCourierDeliveryServiceId()).isEqualTo(COURIER_DELIVERY_SERVICE_ID + 1);
    }

    @Test
    void pickupPointNotFound() {
        assertThatThrownBy(() -> courierMappingCommandService.createOrUpdate(1000, COURIER_DELIVERY_SERVICE_ID))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
        assertThat(pickupPointCourierMappingRepository.findAll()).isEmpty();
    }

    @Data
    private static class PickupPointCourierMappingParams {
        private final long pvzMarketId;
        private final long courierDeliveryServiceId;
    }
}
