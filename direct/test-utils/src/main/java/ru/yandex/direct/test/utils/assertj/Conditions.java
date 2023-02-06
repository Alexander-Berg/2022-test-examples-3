package ru.yandex.direct.test.utils.assertj;

import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.hamcrest.Matcher;

/**
 * Позволяет использовать матчеры в asserj. <br/>
 * Подходит для всех методов {@link org.assertj.core.api.AbstractAssert}, которые принимают {@link Condition}
 * <pre>
 * Assertions.assertThat("asdf").is(Conditions.matchedBy(equalTo("asdf")));
 */
public class Conditions {
    /*
    Хорошо было бы принимать Matcher<T>, но это не работает в Java9/10, похоже на этот баг
    https://bugs.java.com/view_bug.do?bug_id=8155072
     */
    public static <T> Condition<T> matchedBy(final Matcher<?> matcher) {
        return new HamcrestCondition(matcher);
    }
}
