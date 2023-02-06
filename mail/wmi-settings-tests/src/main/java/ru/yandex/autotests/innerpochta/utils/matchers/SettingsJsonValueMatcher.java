package ru.yandex.autotests.innerpochta.utils.matchers;

import org.apache.http.impl.client.DefaultHttpClient;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;

import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.innerpochta.utils.SettingsUtils.getSettingValue;

public class SettingsJsonValueMatcher extends TypeSafeMatcher<DefaultHttpClient> {

    private Oper<?> oper;
    private String optionName;
    private Matcher<String> expectedMatcher;
    private String actualValue;

    public SettingsJsonValueMatcher(String optionName, Matcher<String> expectedValue) {
        this.optionName = optionName;
        this.expectedMatcher = expectedValue;
    }

    @Override
    protected boolean matchesSafely(DefaultHttpClient hc) {
        actualValue = getSettingValue(oper.get().via(hc).toString(), optionName);
        return expectedMatcher.matches(actualValue);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Ожидалось значение ").appendValue(optionName).appendText(" ")
                .appendDescriptionOf(expectedMatcher);
    }

    @Override
    protected void describeMismatchSafely(DefaultHttpClient item, Description mismatchDescription) {
        expectedMatcher.describeMismatch(actualValue, mismatchDescription);

    }

    public SettingsJsonValueMatcher with(Oper<?> oper) {
        this.oper = oper;
        return this;
    }

    @Factory
    public static SettingsJsonValueMatcher hasSetting(String name, String value) {
        return hasSetting(name, is((value.equals("off") ? "" : value)));
    }

    @Factory
    public static SettingsJsonValueMatcher hasSetting(String optionName, Matcher<String> expectedValue) {
        return new SettingsJsonValueMatcher(optionName, expectedValue);
    }
}
