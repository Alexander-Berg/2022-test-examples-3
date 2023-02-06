package ru.yandex.market.checker.matchers;

import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.checker.api.model.SchedulerType;
import ru.yandex.market.checker.api.model.TaskDto;
import ru.yandex.market.checker.api.model.TaskStatus;
import ru.yandex.market.mbi.util.MbiMatchers;

public class TaskDtoMatchers {
    public static Matcher<TaskDto> hasId(Long expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getId, expectedValue, "id")
                .build();
    }

    public static Matcher<TaskDto> hasSchedulerId(Long expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getSchedulerId, expectedValue, "schedulerId")
                .build();
    }

    public static Matcher<TaskDto> hasType(SchedulerType schedulerType) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getType, schedulerType, "type")
                .build();
    }

    public static Matcher<TaskDto> hasStatus(TaskStatus expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getStatus, expectedValue, "status")
                .build();
    }

    public static Matcher<TaskDto> hasResultTable(String expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getResultTable, expectedValue, "resultTable")
                .build();
    }

    public static Matcher<TaskDto> hasFirstComponent(String expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getFirstComponent, expectedValue, "firstComponent")
                .build();
    }

    public static Matcher<TaskDto> hasSecondComponent(String expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getSecondComponent, expectedValue, "secondComponent")
                .build();
    }

    public static Matcher<TaskDto> hasFirstComponentTable(String expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getFirstComponentTable, expectedValue, "firstComponentTable")
                .build();
    }

    public static Matcher<TaskDto> hasSecondComponentTable(String expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getSecondComponentTable, expectedValue, "secondComponentTable")
                .build();
    }

    public static Matcher<TaskDto> hasDateFrom(LocalDate expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getDateFrom, expectedValue, "dateFrom")
                .build();
    }

    public static Matcher<TaskDto> hasDateTo(LocalDate expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getDateTo, expectedValue, "dateTo")
                .build();
    }

    public static Matcher<TaskDto> hasTechnicalReport(String expectedValue) {
        return MbiMatchers.<TaskDto>newAllOfBuilder()
                .add(TaskDto::getTechnicalReport, expectedValue, "technicalReport")
                .build();
    }
}
