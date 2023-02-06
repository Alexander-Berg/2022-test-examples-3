package ru.yandex.market.archive;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.function.Predicate;

import javax.annotation.Nonnull;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.archive.schema.DateSlice;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author snoop
 */
public class UtilitiesTest {

    @Test
    public void yearRetention_hourSlice_getArchiveFilenameCleanupFilter() {
        final Predicate<String> filter = filter(LocalDateTime.of(2014, Month.JANUARY, 1, 0, 0), DateSlice.DH);
        Assertions.assertTrue(filter.test("2013"));
        Assertions.assertTrue(filter.test("2013-12"));
        Assertions.assertTrue(filter.test("2013-12-31"));
        Assertions.assertTrue(filter.test("2013-12-31-23"));
        Assertions.assertFalse(filter.test("2014"));
        Assertions.assertFalse(filter.test("2015"));
        Assertions.assertFalse(filter.test("2014-01"));
        Assertions.assertFalse(filter.test("2015-06"));
        Assertions.assertFalse(filter.test("2014-01-01"));
        Assertions.assertFalse(filter.test("2016-03-31"));
        Assertions.assertFalse(filter.test("2014-01-01-00"));
        Assertions.assertFalse(filter.test("2017-04-16-09"));
    }

    @Test
    public void yearRetention_daySlice_getArchiveFilenameCleanupFilter() {
        final Predicate<String> filter = filter(LocalDateTime.of(2014, Month.JANUARY, 1, 0, 0), DateSlice.DT);
        Assertions.assertTrue(filter.test("2013"));
        Assertions.assertTrue(filter.test("2013-12"));
        Assertions.assertTrue(filter.test("2013-12-31"));
        Assertions.assertFalse(filter.test("2014"));
        Assertions.assertFalse(filter.test("2014-01"));
        Assertions.assertFalse(filter.test("2014-01-01"));
    }

    @Test
    public void yearRetention_monthSlice_getArchiveFilenameCleanupFilter() {
        final Predicate<String> filter = filter(LocalDateTime.of(2014, Month.JANUARY, 1, 0, 0), DateSlice.YM);
        Assertions.assertTrue(filter.test("2013"));
        Assertions.assertTrue(filter.test("2013-12"));
        Assertions.assertFalse(filter.test("2014"));
        Assertions.assertFalse(filter.test("2015"));
        Assertions.assertFalse(filter.test("2014-01"));
        Assertions.assertFalse(filter.test("2015-12"));
    }

    @Test
    public void monthRetention_hourSlice_getArchiveFilenameCleanupFilter() {
        final Predicate<String> filter = filter(LocalDateTime.of(2014, Month.APRIL, 1, 0, 0), DateSlice.DH);
        Assertions.assertTrue(filter.test("2013-12"));
        //it's not possible to have yearly tables while retention age unit is month
//        assertFalse(filter.test("2014"));
        Assertions.assertTrue(filter.test("2014-03"));
        Assertions.assertTrue(filter.test("2014-03-31"));
        Assertions.assertTrue(filter.test("2014-03-31-23"));
        Assertions.assertFalse(filter.test("2014-04"));
        Assertions.assertFalse(filter.test("2014-04-01"));
        Assertions.assertFalse(filter.test("2014-04-01-00"));
    }

    @Test
    public void monthRetention_daySlice_getArchiveFilenameCleanupFilter() {
        final Predicate<String> filter = filter(LocalDateTime.of(2014, Month.APRIL, 1, 0, 0), DateSlice.DT);
        Assertions.assertTrue(filter.test("2013-12"));
        Assertions.assertTrue(filter.test("2014-03"));
        Assertions.assertTrue(filter.test("2014-03-31"));
        Assertions.assertFalse(filter.test("2014-04"));
        Assertions.assertFalse(filter.test("2014-05"));
        Assertions.assertFalse(filter.test("2014-04-01"));
        Assertions.assertFalse(filter.test("2014-05-01"));
    }

    @Test
    public void monthRetention_monthSlice_getArchiveFilenameCleanupFilter() {
        final Predicate<String> filter = filter(LocalDateTime.of(2014, Month.APRIL, 1, 0, 0), DateSlice.YM);
        Assertions.assertTrue(filter.test("2013-12"));
        Assertions.assertTrue(filter.test("2014-03"));
        Assertions.assertFalse(filter.test("2014-04"));
        Assertions.assertFalse(filter.test("2014-05"));
    }

    @Test
    public void dayRetention_hourSlice_getArchiveFilenameCleanupFilter() {
        final Predicate<String> filter = filter(LocalDateTime.of(2014, Month.APRIL, 16, 0, 0), DateSlice.DH);
        Assertions.assertTrue(filter.test("2013-12-31"));
        Assertions.assertTrue(filter.test("2014-03-31"));
        Assertions.assertTrue(filter.test("2014-04-15"));
        Assertions.assertTrue(filter.test("2014-04-15-23"));
        Assertions.assertFalse(filter.test("2014-04-16"));
        Assertions.assertFalse(filter.test("2014-04-16-00"));
    }

    @Test
    public void dayRetention_daySlice_getArchiveFilenameCleanupFilter() {
        final Predicate<String> filter = filter(LocalDateTime.of(2014, Month.APRIL, 16, 0, 0), DateSlice.DT);
        Assertions.assertTrue(filter.test("2013-12-31"));
        Assertions.assertTrue(filter.test("2014-03-31"));
        Assertions.assertTrue(filter.test("2014-04-15"));
        Assertions.assertFalse(filter.test("2014-04-16"));
        Assertions.assertFalse(filter.test("2015-04-17"));
    }

    @Test
    public void hourRetention_hourSlice_getArchiveFilenameCleanupFilter() {
        final Predicate<String> filter = filter(LocalDateTime.of(2014, Month.APRIL, 16, 9, 0), DateSlice.DH);
        Assertions.assertTrue(filter.test("2013-12-31-11"));
        Assertions.assertTrue(filter.test("2014-03-31-05"));
        Assertions.assertTrue(filter.test("2014-04-15-23"));
        Assertions.assertTrue(filter.test("2014-04-16-08"));
        Assertions.assertFalse(filter.test("2014-04-16-09"));
        Assertions.assertFalse(filter.test("2015-04-16-10"));
    }

    @Nonnull
    private Predicate<String> filter(LocalDateTime dt, DateSlice slice) {
        JobConfiguration cfg = mock(JobConfiguration.class);
        when(cfg.getDateSlice()).thenReturn(slice);
        when(cfg.retentionBoundary()).thenReturn(dt);
        return Utilities.getArchiveFilenameCleanupFilter(cfg);
    }

}
