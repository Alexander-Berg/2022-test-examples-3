package ru.yandex.autotests.testpers.mail.mon.misc;

import com.jayway.restassured.specification.RequestSpecification;
import org.hamcrest.CustomTypeSafeMatcher;
import ru.yandex.autotests.testpers.mail.mon.beans.Message;

import java.util.List;

import static ch.lambdaj.collection.LambdaCollections.with;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.testpers.mail.mon.beans.MessageMatchers.withSubject;
import static ru.yandex.autotests.testpers.mail.mon.misc.oper.Commands.messages;

/**
 * User: lanwen
 * Date: 04.02.15
 * Time: 13:57
 */
public class MessagesMatcher extends CustomTypeSafeMatcher<String> {

    private RequestSpecification request;

    private MessagesMatcher(RequestSpecification auth) {
        super("has subject in list");
        this.request = auth;
    }

    private List<Message> msgs;

    public static MessagesMatcher subjectExistsInMessagesWith(RequestSpecification auth) {
        return new MessagesMatcher(auth);
    }

    @Override
    protected boolean matchesSafely(String subj) {
        msgs = messages(request).getMessage();
        return with(msgs).exists(withSubject(equalTo(subj)));
    }

    @Override
    protected void describeMismatchSafely(String item, org.hamcrest.Description mismatchDescription) {
        mismatchDescription.appendValue(item).appendText(" not found in ")
                .appendValue(msgs.size())
                .appendText(" other (last was ")
                .appendText(msgs.isEmpty()
                        ? "never"
                        : formatDurationWords(System.currentTimeMillis() - ts(msgs.get(0)), true, true))
                .appendText(" ago!)");
    }

    private Long ts(Message msg) {
        return Long.parseLong(msg.getDate().getTimestamp());
    }
}


