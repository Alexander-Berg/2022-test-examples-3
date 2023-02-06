package ru.yandex.market.stat.hyperduct.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.stat.hyperduct.storage.dao.HyperductDao;
import ru.yandex.market.stat.hyperduct.storage.dao.PeriodUpdate;
import ru.yandex.market.stat.hyperduct.utils.PeriodUtils;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kateleb on 24.10.19.
 */
@RunWith(MockitoJUnitRunner.class)
public class HyperductServiceTest {

    @Mock
    private HyperductDao dao;
    private HyperductService service;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        service = new HyperductService(dao);
    }

    @Test
    public void testPipeline() {
        LocalDateTime lastUpdateTime = LocalDate.of(2019, 10, 20).atStartOfDay();
        LocalDateTime expectedMaxUpdateTime = lastUpdateTime.plusDays(2);
        Set<String> expectedReloadPeriods = PeriodUtils.lastPeriods(3);
        String latestPeriod = expectedReloadPeriods.iterator().next();
        expectedReloadPeriods.add("May-19");

        when(dao.getLastProcessedUpdateTime()).thenReturn(lastUpdateTime);
        when(dao.getPeriodsUpdatedAfter(lastUpdateTime))
                .thenReturn(Arrays.asList(new PeriodUpdate(latestPeriod, expectedMaxUpdateTime),
                        new PeriodUpdate("May-19", lastUpdateTime.plusDays(1))));
        when(dao.updateLastUpdatedTime(eq(expectedReloadPeriods), eq(expectedMaxUpdateTime), any(LocalDateTime.class))).thenReturn(true);

        Assert.assertThat(service.addNewIncrementialDataToAggregation(), is(true));

        verify(dao).updateLastUpdatedTime(eq(expectedReloadPeriods), eq(expectedMaxUpdateTime), any(LocalDateTime.class));
        verify(dao).insertNewAggregations(expectedReloadPeriods);
    }
}
