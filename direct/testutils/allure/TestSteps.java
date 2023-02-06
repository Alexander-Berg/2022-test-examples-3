package ru.yandex.autotests.irt.testutils.allure;

import org.hamcrest.Matcher;
import org.junit.Assert;

public class TestSteps {
    private TestSteps() {
    }

    public static <T> void assertThat(String stepMessage, T actualResult, Matcher<T> matcher) {
        Assert.assertThat("Проверка не пройдена: " + stepMessage, actualResult, matcher);
    }

    public static <T> void assumeThat(String assumeMessage, T actualResult, Matcher<? super T> matcher) {
        try {
            Assert.assertThat("Предполагалось, что " + assumeMessage, actualResult, matcher);
        } catch (AssertionError e) {
            throw new AssumptionException(e.getMessage(), e);
        }
    }
}
