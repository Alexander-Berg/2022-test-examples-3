package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessagePage;

import static org.hamcrest.Matchers.hasSize;

public class MessagePageCountMatcher extends TypeSafeMatcher<MessagePage> {
    private Matcher<Integer> matcher;

    public boolean matchesSafely(MessagePage messagePage) {
        return hasSize(matcher).matches(messagePage.allDisplayedMessagesBlocks());
    }

    public MessagePageCountMatcher(Matcher<Integer> matcher) {
        this.matcher = matcher;
    }

    @Factory
    public static MessagePageCountMatcher messagePagesCount(Matcher<Integer> matcher) {
        return new MessagePageCountMatcher(matcher);
    }

    @Override
    public void describeMismatchSafely(MessagePage messagePage, Description description) {
        description.appendText("Количество загруженных страниц с письмами: ")
                .appendValue(messagePage.allDisplayedMessagesBlocks().size());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Количество загруженных страниц с письмами должно быть ").appendDescriptionOf(matcher);
    }
}
