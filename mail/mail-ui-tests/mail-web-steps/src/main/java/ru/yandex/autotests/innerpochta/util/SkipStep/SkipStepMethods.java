package ru.yandex.autotests.innerpochta.util.SkipStep;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

public final class SkipStepMethods {

    public SkipStepMethods() {
    }

    public static <T> void assumeStepCanContinue(T actual, Matcher<? super T> matcher) {
        assumeStepCanContinue("", actual, matcher);
    }

    public static <T> void assumeStepCanContinue(String reason, T actual, Matcher<? super T> matcher) {
        if (!matcher.matches(actual)) {
            Description description = new StringDescription();
            description.appendText(reason).appendText("\nExpected: ")
                .appendDescriptionOf(matcher).appendText("\n     but: ");
            matcher.describeMismatch(actual, description);
            throw new SkipStepException(description.toString());
        }
    }
}
