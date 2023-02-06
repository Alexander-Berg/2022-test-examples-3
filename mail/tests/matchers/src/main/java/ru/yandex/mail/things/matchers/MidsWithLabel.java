package ru.yandex.mail.things.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.CombinableMatcher;
import org.hamcrest.core.IsAnything;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;
import ru.yandex.mail.tests.hound.MessagesByLabel;
import ru.yandex.mail.tests.hound.MessagesUnread;

import java.util.List;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;


public class MidsWithLabel extends TypeSafeMatcher<UserCredentials> {
    private final String lid;
    private final String mid;
    private final String fid;

    public MidsWithLabel(String mid, String lid) {
        this.lid = lid;
        this.mid = mid;
        this.fid = null;
    }

    public MidsWithLabel(String mid, String fid, String lid) {
        this.lid = lid;
        this.mid = mid;
        this.fid = fid;
    }

    private MessagesUnread unread(UserCredentials rule) {
        return MessagesUnread.messagesUnreadByFolder(HoundApi.apiHound(
                HoundProperties.properties()
                        .houndUri(),
                props().getCurrentRequestId()
                )
                        .messagesUnread()
                        .withUid(rule.account().uid())
                        .withFirst("0")
                        .withCount("30")
                        .post(shouldBe(HoundResponses.ok200()))
        );
    }

    private MessagesUnread unreadByFolder(UserCredentials rule) {
        return MessagesUnread.messagesUnreadByFolder(HoundApi.apiHound(
                HoundProperties.properties()
                        .houndUri(),
                props().getCurrentRequestId()
                )
                        .messagesUnreadByFolder()
                        .withUid(rule.account().uid())
                        .withFid(fid)
                        .withFirst("0")
                        .withCount("30")
                        .post(shouldBe(HoundResponses.ok200()))
        );
    }

    private MessagesByLabel byLabel(UserCredentials rule) {
        return MessagesByLabel.messagesByLabel(
                HoundApi.apiHound(
                        HoundProperties.properties()
                                .houndUri(),
                        props().getCurrentRequestId()
                )
                        .messagesByLabel()
                        .withUid(rule.account().uid())
                        .withFirst("0")
                        .withCount("30")
                        .withLid(lid)
                        .post(shouldBe(HoundResponses.ok200()))
        );
    }

    @Override
    protected boolean matchesSafely(UserCredentials rule) {
        if (lid.equals("FAKE_SEEN_LBL"))  {
            return fid == null ? !unread(rule).hasMessage(mid)
                    : !unreadByFolder(rule).hasMessage(mid, fid);
        } else {
            return fid == null ? byLabel(rule).hasMessageWithLid(mid)
                    : byLabel(rule).hasMessageWithLidInFolder(mid, fid);
        }
    }

    @Override
    public void describeTo(Description description) {
        description
                .appendText("должны быть метки: ").appendText(lid)
                .appendText(" на письме с mid - ").appendValue(mid);
    }

    @Factory
    public static MidsWithLabel hasMsgWithLid(String mid, String lid) {
        return new MidsWithLabel(mid, lid);

    }

    @Factory
    public static MidsWithLabel hasMsgWithLidInFolder(String mid, String fid, String lid) {
        return new MidsWithLabel(mid, fid, lid);
    }

    @Factory
    public static Matcher<UserCredentials> hasMsgsWithLidInFolder(List<String> mids, String fid, String lid) {
        CombinableMatcher<UserCredentials> both =
                new CombinableMatcher<>(
                        new IsAnything<>("каждое из писем с меткой")
                );
        for (String mid : mids) {
            both = both.and(hasMsgWithLidInFolder(mid, fid, lid));
        }
        return both;

    }

    @Factory
    public static Matcher<UserCredentials> hasMsgsWithLid(List<String> mids, String lid) {
        CombinableMatcher<UserCredentials> both =
                new CombinableMatcher<>(
                        new IsAnything<>("каждое из писем с меткой")
                );
        for (String mid : mids) {
            both = both.and(hasMsgWithLid(mid, lid));
        }
        return both;
    }

    @Factory
    public static Matcher<UserCredentials> hasMsgsWithLids(List<String> mids, List<String> lids) {
        CombinableMatcher<UserCredentials> both =
                new CombinableMatcher<>(
                        new IsAnything<>("каждое из писем с меткой")
                );
        for (String mid : mids) {
            for (String lid : lids) {
                both = both.and(hasMsgWithLid(mid, lid));
            }
        }
        return both;
    }
}

