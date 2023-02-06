package ru.yandex.mail.things.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.hound.Folders;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.hound.MessagesByFolder;
import ru.yandex.mail.tests.hound.MessagesUnread;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.tests.hound.MessagesByFolder.messagesByFolder;

public abstract class IsThereMessagesMatcher extends TypeSafeMatcher<UserCredentials> {
    @Factory
    public static IsThereMessagesMatcher hasMsgIn(String subj, String fid) {
        return new IsThereMessagesWithSubjectMatcher(subj, equalTo(1), fid);
    }

    @Factory
    public static IsThereMessagesMatcher hasMsgsIn(Integer count, String fid) {
        return new IsThereMessagesInFolderMatcher(equalTo(count), fid);
    }

    @Factory
    public static IsThereMessagesMatcher hasMsgsIn(String subj, Integer count, String fid) {
        return new IsThereMessagesWithSubjectMatcher(subj, equalTo(count), fid);
    }

    @Factory
    public static IsThereMessagesMatcher hasMsgsStrictSubjectIn(String subj, Integer count, String fid) {
        return new IsThereMessagesWithSubjectStrictMatcher(subj, equalTo(count), fid);
    }

    @Factory
    public static TypeSafeMatcher<UserCredentials> hasNewMsgsInFid(final Matcher<Integer> count, final String fid) {
        return new TypeSafeMatcher<UserCredentials>() {
            int newMsgs = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("Обнаружено новых сообщений: ").appendValue(newMsgs)
                        .appendText(" в папке ")
                        .appendValue(fid);
            }

            @Override
            protected boolean matchesSafely(UserCredentials rule) {
                newMsgs = MessagesUnread.messagesUnreadByFolder(HoundApi.apiHound(
                        HoundProperties.properties()
                                .houndUri(),
                        props().getCurrentRequestId()
                        )
                        .messagesUnreadByFolder()
                        .withUid(rule.account().uid())
                        .withFid(fid)
                        .withFirst("0")
                        .withCount("100")
                        .post(shouldBe(HoundResponses.ok200()))
                ).messageCount();

                return count.matches(newMsgs);
            }
        };
    }

    @Factory
    public static TypeSafeMatcher<UserCredentials> hasCountMsgsInFid(final Matcher<Integer> matcher, final String fid) {
        return new TypeSafeMatcher<UserCredentials>() {
            int count = 0;

            @Override
            protected boolean matchesSafely(UserCredentials rule) {
                count = Folders.folders(
                        HoundApi.apiHound(
                                HoundProperties.properties()
                                        .houndUri(),
                                props().getCurrentRequestId()
                        )
                                .folders()
                                .withUid(rule.account().uid())
                                .post(shouldBe(HoundResponses.ok200()))
                )
                        .count(fid);

                return matcher.matches(count);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Обнаружено: ")
                        .appendValue(count)
                        .appendText(" в папке ")
                        .appendValue(fid);
            }
        };
    }
}

class IsThereMessagesInFolderMatcher extends IsThereMessagesMatcher {
    protected String fid;
    protected Matcher<Integer> expectedCount;

    IsThereMessagesInFolderMatcher(Matcher<Integer> count, String fid_) {
        assertThat(fid_, is(notNullValue()));
        assertThat(count, is(notNullValue()));

        fid = fid_;
        expectedCount = count;
    }

    @Override
    protected boolean matchesSafely(UserCredentials rule) {
        Integer count = Folders.folders(
                HoundApi.apiHound(
                        HoundProperties.properties()
                                .houndUri(),
                        props().getCurrentRequestId()
                )
                        .folders()
                        .withUid(rule.account().uid())
                        .post(shouldBe(HoundResponses.ok200()))
        )
                .count(fid);

        return expectedCount.matches(count);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Ожидалось сообщений - ")
                .appendDescriptionOf(expectedCount)
                .appendText(" в папке ")
                .appendText(fid);
    }
}

class IsThereMessagesWithSubjectMatcher extends IsThereMessagesInFolderMatcher {
    protected String subject;

    IsThereMessagesWithSubjectMatcher(String subj, Matcher<Integer> count, String fid) {
        super(count, fid);
        assertThat(subj, is(notNullValue()));
        subject = subj;
    }

    protected MessagesByFolder getMessagesByFolder(UserCredentials rule) {
        return messagesByFolder(
                HoundApi.apiHound(HoundProperties.properties().houndUri(), props().getCurrentRequestId())
                        .messagesByFolder()
                        .withUid(rule.account().uid())
                        .withFid(fid)
                        .withFirst("0")
                        .withCount("100")
                        .post(shouldBe(HoundResponses.ok200()))
        );
    }

    @Override
    protected boolean matchesSafely(UserCredentials rule) {
        Integer count = getMessagesByFolder(rule)
                .countIf(envelope -> envelope.getSubjectInfo().getSubject().equals(subject));

        return expectedCount.matches(count);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Ожидалось сообщений - ")
                .appendDescriptionOf(expectedCount)
                .appendText(" с темой ")
                .appendValue(subject.isEmpty() ? "\"\"" : subject)
                .appendText(" в папке ")
                .appendText(fid);
    }
}

class IsThereMessagesWithSubjectStrictMatcher extends IsThereMessagesWithSubjectMatcher {
    @Override
    protected boolean matchesSafely(UserCredentials rule) {
        Integer count = getMessagesByFolder(rule)
                .countIf(envelope -> envelope.getSubject().equals(subject));

        return expectedCount.matches(count);
    }

    IsThereMessagesWithSubjectStrictMatcher(String subj, Matcher<Integer> count, String fid) {
        super(subj, count, fid);
    }
}