package ru.yandex.market.tsum.pipe.ui.common.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.List;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 02/07/2019
 */
public class ListSizeMoreThanMatcher extends BaseMatcher<List> {
    private final int atLeast;

    public ListSizeMoreThanMatcher(int atLeast) {
        this.atLeast = atLeast;
    }


    @Override
    public void describeTo(Description description) {

    }

    @Override
    public boolean matches(Object item) {
        return ((List) item).size() >= atLeast;
    }

    @Override
    public void describeMismatch(Object item, Description mismatchDescription) {
        mismatchDescription.appendText(
            String.format("Expected at least %d but was %d elements", atLeast, ((List) item).size())
        );
    }
}
