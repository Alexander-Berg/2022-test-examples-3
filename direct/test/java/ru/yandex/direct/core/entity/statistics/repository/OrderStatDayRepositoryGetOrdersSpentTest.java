package ru.yandex.direct.core.entity.statistics.repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.statistics.model.SpentInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.DateTimeUtils.MSK;

@Ignore("Ходит в yt, для ручного запуска")
@CoreTest
@RunWith(SpringRunner.class)
public class OrderStatDayRepositoryGetOrdersSpentTest {

    @Autowired
    private OrderStatDayRepository orderStatDayRepository;

    @Test
    public void emptyResult() {
        Long orderId = 1L;

        Map<Long, SpentInfo> res = orderStatDayRepository.getOrdersSpent(
                Collections.singleton(orderId), LocalDate.now());

        assertThat(res).isEmpty();
    }

    @Test
    public void nonEmptyResult() {
        Long orderId = 13115893L;
        LocalDate day = LocalDate.of(2017, 1, 19);

        Map<Long, List<SpentInfo>> res = orderStatDayRepository.getOrdersSpent(
                List.of(orderId), List.of(day), List.of(day));

        SpentInfo expected = new SpentInfo(orderId, day.atStartOfDay().atZone(MSK).toInstant(), 0L, 17L, 643L);

        assertThat(res).containsEntry(orderId, List.of(expected));
    }

    @Test
    public void multipleIds() {
        Map<Long, List<SpentInfo>> expected = new HashMap<>();

        List<Long> orderIds = List.of(14059405L, 17098939L, 15363824L, 10021741L);
        LocalDate day = LocalDate.of(2017, 10, 10);
        List<LocalDate> startDates = List.of(day, day, day, day);
        List<LocalDate> endDates = List.of(day, day, day, day.plusDays(1));

        expected.put(14059405L, List.of(
                new SpentInfo(14059405L, day.atStartOfDay().atZone(MSK).toInstant(), 476072L, 17L, 643L)));
        expected.put(17098939L, List.of(
                new SpentInfo(17098939L, day.atStartOfDay().atZone(MSK).toInstant(), 0L, 17L, 643L)));
        expected.put(15363824L, List.of(
                new SpentInfo(15363824L, day.atStartOfDay().atZone(MSK).toInstant(), 37244L, 17L, 643L)));
        expected.put(10021741L, List.of(
                new SpentInfo(10021741L, day.atStartOfDay().atZone(MSK).toInstant(), 4543767L, 17L, 643L),
                new SpentInfo(10021741L, day.plusDays(1).atStartOfDay().atZone(MSK).toInstant(), 4720000L, 17L, 643L)));

        Map<Long, List<SpentInfo>> res = orderStatDayRepository.getOrdersSpent(orderIds, startDates, endDates);

        assertThat(res).isEqualTo(expected);
    }

}
