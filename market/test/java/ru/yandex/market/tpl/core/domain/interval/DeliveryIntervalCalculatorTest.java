package ru.yandex.market.tpl.core.domain.interval;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.pickup.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DeliveryIntervalCalculatorTest {

    private final DeliveryIntervalCalculator deliveryIntervalCalculator;
    private final TestDataFactory testDataFactory;

    @Test
    void calculate() {
        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 123L,
                DeliveryService.DEFAULT_DS_ID);
        LocalDate deliveryDate = LocalDate.of(2021, Month.APRIL, 26);

        Interval defaultIntervalForPickupPointThatWasntSynced = deliveryIntervalCalculator.calculate(
                deliveryDate,
                null,
                pickupPoint,
                DeliveryService.DEFAULT_DS_ID
        );
        assertThat(defaultIntervalForPickupPointThatWasntSynced).isEqualTo(DateTimeUtil.PICKUP_INTERVAL
                .toInterval(deliveryDate, DateTimeUtil.DEFAULT_ZONE_ID));

        pickupPoint.setLastSyncAt(Instant.now());
        pickupPoint.setLastScheduleSyncAt(Instant.now());
        pickupPoint.putScheduleRecord(
                DayOfWeek.MONDAY,
                new LocalTimeInterval(LocalTime.of(9, 0), LocalTime.of(14, 0))
        );

        Interval intervalCalculatedFromPickupPointSchedule = deliveryIntervalCalculator.calculate(
                deliveryDate,
                null,
                pickupPoint,
                DeliveryService.DEFAULT_DS_ID
        );
        Instant left2 = Instant.parse("2021-04-26T06:00:00.00Z");
        Instant right2 = Instant.parse("2021-04-26T11:00:00.00Z");
        assertThat(intervalCalculatedFromPickupPointSchedule).isEqualTo(new Interval(left2, right2));

        Interval defaultPickupPointIntervalShiftedToTomorrow = deliveryIntervalCalculator.calculate(
                LocalDate.of(2021, Month.APRIL, 27),
                null,
                pickupPoint,
                DeliveryService.DEFAULT_DS_ID
        );
        Instant left3 = Instant.parse("2021-04-28T03:00:00.00Z");
        Instant right3 = Instant.parse("2021-04-28T12:00:00.00Z");
        assertThat(defaultPickupPointIntervalShiftedToTomorrow).isEqualTo(new Interval(left3, right3));

        Interval clientIntervalFromDsApiRequest = deliveryIntervalCalculator.calculate(
                deliveryDate,
                new ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval(
                        LocalTime.of(12, 0),
                        LocalTime.of(16, 0)
                ),
                null,
                DeliveryService.DEFAULT_DS_ID
        );
        Instant left4 = Instant.parse("2021-04-26T09:00:00.00Z");
        Instant right4 = Instant.parse("2021-04-26T13:00:00.00Z");
        assertThat(clientIntervalFromDsApiRequest).isEqualTo(new Interval(left4, right4));
    }
}
