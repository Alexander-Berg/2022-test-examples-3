package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessagePage;
import ru.yandex.autotests.innerpochta.util.Utils;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.10.12
 * Time: 19:19
 */
public class MessageIsRepliedMatcher extends TypeSafeMatcher<MessagePage> {

    private int messageIndex;

    public boolean matchesSafely(MessagePage messagePage) {
        return Utils.isPresent().matches(messagePage.displayedMessages().list()
                .get(messageIndex).repliedArrow());
    }

    public MessageIsRepliedMatcher(int messageIndex) {
        this.messageIndex = messageIndex;
    }

    @Factory
    public static MessageIsRepliedMatcher messageIsReplied(int messageIndex) {
        return new MessageIsRepliedMatcher(messageIndex);
    }

    @Override
    public void describeMismatchSafely(MessagePage messagePage, Description description) {
        description.appendText("У письма не появился значок ответа");
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("У письма есть значок ответа");
    }
}
