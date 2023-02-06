package ru.yandex.autotests.innerpochta.matchers.message;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;

/**
 * User: lanwen
 * Date: 04.10.13
 * Time: 21:38
 */
public class MessageFolderMatcher extends FeatureMatcher<MessageBlock, MailElement> {

    public MessageFolderMatcher(Matcher<? super MailElement> subMatcher) {
        super(subMatcher, "message folder expected", "message folder actual");
    }

    @Override
    protected MailElement featureValueOf(MessageBlock messageBlock) {
        return messageBlock.folder();
    }

    public static MessageFolderMatcher messageFolder(Matcher<? super MailElement> subMatcher) {
        return new MessageFolderMatcher(subMatcher);
    }
}
