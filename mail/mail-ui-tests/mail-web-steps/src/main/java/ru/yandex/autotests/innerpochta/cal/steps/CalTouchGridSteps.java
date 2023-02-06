package ru.yandex.autotests.innerpochta.cal.steps;

import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.qatools.allure.annotations.Step;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class CalTouchGridSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    public CalTouchGridSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Открываем календарь на дне, который был {0} дней назад")
    public CalTouchGridSteps openPastDayGrid(int daysBefore) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDateTime date = LocalDateTime.now();
        user.defaultSteps()
            .opensDefaultUrlWithPostFix("?show_date=" + dateFormat.format(date.minusDays(daysBefore)));
        return this;
    }

    @Step("Открываем календарь на дне, который будет через {0} дней")
    public CalTouchGridSteps openFutureDayGrid(int daysAfter) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDateTime date = LocalDateTime.now();
        user.defaultSteps()
            .opensDefaultUrlWithPostFix("?show_date=" + dateFormat.format(date.plusDays(daysAfter)));
        return this;
    }

    @Step("Проверяем, что находимся на сетке сегодняшнего дня")
    public CalTouchGridSteps shouldBeOnTodayGrid() {
        user.defaultSteps()
            .shouldBeOnUrl(both(containsString("day?")).and(not(containsString("show_date="))));
        return this;
    }

    @Step("Проверяем, что находимся на сетке дня, который будет через {0} дней")
    public CalTouchGridSteps shouldBeOnFutureDayGrid(int daysAfter) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDateTime date = LocalDateTime.now();
        user.defaultSteps()
            .shouldBeOnUrl(containsString("?show_date=" + dateFormat.format(date.plusDays(daysAfter))));
        return this;
    }

    @Step("Проверяем, что находимся на сетке дня, который был {0} дней назад")
    public CalTouchGridSteps shouldBeOnPastDayGrid(int daysBefore) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDateTime date = LocalDateTime.now();
        user.defaultSteps()
            .shouldBeOnUrl(containsString("?show_date=" + dateFormat.format(date.minusDays(daysBefore))));
        return this;
    }
}
