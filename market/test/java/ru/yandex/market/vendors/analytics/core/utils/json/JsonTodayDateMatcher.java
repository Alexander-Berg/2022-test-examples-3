package ru.yandex.market.vendors.analytics.core.utils.json;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import net.javacrumbs.jsonunit.core.ParametrizedMatcher;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.market.vendors.analytics.core.utils.DateTimeFormatterFactory;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * <pre>
 * Сравнивает пришедшую дату с (текущей датой + заданный период).
 * Период задается как число и тип периода. Например, "1d" - один день вперёд, "-2m" - два месяца назад.
 *
 * Поддерживаемые типы периодов:
 * <li>
 *     <ol>d - день</ol>
 *     <ol>w - неделя</ol>
 *     <ol>m - месяц</ol>
 *     <ol>q - 3 месяца</ol>
 *     <ol>y - год</ol>
 * </li>
 * </pre>
 *
 * @author antipov93.
 */
public class JsonTodayDateMatcher extends TypeSafeMatcher<String> implements ParametrizedMatcher {

    private static final DateTimeFormatter DTF = DateTimeFormatterFactory.getInstance();

    private Period period;

    @Override
    protected boolean matchesSafely(String item) {
        var expected = LocalDate.now().plus(period);
        var actual = LocalDate.parse(item, DTF);
        return expected.equals(actual);
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(period);
    }

    @Override
    protected void describeMismatchSafely(String item, Description mismatchDescription) {
        mismatchDescription.appendText("It's not today plus ").appendValue(period);
    }

    @Override
    public void setParameter(String parameter) {
        this.period = convertPeriod(parameter);
    }

    private static Period convertPeriod(String parameter) {
        if (StringUtils.isBlank(parameter)) {
            return Period.ZERO;
        }
        try {
            String periodType = parameter.substring(parameter.length() - 1).toLowerCase();
            int duration = Integer.parseInt(parameter.substring(0, parameter.length() - 1));
            switch (periodType) {
                case "d":
                    return Period.ofDays(duration);
                case "m":
                    return Period.ofMonths(duration);
                case "y":
                    return Period.ofYears(duration);
                case "q":
                    return Period.ofMonths(3 * duration);
                case "w":
                    return Period.ofDays(7 * duration);
                case "r":
                    return Period.ofDays((int) LocalDate.now().until(
                            LocalDate.now().plusMonths(duration).withDayOfMonth(1), DAYS)
                    );
                default:
                    throw new RuntimeException("Unknown period type " + periodType);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse period from '" + parameter + "'", e);
        }
    }
}
