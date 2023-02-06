package ru.yandex.market.delivery.transport_manager.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.domain.entity.Schedule;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationConfig;
import ru.yandex.market.delivery.transport_manager.domain.enums.ConfigTransportationType;
import ru.yandex.market.delivery.transport_manager.util.TransportationIntervalsCalculator.ScheduleIntervalDto;

import static org.assertj.core.api.Assertions.assertThat;

public class TransportationIntervalsCalculatorTest {

    private static final long SHIPPER_ID = 1L;
    private static final long RECEIVER_ID = 2L;

    private static final TransportationConfig CONFIG = new TransportationConfig()
        .setOutboundPartnerId(SHIPPER_ID)
        .setInboundPartnerId(RECEIVER_ID)
        .setTransportationType(ConfigTransportationType.ORDERS_OPERATION)
        .setTransportationSchedule(
            List.of(
                new Schedule().setDay(1).setTimeFrom(LocalTime.of(10, 0, 0)).setTimeTo(LocalTime.of(11, 0, 0)),
                new Schedule().setDay(2).setTimeFrom(LocalTime.of(11, 10, 0)).setTimeTo(LocalTime.of(12, 10, 0)),
                new Schedule().setDay(3).setTimeFrom(LocalTime.of(12, 20, 0)).setTimeTo(LocalTime.of(13, 20, 0)),
                new Schedule().setDay(4).setTimeFrom(LocalTime.of(13, 30, 0)).setTimeTo(LocalTime.of(14, 30, 0)),
                new Schedule().setDay(5).setTimeFrom(LocalTime.of(14, 40, 0)).setTimeTo(LocalTime.of(15, 40, 0))
            )
        );

    private TestableClock clock;

    @BeforeEach
    void setUp() {
        clock = new TestableClock();
        // Вторник, после 21:00
        clock.setFixed(
            LocalDateTime.of(2021, 3, 16, 21, 0, 1).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );
    }

    @Test
    @DisplayName("Проверка интервалов outbound для забора")
    void testCalcForOutboundWithdraw() {
        CONFIG.setDuration(300);
        CONFIG.setMovingPartnerId(RECEIVER_ID);
        // (timeFrom; timeTo)
        List<ScheduleIntervalDto> expected = List.of(
            new ScheduleIntervalDto(datetime(22, 10, 0), datetime(22, 11, 0), null, null),
            new ScheduleIntervalDto(datetime(23, 11, 10), datetime(23, 12, 10), null, null),
            new ScheduleIntervalDto(datetime(18, 13, 30), datetime(18, 14, 30), null, null),
            new ScheduleIntervalDto(datetime(19, 14, 40), datetime(19, 15, 40), null, null)
        );

        assertThat(TransportationIntervalsCalculator.calcForOutbound(CONFIG, LocalDateTime.now(clock), 7))
            .isEqualTo(expected);
    }

    @Test
    @DisplayName("Проверка интервалов outbound для самопривоза")
    void testCalcForOutboundImport() {
        CONFIG.setDuration(300);
        CONFIG.setMovingPartnerId(SHIPPER_ID);
        // (timeFrom - duration; timeTo - duration)
        List<ScheduleIntervalDto> expected = List.of(
            new ScheduleIntervalDto(datetime(22, 5, 0), datetime(22, 6, 0), null, null),
            new ScheduleIntervalDto(datetime(23, 6, 10), datetime(23, 7, 10), null, null),
            new ScheduleIntervalDto(datetime(18, 8, 30), datetime(18, 9, 30), null, null),
            new ScheduleIntervalDto(datetime(19, 9, 40), datetime(19, 10, 40), null, null)
        );

        assertThat(TransportationIntervalsCalculator.calcForOutbound(CONFIG, LocalDateTime.now(clock), 7))
            .isEqualTo(expected);
    }

