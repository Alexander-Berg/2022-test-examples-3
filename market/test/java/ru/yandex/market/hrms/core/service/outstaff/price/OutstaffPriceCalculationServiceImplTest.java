package ru.yandex.market.hrms.core.service.outstaff.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.outstaff.shift.Context;
import ru.yandex.market.hrms.core.service.outstaff.shift.OutstaffEvent;
import ru.yandex.market.hrms.core.service.outstaff.shift.OutstaffShiftCalculationServiceImpl;
import ru.yandex.market.hrms.model.outstaff.OutStaffShiftType;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.assertj.core.api.Assertions.withinPercentage;

@DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.before.csv")
class OutstaffPriceCalculationServiceImplTest extends AbstractCoreTest {
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Moscow");

    @Autowired
    OutstaffPriceCalculationServiceImpl priceCalculationService;
    @Autowired
    OutstaffShiftCalculationServiceImpl shiftCalculationService;

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.simpleDay.csv")
    void simpleDay() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        var priceShift = priceCalculationService.calculate(shiftResult);

        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));
        assertThatExtractedFrom(shiftO.get())
                .containsExactly(100L, LocalDate.parse("2021-06-01"), OutStaffShiftType.FIRST_SHIFT, 1);

        Assertions.assertThat(shiftO.get().getPriceEvents()).containsExactlyInAnyOrder(
                OutstaffPriceEvent.builder()
                        .type(OutstaffEvent.Type.WMS_OPERATION)
                        .operationName("raw_operation_type")
                        .rawName("raw_operation_type")
                        .shiftDate(LocalDate.parse("2021-06-01"))
                        .start(Instant.parse("2021-06-01T09:30:00Z"))
                        .finish(Instant.parse("2021-06-01T09:31:00Z"))
                        .zoneId(ZONE_ID)
                        .build()
        );
        Assertions.assertThat(shiftO.get().getTotalPrice()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.shiftWithBaseRates.csv")
    void shiftWithBaseRates() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        var priceShift = priceCalculationService.calculate(shiftResult);

        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));
        assertThatExtractedFrom(shiftO.get())
                .containsExactly(100L, LocalDate.parse("2021-06-01"), OutStaffShiftType.FIRST_SHIFT, 1);

        Assertions.assertThat(shiftO.get().getPriceEvents()).containsExactlyInAnyOrder(
                OutstaffPriceEvent.builder()
                        .type(OutstaffEvent.Type.WMS_OPERATION)
                        .operationName("АутстаффКонсолидация")
                        .rawName("Перемещение штучное, шт ЗОНА КОНСОЛИДАЦИИ 89")
                        .shiftDate(LocalDate.parse("2021-06-01"))
                        .start(Instant.parse("2021-06-01T09:30:00Z"))
                        .finish(Instant.parse("2021-06-01T09:31:00Z"))
                        .zoneId(ZONE_ID)
                        .unit("штуки")
                        .count(new BigDecimal("5"))
                        .baseRate(new BigDecimal("50"))
                        .build()
        );
        Assertions.assertThat(shiftO.get().getTotalPrice())
                .isCloseTo(new BigDecimal("250"), withinPercentage(0.11));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.baseRatesWithEndTime.csv")
    void baseRatesWithEndTime() {
        mockClock(LocalDate.of(2021, 6, 1));
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        var priceShift = priceCalculationService.calculate(shiftResult);

        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));
        assertThatExtractedFrom(shiftO.get())
                .containsExactly(100L, LocalDate.parse("2021-06-01"), OutStaffShiftType.FIRST_SHIFT, 2);

        Assertions.assertThat(shiftO.get().getPriceEvents()).containsExactlyInAnyOrder(
                OutstaffPriceEvent.builder()
                        .type(OutstaffEvent.Type.WMS_OPERATION)
                        .operationName("Перемещение штучное, шт ЗОНА КОНСОЛИДАЦИИ 89")
                        .rawName("Перемещение штучное, шт ЗОНА КОНСОЛИДАЦИИ 89")
                        .shiftDate(LocalDate.parse("2021-06-01"))
                        .start(Instant.parse("2021-06-01T09:30:00Z"))
                        .finish(Instant.parse("2021-06-01T09:31:00Z"))
                        .zoneId(ZONE_ID)
                        .operationName("АутстаффКонсолидация")
                        .unit("штуки")
                        .count(new BigDecimal("5"))
                        .baseRate(new BigDecimal("50"))
                        .build(),
                OutstaffPriceEvent.builder()
                        .type(OutstaffEvent.Type.WMS_OPERATION)
                        .operationName("Перемещение штучное, шт ЗОНА КОНСОЛИДАЦИИ 90")
                        .rawName("Перемещение штучное, шт ЗОНА КОНСОЛИДАЦИИ 90")
                        .shiftDate(LocalDate.parse("2021-06-01"))
                        .start(Instant.parse("2021-06-01T09:45:00Z"))
                        .finish(Instant.parse("2021-06-01T09:46:00Z"))
                        .zoneId(ZONE_ID)
                        .operationName("АутстаффКонсолидация")
                        .unit("штуки")
                        .count(new BigDecimal("5"))
                        .baseRate(new BigDecimal("50"))
                        .build()
        );
        Assertions.assertThat(shiftO.get().getTotalPrice())
                .isCloseTo(new BigDecimal("500"), withinPercentage(0.11));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.allEvents.csv")
    void allEvents() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);

        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));
        assertThatExtractedFrom(shiftO.get())
                .containsExactly(100L, LocalDate.parse("2021-06-01"), OutStaffShiftType.FIRST_SHIFT, 3);

        Assertions.assertThat(shiftO.get().getPriceEvents()).containsExactlyInAnyOrder(
                OutstaffPriceEvent.builder()
                        .type(OutstaffEvent.Type.WMS_OPERATION)
                        .operationName("АутстаффКонсолидация")
                        .rawName("Перемещение штучное, шт ЗОНА КОНСОЛИДАЦИИ 89")
                        .shiftDate(LocalDate.parse("2021-06-01"))
                        .start(Instant.parse("2021-06-01T09:30:00Z"))
                        .finish(Instant.parse("2021-06-01T09:31:00Z"))
                        .zoneId(ZONE_ID)
                        .unit("штуки")
                        .count(new BigDecimal("5"))
                        .baseRate(new BigDecimal("50"))
                        .build(),
                OutstaffPriceEvent.builder()
                        .type(OutstaffEvent.Type.NON_PRODUCTION_OPERATION)
                        .operationName("АутстаффНаставничество")
                        .rawName("Non production operation")
                        .shiftDate(LocalDate.parse("2021-06-01"))
                        .start(Instant.parse("2021-06-01T09:00:00Z"))
                        .finish(Instant.parse("2021-06-01T09:15:00Z"))
                        .zoneId(ZONE_ID)
                        .unit("час")
                        .count(new BigDecimal("0.25"))
                        .baseRate(new BigDecimal("100"))
                        .build(),
                OutstaffPriceEvent.builder()
                        .type(OutstaffEvent.Type.TIMEX)
                        .operationName("ФФЦ Софьино - операционный зал")
                        .rawName("ФФЦ Софьино - операционный зал")
                        .shiftDate(LocalDate.parse("2021-06-01"))
                        .start(Instant.parse("2021-06-01T08:45:00Z"))
                        .finish(Instant.parse("2021-06-01T15:45:00Z"))
                        .zoneId(ZONE_ID)
                        .build()
        );
        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("275"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.overrideEvents.csv")
    void overrideEvents() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);

        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));
        assertThatExtractedFrom(shiftO.get())
                .containsExactly(100L, LocalDate.parse("2021-06-01"), OutStaffShiftType.FIRST_SHIFT, 3);

        assertThatExtractedFrom(find(shiftO, "АутстаффКонсолидация"))
                .containsExactly(
                        "АутстаффКонсолидация", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                        "штуки", 5., 50., // unit, count, baseRate
                        3.75, 0.25, 187.5 // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "АутстаффНаставничество"))
                .containsExactly(
                        "АутстаффНаставничество", "2021-06-01T09:00:00Z", "2021-06-01T09:15:00Z",
                        "час", 0.25, 100., // unit, count, baseRate
                        0.25, 0., 25. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "ФФЦ Софьино - операционный зал"))
                .containsExactly(
                        "ФФЦ Софьино - операционный зал", "2021-06-01T08:45:00Z", "2021-06-01T09:45:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("212.5"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.overrideEvents2.csv")
    void overrideEvents2() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);

        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));
        assertThatExtractedFrom(shiftO.get())
                .containsExactly(100L, LocalDate.parse("2021-06-01"), OutStaffShiftType.FIRST_SHIFT, 5);

        assertThatExtractedFrom(find(shiftO, "АутстаффНаставничество"))
                .containsExactly(
                        "АутстаффНаставничество", "2021-06-01T09:00:00Z", "2021-06-01T09:15:00Z",
                        "час", 0.25, 100., // unit, count, baseRate
                        0.25, 0., 25. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "АутстаффКонсолидация"))
                .containsExactly(
                        "АутстаффКонсолидация", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                        "штуки", 5., 50., // unit, count, baseRate
                        3.75, 0.25, 187.5 // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Пик 1"))
                .containsExactly(
                        "Пик", "2021-06-01T09:20:00Z", "2021-06-01T09:20:00Z",
                        "sku", 1., 30., // unit, count, baseRate
                        0., 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Пик 2"))
                .containsExactly(
                        "Пик", "2021-06-01T11:30:00Z", "2021-06-01T11:30:00Z",
                        "sku", 1., 30., // unit, count, baseRate
                        1., 0., 30. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "ФФЦ Софьино - операционный зал"))
                .containsExactly(
                        "ФФЦ Софьино - операционный зал", "2021-06-01T08:45:00Z", "2021-06-01T15:45:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("242.5"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.lunchWholeShift.csv")
    void lunchWholeShift() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));

        assertThatExtractedFrom(find(shiftO, "Наставничество"))
                .containsExactly(
                        "Наставничество", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                        "час", 1., 100., // unit, count, baseRate
                        0., 1., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Обед"))
                .containsExactly(
                        "Обед", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(BigDecimal.ZERO, withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.lunchZeroDuration.csv")
    void lunchZeroDuration() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));

        assertThatExtractedFrom(find(shiftO, "Наставничество"))
                .containsExactly(
                        "Наставничество", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Обед"))
                .containsExactly(
                        "Обед", "2021-06-01T09:00:00Z", "2021-06-01T09:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(BigDecimal.ZERO, withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.forceBreak.csv")
    void forceBreak() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));

        // принудительный перерыв будет расчитываться как
        // длительность смены (5 часов) * 1/12 = 0.417 часов (но не меньше 0.5 часа) = 0.5
        assertThatExtractedFrom(find(shiftO, "Принудительный перерыв"))
                .containsExactly(
                        "Принудительный перерыв", "2021-06-01T09:00:00Z", "2021-06-01T09:30:00Z",
                        "час", 0.5, null, // unit, count, baseRate
                        0.5, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Наставничество"))
                .containsExactly(
                        "Наставничество", "2021-06-01T09:00:00Z", "2021-06-01T14:00:00Z",
                        "час", 5., 100., // unit, count, baseRate
                        4.5, 0.5, 450. // effectiveCount, breakHours, effectiveTotalPrice
                );

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("450"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.bigForceBreak.csv")
    void bigForceBreak() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));

        // принудительный перерыв будет расчитываться как
        // длительность смены (12 часов) * 1/12 = 1 часов (но не меньше 0.5 часа) = 1 часов
        assertThatExtractedFrom(find(shiftO, "Принудительный перерыв"))
                .containsExactly(
                        "Принудительный перерыв", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                        "час", 1., null, // unit, count, baseRate
                        1., 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Наставничество"))
                .containsExactly(
                        "Наставничество", "2021-06-01T09:00:00Z", "2021-06-01T14:00:00Z",
                        "час", 5., 100., // unit, count, baseRate
                        4., 1., 400. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Not paid"))
                .containsExactly(
                        "Not paid", "2021-06-01T21:00:00Z", "2021-06-01T21:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0.// effectiveCount, breakHours, effectiveTotalPrice
                );

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("400"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.bigForceBreakWithLunch.csv")
    void bigForceBreakWithLunch() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));

        // принудительный перерыв будет расчитываться как
        // длительность смены (12 часов) * 1/12 = 1 часов (но не меньше 0.5 часа) = 1 часов
        // отнимаем перерыв (15 минут) и обед (15 минут) = 1 час - 30 минут: 0.5 часа
        assertThatExtractedFrom(find(shiftO, "Принудительный перерыв"))
                .containsExactly(
                        "Принудительный перерыв", "2021-06-01T09:00:00Z", "2021-06-01T09:30:00Z",
                        "час", 0.5, null, // unit, count, baseRate
                        0.5, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );
        // У наставничества отнимается 0.5 минут от принудительного перерыва
        // А потом из-за перерыва (15 минут), который совпал по времени: итого 0.75 часов
        assertThatExtractedFrom(find(shiftO, "Наставничество"))
                .containsExactly(
                        "Наставничество", "2021-06-01T09:00:00Z", "2021-06-01T14:00:00Z",
                        "час", 5., 100., // unit, count, baseRate
                        4.25, 0.75, 425. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Not paid"))
                .containsExactly(
                        "Not paid", "2021-06-01T21:00:00Z", "2021-06-01T21:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0.// effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "перерыв"))
                .containsExactly(
                        "Перерыв", "2021-06-01T09:05:00Z", "2021-06-01T09:20:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0.// effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Обед"))
                .containsExactly(
                        "Обед", "2021-06-01T14:00:00Z", "2021-06-01T14:15:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0.// effectiveCount, breakHours, effectiveTotalPrice
                );

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("425"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.forceBreakFromPricefulEvents.csv")
    void forceBreakFromPricefulEvents() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));

        // принудительный перерыв будет расчитываться как
        // длительность смены (6 часов) * 1/12 = 0.5 часов (но не меньше 0.5 часа) = 0.5 часов
        assertThatExtractedFrom(find(shiftO, "Принудительный перерыв"))
                .containsExactly(
                        "Принудительный перерыв", "2021-06-01T09:00:00Z", "2021-06-01T09:30:00Z",
                        "час", 0.5, null, // unit, count, baseRate
                        0.5, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                );
        // У консолидации отнимается 0.5 минут от принудительного перерыва
        // так как это самое дорогое событие
        assertThatExtractedFrom(find(shiftO, "Консолидация"))
                .containsExactly(
                        "Консолидация", "2021-06-01T11:00:00Z", "2021-06-01T13:00:00Z",
                        "час", 2., 500., // unit, count, baseRate
                        1.5, 0.5, 750. // effectiveCount, breakHours, effectiveTotalPrice
                );
        // У остальных событий ничего не отнимается
        assertThatExtractedFrom(find(shiftO, "Наставничество"))
                .containsExactly(
                        "Наставничество", "2021-06-01T09:00:00Z", "2021-06-01T11:00:00Z",
                        "час", 2., 100., // unit, count, baseRate
                        2., 0., 200. // effectiveCount, breakHours, effectiveTotalPrice
                );
        assertThatExtractedFrom(find(shiftO, "Игра в теннис"))
                .containsExactly(
                        "Игра в теннис", "2021-06-01T13:00:00Z", "2021-06-01T15:00:00Z",
                        "час", 2., 10., // unit, count, baseRate
                        2., 0., 20.// effectiveCount, breakHours, effectiveTotalPrice
                );

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("970"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.doubleRates.csv")
    void doubleRates() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));

        assertThatExtractedFrom(shiftO.get().getPriceEvents())
                .containsExactlyInAnyOrder(Tuple.tuple(
                        "Наставничество", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                        "час", 1., 100., // unit, count, baseRate
                        1., 0., 100. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "Наставничество", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                        "штуки", 2., 30., // unit, count, baseRate
                        2., 0., 60. // effectiveCount, breakHours, effectiveTotalPrice
                ));

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("160"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.outsideOperZone.csv")
    void dontPayForNonProductionOperationsOutsideOperationZone() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));

        assertThatExtractedFrom(shiftO.get().getPriceEvents())
                .containsExactlyInAnyOrder(Tuple.tuple(
                                "ФФЦ Софьино - операционный зал", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                                null, null, null, // unit, count, baseRate
                                null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                        ), Tuple.tuple(
                                "ФФЦ Софьино - операционный зал", "2021-06-01T11:00:00Z", "2021-06-01T12:00:00Z",
                                null, null, null, // unit, count, baseRate
                                null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                        ), Tuple.tuple(
                                "Перемещение", "2021-06-01T09:30:00Z", "2021-06-01T10:30:00Z",
                                "час", 1., 100., // unit, count, baseRate
                                0.5, 0.5, 50. // effectiveCount, breakHours, effectiveTotalPrice
                        ), Tuple.tuple(
                                "Наставничество", "2021-06-01T12:00:00Z", "2021-06-01T13:00:00Z",
                                "час", 1., 100., // unit, count, baseRate
                                0., 1., 0. // effectiveCount, breakHours, effectiveTotalPrice
                        ), Tuple.tuple(
                                "Пик", "2021-06-01T09:00:00Z", "2021-06-01T09:30:00Z",
                                "штуки", 5., 100., // unit, count, baseRate
                                5., 0., 500. // effectiveCount, breakHours, effectiveTotalPrice
                        ), Tuple.tuple(
                                null, "2021-06-01T10:00:00Z", "2021-06-01T11:00:00Z",
                                null, null, null, // unit, count, baseRate
                                null, 0.5, 0.0 // effectiveCount, breakHours, effectiveTotalPrice
                        )
                );

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("550"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.ignore_wms_npo.csv")
    void dontPayForIgnoredWmsNpo() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(100L, LocalDate.parse("2021-06-01"));

        assertThatExtractedFrom(shiftO.get().getPriceEvents())
                .containsExactlyInAnyOrder(Tuple.tuple(
                        "ФФЦ Софьино - операционный зал", "2021-06-01T09:00:00Z", "2021-06-01T10:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "ФФЦ Софьино - операционный зал", "2021-06-01T11:00:00Z", "2021-06-01T12:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "Наставничество", "2021-06-01T12:00:00Z", "2021-06-01T13:00:00Z",
                        "час", 1., 100., // unit, count, baseRate
                        0., 1., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "Пик", "2021-06-01T09:00:00Z", "2021-06-01T09:30:00Z",
                        "штуки", 5., 100., // unit, count, baseRate
                        5., 0., 500. // effectiveCount, breakHours, effectiveTotalPrice
                ));

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("500"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.outsideOperZone.csv")
    void dontPayForNpoOutsideOperationZoneThreeShifts() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(3L)
                .outstaffIds(List.of(205L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(205L, LocalDate.parse("2021-06-02"));

        assertThatExtractedFrom(shiftO.get().getPriceEvents())
                .containsExactlyInAnyOrder(Tuple.tuple(
                        "ФФЦ Ростов - операционный зал", "2021-06-02T06:10:00Z", "2021-06-02T10:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "ФФЦ Ростов - операционный зал", "2021-06-02T11:00:00Z", "2021-06-02T12:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "Перемещение", "2021-06-02T09:30:00Z", "2021-06-02T10:30:00Z",
                        "час", 1., 100., // unit, count, baseRate
                        0., 1., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "Наставничество", "2021-06-02T09:30:00Z", "2021-06-02T11:30:00Z",
                        "час", 2., 100., // unit, count, baseRate
                        1., 1., 100. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "Пик", "2021-06-02T09:00:00Z", "2021-06-02T09:30:00Z",
                        "штуки", 5., 100., // unit, count, baseRate
                        5., 0., 500. // effectiveCount, breakHours, effectiveTotalPrice
                ));

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("600"), withinPercentage(0.01));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffPriceCalculationServiceImplTest.outsideOperZone.csv")
    void payForAllOperationsInOperZoneThreeShifts() {
        var shiftResult = shiftCalculationService.calculate(Context.builder()
                .domainId(2L)
                .outstaffIds(List.of(200L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-07-01"), LocalDate.parse("2021-08-01")))
                .build()
        );
        var priceShift = priceCalculationService.calculate(shiftResult);
        var shiftO = priceShift.getShiftPricesByOutstaffId(200L, LocalDate.parse("2021-07-02"));

        assertThatExtractedFrom(shiftO.get().getPriceEvents())
                .containsExactlyInAnyOrder(Tuple.tuple(
                        "ФФЦ Томилино - операционный зал", "2021-07-02T06:10:00Z", "2021-07-02T10:00:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "ФФЦ Томилино - операционный зал", "2021-07-02T11:00:00Z", "2021-07-02T12:40:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "SC operation", "2021-07-02T09:30:00Z", "2021-07-02T10:30:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "Наставничество", "2021-07-02T11:30:00Z", "2021-07-02T12:30:00Z",
                        "час", 1., 100., // unit, count, baseRate
                        1., 0., 100. // effectiveCount, breakHours, effectiveTotalPrice
                ), Tuple.tuple(
                        "SC operation", "2021-07-02T09:00:00Z", "2021-07-02T09:30:00Z",
                        null, null, null, // unit, count, baseRate
                        null, 0., 0. // effectiveCount, breakHours, effectiveTotalPrice
                ));

        Assertions.assertThat(shiftO.get().getTotalPrice()).isCloseTo(new BigDecimal("100"), withinPercentage(0.01));
    }

    @Nullable
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static OutstaffPriceEvent find(Optional<OutstaffPriceShift> shiftPriceO, String name) {
        var data = shiftPriceO.get().getPriceEvents().stream()
                .filter(v -> v.getOperationName().equals(name) || v.getRawName().equals(name))
                .collect(Collectors.toList());
        if (data.size() > 1) {
            throw new IllegalArgumentException("Several data matches: " + name + ": " + data);
        }
        if (data.isEmpty()) {
            throw new IllegalArgumentException("No data matches: " + name);
        }
        return data.get(0);
    }

    private static AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertThatExtractedFrom(OutstaffPriceShift priceShift) {
        return Assertions.assertThat(priceShift)
                .extracting(
                        OutstaffPriceShift::getOutstaffId,
                        OutstaffPriceShift::getShiftDate,
                        OutstaffPriceShift::getShiftType,
                        o -> o.getPriceEvents().size()
                );
    }

    private static AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertThatExtractedFrom(OutstaffPriceEvent priceEvent) {
        return Assertions.assertThat(priceEvent)
                .extracting(
                        OutstaffPriceEvent::getOperationName,
                        o -> o.getStart().toString(),
                        o -> o.getFinish().toString(),
                        OutstaffPriceEvent::getUnit,
                        o -> Optional.ofNullable(o.getCount()).map(BigDecimal::doubleValue).orElse(null),
                        o -> Optional.ofNullable(o.getBaseRate()).map(BigDecimal::doubleValue).orElse(null),
                        o -> Optional.ofNullable(o.getEffectiveCount()).map(BigDecimal::doubleValue).orElse(null),
                        OutstaffPriceEvent::getBreakHours,
                        o -> Optional.ofNullable(o.getTotalPrice()).map(BigDecimal::doubleValue).orElse(null)
                );
    }

    private AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertThatExtractedFrom(List<OutstaffPriceEvent> priceEvents) {
        return Assertions.assertThat(priceEvents)
                .extracting(
                        OutstaffPriceEvent::getOperationName,
                        o -> o.getStart().toString(),
                        o -> o.getFinish().toString(),
                        OutstaffPriceEvent::getUnit,
                        o -> Optional.ofNullable(o.getCount()).map(BigDecimal::doubleValue).orElse(null),
                        o -> Optional.ofNullable(o.getBaseRate()).map(BigDecimal::doubleValue).orElse(null),
                        o -> Optional.ofNullable(o.getEffectiveCount()).map(BigDecimal::doubleValue).orElse(null),
                        OutstaffPriceEvent::getBreakHours,
                        o -> Optional.ofNullable(o.getTotalPrice()).map(BigDecimal::doubleValue).orElse(null)
                );
    }
}
