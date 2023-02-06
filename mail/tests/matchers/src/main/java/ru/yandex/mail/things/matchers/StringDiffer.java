package ru.yandex.mail.things.matchers;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.accordeon;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.html;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.iframe;
import static ru.yandex.autotests.plugins.testpers.html.common.HtmlUtils.link;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 19.05.13
 * Time: 12:37
 */
public class StringDiffer extends TypeSafeMatcher<String> {
    private String expected;
    private List<String> excludePatterns = new ArrayList<>();

    public StringDiffer(String expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(String actual) {
        for (String pattern : excludePatterns) {
            expected = expected.replaceAll(pattern, "");
            actual = actual.replaceAll(pattern, "");
        }
        return expected.equals(actual);
    }

    @Override
    protected void describeMismatchSafely(String actual, Description mismatchDescription) {
        for (String pattern : excludePatterns) {
            expected = expected.replaceAll(pattern, "");
            actual = actual.replaceAll(pattern, "");
        }

        String diff = StringUtils.difference(expected, actual);

        mismatchDescription.appendText("Строки различаются - ");
        String url = elliptics().path(this.getClass()).randomize().name("prettydiff.htm")
                .put(diff)
                .get().url();
        mismatchDescription.appendText(link(url, "DIFF")).appendText("\n");
        mismatchDescription.appendText(html(accordeon("Actual", actual)));
        mismatchDescription.appendText(html(accordeon("DIFF", iframe(url))));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(html(accordeon("Expected", expected)));
    }

    public static StringDiffer notDiffersWith(String expected) {
        return new StringDiffer(expected);
    }

    public StringDiffer exclude(String pattern) {
        excludePatterns.add(pattern);
        return this;
    }
}
