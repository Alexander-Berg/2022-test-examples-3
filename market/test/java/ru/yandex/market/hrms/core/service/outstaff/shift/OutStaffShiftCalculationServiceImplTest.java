package ru.yandex.market.hrms.core.service.outstaff.shift;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.model.outstaff.OutStaffShiftType;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

@DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.before.csv")
class OutStaffShiftCalculationServiceImplTest extends AbstractCoreTest {
    private static final String[] ELEMENTS_TO_COMPARE = {"outstaffId", "shiftDate", "shiftType", "wmsLogin", "scLogin",
            "firstActivityTs", "lastActivityTs", "startTs", "controlTs"};

    @Autowired
    OutstaffShiftCalculationServiceImpl shiftCalculationService;

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.simpleDay.csv")
    void simpleDay() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        // У пользователя user-100 будет дневная явка за 1 июня, так как он пришел в 12:30 и ничего больше не делал
        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T11:00:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T12:31:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.simpleDay.csv")
    void simpleDay6HoursBetweenShifts() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(6L)
                .outstaffIds(List.of(101L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        // У пользователя user-100 будет дневная явка за 1 июня, так как он пришел в 12:30 и ничего больше не делал
        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T01:30:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T01:31:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T20:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T08:50:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T08:51:00.00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00.00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.simpleDay.csv")
    void dontReturnOutIntervalShifts() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-02"), LocalDate.parse("2021-06-02")))
                .build()
        );
        Assertions.assertThat(shift.getShifts()).isEmpty();

        shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-05-31"), LocalDate.parse("2021-05-31")))
                .build()
        );
        Assertions.assertThat(shift.getShifts()).isEmpty();

        shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-05-31"), LocalDate.parse("2021-06-01")))
                .build()
        );
        Assertions.assertThat(shift.getShifts()).hasSize(1);

        shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-06-01")))
                .build()
        );
        Assertions.assertThat(shift.getShifts()).hasSize(1);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.simpleNight.csv")
    void simpleNight() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T03:11:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T03:20:00.00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00.00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.simpleNight2.csv")
    void simpleNight2() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T23:00:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T00:00:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T20:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.dontForgetNight.csv")
    void dontForgetNight() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-06-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T23:00:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T04:20:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T20:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.dontForgetNight.csv")
    void dontForgetLongNight() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(4L)
                .outstaffIds(List.of(101L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-05-31"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .scLogin("test-login-101")
                                .wmsLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T16:30:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T16:31:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T16:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T20:30:00.00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .scLogin("test-login-101")
                                .wmsLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T03:10:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T20:30:00.00+03:00"))
                                .startTs(toInstant("2021-06-02T04:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T20:30:00.00+03:00"))
                                .build()
                );
    }


    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.earlyDay.csv")
    void earlyDay() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T05:00:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T06:00:00.00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00.00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.longNight.csv")
    void longNight() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T17:00:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T05:20:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T20:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.severalEvents.csv")
    void severalEvents() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L, 102L, 103L, 104L, 105L, 106L, 107L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-05-31"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShiftsByOutstaffId(100L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L).wmsLogin("user-100")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T10:40:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T15:55:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
        Assertions.assertThat(shift.getShiftsByOutstaffId(101L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(101L).wmsLogin("user-101")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T10:40:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T17:10:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
        Assertions.assertThat(shift.getShiftsByOutstaffId(102L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(102L).wmsLogin("user-102")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T10:40:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T23:10:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
        Assertions.assertThat(shift.getShiftsByOutstaffId(103L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(103L).wmsLogin("user-103")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T10:40:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T06:10:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
        Assertions.assertThat(shift.getShiftsByOutstaffId(104L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(104L).wmsLogin("user-104")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T10:40:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T10:10:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(104L).wmsLogin("user-104")
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T12:10:00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00.00+03:00"))
                                .build()
                );
        Assertions.assertThat(shift.getShiftsByOutstaffId(105L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(105L).wmsLogin("user-105")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T05:40:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T10:10:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(105L).wmsLogin("user-105")
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T12:10:00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00.00+03:00"))
                                .build()
                );
        Assertions.assertThat(shift.getShiftsByOutstaffId(106L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(106L).wmsLogin("user-106")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T17:55:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(106L).wmsLogin("user-106")
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:10:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T17:56:00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00.00+03:00"))
                                .build()
                );
        Assertions.assertThat(shift.getShiftsByOutstaffId(107L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(107L).wmsLogin("user-107")
                                .shiftDate(LocalDate.parse("2021-05-31"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T00:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T02:55:00+03:00"))
                                .startTs(toInstant("2021-05-31T20:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-01T10:30:00.00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(107L).wmsLogin("user-107")
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T04:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T10:20:00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00.00+03:00"))
                                .build()
                );
    }

    // https://st.yandex-team.ru/HRMS-554#610bec1d4e3d8b0abb247554
    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.dayAndNight.csv")
    void bothDayAndNightShiftInOneDay() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T11:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T00:10:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
    }

    // https://st.yandex-team.ru/HRMS-554#610bed0f9fd7920b79cbec8b
    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.onControlSwap.csv")
    void onControlSwap() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-05-31"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShiftsByOutstaffId(100L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T03:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T10:50:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );

        Assertions.assertThat(shift.getShiftsByOutstaffId(101L))
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-05-31"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T01:50:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T01:59:00+03:00"))
                                .startTs(toInstant("2021-05-31T20:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-01T10:30:00.00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T10:30:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T10:50:00+03:00"))
                                .startTs(toInstant("2021-06-01T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:30:00.00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.nonProductionOperations.csv")
    void nonProductionOperations() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T13:00:00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00.00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T18:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T18:15:00+03:00"))
                                .startTs(toInstant("2021-06-02T20:00:00.00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00.00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.timex.csv")
    void timex() {
        mockClock(LocalDateTime.of(2021, 6, 7, 6, 0, 0));

        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L, 102L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin(null)
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-03T10:30:00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-04"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-05T00:15:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-05T10:30:00+03:00"))
                                .startTs(toInstant("2021-06-04T20:00:00+03:00"))
                                .controlTs(toInstant("2021-06-05T10:30:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-05"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-05T04:15:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-05T11:27:00+03:00"))
                                .startTs(toInstant("2021-06-05T08:00:00+03:00"))
                                .controlTs(toInstant("2021-06-06T10:30:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-07"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-07T03:02:10+03:00"))
                                .lastActivityTs(toInstant("2021-06-08T10:30:00+03:00"))
                                .startTs(toInstant("2021-06-07T08:00:00+03:00"))
                                .controlTs(toInstant("2021-06-08T10:30:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.fake_timex.action_before.csv")
    void splitTwoShiftsIfOneEndedByFakedTimexAndAnotherStartedBeforeIt() {
        mockClock(LocalDateTime.of(2021, 10, 7, 6, 0, 0));

        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L, 102L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-10-01"), LocalDate.parse("2021-11-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-10-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-10-01T09:15:30+03:00"))
                                .lastActivityTs(toInstant("2021-10-01T20:59:30+03:00"))
                                .startTs(toInstant("2021-10-01T08:00:00+03:00"))
                                .controlTs(toInstant("2021-10-02T10:30:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-10-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-10-02T09:00:08+03:00"))
                                .lastActivityTs(toInstant("2021-10-02T21:00:08+03:00"))
                                .startTs(toInstant("2021-10-02T08:00:00+03:00"))
                                .controlTs(toInstant("2021-10-03T10:30:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.fake_timex.timexBbeforeFake.csv")
    void splitTwoShiftsIfOneShouldBeEndedByFakedTimexButAnotherStartedWithTimexBeforeIt() {
        mockClock(LocalDateTime.of(2021, 10, 7, 6, 0, 0));

        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L, 102L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-10-01"), LocalDate.parse("2021-11-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-10-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-10-01T08:15:51+03:00"))
                                .lastActivityTs(toInstant("2021-10-01T20:15:51+03:00"))
                                .startTs(toInstant("2021-10-01T08:00:00+03:00"))
                                .controlTs(toInstant("2021-10-02T10:30:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-10-02"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-10-02T07:51:25+03:00"))
                                .lastActivityTs(toInstant("2021-10-03T10:30:00+03:00"))
                                .startTs(toInstant("2021-10-02T08:00:00+03:00"))
                                .controlTs(toInstant("2021-10-03T10:30:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.allEvents.csv")
    void allEvents() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T18:00:00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T18:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T18:15:00+03:00"))
                                .startTs(toInstant("2021-06-02T20:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.ignore_wms_npo.csv")
    void ignoreWmsNpo() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T13:00:00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T18:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T18:15:00+03:00"))
                                .startTs(toInstant("2021-06-02T20:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00+03:00"))
                                .build()
                );

        Assertions.assertThat(shift.getShifts()
                        .stream()
                        .flatMap(x -> x.getEventList().stream())
                        .map(OutstaffEvent::getType)
                        .collect(Collectors.toList()))
                .doesNotContain(OutstaffEvent.Type.WMS_NON_PRODUCTION_OPERATION);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.consider_wms_npo.csv")
    void considerWmsNpo() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(1L)
                .outstaffIds(List.of(100L, 101L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T13:00:00+03:00"))
                                .startTs(toInstant("2021-06-02T08:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T18:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T18:51:00+03:00"))
                                .startTs(toInstant("2021-06-02T20:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T10:30:00+03:00"))
                                .build()
                );

        Assertions.assertThat(shift.getShifts()
                        .stream()
                        .flatMap(x -> x.getEventList().stream())
                        .map(OutstaffEvent::getType)
                        .collect(Collectors.toList()))
                .contains(OutstaffEvent.Type.WMS_NON_PRODUCTION_OPERATION);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.three_shifts.csv")
    void simpleDayThreeShifts() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(2L)
                .outstaffIds(List.of(105L, 106L, 107L, 108L, 109L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(105L)
                                .wmsLogin("user-105")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T12:30:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T12:31:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T14:15:00+03:00"))
                                .controlTs(toInstant("2021-06-02T02:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(106L)
                                .wmsLogin("user-106")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T06:20:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T15:03:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T06:15:00+03:00"))
                                .controlTs(toInstant("2021-06-01T18:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(107L)
                                .wmsLogin("user-107")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T13:15:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T16:31:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T14:15:00+03:00"))
                                .controlTs(toInstant("2021-06-02T02:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(108L)
                                .wmsLogin("user-108")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T14:20:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T23:12:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T14:15:00+03:00"))
                                .controlTs(toInstant("2021-06-02T02:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(109L)
                                .wmsLogin("user-109")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.THIRD_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T22:20:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T06:04:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T22:15:00+03:00"))
                                .controlTs(toInstant("2021-06-02T10:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(109L)
                                .wmsLogin("user-109")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T10:16:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T10:17:00.00+03:00"))
                                .startTs(toInstant("2021-06-02T09:15:00+03:00"))
                                .controlTs(toInstant("2021-06-02T21:15:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.timex_three_shifts.csv")
    void timexThreeShifts() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(2L)
                .outstaffIds(List.of(105L, 106L, 107L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(105L)
                                .wmsLogin("user-105")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T21:15:00+03:00"))
                                .startTs(toInstant("2021-06-02T09:15:00+03:00"))
                                .controlTs(toInstant("2021-06-02T21:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(105L)
                                .wmsLogin("user-105")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-04"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-05T00:15:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-05T11:15:00+03:00"))
                                .startTs(toInstant("2021-06-04T23:15:00+03:00"))
                                .controlTs(toInstant("2021-06-05T11:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(106L)
                                .wmsLogin("user-106")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-05"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-05T06:16:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-05T11:27:00+03:00"))
                                .startTs(toInstant("2021-06-05T08:15:00+03:00"))
                                .controlTs(toInstant("2021-06-05T20:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(106L)
                                .wmsLogin("user-106")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-06"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-07T03:02:10+03:00"))
                                .lastActivityTs(toInstant("2021-06-07T11:15:00+03:00"))
                                .startTs(toInstant("2021-06-06T23:15:00+03:00"))
                                .controlTs(toInstant("2021-06-07T11:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(106L)
                                .wmsLogin("user-106")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-08"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-08T15:02:10+03:00"))
                                .lastActivityTs(toInstant("2021-06-09T04:15:00+03:00"))
                                .startTs(toInstant("2021-06-08T16:15:00+03:00"))
                                .controlTs(toInstant("2021-06-09T04:15:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.allEvents_three_shifts.csv")
    void allEventsThreeShifts() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(2L)
                .outstaffIds(List.of(100L, 101L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(100L)
                                .wmsLogin("user-100")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T12:15:00+03:00"))
                                .startTs(toInstant("2021-06-02T09:15:00+03:00"))
                                .controlTs(toInstant("2021-06-02T21:15:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(101L)
                                .wmsLogin("user-101")
                                .scLogin(null)
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T18:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T18:15:00+03:00"))
                                .startTs(toInstant("2021-06-02T16:15:00+03:00"))
                                .controlTs(toInstant("2021-06-03T04:15:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.four_shifts.csv")
    void simpleDayFourShifts() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(3L)
                .outstaffIds(List.of(105L, 106L, 107L, 108L, 109L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-05-31"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(105L)
                                .wmsLogin(null)
                                .scLogin("test-login-105")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T12:30:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T12:31:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T12:00:00+03:00"))
                                .controlTs(toInstant("2021-06-02T04:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(106L)
                                .wmsLogin(null)
                                .scLogin("test-login-106")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T06:20:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T15:03:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T09:00:00+03:00"))
                                .controlTs(toInstant("2021-06-02T01:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(107L)
                                .wmsLogin(null)
                                .scLogin("test-login-107")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T13:15:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T16:31:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T12:00:00+03:00"))
                                .controlTs(toInstant("2021-06-02T04:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(108L)
                                .wmsLogin(null)
                                .scLogin("test-login-108")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.THIRD_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T14:20:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-01T23:12:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T15:00:00+03:00"))
                                .controlTs(toInstant("2021-06-02T07:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(109L)
                                .wmsLogin(null)
                                .scLogin("test-login-109")
                                .shiftDate(LocalDate.parse("2021-06-01"))
                                .shiftType(OutStaffShiftType.FOURTH_SHIFT)
                                .firstActivityTs(toInstant("2021-06-01T22:20:00.00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T10:17:00.00+03:00"))
                                .startTs(toInstant("2021-06-01T21:00:00+03:00"))
                                .controlTs(toInstant("2021-06-02T13:00:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.timex_four_shifts.csv")
    void timexFourShifts() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(3L)
                .outstaffIds(List.of(200L, 201L, 202L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(200L)
                                .wmsLogin(null)
                                .scLogin("test-login-0")
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-03T04:00:00+03:00"))
                                .startTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T04:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(200L)
                                .wmsLogin(null)
                                .scLogin("test-login-0")
                                .shiftDate(LocalDate.parse("2021-06-04"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-05T00:15:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-05T09:44:00+03:00"))
                                .startTs(toInstant("2021-06-04T21:00:00+03:00"))
                                .controlTs(toInstant("2021-06-05T13:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(200L)
                                .wmsLogin(null)
                                .scLogin("test-login-0")
                                .shiftDate(LocalDate.parse("2021-06-07"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-07T09:15:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-07T18:15:00+03:00"))
                                .startTs(toInstant("2021-06-07T09:00:00+03:00"))
                                .controlTs(toInstant("2021-06-08T01:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(201L)
                                .wmsLogin(null)
                                .scLogin("test-login-1")
                                .shiftDate(LocalDate.parse("2021-06-05"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-05T06:16:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-05T11:27:00+03:00"))
                                .startTs(toInstant("2021-06-05T09:00:00+03:00"))
                                .controlTs(toInstant("2021-06-06T01:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(201L)
                                .wmsLogin(null)
                                .scLogin("test-login-1")
                                .shiftDate(LocalDate.parse("2021-06-07"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-07T03:02:10+03:00"))
                                .lastActivityTs(toInstant("2021-06-08T01:00:00+03:00"))
                                .startTs(toInstant("2021-06-07T09:00:00+03:00"))
                                .controlTs(toInstant("2021-06-08T01:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(201L)
                                .wmsLogin(null)
                                .scLogin("test-login-1")
                                .shiftDate(LocalDate.parse("2021-06-08"))
                                .shiftType(OutStaffShiftType.BIOMETRY_SHIFT)
                                .firstActivityTs(toInstant("2021-06-08T15:02:10+03:00"))
                                .lastActivityTs(toInstant("2021-06-09T07:00:00+03:00"))
                                .startTs(toInstant("2021-06-08T15:00:00+03:00"))
                                .controlTs(toInstant("2021-06-09T07:00:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.allEvents_four_shifts.csv")
    void allEventsFourShifts() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(3L)
                .outstaffIds(List.of(200L, 201L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .usingElementComparatorOnFields(ELEMENTS_TO_COMPARE)
                .containsExactlyInAnyOrder(
                        OutStaffShift.builder()
                                .outstaffId(200L)
                                .wmsLogin(null)
                                .scLogin("test-login-0")
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.SECOND_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T15:30:52+03:00"))
                                .startTs(toInstant("2021-06-02T12:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T04:00:00+03:00"))
                                .build(),
                        OutStaffShift.builder()
                                .outstaffId(201L)
                                .wmsLogin(null)
                                .scLogin("test-login-1")
                                .shiftDate(LocalDate.parse("2021-06-02"))
                                .shiftType(OutStaffShiftType.THIRD_SHIFT)
                                .firstActivityTs(toInstant("2021-06-02T18:00:00+03:00"))
                                .lastActivityTs(toInstant("2021-06-02T18:15:00+03:00"))
                                .startTs(toInstant("2021-06-02T15:00:00+03:00"))
                                .controlTs(toInstant("2021-06-03T07:00:00+03:00"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "OutstaffShiftCalculationServiceImplTest.dontForgetNight.csv")
    void noScLoginsShouldNotFail() {
        var shift = shiftCalculationService.calculate(Context.builder()
                .domainId(4L)
                .outstaffIds(List.of(501L))
                .interval(new LocalDateInterval(LocalDate.parse("2021-06-01"), LocalDate.parse("2021-07-01")))
                .build()
        );

        Assertions.assertThat(shift.getShifts())
                .isEmpty();
    }


    Instant toInstant(String instantStr) {
        return OffsetDateTime.parse(instantStr).toInstant();
    }
}
