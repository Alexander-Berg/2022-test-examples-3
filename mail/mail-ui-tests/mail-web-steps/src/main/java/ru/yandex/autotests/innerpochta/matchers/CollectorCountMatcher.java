package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.10.12
 * Time: 19:19
 */
public class CollectorCountMatcher extends TypeSafeMatcher<WebDriverRule> {
    private Matcher<Integer> matcher;

    public boolean matchesSafely(WebDriverRule webDriverRule) {
        try {
            return matcher.matches(new GetPagesSteps(webDriverRule).CollectorSettingsPage().blockMain()
                    .blockConnected().collectors().size());
        } catch (StaleElementReferenceException e) {
            return false;
        }

    }

    public CollectorCountMatcher(Matcher<Integer> matcher) {
        this.matcher = matcher;
    }

    @Factory
    public static CollectorCountMatcher collectorCount(Matcher<Integer> matcher) {
        return new CollectorCountMatcher(matcher);
    }

    @Override
    public void describeMismatchSafely(WebDriverRule webDriverRule, Description description) {
        matcher.describeMismatch(new GetPagesSteps(webDriverRule).CollectorSettingsPage().blockMain()
                .blockConnected().collectors().size(), description);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Количество созданных сборщиков должно быть ").appendDescriptionOf(matcher);
    }
}
