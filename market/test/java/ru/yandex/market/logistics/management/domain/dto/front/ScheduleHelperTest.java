package ru.yandex.market.logistics.management.domain.dto.front;

import java.time.LocalDate;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ScheduleHelperTest {

    @Test
    public void testFilterRange() {
        Assertions.assertEquals(2,
            ScheduleHelper.filterRange(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 1, 2),
                (cDay) -> true,
                Function.identity()).size());
    }
}
