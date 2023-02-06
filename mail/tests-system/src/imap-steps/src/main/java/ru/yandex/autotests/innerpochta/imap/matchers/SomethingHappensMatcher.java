package ru.yandex.autotests.innerpochta.imap.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;

import static ru.yandex.autotests.innerpochta.imap.requests.NoOpRequest.noOp;

public class SomethingHappensMatcher extends TypeSafeMatcher<ImapClient> {

    @Factory
    public static Matcher<ImapClient> somethingHappens() {
        return new SomethingHappensMatcher();
    }

    @Override
    public boolean matchesSafely(ImapClient imap) {
        return imap.request(noOp()).numberOfLines() > 1;

    }

    @Override
    public void describeTo(Description description) {
        description.appendText("new message is received");
    }

}
