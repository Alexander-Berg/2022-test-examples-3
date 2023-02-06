package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessagePage;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.DisplayedMessagesBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;

import java.util.ArrayList;
import java.util.List;

import static org.cthul.matchers.CthulMatchers.and;
import static org.cthul.matchers.chain.NOrChainMatcher.nor;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;

public class MessageExistsMatcher extends TypeSafeMatcher<MessagePage> {
    private String subject;
    private boolean hasPrefix;

    public boolean matchesSafely(MessagePage messagePage) {
        try {
            for (DisplayedMessagesBlock page : messagePage.allDisplayedMessagesBlocks()) {
                for (MessageBlock message : page.list()) {
                    if (exists().matches(message) && exists().matches(message.subject())
                            && message.subject().getText().contains(subject)) {
                        return !hasPrefix || exists().matches(message.prefix());
                    }
                }
            }
        } catch (StaleElementReferenceException e) {
            return false;
        }
        return false;
    }

    public MessageExistsMatcher(String subject, Boolean hasPrefix) {
        this.hasPrefix = hasPrefix;
        this.subject = subject;
    }

    @Factory
    public static Matcher<MessagePage> messageWithSubjectExists(String... subjects) {
        List<Matcher<MessagePage>> matchers = new ArrayList<>();
        for (String subj : subjects) {
            matchers.add(new MessageExistsMatcher(subj, false));
        }
        return and(matchers);
    }


    @Factory
    public static Matcher<MessagePage> msgNotExists(String... subjects) {
        List<Matcher<MessagePage>> matchers = new ArrayList<>();
        for (String subj : subjects) {
            matchers.add(new MessageExistsMatcher(subj, false));
        }
        return nor(matchers);
    }


    @Factory
    public static Matcher<MessagePage> messageWithSubjectAndPrefixExists(String... subjects) {
        List<Matcher<MessagePage>> matchers = new ArrayList<>();
        for (String subj : subjects) {
            matchers.add(new MessageExistsMatcher(subj, true));
        }
        return and(matchers);
    }

    @Override
    public void describeMismatchSafely(MessagePage messagePage, Description description) {
        description.appendText("не обнаружено письмо: ").appendValue(subject);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("существует письмо: ").appendValue(subject);
    }
}
