package ru.yandex.market.mboc.common.masterdata.parsing.utils;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.model.SupplyEvent;

public class SupplyScheduleConverterTest {

    @Test
    public void convertToString() {
        List<SupplyEvent> supplyEvents = Arrays.asList(
            new SupplyEvent(DayOfWeek.MONDAY),
            new SupplyEvent(DayOfWeek.WEDNESDAY)
        );
        String schedule = SupplyScheduleConverter.convertToString(supplyEvents);

        Assertions.assertThat(schedule).isEqualTo("пн,ср");
    }
}
