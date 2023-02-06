package ru.yandex.autotests.innerpochta.matchers;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

/**
 * User: alex89
 * Date: 10.10.12
 */
public class GreetingMessageIconsMatcher extends TypeSafeMatcher<MailElement> {
    private String param;

    public boolean matchesSafely(MailElement icon) {
        return icon.getAttribute("class").contains(param);

    }

    public GreetingMessageIconsMatcher(String param) {
        this.param = param;
    }

    @Factory
    public static GreetingMessageIconsMatcher containsIcon(String param) {
        return new GreetingMessageIconsMatcher(param);
    }

    @Override
    public void describeMismatchSafely(MailElement o, Description description) {
        description.appendText("Иконка '").appendText(o.getAttribute("class"));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Иконка ").appendText(param);
    }

}
