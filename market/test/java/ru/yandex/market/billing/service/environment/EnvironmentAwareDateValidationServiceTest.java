package ru.yandex.market.billing.service.environment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentAwareDateValidationServiceTest extends FunctionalTest {

    private static final Instant MAY_1 = DateTimes.toInstantAtDefaultTz(2019, 5, 1, 15, 34, 43);
    private static final Instant MAY_15 = DateTimes.toInstantAtDefaultTz(2019, 5, 15, 15, 34, 43);

    @Autowired
    private EnvironmentService environmentService;

    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;

    @BeforeEach
    void setUp() {
        environmentAwareDateValidationService = new EnvironmentAwareDateValidationService(
                Clock.fixed(MAY_15, ZoneOffset.UTC),
                environmentService
        );
    }

    @Test
    void testRequiredDateInCurrentMonth() {
        for (int i = 1; i <= 31; i++) {
            final LocalDate targetDate = LocalDate.of(2019, 5, i);
            assertTrue(EnvironmentAwareDateValidationService.isCurrentMonthOrFirstDayOfNextMonth(targetDate, MAY_1));
        }
    }

    @Test
    void testTodayIsTheFirstDayOfMonthAndTheRequiredDateInThePastMonth() {
        for (int i = 1; i <= 30; i++) {
            final LocalDate targetDate = LocalDate.of(2019, 4, i);
            assertTrue(EnvironmentAwareDateValidationService.isCurrentMonthOrFirstDayOfNextMonth(targetDate, MAY_1));
        }
    }

    @Test
    void testRequiredDateInThePastMonth() {
        for (int i = 1; i <= 31; i++) {
            final LocalDate targetDate = LocalDate.of(2019, 3, i);
            assertFalse(EnvironmentAwareDateValidationService.isCurrentMonthOrFirstDayOfNextMonth(targetDate, MAY_1));
        }
    }

    @Test
    void testValidDate() {
        final LocalDate targetDate = LocalDate.of(2019, 5, 6);
        environmentAwareDateValidationService.verifyDate(
                targetDate,
                EnvironmentAwareDateValidationService::isCurrentMonthOrFirstDayOfNextMonth,
                EnvironmentAwareDateValidationServiceTest.class
        );
    }

    @Test
    @DbUnitDataSet(before = "EnvironmentAwareDateValidationServiceTest.testIgnoreDateValidationIsTurnedOn.before.csv")
    void testIgnoreDateValidationIsTurnedOn() {
        final LocalDate targetDate = LocalDate.of(2019, 4, 14);
        environmentAwareDateValidationService.verifyDate(
                targetDate,
                EnvironmentAwareDateValidationService::isCurrentMonthOrFirstDayOfNextMonth,
                EnvironmentAwareDateValidationServiceTest.class
        );
    }

    @Test
    @DbUnitDataSet(before = "EnvironmentAwareDateValidationServiceTest.testIgnoreDateValidationIsTurnedOff.before.csv")
    void testIgnoreDateValidationIsTurnedOff() {
        final LocalDate targetDate = LocalDate.of(2019, 4, 14);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () ->
                        environmentAwareDateValidationService.verifyDate(
                                targetDate,
                                EnvironmentAwareDateValidationService::isCurrentMonthOrFirstDayOfNextMonth,
                                EnvironmentAwareDateValidationServiceTest.class
                        )
        );

        assertEquals("Required date 2019-04-14 cannot be used.", exception.getMessage());
    }
}