    @Test
    @DisplayName("Проверка интервалов movement для забора")
    void testCalcForMovementWithdraw() {
        CONFIG.setDuration(300);
        CONFIG.setMovingPartnerId(RECEIVER_ID);
        // (timeTo; timeFrom + duration) or (timeFrom + duration; timeTo)
        List<ScheduleIntervalDto> expected = List.of(
            new ScheduleIntervalDto(datetime(22, 11, 0), datetime(22, 15, 0), null, null),
            new ScheduleIntervalDto(datetime(23, 12, 10), datetime(23, 16, 10), null, null),
            new ScheduleIntervalDto(datetime(18, 14, 30), datetime(18, 18, 30), null, null),
            new ScheduleIntervalDto(datetime(19, 15, 40), datetime(19, 19, 40), null, null)
        );

        assertThat(TransportationIntervalsCalculator.calcForMovement(CONFIG, LocalDateTime.now(clock), 7))
            .isEqualTo(expected);
    }

    @Test
    @DisplayName("Проверка пересекающихся интервалов movement для самопривоза")
    void testCalcForMovementImport() {
        CONFIG.setDuration(1);
        CONFIG.setMovingPartnerId(SHIPPER_ID);
        // (timeTo - duration; timeFrom) or (timeFrom; timeTo - duration)
        List<ScheduleIntervalDto> expected = List.of(
            new ScheduleIntervalDto(datetime(22, 10, 0), datetime(22, 10, 59), null, null),
            new ScheduleIntervalDto(datetime(23, 11, 10), datetime(23, 12, 9), null, null),
            new ScheduleIntervalDto(datetime(18, 13, 30), datetime(18, 14, 29), null, null),
            new ScheduleIntervalDto(datetime(19, 14, 40), datetime(19, 15, 39), null, null)
        );

        assertThat(TransportationIntervalsCalculator.calcForMovement(CONFIG, LocalDateTime.now(clock), 7))
            .isEqualTo(expected);
    }

    @Test
    @DisplayName("Проверка интервалов inbound для забора")
    void testCalcForInboundWithdraw() {
        CONFIG.setDuration(300);
        CONFIG.setMovingPartnerId(RECEIVER_ID);
        // (timeFrom + duration; timeTo + duration)
        List<ScheduleIntervalDto> expected = List.of(
            new ScheduleIntervalDto(datetime(22, 15, 0), datetime(22, 16, 0), null, null),
            new ScheduleIntervalDto(datetime(23, 16, 10), datetime(23, 17, 10), null, null),
            new ScheduleIntervalDto(datetime(18, 18, 30), datetime(18, 19, 30), null, null),
            new ScheduleIntervalDto(datetime(19, 19, 40), datetime(19, 20, 40), null, null)
        );

        assertThat(TransportationIntervalsCalculator.calcForInbound(CONFIG, LocalDateTime.now(clock), 7))
            .isEqualTo(expected);
    }

    @Test
    @DisplayName("Проверка интервалов inbound для самопривоза")
    void testCalcForInboundImport() {
        CONFIG.setDuration(300);
        CONFIG.setMovingPartnerId(SHIPPER_ID);
        // (timeFrom; timeTo)
        List<ScheduleIntervalDto> expected = List.of(
            new ScheduleIntervalDto(datetime(22, 10, 0), datetime(22, 11, 0), null, null),
            new ScheduleIntervalDto(datetime(23, 11, 10), datetime(23, 12, 10), null, null),
            new ScheduleIntervalDto(datetime(18, 13, 30), datetime(18, 14, 30), null, null),
            new ScheduleIntervalDto(datetime(19, 14, 40), datetime(19, 15, 40), null, null)
        );

        assertThat(TransportationIntervalsCalculator.calcForInbound(CONFIG, LocalDateTime.now(clock), 7))
            .isEqualTo(expected);
    }

    @Test
    @DisplayName("Проверка перескока на следующий день в поздних интервалах")
    void testJumpToNextDay() {
        CONFIG.setDuration(600);
        CONFIG.setMovingPartnerId(RECEIVER_ID);
        // (timeFrom + duration; timeTo + duration)
        List<ScheduleIntervalDto> expected = List.of(
            new ScheduleIntervalDto(datetime(22, 20, 0), datetime(22, 21, 0), null, null),
            new ScheduleIntervalDto(datetime(23, 21, 10), datetime(23, 22, 10), null, null),
            new ScheduleIntervalDto(datetime(18, 23, 30), datetime(19, 0, 30), null, null),
            new ScheduleIntervalDto(datetime(20, 0, 40), datetime(20, 1, 40), null, null)
        );

        assertThat(TransportationIntervalsCalculator.calcForInbound(CONFIG, LocalDateTime.now(clock), 7))
            .isEqualTo(expected);
    }

