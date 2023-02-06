package ru.yandex.autotests.innerpochta.imap.matchers;

import java.util.List;

import com.google.common.base.Splitter;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;


public class LinesMatcher extends TypeSafeMatcher<String> {

    private String expected;
    private List<String> actualList;

    public LinesMatcher(String expected) {
        this.expected = expected;
    }

    @Factory
    public static LinesMatcher hasSameLine(String expected) {
        return new LinesMatcher(expected);
    }

    @Override
    protected boolean matchesSafely(String actual) {
        actualList = Splitter.on(" ").splitToList(actual);
        return hasSameItemsAsList(Splitter.on(" ").splitToList(expected)).matches(actualList);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("в списке заголовков от сервера ")
                .appendText("должны увидеть \n" + expected);
    }
}
