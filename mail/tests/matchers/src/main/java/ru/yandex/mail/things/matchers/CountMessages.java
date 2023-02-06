package ru.yandex.mail.things.matchers;

import org.apache.log4j.LogManager;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.hound.HoundApi;
import ru.yandex.mail.tests.hound.HoundProperties;
import ru.yandex.mail.tests.hound.HoundResponses;

import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.tests.hound.Folders.folders;


public class CountMessages extends TypeSafeMatcher<UserCredentials> {
    private Matcher<Integer> expected;
    private String fid;

    public CountMessages(Matcher<Integer> expected, String fid) {
        this.expected = expected;
        this.fid = fid;
    }

    @Override
    public void describeTo(Description description) {
        description
                .appendText("Ожидалось сообщений - ").appendDescriptionOf(expected)
                .appendText(" в папке: ").appendText(fid);
    }

    @Override
    protected boolean matchesSafely(UserCredentials rule) {
        Integer count = folders(
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


        String msg = new StringBuilder()
                .append("Сообщений ")
                .append(expected)
                .append(" / ")
                .append(count)
                .append(" в папке - '")
                .append(fid)
                .append("'")
                .toString();

        LogManager.getLogger("WAIT4DELIVER").info(msg);


        return expected.matches(count);
    }

    @Factory
    public static CountMessages hasCountMsgsIn(Matcher<Integer> count, String fid) {
        return new CountMessages(count, fid);
    }
}

