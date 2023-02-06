package ru.yandex.market.pvz.core.domain.logbroker.sc.consume;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.capacity.PickupPointCapacityCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.capacity.PickupPointCapacityParams;
import ru.yandex.market.pvz.core.domain.pickup_point.capacity.PickupPointCapacityQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.sc.internal.model.ScOrderDiffDto;
import ru.yandex.market.sc.internal.model.ScPvzEventType;
import ru.yandex.market.sc.internal.model.ScPvzPayloadDto;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ScDropoffDiffEventListenerTest {

    private static final LocalDate SHIPMENT_DATE = LocalDate.of(2021, 8, 7);

    private final TestPickupPointFactory pickupPointFactory;
    private final ScDropoffDiffEventListener scDropoffDiffEventListener;
    private final PickupPointCapacityQueryService pickupPointCapacityQueryService;
    private final PickupPointCapacityCommandService pickupPointCapacityCommandService;

    @Test
    void testDiffAppliedIfNoCapacityRecord() {
        int diff = 5;
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();

        scDropoffDiffEventListener.handle(new ScPvzPayloadDto<>(
                ScPvzEventType.DROPOFF_ORDER_COUNT_DIFF,
                Instant.now(),
                new ScOrderDiffDto(pickupPoint.getLmsId(), SHIPMENT_DATE, diff)
        ));

        assertThat(pickupPointCapacityQueryService.getAll()).containsExactlyInAnyOrderElementsOf(List.of(
                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint.getId())
                        .date(SHIPMENT_DATE)
                        .dropoffCount(diff)
                        .build(),

                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint.getId())
                        .date(SHIPMENT_DATE.minusDays(1))
                        .dropoffCount(diff)
                        .build()
        ));
    }

    @Test
    void testDiffAppliedToExistingCapacityRecord() {
        int sourceCount = 6;
        int diff = -2;

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointCapacityCommandService.applyDropoffCapacityDiff(
                pickupPoint.getLmsId(), List.of(SHIPMENT_DATE), sourceCount);

        scDropoffDiffEventListener.handle(new ScPvzPayloadDto<>(
                ScPvzEventType.DROPOFF_ORDER_COUNT_DIFF,
                Instant.now(),
                new ScOrderDiffDto(pickupPoint.getLmsId(), SHIPMENT_DATE, diff)
        ));

        assertThat(pickupPointCapacityQueryService.getAll()).containsExactlyInAnyOrderElementsOf(List.of(
                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint.getId())
                        .date(SHIPMENT_DATE)
                        .dropoffCount(sourceCount + diff)
                        .build(),

                PickupPointCapacityParams.builder()
                        .pickupPointId(pickupPoint.getId())
                        .date(SHIPMENT_DATE.minusDays(1))
                        .dropoffCount(0) //should not be less than zero
                        .build()
        ));
    }


}
