package ru.yandex.market.pvz.core.domain.delivery_service;

import java.time.LocalDate;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestCourierDsDayOffFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParams.DEFAULT_COURIER_DELIVERY_SERVICE_ID;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CourierDsDayOffQueryServiceTest {

    private final TestPickupPointFactory pickupPointFactory;
    private final TestPickupPointCourierMappingFactory pickupPointCourierMappingFactory;
    private final TestCourierDsDayOffFactory courierDsDayOffFactory;

    private final CourierDsDayOffQueryService courierDsDayOffQueryService;

    @Test
    void getDayOffs() {
        LocalDate sinceDate = LocalDate.of(2021, 2, 9);
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointCourierMappingFactory.create(
                TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParamsBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build());

        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.minusDays(2))
                .build());
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.minusDays(1))
                .build());
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate)
                .build());
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.plusDays(1))
                .build());
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.plusDays(2))
                .build());
        courierDsDayOffFactory.create(TestCourierDsDayOffFactory.CourierDsDayOffParams.builder()
                .courierDeliveryServiceId(DEFAULT_COURIER_DELIVERY_SERVICE_ID)
                .dayOff(sinceDate.plusDays(5))
                .build());

        Set<LocalDate> actual = courierDsDayOffQueryService.getDayOffsFromDate(pickupPoint.getId(), sinceDate);

        Set<LocalDate> expected = Set.of(
                sinceDate, sinceDate.plusDays(1), sinceDate.plusDays(2), sinceDate.plusDays(5));

        assertThat(actual).isEqualTo(expected);
    }
}
