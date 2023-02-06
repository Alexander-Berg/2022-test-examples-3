package ru.yandex.direct.core.entity.statistics.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ru.yandex.direct.core.entity.statistics.model.StatisticsDateRange;
import ru.yandex.direct.core.entity.statistics.repository.OrderStatDayRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class GetLastDayOfCampaignsTest {
    private static final LocalDate minUpdateDate = LocalDate.ofYearDay(2005, 200);
    private OrderStatService orderStatService;
    private OrderStatDayRepository orderStatDayRepository;

    @Before
    public void init() {
        orderStatDayRepository = mock(OrderStatDayRepository.class);
        orderStatService = new OrderStatService(orderStatDayRepository, null,null,null, null, null, null);
    }

    @Test
    public void getLastDayOfCampaignsPositiveTest() {
        LocalDate date1 = LocalDate.ofYearDay(2017, 340);
        LocalDate date2 = LocalDate.ofYearDay(2018, 40);
        Map<Long, StatisticsDateRange> mockValue = new HashMap<>();
        mockValue.put(1L, new StatisticsDateRange(minUpdateDate, date1));
        mockValue.put(2L, new StatisticsDateRange(minUpdateDate, date2));
        when(orderStatDayRepository.getOrdersFirstLastDay(any())).thenReturn(mockValue);
        Map<Long, LocalDate> lastDayOfCampaigns = orderStatService.getLastDayOfCampaigns(List.of(1L, 2L, 3L));
        Map<Long, LocalDate> expected = new HashMap<>();
        expected.put(1L, date1);
        expected.put(2L, date2);
        expected.put(3L, null);
        assertThat(lastDayOfCampaigns).isEqualTo(expected);
    }
}
