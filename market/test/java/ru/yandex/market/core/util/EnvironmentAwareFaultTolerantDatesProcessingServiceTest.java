package ru.yandex.market.core.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.environment.CompareAndUpdateEnvironmentService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

class EnvironmentAwareFaultTolerantDatesProcessingServiceTest extends FunctionalTest {

    private static final String KEY = "key";
    private static final String STOP_ON_EXCEPTION_KEY = "stopOnExceptionKey";

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2020-04-17T10:00:00Z"),
            ZoneOffset.systemDefault());
    private final DateProcessor dateProcessor = Mockito.mock(DateProcessor.class);
    private final DateProcessor dateProcessorWithException = Mockito.spy(new DateProcessorWithException());
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private CompareAndUpdateEnvironmentService compareAndUpdateEnvironmentService;
    private EnvironmentAwareFaultTolerantDatesProcessingService datesProcessingService;

    @BeforeEach
    void init() {
        datesProcessingService = new EnvironmentAwareFaultTolerantDatesProcessingService(
                CLOCK, environmentService, compareAndUpdateEnvironmentService
        );
        Mockito.reset(dateProcessor);
        Mockito.reset(dateProcessorWithException);
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.before.csv",
            after = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.after.csv"
    )
    void testProcessDaysUntilToday() {
        datesProcessingService.processDaysUntilToday(dateProcessor, KEY, STOP_ON_EXCEPTION_KEY);

        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 15)));
        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 16)));
        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 17)));

        Mockito.verifyNoMoreInteractions(dateProcessor);
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testProcessDaysUntilTodayWithExceptions.before.csv",
            after = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testProcessDaysUntilTodayWithExceptions.after.csv"
    )
    void testProcessDaysUntilTodayWithExceptions() {
        RuntimeException exception = null;
        try {
            datesProcessingService.processDaysUntilToday(dateProcessorWithException, KEY, STOP_ON_EXCEPTION_KEY);
        } catch (RuntimeException e) {
            exception = e;
        }

        // Проверяем, что исключение было
        assertNotNull(exception);
        assertThat(exception.getSuppressed().length, is(1));

        // Проверяем, что метод process() был вызван для всех требуемых дат.
        Mockito.verify(dateProcessorWithException, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 15)));
        Mockito.verify(dateProcessorWithException, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 16)));
        Mockito.verify(dateProcessorWithException, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 17)));
        Mockito.verifyNoMoreInteractions(dateProcessorWithException);
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testStopWhenProcessDaysUntilTodayWithExceptions.before.csv",
            after = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testStopWhenProcessDaysUntilTodayWithExceptions.after.csv"
    )
    void testStopWhenProcessDaysUntilTodayWithExceptions() {
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> datesProcessingService.processDaysUntilToday(dateProcessorWithException, KEY,
                        STOP_ON_EXCEPTION_KEY)
        );

        Assertions.assertEquals("Unable to process date 2020-04-16", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.before.csv",
            after = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.after.csv"
    )
    void testProcessDaysUntilYesterday() {
        datesProcessingService.processDaysUntilYesterday(dateProcessor, KEY, STOP_ON_EXCEPTION_KEY);

        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 15)));
        Mockito.verify(dateProcessor, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 16)));

        Mockito.verifyNoMoreInteractions(dateProcessor);
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testProcessDaysUntilYesterdayWithExceptions.before.csv",
            after = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testProcessDaysUntilYesterdayWithExceptions.after.csv"
    )
    void testProcessDaysUntilYesterdayWithExceptions() {
        RuntimeException exception = null;
        try {
            datesProcessingService.processDaysUntilYesterday(dateProcessorWithException, KEY, STOP_ON_EXCEPTION_KEY);
        } catch (RuntimeException e) {
            exception = e;
        }

        // Проверяем, что исключение было
        assertNotNull(exception);
        assertThat(exception.getSuppressed().length, is(1));

        // Проверяем, что метод process() был вызван для всех требуемых дат.
        Mockito.verify(dateProcessorWithException, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 14)));
        Mockito.verify(dateProcessorWithException, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 15)));
        Mockito.verify(dateProcessorWithException, Mockito.times(1)).process(Mockito.eq(LocalDate.of(2020, 4, 16)));
        Mockito.verifyNoMoreInteractions(dateProcessorWithException);
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testStopWhenProcessDaysUntilYesterdayWithExceptions.before.csv",
            after = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testStopWhenProcessDaysUntilYesterdayWithExceptions.after.csv"
    )
    void testStopWhenProcessDaysUntilYesterdayWithExceptions() {
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> datesProcessingService.processDaysUntilYesterday(dateProcessorWithException, KEY,
                        STOP_ON_EXCEPTION_KEY)
        );

        Assertions.assertEquals("Unable to process date 2020-04-16", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testVerifyDateIsNotInTheFuture.before.csv"
    )
    void testVerifyDateIsNotInTheFuture() {
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> datesProcessingService.processDaysUntilToday(dateProcessor, KEY, STOP_ON_EXCEPTION_KEY)
        );

        Assertions.assertEquals("The required date 2020-04-18 is in the future", exception.getMessage());
    }

    @Test
    void testDateIsMissing() {
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> datesProcessingService.processDaysUntilToday(dateProcessor, KEY, STOP_ON_EXCEPTION_KEY)
        );

        Assertions.assertEquals("Value is empty for key: key", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(
            before = "EnvironmentAwareFaultTolerantDatesProcessingServiceTest.testCannotParseDate.before.csv"
    )
    void testCannotParseDate() {
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> datesProcessingService.processDaysUntilToday(dateProcessor, KEY, STOP_ON_EXCEPTION_KEY)
        );

        Assertions.assertEquals("Cannot parse date for key: key", exception.getMessage());
    }

    private static class DateProcessorWithException implements DateProcessor {
        private int count = 0;

        @Override
        public void process(LocalDate date) {
            if (count > 0) {
                throw new IllegalStateException();
            }
            count++;
        }
    }
}
