package ru.yandex.market.checkout.util.matching;

import java.util.Date;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Сравнивает две даты с точностью до одной секунды.
 *
 * @author Nicolai Iusiumbeli <armor@yandex-team.ru>
 * date: 27/01/2017
 */
public class DateSecPrecisionMatcher extends BaseMatcher<Date> {

    private final Date expectedValue;

    public DateSecPrecisionMatcher(Date expectedValue) {
        this.expectedValue = expectedValue;
    }

    private static boolean areEqual(Date actual, Date expected) {
        return actual.getTime() / 1000 == expected.getTime() / 1000;
    }

    @Override
    public boolean matches(Object actual) {
        if (actual == null) {
            return expectedValue == null;
        }

        return actual instanceof Date && areEqual((Date) actual, expectedValue);
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expectedValue);
    }
}
