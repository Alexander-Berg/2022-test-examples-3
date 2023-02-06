package ru.yandex.market.checker.matchers;

import java.time.Instant;

import org.hamcrest.Matcher;

import ru.yandex.market.checker.model.Task;
import ru.yandex.market.mbi.util.MbiMatchers;

public class TaskMatchers {
    public static Matcher<Task> hasDateFrom(Instant expectedValue) {
        return MbiMatchers.<Task>newAllOfBuilder()
                .add(Task::getDateFrom, expectedValue, "dateFrom")
                .build();
    }

    public static Matcher<Task> hasDateTo(Instant expectedValue) {
        return MbiMatchers.<Task>newAllOfBuilder()
                .add(Task::getDateTo, expectedValue, "dateTo")
                .build();
    }

}
