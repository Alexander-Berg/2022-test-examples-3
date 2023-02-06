package ru.yandex.market.checkout.util.matching;

import java.util.Date;
import java.util.regex.Pattern;

import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author Nicolai Iusiumbeli <armor@yandex-team.ru>
 * date: 27/01/2017
 */
public final class Matchers {

    private Matchers() {
    }

    public static Matcher<String> asNumber(Matcher<Number> numberMatcher) {
        return new AsNumberMatcher(numberMatcher);
    }

    public static Matcher<Date> eqSecondPrecision(Date expected) {
        return new DateSecPrecisionMatcher(expected);
    }

    public static Matcher<String> matchesPattern(Pattern pattern) {
        return new MatchesPattern(pattern);
    }

    public static Matcher<String> matchesPattern(String regex) {
        return new MatchesPattern(Pattern.compile(regex));
    }

    public static Matcher<Integer> zero() {
        return equalTo(0);
    }
}
