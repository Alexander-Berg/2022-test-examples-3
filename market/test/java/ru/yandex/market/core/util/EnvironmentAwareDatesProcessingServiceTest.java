package ru.yandex.market.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.environment.CompareAndUpdateEnvironmentService;
import ru.yandex.market.core.environment.EnvironmentAwareDatesProcessingService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

class EnvironmentAwareDatesProcessingServiceTest extends FunctionalTest {

    private static final String KEY = "key";

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2020-04-17T10:00:00Z"),
            ZoneOffset.systemDefault());
    private final DateProcessor dateProcessor = Mockito.mock(DateProcessor.class);
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private CompareAndUpdateEnvironmentService compareAndUpdateEnvironmentService;
    private EnvironmentAwareDatesProcessingService datesProcessingService;

    @BeforeEach
    void init() {
        datesProcessingService = new EnvironmentAwareDatesProcessingService(
                CLOCK, environmentService, compareAndUpdateEnvironmentService
        );
        Mockito.reset(dateProcessor);
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareDatesProcessingServiceTest.before.csv",
            after = "EnvironmentAwareDatesProcessingServiceTest.after.csv"
    )
    void testProcessDaysUntilToday() {
        datesProcessingService.processDaysUntilToday(dateProcessor, KEY);

        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 15)));
        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 16)));
        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 17)));

        Mockito.verifyNoMoreInteractions(dateProcessor);
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareDatesProcessingServiceTest.before.csv",
            after = "EnvironmentAwareDatesProcessingServiceTest.after.csv"
    )
    void testProcessDaysUntilYesterday() {
        datesProcessingService.processDaysUntilYesterday(dateProcessor, KEY);

        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 15)));
        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 16)));

        Mockito.verifyNoMoreInteractions(dateProcessor);
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareDatesProcessingServiceTest.testVerifyDateIsNotInTheFuture.before.csv"
    )
    void testVerifyDateIsNotInTheFuture() {
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> datesProcessingService.processDaysUntilToday(dateProcessor, KEY)
        );

        Assertions.assertEquals("The required date 2020-04-18 is in the future", exception.getMessage());
    }

    @Test
    void testDateIsMissing() {
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> datesProcessingService.processDaysUntilToday(dateProcessor, KEY)
        );

        Assertions.assertEquals("Value is empty for key: key", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareDatesProcessingServiceTest.testCannotParseDate.before.csv"
    )
    void testCannotParseDate() {
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> datesProcessingService.processDaysUntilToday(dateProcessor, KEY)
        );

        Assertions.assertEquals("Cannot parse date for key: key", exception.getMessage());
    }
}