    @Test
    @DisplayName("Генерация регулярного xdoc на месяц вперед")
    void testGenerateRegularXDoc() {
        TransportationConfig interwarehouseConfig = new TransportationConfig()
            .setOutboundPartnerId(SHIPPER_ID)
            .setInboundPartnerId(RECEIVER_ID)
            .setDuration(0)
            .setTransportationType(ConfigTransportationType.XDOC_TRANSPORT)
            .setTransportationSchedule(
                List.of(
                    new Schedule()
                        .setDay(2).
                        setTimeFrom(LocalTime.of(10, 0, 0))
                        .setTimeTo(LocalTime.of(11, 0, 0))
                        .setPallets(5)
                        .setTransportId(1L),
                    new Schedule()
                        .setDay(4)
                        .setTimeFrom(LocalTime.of(11, 10, 0))
                        .setTimeTo(LocalTime.of(12, 10, 0))
                        .setPallets(10)
                )
            );

        List<ScheduleIntervalDto> expected = List.of(
            new ScheduleIntervalDto(datetime(23, 10, 0), datetime(23, 11, 0), 5, 1L),
            new ScheduleIntervalDto(datetime(30, 10, 0), datetime(30, 11, 0), 5, 1L),
            new ScheduleIntervalDto(datetime(4, 6, 10, 0), datetime(4, 6, 11, 0), 5, 1L),
            new ScheduleIntervalDto(datetime(4, 13, 10, 0), datetime(4, 13, 11, 0), 5, 1L),
            new ScheduleIntervalDto(datetime(18, 11, 10), datetime(18, 12, 10), 10, null),
            new ScheduleIntervalDto(datetime(25, 11, 10), datetime(25, 12, 10), 10, null),
            new ScheduleIntervalDto(datetime(4, 1, 11, 10), datetime(4, 1, 12, 10), 10, null),
            new ScheduleIntervalDto(datetime(4, 8, 11, 10), datetime(4, 8, 12, 10), 10, null),
            new ScheduleIntervalDto(datetime(4, 15, 11, 10), datetime(4, 15, 12, 10), 10, null)
        );

        assertThat(TransportationIntervalsCalculator.calcForInbound(interwarehouseConfig,
            LocalDateTime.now(clock), 30))
            .isEqualTo(expected);
    }

    @Test
    void testNoCutoffForInterwarehouse() {
        TransportationConfig interwarehouseConfig = new TransportationConfig()
            .setOutboundPartnerId(SHIPPER_ID)
            .setInboundPartnerId(RECEIVER_ID)
            .setDuration(0)
            .setTransportationType(ConfigTransportationType.XDOC_TRANSPORT)
            .setTransportationSchedule(
                List.of(
                    new Schedule()
                        .setDay(3).
                        setTimeFrom(LocalTime.of(10, 0, 0))
                        .setTimeTo(LocalTime.of(11, 0, 0))
                        .setPallets(5)
                        .setTransportId(1L)
                )
            );

        List<ScheduleIntervalDto> expected = List.of(
            new ScheduleIntervalDto(datetime(17, 10, 0), datetime(17, 11, 0), 5, 1L),
            new ScheduleIntervalDto(datetime(24, 10, 0), datetime(24, 11, 0), 5, 1L),
            new ScheduleIntervalDto(datetime(31, 10, 0), datetime(31, 11, 0), 5, 1L),
            new ScheduleIntervalDto(datetime(4, 7, 10, 0), datetime(4, 7, 11, 0), 5, 1L),
            new ScheduleIntervalDto(datetime(4, 14, 10, 0), datetime(4, 14, 11, 0), 5, 1L)
        );

        assertThat(TransportationIntervalsCalculator.calcForInbound(interwarehouseConfig,
            LocalDateTime.now(clock), 30))
            .isEqualTo(expected);
    }

    private LocalDateTime datetime(int dayOfMonth, int hour, int minute) {
        return LocalDateTime.of(2021, 3, dayOfMonth, hour, minute, 0);
    }

    private LocalDateTime datetime(int month, int dayOfMonth, int hour, int minute) {
        return LocalDateTime.of(2021, month, dayOfMonth, hour, minute, 0);
    }
}
