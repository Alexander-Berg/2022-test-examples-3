package ru.yandex.market.checkout.util.matching;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static ru.yandex.common.util.date.DateUtil.asInstant;

/**
 * @author mkasumov
 */
public class DateFormatMatcher extends CustomTypeSafeMatcher<String> {

    private final DateFormat dateFormat;
    private final Matcher<Instant> delegateMatcher;

    public DateFormatMatcher(String datePattern, Matcher<Instant> delegateMatcher) {
        super("a date in format " + datePattern);
        dateFormat = new SimpleDateFormat(datePattern);
        this.delegateMatcher = delegateMatcher;
    }

    @Override
    protected void describeMismatchSafely(String item, Description mismatchDescription) {
        try {
            delegateMatcher.describeMismatch(parseInstant(item), mismatchDescription);
        } catch (ParseException e) {
            mismatchDescription.appendText("invalid date format: ").appendValue(item);
        }
    }

    @Override
    protected boolean matchesSafely(String item) {
        try {
            return delegateMatcher.matches(parseInstant(item));
        } catch (ParseException e) {
            return false;
        }
    }

    private Instant parseInstant(String item) throws ParseException {
        return asInstant(dateFormat.parse(item));
    }
}
