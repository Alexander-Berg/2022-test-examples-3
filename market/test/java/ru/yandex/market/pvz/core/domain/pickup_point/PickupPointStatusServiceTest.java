package ru.yandex.market.pvz.core.domain.pickup_point;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointCalendarLogFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_FROM;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointStatusServiceTest {

    private final TestableClock clock;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestPickupPointCalendarLogFactory pickupPointCalendarLogFactory;

    private final PickupPointStatusService pickupPointStatusService;

    private final TestBrandRegionFactory brandRegionFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestLegalPartnerTerminationFactory terminationFactory;

    @Test
    void needToStartShiftWithNoCalendarLog() {
        var pickupPoint = createActivePickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime now = LocalDateTime.of(2021, 2, 19, DEFAULT_TIME_FROM.plusHours(2).getHour(), 0);
        clock.setFixed(now.atZone(zone).toInstant(), zone);

        boolean needToStartShift = pickupPointStatusService.needToStartShift(pickupPoint);

        assertThat(needToStartShift).isTrue();
    }

    @Test
    void noNeedToStartShiftOnNotWorkingTime() {
        var pickupPoint = createActivePickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime now = LocalDateTime.of(2021, 2, 19, DEFAULT_TIME_FROM.minusHours(2).getHour(), 0);
        clock.setFixed(now.atZone(zone).toInstant(), zone);

        boolean needToStartShift = pickupPointStatusService.needToStartShift(pickupPoint);

        assertThat(needToStartShift).isFalse();
    }

    @Test
    void noNeedToStartShiftOnHoliday() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .schedule(TestPickupPointFactory.PickupPointScheduleTestParams.builder()
                                .scheduleDays(List.of(
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.TUESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.WEDNESDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.THURSDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.FRIDAY)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SATURDAY)
                                                .isWorkingDay(false)
                                                .build(),
                                        TestPickupPointFactory.PickupPointScheduleDayTestParams.builder()
                                                .dayOfWeek(DayOfWeek.SUNDAY)
                                                .isWorkingDay(false)
                                                .build()
                                ))
                                .build())
                        .build());
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime now = LocalDateTime.of(2021, 2, 20, DEFAULT_TIME_FROM.plusHours(2).getHour(), 0);
        clock.setFixed(now.atZone(zone).toInstant(), zone);

        boolean needToStartShift = pickupPointStatusService.needToStartShift(pickupPoint);

        assertThat(needToStartShift).isFalse();
    }

    @Test
    void shiftAlreadyBeenStarted() {
        var pickupPoint = createActivePickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime now = LocalDateTime.of(2021, 2, 19, DEFAULT_TIME_FROM.plusHours(2).getHour(), 0);

        pickupPointCalendarLogFactory.startShiftAtDate(pickupPoint.getId(), now, zone, clock);

        now = LocalDateTime.of(2021, 2, 19, DEFAULT_TIME_FROM.plusHours(3).getHour(), 0);
        clock.setFixed(now.atZone(zone).toInstant(), zone);
        boolean needToStartShift = pickupPointStatusService.needToStartShift(pickupPoint);

        assertThat(needToStartShift).isFalse();
    }

    @Test
    void noNeedToStartShiftPartnerTerminate() {
        brandRegionFactory.create(TestBrandRegionFactory.BrandRegionTestParams.builder()
                .region("Воронеж")
                .dailyTransmissionThreshold(5)
                .build());

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory
                .forceApprove(legalPartner.getId(), LocalDate.of(2021, 1, 1));

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build());
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .returnAllowed(true)
                        .brandingType(PickupPointBrandingType.FULL)
                        .brandDate(LocalDate.of(2021, 1, 5))
                        .brandRegion("Воронеж")
                        .build());
        terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.DEBT)
                                        .fromTime(OffsetDateTime.now())
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        boolean needToStartShift = pickupPointStatusService.needToStartShift(pickupPoint);
        assertThat(needToStartShift).isFalse();
    }

    private PickupPoint createActivePickupPoint() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        return pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder().active(true).build());
    }
}
