package ru.yandex.market.tsum.pipe.ui.common.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.List;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 02/07/2019
 */
public class ListExactSizeMatcher extends BaseMatcher<List> {
    private final int expectedCount;

    public ListExactSizeMatcher(int expectedCount) {
        this.expectedCount = expectedCount;
    }


    @Override
    public void describeTo(Description description) {

    }

    @Override
    public boolean matches(Object item) {
        return ((List) item).size() == expectedCount;
    }

    @Override
    public void describeMismatch(Object item, Description mismatchDescription) {
        mismatchDescription.appendText(
            String.format("Expected %d but was %d elements", expectedCount, ((List) item).size())
        );
    }
}
