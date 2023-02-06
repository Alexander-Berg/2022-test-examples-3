package ru.yandex.travel.hotels.common.promo.mir;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.hotels.proto.EMirEligibility;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.TMirPromoStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class MirEligibilityServiceTest {
    private final static Instant justAfterStage2Starts = Instant.parse("2020-10-14T21:01:00.01Z");
    private final static Instant justBeforeStage2Starts = Instant.parse("2020-10-14T20:59:59.00Z");
    private final static Instant justBeforeStage2Ends = Instant.parse("2020-12-05T20:58:59.00Z");
    private final static Instant justAfterStage2Ends = Instant.parse("2020-12-05T21:00:00.01Z");

    private final static LocalDate defaultStage2Checkin = LocalDate.of(2020, 12, 1);
    private final static LocalDate lastStage2Checkout = LocalDate.of(2021, 1, 10);


    private MirEligibilityService service;

    @Before
    public void createService() {
        var props = new MirProperties();
        props.setWhitelist(null);
        var stage2 = new MirProperties.Stage();
        stage2.setStageStarts(Instant.parse("2020-10-14T21:01:00Z"));
        stage2.setStageEnds(Instant.parse("2020-12-05T20:59:00Z"));
        stage2.setLastCheckout(Instant.parse("2021-01-10T20:59:00Z"));
        stage2.setCashbackRate(BigDecimal.valueOf(0.2));
        stage2.setMaxCashbackAmount(BigDecimal.valueOf(20000));
        stage2.setMinLOS(2);
        props.setStages(Map.of("2", stage2));

        var stage3 = new MirProperties.Stage();
        stage3.setStageStarts(Instant.parse("2021-03-17T21:01:00.00Z"));
        stage3.setStageEnds(Instant.parse("2021-06-15T20:59:00.00Z"));
        stage3.setLastCheckout(Instant.parse("2021-06-30T20:59:00.00Z"));
        stage3.setCashbackRate(BigDecimal.valueOf(0.2));
        stage3.setMaxCashbackAmount(BigDecimal.valueOf(20000));
        stage3.setMinLOS(2);

        var stage4 = new MirProperties.Stage();
        stage4.setStageStarts(Instant.parse("2021-06-15T21:01:00Z"));
        stage4.setStageEnds(Instant.parse("2021-07-31T20:59:00Z"));
        stage4.setFirstCheckin(Instant.parse("2021-09-30T21:00:00Z"));
        stage4.setLastCheckout(Instant.parse("2021-12-24T21:00:00Z"));
        stage4.setCashbackRate(BigDecimal.valueOf(0.2));
        stage4.setMaxCashbackAmount(BigDecimal.valueOf(20000));
        stage4.setMinLOS(2);


        props.setStages(Map.of("2", stage2, "3", stage3, "4", stage4));

        service = new MirEligibilityService(props,
                (partnerId, hotelId) -> {
                    if (hotelId.equals("1")) {
                        return "101";
                    } else {
                        return null;
                    }
                }, null);
    }

    @Test
    public void testEligibleAtVeryStartOfStage2() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "1", justAfterStage2Starts,
                defaultStage2Checkin, lastStage2Checkout, BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_ELIGIBLE);
        assertThat(res.getCashbackAmount().getValue()).isEqualTo(20);
    }

    @Test
    public void testEligibleAtVeryEndOfStage2() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "1", justBeforeStage2Ends,
                defaultStage2Checkin, lastStage2Checkout, BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_ELIGIBLE);
        assertThat(res.getCashbackAmount().getValue()).isEqualTo(20);
    }

    @Test
    public void testEligibleAtVeryEndOfStage2WithNextDayCheckout() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "1", justBeforeStage2Ends,
                defaultStage2Checkin, lastStage2Checkout.plusDays(1), BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_WRONG_STAY_DATES);
    }

    @Test
    public void testEligibleMaxLimit() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "1", justAfterStage2Starts,
                defaultStage2Checkin, lastStage2Checkout, BigDecimal.valueOf(200000));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_ELIGIBLE);
        assertThat(res.getCashbackAmount().getValue()).isEqualTo(20000);
    }

    @Test
    public void testEligibleMaxLimitRoundDown() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "1", justAfterStage2Starts,
                defaultStage2Checkin, lastStage2Checkout, BigDecimal.valueOf(99));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_ELIGIBLE);
        assertThat(res.getCashbackAmount().getValue()).isEqualTo(19);
    }

    @Test
    public void testBlacklisted() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "404", justAfterStage2Starts,
                defaultStage2Checkin, lastStage2Checkout, BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_BLACKLISTED);
        assertThat(res.hasCashbackAmount()).isFalse();
    }

    @Test
    public void testInvalidLos() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "1", justAfterStage2Starts,
                defaultStage2Checkin, defaultStage2Checkin.plusDays(1), BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_WRONG_LOS);
        assertThat(res.hasCashbackAmount()).isFalse();
    }

    @Test
    public void testInvalidCheckoutOfStage2() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "1", justAfterStage2Starts,
                lastStage2Checkout, lastStage2Checkout.plusDays(2), BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_WRONG_STAY_DATES);
        assertThat(res.hasCashbackAmount()).isFalse();
    }

    @Test
    public void testTooEarlyOfStage2() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "1", justBeforeStage2Starts,
                defaultStage2Checkin, defaultStage2Checkin.plusDays(2), BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_WRONG_BOOKING_DATE);
        assertThat(res.hasCashbackAmount()).isFalse();
    }

    @Test
    public void testTooLateOfStage2() {
        TMirPromoStatus res = service.checkEligibility(EPartnerId.PI_TRAVELLINE, "1", justAfterStage2Ends,
                defaultStage2Checkin, defaultStage2Checkin.plusDays(2), BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_WRONG_BOOKING_DATE);
        assertThat(res.hasCashbackAmount()).isFalse();
    }


    @Test
    public void testEligibleBeforeEndOfStage3() {
        TMirPromoStatus res = service.checkEligibility(
                EPartnerId.PI_TRAVELLINE, "1",
                LocalDateTime.of(2021, 6, 15, 23, 50, 0).toInstant(ZoneOffset.ofHours(3)),
                LocalDate.of(2021, 6, 20),
                LocalDate.of(2021, 6, 30),
                BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_ELIGIBLE);
        assertThat(res.getCashbackAmount().getValue()).isEqualTo(20);
    }

    @Test
    public void testNonEligibleBeforeEndOfStage3ButTooLateCheckout() {
        TMirPromoStatus res = service.checkEligibility(
                EPartnerId.PI_TRAVELLINE, "1",
                LocalDateTime.of(2021, 6, 15, 23, 50, 0).toInstant(ZoneOffset.ofHours(3)),
                LocalDate.of(2021, 6, 20),
                LocalDate.of(2021, 7, 1),
                BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_WRONG_STAY_DATES);
    }

    @Test
    public void testNonEligibleAfterEndOfStage3DuringStage4ButTooEarly() {
        TMirPromoStatus res = service.checkEligibility(
                EPartnerId.PI_TRAVELLINE, "1",
                LocalDateTime.of(2021, 6, 16, 0, 2, 0).toInstant(ZoneOffset.ofHours(3)),
                LocalDate.of(2021, 9, 30),
                LocalDate.of(2021, 10, 5),
                BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_WRONG_STAY_DATES); // this is stage 4 already, but dates are wrong
    }


    @Test
    public void testEligibleForStage4OnFirstAvailableCheckin() {
        TMirPromoStatus res = service.checkEligibility(
                EPartnerId.PI_TRAVELLINE, "1",
                LocalDateTime.of(2021, 6, 16, 0, 2, 0).toInstant(ZoneOffset.ofHours(3)),
                LocalDate.of(2021, 10, 1),
                LocalDate.of(2021, 10, 3),
                BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_ELIGIBLE);
        assertThat(res.getCashbackAmount().getValue()).isEqualTo(20);
    }

    @Test
    public void testEligibleForStage4OnLastAvailableCheckout() {
        TMirPromoStatus res = service.checkEligibility(
                EPartnerId.PI_TRAVELLINE, "1",
                LocalDateTime.of(2021, 6, 16, 0, 2, 0).toInstant(ZoneOffset.ofHours(3)),
                LocalDate.of(2021, 12, 1),
                LocalDate.of(2021, 12, 24),
                BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_ELIGIBLE);
        assertThat(res.getCashbackAmount().getValue()).isEqualTo(20);
    }

    @Test
    public void testNonEligibleForStage4TooLateCheckout() {
        TMirPromoStatus res = service.checkEligibility(
                EPartnerId.PI_TRAVELLINE, "1",
                LocalDateTime.of(2021, 6, 16, 0, 2, 0).toInstant(ZoneOffset.ofHours(3)),
                LocalDate.of(2021, 12, 1),
                LocalDate.of(2021, 12, 25),
                BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_WRONG_STAY_DATES);
    }

    @Test
    public void testNonEligibleForStage4RightAfterEnd() {
        TMirPromoStatus res = service.checkEligibility(
                EPartnerId.PI_TRAVELLINE, "1",
                LocalDateTime.of(2021, 8, 1, 0, 0, 0).toInstant(ZoneOffset.ofHours(3)),
                LocalDate.of(2021, 12, 1),
                LocalDate.of(2021, 12, 22),
                BigDecimal.valueOf(100));
        assertThat(res.getEligibility()).isEqualTo(EMirEligibility.ME_WRONG_BOOKING_DATE);
    }

}
