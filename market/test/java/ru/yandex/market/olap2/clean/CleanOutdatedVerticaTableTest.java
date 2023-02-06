package ru.yandex.market.olap2.clean;

import java.time.LocalDate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.olap2.dao.VerticaDao;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.olap2.clean.CleanOutdatedVerticaTable.MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE;

@RunWith(MockitoJUnitRunner.class)
public class CleanOutdatedVerticaTableTest {

    @Mock
    private VerticaDao verticaDao;
    private CleanOutdatedVerticaTable job; //= new CleanOutdatedVerticaTable(verticaDao);


    @Test
    public void testForMidMonth() {
        LocalDate currDate = LocalDate.parse("2020-04-10");
        when(verticaDao.getOldestPartition(MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE)).thenReturn("2020-01-14");
        job = new CleanOutdatedVerticaTable(verticaDao);

        job.delete30daysOlderThan(currDate);
        verify(verticaDao, times(2)).getOldestPartition(MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE);
        verify(verticaDao).dropPartitions(MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE,
                LocalDate.parse("2020-01-14"), LocalDate.parse("2020-01-30")); // но не 31 янв

        verify(verticaDao).dropPartitions(MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE,
                LocalDate.parse("2020-02-01"), LocalDate.parse("2020-02-28")); //но не 29 фев
        verify(verticaDao).dropPartitions(MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE,
                LocalDate.parse("2020-03-01"), LocalDate.parse("2020-03-10"));
        verify(verticaDao).dropPartitions(MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE,
                LocalDate.parse("2020-01-14"), LocalDate.now().minusYears(1));
        verifyNoMoreInteractions(verticaDao);
    }

    @Test
    public void testForEndMonth() {
        LocalDate currDate = LocalDate.parse("2020-08-31");
        when(verticaDao.getOldestPartition(MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE)).thenReturn("2020-06-30");
        job = new CleanOutdatedVerticaTable(verticaDao);

        job.delete30daysOlderThan(currDate);
        verify(verticaDao).getOldestPartition(MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE);
        verify(verticaDao).dropPartitions(MATRIX_COVERAGE_REGIONAL_PARTITIONED_TABLE,
                LocalDate.parse("2020-07-01"), LocalDate.parse("2020-07-30")); // но не 31 июля
        verifyNoMoreInteractions(verticaDao);
    }

}
