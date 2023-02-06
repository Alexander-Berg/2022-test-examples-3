package ru.yandex.market.pvz.core.domain.logbroker.sc.consume;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.capacity.PickupPointCapacityParams;
import ru.yandex.market.pvz.core.domain.pickup_point.capacity.PickupPointCapacityQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.sc.internal.model.ScOrderCountSnapshotDto;
import ru.yandex.market.sc.internal.model.ScOrderCountSnapshotDto.ScOrderCountDto;
import ru.yandex.market.sc.internal.model.ScPvzEventType;
import ru.yandex.market.sc.internal.model.ScPvzPayloadDto;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.NEW_DROP_OFF_CAPACITY_ENABLED;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ScDropoffSnapshotEventListenerTest {
    private static final LocalDate SHIPMENT_DATE = LocalDate.of(2021, 8, 7);

    private final TestPickupPointFactory pickupPointFactory;
    private final ScDropoffSnapshotEventListener scDropoffSnapshotEventListener;
    private final PickupPointCapacityQueryService pickupPointCapacityQueryService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @BeforeEach
    void setUp() {
        configurationGlobalCommandService.setValue(NEW_DROP_OFF_CAPACITY_ENABLED, true);
    }

    @Test
    void testSaveSnapshot() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        PickupPoint pickupPoint2 = pickupPointFactory.createPickupPoint();

        scDropoffSnapshotEventListener.handle(new ScPvzPayloadDto<>(
                ScPvzEventType.DROPOFF_ORDER_COUNT_SNAPSHOT,
                Instant.now(),
                new ScOrderCountSnapshotDto(List.of(
                        new ScOrderCountDto(pickupPoint.getLmsId(), SHIPMENT_DATE, 10),
                        new ScOrderCountDto(pickupPoint.getLmsId(), SHIPMENT_DATE.minusDays(1), 5),
                        new ScOrderCountDto(pickupPoint2.getLmsId(), SHIPMENT_DATE, 11),
                        new ScOrderCountDto(pickupPoint2.getLmsId(), SHIPMENT_DATE.minusDays(1), 6),
                        new ScOrderCountDto(123L, SHIPMENT_DATE.minusDays(1), 5),
                        new ScOrderCountDto(1234L, SHIPMENT_DATE.minusDays(1), 5)
                ))
        ));

        assertThat(pickupPointCapacityQueryService.getAll()).containsExactlyInAnyOrderElementsOf(List.of(
                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint.getId())
                        .date(SHIPMENT_DATE)
                        .dropoffCount(10)
                        .build(),

                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint.getId())
                        .date(SHIPMENT_DATE.minusDays(1))
                        .dropoffCount(15)
                        .build(),

                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint2.getId())
                        .date(SHIPMENT_DATE)
                        .dropoffCount(11)
                        .build(),

                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint2.getId())
                        .date(SHIPMENT_DATE.minusDays(1))
                        .dropoffCount(17)
                        .build()
        ));
    }

    @Test
    void testSnapshotOverwritesOldData() {
        int count = 15;
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        PickupPoint pickupPoint2 = pickupPointFactory.createPickupPoint();

        scDropoffSnapshotEventListener.handle(new ScPvzPayloadDto<>(
                ScPvzEventType.DROPOFF_ORDER_COUNT_SNAPSHOT,
                Instant.now(),
                new ScOrderCountSnapshotDto(List.of(
                        new ScOrderCountDto(pickupPoint.getLmsId(), SHIPMENT_DATE, count),
                        new ScOrderCountDto(pickupPoint2.getLmsId(), SHIPMENT_DATE, count)
                ))
        ));

        scDropoffSnapshotEventListener.handle(new ScPvzPayloadDto<>(
                ScPvzEventType.DROPOFF_ORDER_COUNT_SNAPSHOT,
                Instant.now(),
                new ScOrderCountSnapshotDto(List.of(
                        new ScOrderCountDto(pickupPoint.getLmsId(), SHIPMENT_DATE, count + 1),
                        new ScOrderCountDto(pickupPoint2.getLmsId(), SHIPMENT_DATE, count + 2)
                ))
        ));

        assertThat(pickupPointCapacityQueryService.getAll()).containsExactlyInAnyOrderElementsOf(List.of(
                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint.getId())
                        .date(SHIPMENT_DATE)
                        .dropoffCount(count + 1)
                        .build(),

                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint2.getId())
                        .date(SHIPMENT_DATE)
                        .dropoffCount(count + 2)
                        .build()
        ));
    }

}
