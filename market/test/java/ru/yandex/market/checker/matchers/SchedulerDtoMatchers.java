package ru.yandex.market.checker.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.checker.api.model.SchedulerDto;
import ru.yandex.market.checker.api.model.SchedulerStatus;
import ru.yandex.market.checker.api.model.SchedulerType;
import ru.yandex.market.mbi.util.MbiMatchers;

public class SchedulerDtoMatchers {
    public static Matcher<SchedulerDto> hasId(Long expectedValue) {
        return MbiMatchers.<SchedulerDto>newAllOfBuilder()
                .add(SchedulerDto::getId, expectedValue, "id")
                .build();
    }

    public static Matcher<SchedulerDto> hasName(String expectedValue) {
        return MbiMatchers.<SchedulerDto>newAllOfBuilder()
                .add(SchedulerDto::getName, expectedValue, "name")
                .build();
    }

    public static Matcher<SchedulerDto> hasType(SchedulerType schedulerType) {
        return MbiMatchers.<SchedulerDto>newAllOfBuilder()
                .add(SchedulerDto::getType, schedulerType, "type")
                .build();
    }

    public static Matcher<SchedulerDto> hasStatus(SchedulerStatus expectedValue) {
        return MbiMatchers.<SchedulerDto>newAllOfBuilder()
                .add(SchedulerDto::getStatus, expectedValue, "status")
                .build();
    }
}
