package ru.yandex.market.checker.matchers;

import java.time.temporal.ChronoUnit;

import org.hamcrest.Matcher;

import ru.yandex.market.checker.api.model.SchedulerStatus;
import ru.yandex.market.checker.model.Scheduler;
import ru.yandex.market.mbi.util.MbiMatchers;

public class SchedulerMatchers {
    public static Matcher<Scheduler> hasId(Long expectedValue) {
        return MbiMatchers.<Scheduler>newAllOfBuilder()
                .add(Scheduler::getId, expectedValue, "id")
                .build();
    }

    public static Matcher<Scheduler> hasName(String expectedValue) {
        return MbiMatchers.<Scheduler>newAllOfBuilder()
                .add(Scheduler::getName, expectedValue, "name")
                .build();
    }

    public static Matcher<Scheduler> hasDescription(String expectedValue) {
        return MbiMatchers.<Scheduler>newAllOfBuilder()
                .add(Scheduler::getDescription, expectedValue, "description")
                .build();
    }

    public static Matcher<Scheduler> hasStatus(SchedulerStatus expectedValue) {
        return MbiMatchers.<Scheduler>newAllOfBuilder()
                .add(Scheduler::getStatus, expectedValue, "status")
                .build();
    }

    public static Matcher<Scheduler> hasTimeWindowSize(Integer expectedValue) {
        return MbiMatchers.<Scheduler>newAllOfBuilder()
                .add(Scheduler::getTimeWindowSize, expectedValue, "timeWindowSize")
                .build();
    }

    public static Matcher<Scheduler> hasTimeWindowUnit(ChronoUnit expectedValue) {
        return MbiMatchers.<Scheduler>newAllOfBuilder()
                .add(Scheduler::getTimeWindowUnit, expectedValue, "timeWindowUnit")
                .build();
    }
}
