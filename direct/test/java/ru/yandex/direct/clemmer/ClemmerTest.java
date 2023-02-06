package ru.yandex.direct.clemmer;

import java.util.List;
import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class ClemmerTest {
    private static TypeSafeMatcher<ClemmerWord> wordWithFlags(String text, int flags) {
        return new TypeSafeMatcher<ClemmerWord>(ClemmerWord.class) {
            @Override
            protected boolean matchesSafely(ClemmerWord item) {
                return Objects.equals(item.getText(), text) && item.getFlags() == flags;
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(text).appendText(" with flags=").appendValue(flags);
            }

            @Override
            protected void describeMismatchSafely(ClemmerWord item, Description description) {
                description.appendText("was ").appendValue(item.getText()).appendText(" with flags=")
                        .appendValue(item.getFlags());
            }
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    void basicUsage() {
        List<ClemmerWord> words = Clemmer.analyze2("всякая !фигня -слово -!всякое");
        assertThat(words, contains(
                wordWithFlags("всякая", 0),
                wordWithFlags("фигня", ClemmerFlag.EXACT),
                wordWithFlags("слово", ClemmerFlag.MINUS),
                wordWithFlags("всякое", ClemmerFlag.MINUS | ClemmerFlag.EXACT)
        ));
    }
}
