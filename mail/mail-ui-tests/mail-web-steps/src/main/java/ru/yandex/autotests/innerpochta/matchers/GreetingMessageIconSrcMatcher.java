package ru.yandex.autotests.innerpochta.matchers;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.10.12
 * Time: 19:19
 */
public class GreetingMessageIconSrcMatcher extends TypeSafeMatcher<MailElement> {
    private String param;

    public boolean matchesSafely(MailElement icon) {
        return icon.getAttribute("src").matches(param);

    }

    public GreetingMessageIconSrcMatcher(String param) {
        this.param = param;
    }

    @Factory
    public static GreetingMessageIconSrcMatcher containsSrc(String param) {
        return new GreetingMessageIconSrcMatcher(param);
    }

    @Override
    public void describeMismatchSafely(MailElement o, Description description) {
        description.appendText("Адрес иконки: '").appendText(o.getAttribute("src"))
                .appendText("' не соответствует паттерну");
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Иконка имеет адрес соответствующий паттерну: ").appendText(param);
    }
}
