package ru.yandex.autotests.market.stat.beans.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.market.stat.beans.meta.TmsRunState;
import ru.yandex.autotests.market.stat.beans.meta.TmsStatus;

/**
 * Created by jkt on 01.07.14.
 */
public class JobHasStatusAsMatcher extends TypeSafeMatcher<TmsRunState> {

    private TmsStatus expectedStatus;

    public static JobHasStatusAsMatcher hasStatus(TmsStatus expectedStatus) {
        return new JobHasStatusAsMatcher(expectedStatus);
    }

    private JobHasStatusAsMatcher(TmsStatus expectedStatus) {
        super();
        this.expectedStatus = expectedStatus;
    }

    @Override
    protected boolean matchesSafely(TmsRunState TmsRunState) {
        if (TmsRunState == null) {
            throw new IllegalArgumentException("TmsRunState bean can not be null");
        }
        return expectedStatus.matches(TmsRunState.getJobStatus());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("has status as " + expectedStatus.mask());
    }
}